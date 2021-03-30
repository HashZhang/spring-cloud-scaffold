package com.github.hashjang.spring.cloud.iiford.service.common.feign;

import feign.Feign;
import feign.InvocationHandlerFactory;
import feign.Target;
import org.springframework.cloud.openfeign.FeignClientFactoryBean;
import org.springframework.cloud.openfeign.FeignContext;
import org.springframework.cloud.openfeign.Targeter;

public class CommonFeignTargeter implements Targeter {

    @Override
    public <T> T target(FeignClientFactoryBean factory, Feign.Builder feign, FeignContext context, Target.HardCodedTarget<T> target) {
        feign.invocationHandlerFactory(context.getInstance(factory.getContextId(), InvocationHandlerFactory.class));
        return feign.target(target);
    }
}
