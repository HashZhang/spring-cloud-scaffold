package com.github.hashjang.spring.cloud.iiford.service.common.feign;

import brave.Span;
import brave.Tracer;
import com.alibaba.fastjson.JSON;
import com.github.hashjang.spring.cloud.iiford.service.common.misc.ResponseWrapperException;
import feign.Request;
import feign.Response;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

@Slf4j
public class Resilience4jFeignClient extends ApacheHttpClient {
    private final ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final Tracer tracer;

    public Resilience4jFeignClient(
            HttpClient httpClient,
            ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry,
            Tracer tracer
    ) {
        super(httpClient);
        this.threadPoolBulkheadRegistry = threadPoolBulkheadRegistry;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.tracer = tracer;
    }

    @Override
    public Response execute(Request request, Request.Options options) throws IOException {
        //获取要调用的微服务
        String serviceName = request.requestTemplate().feignTarget().name();
        //获取实例唯一id
        String serviceInstanceId = getServiceInstanceId(request);
        //获取实例+方法唯一id
        String serviceInstanceMethodId = getServiceInstanceMethodId(request);

        ThreadPoolBulkhead threadPoolBulkhead;
        CircuitBreaker circuitBreaker;
        try {
            //每个实例一个线程池
            threadPoolBulkhead = threadPoolBulkheadRegistry.bulkhead(serviceInstanceId, serviceName);
        } catch (ConfigurationNotFoundException e) {
            threadPoolBulkhead = threadPoolBulkheadRegistry.bulkhead(serviceInstanceId);
        }
        try {
            //每个服务实例具体方法一个resilience4j熔断记录器，在服务实例具体方法维度做熔断，所有这个服务的实例具体方法共享这个服务的resilience4j熔断配置
            circuitBreaker = circuitBreakerRegistry.circuitBreaker(serviceInstanceMethodId, serviceName);
        } catch (ConfigurationNotFoundException e) {
            circuitBreaker = circuitBreakerRegistry.circuitBreaker(serviceInstanceMethodId);
        }
        //保持traceId
        Span span = tracer.currentSpan();
        ThreadPoolBulkhead finalThreadPoolBulkhead = threadPoolBulkhead;
        CircuitBreaker finalCircuitBreaker = circuitBreaker;
        Supplier<CompletionStage<Response>> completionStageSupplier = ThreadPoolBulkhead.decorateSupplier(threadPoolBulkhead,
                CircuitBreaker.decorateSupplier(circuitBreaker, () -> {
                    try (Tracer.SpanInScope cleared = tracer.withSpanInScope(span)) {
                        log.info("call url: {} -> {}, ThreadPoolStats: {}, CircuitBreakStats: {}",
                                request.httpMethod(),
                                request.url(),
                                JSON.toJSONString(finalThreadPoolBulkhead.getMetrics()),
                                JSON.toJSONString(finalCircuitBreaker.getMetrics())
                        );
                        Response execute = super.execute(request, options);
                        if (execute.status() != HttpStatus.OK.value()) {
                            //需要关闭，否则返回码不为200抛异常连接不会回收导致连接池耗尽
                            execute.close();
                            //虽然有返回响应，但是算失败了，需要抛出异常让断路器感知到
                            //但是最后返回，只返回 execute 这个 Response
                            throw new ResponseWrapperException(execute.toString(), execute);
                        }
                        return execute;
                    } catch (ResponseWrapperException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new ResponseWrapperException(e.getMessage(), e);
                    }
                })
        );

        try {
            return Try.ofSupplier(completionStageSupplier).get().toCompletableFuture().join();
        } catch (CompletionException e) {
            //内部抛出的所有异常都被封装了一层 CompletionException，所以这里需要取出里面的 Exception
            Throwable cause = e.getCause();
            //如果是我们抛出的 ResponseWrapperException，检查是否是因为 Response 不为 200 需要抛出的，这种情况需要返回对应的 Response
            //这样就可以走 Feign 的机制，防止某些 Feign 的机制失效例如 ErrorDecoder 等
            if (cause instanceof ResponseWrapperException) {
                ResponseWrapperException responseWrapperException = (ResponseWrapperException) cause;
                if (responseWrapperException.getResponse() != null) {
                    return (Response) responseWrapperException.getResponse();
                }
            }
            throw new ResponseWrapperException(cause.getMessage(), cause);
        }
    }

    private String getServiceInstanceId(Request request) throws MalformedURLException {
        String serviceName = request.requestTemplate().feignTarget().name();
        URL url = new URL(request.url());
        return serviceName + ":" + url.getHost() + ":" + url.getPort();
    }

    private String getServiceInstanceMethodId(Request request) throws MalformedURLException {
        String serviceName = request.requestTemplate().feignTarget().name();
        URL url = new URL(request.url());
        //通过微服务名称 + 实例 + 方法的方式，获取唯一id
        String methodName = request.requestTemplate().methodMetadata().method().toGenericString();
        return serviceName + ":" + url.getHost() + ":" + url.getPort() + ":" + methodName;
    }
}
