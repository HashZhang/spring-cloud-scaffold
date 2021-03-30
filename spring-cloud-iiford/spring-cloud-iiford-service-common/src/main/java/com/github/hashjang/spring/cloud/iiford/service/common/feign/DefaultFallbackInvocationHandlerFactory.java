package com.github.hashjang.spring.cloud.iiford.service.common.feign;

import feign.InvocationHandlerFactory;
import feign.Target;
import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.FeignContext;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

@Log4j2
public class DefaultFallbackInvocationHandlerFactory implements InvocationHandlerFactory {
    private final String contextId;
    private final FeignContext feignContext;

    public DefaultFallbackInvocationHandlerFactory(String contextId, FeignContext feignContext) {
        this.contextId = contextId;
        this.feignContext = feignContext;
    }

    private

    @Override
    public InvocationHandler create(Target target, Map<Method, MethodHandler> dispatch) {
        FeignClient feignClient = target.type().getClass().getAnnotation(FeignClient.class);
        if (feignClient.fallback() != void.class) {
            if (feignClient.fallback().isAssignableFrom(target.type())) {

            } else {
                log.warn("fallback {} does not implement {} and is ignored", feignClient.fallback(), target.type());
            }
        }
        if (feignClient.fallbackFactory() != void.class) {

        }
        return null;
    }
}
