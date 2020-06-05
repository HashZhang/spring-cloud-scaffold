package com.github.hashjang.hoxton.service.consumer.config;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.vavr.control.Try;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Arrays;
import java.util.function.Supplier;

@Log4j2
@Aspect
@Component
public class CustomizedCircuitBreakerAspect {
    private final RetryRegistry retryRegistry;

    public CustomizedCircuitBreakerAspect(RetryRegistry retryRegistry) {
        this.retryRegistry = retryRegistry;
    }

    //配置哪些包下的FeignClient进行重试，必须含有@FeignClient注解
    @Around("execution(* com.github.hashjang.hoxton..*(..)) && @within(org.springframework.cloud.openfeign.FeignClient)")
    public Object feignClientWasCalled(final ProceedingJoinPoint pjp) throws Throwable {
        boolean isGet = false;
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        FeignClient annotation = signature.getMethod().getDeclaringClass().getAnnotation(FeignClient.class);
        String serviceName = annotation.value();
        if (StringUtils.isBlank(serviceName)) {
            serviceName = annotation.name();
        }

        //查看是否是GET请求
        RequestMapping requestMapping = signature.getMethod().getAnnotation(RequestMapping.class);
        if (requestMapping != null &&
                (requestMapping.method().length == 0 ||
                        Arrays.asList(requestMapping.method()).contains(RequestMethod.GET))
        ) {
            isGet = true;
        }
        GetMapping getMapping = signature.getMethod().getAnnotation(GetMapping.class);
        if (getMapping != null) {
            isGet = true;
        }
        Retry retry;
        try {
            retry = retryRegistry.retry(serviceName, serviceName);
        } catch (ConfigurationNotFoundException e) {
            retry = retryRegistry.retry(serviceName);
        }
        if (!isGet) {
            //非GET请求，只有在断路器打开的情况下，才会重试
            retry = Retry.of(serviceName, RetryConfig.from(retry.getRetryConfig()).retryExceptions().retryOnException(throwable -> {
                Throwable cause = throwable.getCause();
                if (cause instanceof CallNotPermittedException) {
                    //对于断路器，不区分方法，都重试，因为没有实际调用
                    log.info("retry on circuit breaker is on: {}", cause.getMessage());
                    return true;
                }
                return false;
            }).build());
        }
        //对于GET请求，启用重试机制
        Supplier<Object> objectSupplier = Retry.decorateSupplier(retry, () -> {
            try {
                return pjp.proceed();
            } catch (Throwable throwable) {
                ReflectionUtils.rethrowRuntimeException(throwable);
                return null;
            }
        });
        return Try.ofSupplier(objectSupplier).get();
    }
}
