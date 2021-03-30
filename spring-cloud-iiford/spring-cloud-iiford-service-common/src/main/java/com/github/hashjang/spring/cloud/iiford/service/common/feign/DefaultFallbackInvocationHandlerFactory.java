package com.github.hashjang.spring.cloud.iiford.service.common.feign;

import feign.InvocationHandlerFactory;
import feign.Target;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

public class DefaultFallbackInvocationHandlerFactory implements InvocationHandlerFactory {
    @Override
    public InvocationHandler create(Target target, Map<Method, MethodHandler> dispatch) {
        target
        return null;
    }
}
