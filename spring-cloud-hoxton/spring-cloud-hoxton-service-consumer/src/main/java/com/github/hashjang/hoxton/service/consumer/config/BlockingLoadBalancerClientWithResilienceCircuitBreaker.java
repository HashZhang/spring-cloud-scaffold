package com.github.hashjang.hoxton.service.consumer.config;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;
import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;

public class BlockingLoadBalancerClientWithResilienceCircuitBreaker extends BlockingLoadBalancerClient {
    private final BulkheadRegistry bulkheadRegistry;
    private final ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;
    private final RetryRegistry retryRegistry;

    public BlockingLoadBalancerClientWithResilienceCircuitBreaker(LoadBalancerClientFactory loadBalancerClientFactory, BulkheadRegistry bulkheadRegistry, ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry, CircuitBreakerRegistry circuitBreakerRegistry, RateLimiterRegistry rateLimiterRegistry, RetryRegistry retryRegistry) {
        super(loadBalancerClientFactory);
        this.bulkheadRegistry = bulkheadRegistry;
        this.threadPoolBulkheadRegistry = threadPoolBulkheadRegistry;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.retryRegistry = retryRegistry;
    }

    @Override
    public <T> T execute(String serviceId, LoadBalancerRequest<T> request) throws IOException {
        ServiceInstance serviceInstance = choose(serviceId);
        if (serviceInstance == null) {
            throw new IllegalStateException("No instances available for " + serviceId);
        }
        return execute(serviceId, serviceInstance, request);
    }

    @Override
    public <T> T execute(String serviceId, ServiceInstance serviceInstance, LoadBalancerRequest<T> request) throws IOException {

        //每个实例一个resilience4j熔断记录器，在实例维度做熔断，所有这个服务的实例共享这个服务的resilience4j熔断配置
        ThreadPoolBulkhead threadPoolBulkhead = threadPoolBulkheadRegistry.bulkhead(serviceInstance.getInstanceId(), serviceId);
        Retry retry = retryRegistry.retry(serviceInstance.getInstanceId(), serviceId);
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(serviceInstance.getInstanceId(), serviceId);

        try {
            return retry.executeCallable(
                    () -> threadPoolBulkhead.executeCallable(
                            () -> circuitBreaker.executeCallable(() -> request.apply(serviceInstance))
                    )
            ).toCompletableFuture().join();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            ReflectionUtils.rethrowRuntimeException(e);
        }
        return null;
    }
}
