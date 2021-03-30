package com.github.hashjang.spring.cloud.iiford.service.common.feign;

import feign.InvocationHandlerFactory;
import feign.Target;
import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.FeignContext;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;

@Log4j2
public class DefaultFallbackInvocationHandlerFactory implements InvocationHandlerFactory {
    private final String contextId;
    private final FeignContext feignContext;

    public DefaultFallbackInvocationHandlerFactory(String contextId, FeignContext feignContext) {
        this.contextId = contextId;
        this.feignContext = feignContext;
    }


    @Override
    public InvocationHandler create(Target target, Map<Method, MethodHandler> dispatch) {
        FeignClient feignClient = target.type().getClass().getAnnotation(FeignClient.class);
        if (feignClient.fallback() != void.class) {
            if (feignClient.fallback().isAssignableFrom(target.type())) {
                Object instance = feignContext.getInstance(contextId, feignClient.fallback());
                if (instance == null) {
                    return new DefaultFallbackInvocationHandler(target, dispatch, null);
                }
                FallbackFactory.Default aDefault = new FallbackFactory.Default(instance);
                return new DefaultFallbackInvocationHandler(target, dispatch, aDefault);
            } else {
                log.warn("fallback {} does not implement {} and is ignored", feignClient.fallback(), target.type());
            }
        }
        if (feignClient.fallbackFactory() != void.class) {
            if (feignClient.fallbackFactory().isAssignableFrom(FallbackFactory.class)) {
                Object instance = feignContext.getInstance(contextId, feignClient.fallbackFactory());
                return new DefaultFallbackInvocationHandler(target, dispatch, (FallbackFactory<?>) instance);
            } else {
                log.warn("fallbackFactory {} does not implement {} and is ignored", feignClient.fallback(), FallbackFactory.class);
            }
        }
        return new DefaultFallbackInvocationHandler(target, dispatch, null);
    }

    private static class DefaultFallbackInvocationHandler implements InvocationHandler {

        private final Target<?> target;

        private final Map<Method, InvocationHandlerFactory.MethodHandler> dispatch;

        private final FallbackFactory<?> nullableFallbackFactory;

        private final Map<Method, Method> fallbackMethodMap;

        private DefaultFallbackInvocationHandler(Target<?> target, Map<Method, MethodHandler> dispatch, FallbackFactory<?> nullableFallbackFactory) {
            this.target = target;
            this.dispatch = dispatch;
            this.nullableFallbackFactory = nullableFallbackFactory;
            this.fallbackMethodMap = toFallbackMethod(dispatch);
        }

        static Map<Method, Method> toFallbackMethod(Map<Method, InvocationHandlerFactory.MethodHandler> dispatch) {
            Map<Method, Method> result = new LinkedHashMap<Method, Method>();
            for (Method method : dispatch.keySet()) {
                method.setAccessible(true);
                result.put(method, method);
            }
            return result;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // early exit if the invoked method is from java.lang.Object
            // code is the same as ReflectiveFeign.FeignInvocationHandler
            if ("equals".equals(method.getName())) {
                try {
                    Object otherHandler = args.length > 0 && args[0] != null ? Proxy.getInvocationHandler(args[0]) : null;
                    return equals(otherHandler);
                }
                catch (IllegalArgumentException e) {
                    return false;
                }
            }
            else if ("hashCode".equals(method.getName())) {
                return hashCode();
            }
            else if ("toString".equals(method.getName())) {
                return toString();
            }
            try {
                return this.dispatch.get(method).invoke(args);
            } catch(Exception e) {
                if (nullableFallbackFactory != null) {
                    return this.fallbackMethodMap.get(method).invoke(nullableFallbackFactory.create(e), args);
                }
                throw e;
            }
        }
    }
}
