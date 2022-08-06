package com.manerfan.booster.core.exception.aspect;

import com.manerfan.booster.api.common.dto.response.Response;
import org.springframework.aop.framework.autoproxy.AbstractBeanFactoryAwareAdvisingPostProcessor;
import org.springframework.core.Ordered;

/**
 * ExCatchAndLogAdvisingPostProcessor
 *
 * <pre>
 *       如果方法有异常，并且返回值是{@link Response}，则将异常包装为Response返回
 *       如果异常为 {@link ServiceException}，并且目标类带有{@link ExCacheAndLog}注解，则按照注解参数打印日志
 *
 *       由于将异常包装为Response返回，会丢失异常，为了防止Transaction Rollback问题（或其他依赖Exception的Aspect问题）
 *       将该Aspect的Order设为【最高优先级】【最高优先级】【最高优先级】（洋葱模型最外层）
 *
 *       ---------------------
 *       HsfProvider
 *       ↑          ↓
 *       ┇result   ┇invoke
 *       ↑          ↓
 *       ExCacheAndLogAdvisingPostProcessor
 *       ↑          ↓
 *       ┇throw    ┇invoke
 *       ↑          ↓
 *       someXxxxAspect
 *       ↑          ↓
 *       ┇throw    ┇invoke
 *       ↑          ↓
 *       TransactionAspect
 *       ↑          ↓
 *       ┇throw    ┇invoke
 *       ↑          ↓
 *       someYyyyAspect
 *       ↑          ↓
 *       ┇throw    ┇invoke
 *       ↑          ↓
 *       originalMethod
 *       --------------------
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
public class ExCatchAndLogAdvisingPostProcessor extends AbstractBeanFactoryAwareAdvisingPostProcessor {
    public ExCatchAndLogAdvisingPostProcessor(BaseServiceContext serviceContext) {
        this.advisor = new ExCacheAndLogPointcutAdvisor(serviceContext);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
