package com.manerfan.booster.core.exception.aspect;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.validation.ValidationException;

import com.alibaba.fastjson.JSON;

import com.google.common.collect.Maps;
import com.manerfan.booster.api.common.dto.response.Response;
import com.manerfan.booster.core.exception.BizException;
import com.manerfan.booster.core.exception.ErrorCodes;
import com.manerfan.booster.core.exception.annotation.ExCacheAndLog;
import com.manerfan.booster.core.exception.annotation.ExCacheAndLog.LogLevel;
import com.manerfan.booster.core.exception.annotation.RequestId;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.util.StopWatch;

/**
 * ExCatchAndLogAdvice
 *
 * <pre>
 *      ExCacheAndLog拦截实现
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
@Slf4j(topic = "EX_CATCH_LOG")
public class ExCatchAndLogAdvice implements MethodInterceptor, Ordered {
    private static final String LOG_FORMAT = "Service Method Invoke Error! [tracing: {}] [from: {}] {}\n{}";

    /**
     * 类上ExCacheAndLog注解元数据的缓存
     */
    private final Map<Class<?>, ExCatchLogMeta> logMetas = Maps.newHashMap();

    protected final BaseServiceContext serviceContext;

    public ExCatchAndLogAdvice() {
        this(null);
    }

    public ExCatchAndLogAdvice(BaseServiceContext serviceContext) {
        this.serviceContext = Objects.requireNonNullElse(serviceContext, new BaseServiceContext() {});
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if (!(invocation instanceof ProxyMethodInvocation)) {
            return invocation.proceed();
        }

        ProxyMethodInvocation pmi = (ProxyMethodInvocation)invocation;
        Method method = pmi.getMethod();
        Class<?> targetClass = pmi.getThis().getClass();
        Method targetMethod = targetClass.getMethod(method.getName(), method.getParameterTypes());

        // 找到被 @RequestId 修饰的参数值
        val arguments = pmi.getArguments();
        val params = targetMethod.getParameters();
        // FIXME 需要缓存，加速接口
        val requestedParamIndex = IntStream.range(0, Math.min(arguments.length, params.length))
            .filter(idx -> Objects.nonNull(AnnotationUtils.findAnnotation(params[idx], RequestId.class)))
            .findFirst().orElse(-1);
        val traceId = serviceContext.getTraceId();
        val requestId = requestedParamIndex < 0 ? traceId : Objects.toString(arguments[requestedParamIndex], traceId);
        val trackingId = Stream.of(traceId, requestId)
            .filter(StringUtils::isNotBlank).distinct().collect(Collectors.joining(":"));
        val client = serviceContext.getClientName();

        StopWatch stopWatch = new StopWatch();
        try {
            stopWatch.start();
            Object respValue = invocation.proceed();
            fillStatMonitor(targetClass, targetMethod, respValue, trackingId, requestId, stopWatch, null);
            return respValue;
        } catch (Throwable ex) {
            // 返回值类型
            val returnType = targetMethod.getReturnType();

            // 处理validation异常
            if (ex instanceof ValidationException) {
                String errorMsg = ex.getMessage();
                if (ex instanceof ConstraintViolationException) {
                    errorMsg = ((ConstraintViolationException)ex).getConstraintViolations().stream().findFirst()
                        .map(constraint -> {
                            String path = Optional.ofNullable(constraint.getPropertyPath())
                                .map(Path::toString).orElse(StringUtils.EMPTY);

                            int index = StringUtils.lastIndexOf(path, '.');
                            return (index < 0 ? StringUtils.EMPTY : StringUtils.substring(path, index))
                                + constraint.getMessage();
                        }).orElse("Param Validate Failed!");
                }

                String finalErrorMsg = errorMsg;
                return orThrow(
                    targetClass,
                    targetMethod,
                    returnType,
                    response -> {
                        response.setSuccess(false);
                        response.setErrorCode(ErrorCodes.ILLEGAL_ARGUMENT.getErrorCode());
                        response.setErrorMessage(finalErrorMsg);
                    },
                    trackingId, requestId,
                    ex, stopWatch
                );
            }

            // 处理BizException
            if (ex instanceof BizException) {
                BizException serviceException = (BizException)ex;
                logBizException(pmi, trackingId, client, ex);
                return orThrow(
                    targetClass,
                    targetMethod,
                    returnType,
                    response -> {
                        response.setSuccess(false);
                        response.setErrorCode(serviceException.getErrorCode());
                        response.setErrorMessage(serviceException.getErrorMessage());
                    },
                    trackingId, requestId,
                    ex, stopWatch
                );
            }

            // 处理系统异常
            logSysException(pmi, trackingId, client, ex);
            return orThrow(
                targetClass,
                targetMethod,
                returnType,
                response -> {
                    response.setSuccess(false);
                    response.setErrorCode(ErrorCodes.SYSTEM_ERROR.getErrorCode());
                    response.setErrorMessage(
                        StringUtils.isNotBlank(ex.getMessage()) ? ex.getMessage() : ex.getClass().getSimpleName());
                },
                trackingId, requestId,
                ex, stopWatch
            );
        }
    }

    /**
     * 打印业务异常日志
     */
    private void logBizException(ProxyMethodInvocation pmi, String trackingId, String client, Throwable ex) {
        val logMeta = getLogMeta(pmi.getThis().getClass());
        val logLevel = logMeta.map(ExCatchLogMeta::getLevel).orElse(LogLevel.WARN);
        val logger = logMeta.map(ExCatchLogMeta::getLogger);

        switch (logLevel) {
            case INFO: {
                val methodInfo = invokeMethodInfoWithArgs(pmi);
                logger.ifPresent(log -> log.info(LOG_FORMAT, trackingId, client, ex.getMessage(), methodInfo, ex));
                break;
            }
            case WARN: {
                val methodInfo = invokeMethodInfoWithArgs(pmi);
                log.warn(LOG_FORMAT, trackingId, client, ex.getMessage(), methodInfo, ex);
                logger.ifPresent(log -> log.warn(LOG_FORMAT, trackingId, client, ex.getMessage(), methodInfo, ex));
                break;
            }
            case ERROR:
            case FATAL: {
                val methodInfo = invokeMethodInfoWithArgs(pmi);
                log.error(LOG_FORMAT, trackingId, client, ex.getMessage(), methodInfo, ex);
                logger.ifPresent(log -> log.error(LOG_FORMAT, trackingId, client, ex.getMessage(), methodInfo, ex));
                break;
            }
            default: {
                val methodInfo = invokeMethodInfoWithArgs(pmi);
                logger.ifPresent(log -> log.debug(LOG_FORMAT, trackingId, client, ex.getMessage(), methodInfo, ex));
            }
        }
    }

    /**
     * 打印系统异常日志
     */
    private void logSysException(ProxyMethodInvocation pmi, String trackingId, String client, Throwable ex) {
        val logMeta = getLogMeta(pmi.getThis().getClass());
        val logger = logMeta.map(ExCatchLogMeta::getLogger);

        val methodInfo = invokeMethodInfoWithArgs(pmi);
        log.error(LOG_FORMAT, trackingId, client, ex.getMessage(), methodInfo, ex);
        logger.ifPresent(log -> log.error(LOG_FORMAT, trackingId, client, ex.getMessage(), methodInfo, ex));
    }

    private Optional<ExCatchLogMeta> getLogMeta(Class<?> targetClazz) {
        MergedAnnotations mergedAnnotations = MergedAnnotations.from(targetClazz);
        MergedAnnotation<ExCacheAndLog> mergedExCacheAndLog = mergedAnnotations.get(ExCacheAndLog.class);

        if (Objects.isNull(mergedExCacheAndLog)) {
            return Optional.empty();
        }

        if (!logMetas.containsKey(targetClazz)) {
            logMetas.put(targetClazz, ExCatchLogMeta.from(mergedExCacheAndLog));
        }

        ExCatchLogMeta logMeta = logMetas.get(targetClazz);
        if (Objects.isNull(logMeta) || Objects.isNull(logMeta.getLogger())) {
            return Optional.empty();
        }

        return Optional.of(logMeta);
    }

    /**
     * 原始方法调用信息(含参数信息)
     *
     * @param pmi ProxyMethodInvocation
     * @return 方法调用信息
     */
    private String invokeMethodInfoWithArgs(ProxyMethodInvocation pmi) {
        try {
            val argArray = pmi.getArguments();
            String args = (ArrayUtils.isEmpty(argArray) ? Stream.empty() : Arrays.stream(argArray))
                .map(JSON::toJSONString).collect(Collectors.joining(">> <<"));
            return String.format("%s\nargs: <<%s>>", invokeMethodInfo(pmi), args);
        } catch (Exception ex) {
            return StringUtils.EMPTY;
        }
    }

    /**
     * 原始方法调用信息(无参数信息)
     *
     * @param pmi ProxyMethodInvocation
     * @return 方法调用信息
     */
    private String invokeMethodInfo(ProxyMethodInvocation pmi) {
        try {
            Method method = pmi.getMethod();
            Class<?> targetClass = pmi.getThis().getClass();
            Method targetMethod = targetClass.getMethod(method.getName(), method.getParameterTypes());
            return invokeMethodInfo(targetClass, targetMethod);
        } catch (Exception ex) {
            return "<<Invoke Method Info Detect ERROR!>>";
        }
    }

    private String invokeMethodInfo(Class<?> targetClass, Method targetMethod) {
        try {
            String parameterNames = Arrays.stream(targetMethod.getParameterTypes())
                .map(Class::getName)
                .map(paramName -> {
                    val type = StringUtils.substringAfterLast(paramName, ".");
                    return StringUtils.isNotBlank(type) ? type : paramName;
                })
                .collect(Collectors.joining(", "));
            return String.format("%s#%s(%s)", targetClass.getName(), targetMethod.getName(), parameterNames);
        } catch (Exception ex) {
            return "<<Invoke Method Info Detect ERROR!>>";
        }
    }

    /**
     * 返回Response或抛出异常
     *
     * @param returnType     接口返回类型
     * @param responseSetter 需要返回的result设置
     * @param trackingId     请求 id
     * @param ex             当前的异常
     * @return response
     * @throws Throwable 原始异常
     */
    protected Response orThrow(
        Class<?> targetClass, Method targetMethod,
        Class<?> returnType, Consumer<Response> responseSetter, String trackingId, String requestId,
        Throwable ex, StopWatch stopWatch) throws Throwable {
        if (Objects.nonNull(returnType) && Response.class.isAssignableFrom(returnType)) {
            Response response = (Response)returnType.getDeclaredConstructor().newInstance();
            responseSetter.accept(response);
            fillStatMonitor(targetClass, targetMethod, response, trackingId, requestId, stopWatch, ex);
            return response;
        } else {
            throw ex;
        }
    }

    protected void fillStatMonitor(
        Class<?> targetClass, Method targetMethod,
        Object respValue, String trackingId, String requestId, StopWatch stopWatch, Throwable ex) {
        if (stopWatch.isRunning()) {
            stopWatch.stop();
        }

        if (Objects.nonNull(respValue) && (respValue instanceof Response)) {
            val response = (Response)respValue;
            response.setRequestId(requestId);
            response.setTraceId(trackingId);
            response.setMethod(invokeMethodInfo(targetClass, targetMethod));
            response.setElapsedTime(stopWatch.getTotalTimeMillis());
            response.setException(ex);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 1;
    }
}
