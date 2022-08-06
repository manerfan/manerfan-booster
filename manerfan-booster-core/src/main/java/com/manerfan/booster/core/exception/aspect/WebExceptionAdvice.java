package com.manerfan.booster.core.exception.aspect;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Objects;
import java.util.Optional;

import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.validation.ValidationException;

import com.manerfan.booster.api.common.dto.response.Response;
import com.manerfan.booster.core.exception.BizException;
import com.manerfan.booster.core.exception.ErrorCodes;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.ClassUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * WebExceptionAdvice
 *
 * <pre>
 *      Spring Web异常统一处理
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
@Slf4j(topic = "EX_CATCH_LOG")
public class WebExceptionAdvice extends ExCatchAndLogAdvice {
    public WebExceptionAdvice() {
        super();
    }

    public WebExceptionAdvice(BaseServiceContext serviceContext) {super(serviceContext);}

    @ResponseBody
    @ExceptionHandler({ValidationException.class})
    public final Response handleValidationException(ValidationException validationException, WebRequest request) {
        String errorMsg = validationException.getMessage();
        if (validationException instanceof ConstraintViolationException) {
            errorMsg = ((ConstraintViolationException)validationException).getConstraintViolations().stream()
                .findFirst()
                .map(constraint -> {
                    String path = Optional.ofNullable(constraint.getPropertyPath())
                        .map(Path::toString).orElse(StringUtils.EMPTY);

                    int index = StringUtils.lastIndexOf(path, '.');
                    return (index < 0 ? StringUtils.EMPTY : StringUtils.substring(path, index))
                        + constraint.getMessage();
                }).orElse("Param Validate Failed!");
        }

        log.info("Http Invoke Validate Failed!|{}|{}|{}", traceId(), requestMethod(request), errorMsg);

        Response response = new Response();
        response.setSuccess(false);
        response.setErrorCode(ErrorCodes.ILLEGAL_ARGUMENT.getErrorCode());
        response.setErrorMessage(errorMsg);
        return response;
    }

    @ResponseBody
    @ExceptionHandler({IllegalArgumentException.class})
    public final Response handleIllegalArgumentException(
        IllegalArgumentException illegalArgumentException, WebRequest request) {
        log.info("Http Invoke Argument Illegal!|{}|{}|{}",
            traceId(), requestMethod(request), illegalArgumentException.getMessage(), illegalArgumentException);
        Response response = new Response();
        response.setSuccess(false);
        response.setErrorCode(ErrorCodes.ILLEGAL_ARGUMENT.getErrorCode());
        response.setErrorMessage(
            StringUtils.isNotBlank(illegalArgumentException.getMessage())
                ? illegalArgumentException.getMessage()
                : illegalArgumentException.getClass().getSimpleName());
        response.setException(illegalArgumentException);
        return response;
    }

    @ResponseBody
    @ExceptionHandler({BizException.class})
    public final Response handleBizException(BizException serviceException, WebRequest request) {
        log.error("Http Invoke Error!|{}|{}|{}",
            traceId(), requestMethod(request), serviceException.getErrorMessage(), serviceException);
        Response response = new Response();
        response.setSuccess(false);
        response.setErrorCode(serviceException.getErrorCode());
        response.setErrorMessage(serviceException.getErrorMessage());
        response.setException(serviceException);
        return response;
    }

    @ResponseBody
    @ExceptionHandler({
        HttpRequestMethodNotSupportedException.class, HttpMediaTypeNotSupportedException.class,
        HttpMediaTypeNotAcceptableException.class, MissingPathVariableException.class,
        MissingServletRequestParameterException.class, ServletRequestBindingException.class,
        ConversionNotSupportedException.class, TypeMismatchException.class, HttpMessageNotReadableException.class,
        HttpMessageNotWritableException.class, MethodArgumentNotValidException.class,
        MissingServletRequestPartException.class, BindException.class, NoHandlerFoundException.class,
        AsyncRequestTimeoutException.class})
    public Response handleHttpException(Exception ex, WebRequest request) {
        HttpStatus status;
        String errorMessage = null;
        if (ex instanceof HttpRequestMethodNotSupportedException) {
            status = HttpStatus.METHOD_NOT_ALLOWED;
        } else if (ex instanceof HttpMediaTypeNotSupportedException) {
            status = HttpStatus.UNSUPPORTED_MEDIA_TYPE;
        } else if (ex instanceof HttpMediaTypeNotAcceptableException) {
            status = HttpStatus.NOT_ACCEPTABLE;
        } else if (ex instanceof MissingPathVariableException) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        } else if (ex instanceof MissingServletRequestParameterException) {
            val paramName = ((MissingServletRequestParameterException)ex).getParameterName();
            if (StringUtils.isNotBlank(paramName)) {
                errorMessage = "参数缺失：" + paramName;
            }
            status = HttpStatus.BAD_REQUEST;
        } else if (ex instanceof ServletRequestBindingException) {
            status = HttpStatus.BAD_REQUEST;
        } else if (ex instanceof ConversionNotSupportedException) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        } else if (ex instanceof TypeMismatchException) {
            status = HttpStatus.BAD_REQUEST;
        } else if (ex instanceof HttpMessageNotReadableException) {
            status = HttpStatus.BAD_REQUEST;
        } else if (ex instanceof HttpMessageNotWritableException) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        } else if (ex instanceof MethodArgumentNotValidException) {
            val bindingResult = ((MethodArgumentNotValidException)ex).getBindingResult();
            if (bindingResult.hasErrors()) {
                errorMessage = Optional.ofNullable(bindingResult.getFieldError())
                    .map(FieldError::getDefaultMessage).orElse(null);
            }
            status = HttpStatus.BAD_REQUEST;
        } else if (ex instanceof MissingServletRequestPartException) {
            status = HttpStatus.BAD_REQUEST;
        } else if (ex instanceof BindException) {
            status = HttpStatus.BAD_REQUEST;
        } else if (ex instanceof NoHandlerFoundException) {
            status = HttpStatus.NOT_FOUND;
        } else if (ex instanceof AsyncRequestTimeoutException) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
        } else {
            return handleException(ex, request);
        }

        log.warn("Http Invoke Failed!|{}|{}|{}", traceId(), requestMethod(request), ex.getMessage(), ex);

        Response response = new Response();
        response.setSuccess(false);
        response.setErrorCode(Objects.toString(status.value()));
        response.setErrorMessage(StringUtils.isNotBlank(errorMessage) ? errorMessage : status.getReasonPhrase());
        return response;
    }

    @ResponseBody
    @ExceptionHandler({UndeclaredThrowableException.class})
    public Response handleException(UndeclaredThrowableException ex, WebRequest request) {
        return handleException(ex.getUndeclaredThrowable(), request);
    }

    @ResponseBody
    @ExceptionHandler({Throwable.class})
    public Response handleException(Throwable ex, WebRequest request) {
        log.error("Http Invoke Error!|{}|{}|{}", traceId(), requestMethod(request), ex.getMessage(), ex);
        Response response = new Response();
        response.setSuccess(false);
        response.setErrorCode(ErrorCodes.SYSTEM_ERROR.getErrorCode());
        response.setErrorMessage(
            StringUtils.isNotBlank(ex.getMessage()) ? ex.getMessage() : ex.getClass().getSimpleName());
        response.setException(ex);
        return response;
    }

    public String traceId() {
        return serviceContext.getTraceId();
    }

    public String requestMethod(WebRequest webRequest) {
        val servletRequest = ((ServletWebRequest)webRequest);
        val method = servletRequest.getHttpMethod();
        val requestUri = servletRequest.getRequest().getRequestURI();
        return String.format("[%s]%s", method, requestUri);
    }
}
