package com.github.hashjang.spring.cloud.iiford.service.common.feign;

import brave.Span;
import brave.Tracer;
import com.github.hashjang.spring.cloud.iiford.service.common.HttpBinAnythingResponse;
import feign.Request;
import feign.RetryableException;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClientConfiguration;
import org.springframework.cloud.netflix.eureka.loadbalancer.LoadBalancerEurekaAutoConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.FeignContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.mockito.Mockito.when;

//SpringRunner也包含了MockitoJUnitRunner，所以 @Mock 等注解也生效了
@RunWith(SpringRunner.class)
@SpringBootTest(properties = {
        LoadBalancerEurekaAutoConfiguration.LOADBALANCER_ZONE + "=zone1",
        //验证 thread-pool-bulkhead 相关的配置
        "resilience4j.thread-pool-bulkhead.configs.default.coreThreadPoolSize=" + OpenFeignClientTest.DEFAULT_THREAD_POOL_SIZE,
        "resilience4j.thread-pool-bulkhead.configs.default.maxThreadPoolSize=" + OpenFeignClientTest.DEFAULT_THREAD_POOL_SIZE,
        "resilience4j.thread-pool-bulkhead.configs." + OpenFeignClientTest.CONTEXT_ID_2 + ".coreThreadPoolSize=" + OpenFeignClientTest.TEST_SERVICE_2_THREAD_POOL_SIZE,
        "resilience4j.thread-pool-bulkhead.configs." + OpenFeignClientTest.CONTEXT_ID_2 + ".maxThreadPoolSize=" + OpenFeignClientTest.TEST_SERVICE_2_THREAD_POOL_SIZE,
        "resilience4j.circuitbreaker.configs.default.failureRateThreshold=" + OpenFeignClientTest.DEFAULT_FAILURE_RATE_THRESHOLD,
        "resilience4j.circuitbreaker.configs.default.slidingWindowType=TIME_BASED",
        "resilience4j.circuitbreaker.configs.default.slidingWindowSize=5",
        "resilience4j.circuitbreaker.configs.default.minimumNumberOfCalls=" + OpenFeignClientTest.DEFAULT_MINIMUM_NUMBER_OF_CALLS,
        "resilience4j.circuitbreaker.configs." + OpenFeignClientTest.CONTEXT_ID_2 + ".failureRateThreshold=" + OpenFeignClientTest.TEST_SERVICE_2_FAILURE_RATE_THRESHOLD,
        "resilience4j.circuitbreaker.configs." + OpenFeignClientTest.CONTEXT_ID_2 + ".minimumNumberOfCalls=" + OpenFeignClientTest.TEST_SERVICE_2_MINIMUM_NUMBER_OF_CALLS,
        "resilience4j.retry.configs.default.maxAttempts=" + OpenFeignClientTest.DEFAULT_RETRY,
        "resilience4j.retry.configs.default.retryExceptionPredicate=com.github.hashjang.spring.cloud.iiford.service.common.feign.DefaultRetryOnExceptionPredicate",
        "resilience4j.retry.configs." + OpenFeignClientTest.CONTEXT_ID_2 + ".maxAttempts=" + OpenFeignClientTest.TEST_SERVICE_2_RETRY,
        "resilience4j.retry.configs." + OpenFeignClientTest.CONTEXT_ID_2 + ".retryExceptionPredicate=com.github.hashjang.spring.cloud.iiford.service.common.feign.DefaultRetryOnExceptionPredicate",

})
@Log4j2
public class OpenFeignClientTest {
    public static final String THREAD_ID_HEADER = "Threadid";
    public static final String TEST_SERVICE_1 = "testService1";
    public static final String CONTEXT_ID_1 = "testService1Client";
    public static final int DEFAULT_THREAD_POOL_SIZE = 10;
    public static final int DEFAULT_FAILURE_RATE_THRESHOLD = 50;
    public static final int DEFAULT_MINIMUM_NUMBER_OF_CALLS = 2;
    public static final int DEFAULT_RETRY = 3;
    public static final String TEST_SERVICE_2 = "testService2";
    public static final String CONTEXT_ID_2 = "testService2Client";
    public static final int TEST_SERVICE_2_THREAD_POOL_SIZE = 5;
    public static final int TEST_SERVICE_2_FAILURE_RATE_THRESHOLD = 30;
    public static final int TEST_SERVICE_2_MINIMUM_NUMBER_OF_CALLS = 10;
    public static final int TEST_SERVICE_2_RETRY = 2;

    private static int callCount = 0;
    @Autowired
    private Tracer tracer;
    @Autowired
    private FeignContext feignContext;
    @Autowired
    private TestService1Client testService1Client;
    @Autowired
    private TestService2Client testService2Client;
    @Autowired
    private ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry;
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    @Autowired
    private RetryRegistry retryRegistry;

    @SpringBootApplication(exclude = EurekaDiscoveryClientConfiguration.class)
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @Configuration
    public static class App {
        @Bean
        public ApacheHttpClientAop apacheHttpClientAop() {
            return new ApacheHttpClientAop();
        }

        @Bean
        public DiscoveryClient discoveryClient() {
            ServiceInstance service1Instance1 = Mockito.mock(ServiceInstance.class);
            ServiceInstance service2Instance2 = Mockito.mock(ServiceInstance.class);
            ServiceInstance service1Instance3 = Mockito.mock(ServiceInstance.class);
            Map<String, String> zone1 = Map.ofEntries(
                    Map.entry("zone", "zone1")
            );
            when(service1Instance1.getMetadata()).thenReturn(zone1);
            when(service1Instance1.getInstanceId()).thenReturn("service1Instance1");
            when(service1Instance1.getHost()).thenReturn("httpbin.org");
            when(service1Instance1.getPort()).thenReturn(80);
            when(service2Instance2.getMetadata()).thenReturn(zone1);
            when(service2Instance2.getInstanceId()).thenReturn("service2Instance2");
            when(service2Instance2.getHost()).thenReturn("httpbin.org");
            when(service2Instance2.getPort()).thenReturn(80);
            when(service1Instance3.getMetadata()).thenReturn(zone1);
            when(service1Instance3.getInstanceId()).thenReturn("service1Instance3");
            //这其实就是 httpbin.org ，为了和第一个实例进行区分加上 www
            when(service1Instance3.getHost()).thenReturn("www.httpbin.org");
            when(service1Instance3.getPort()).thenReturn(80);
            DiscoveryClient mock = Mockito.mock(DiscoveryClient.class);
            Mockito.when(mock.getInstances(TEST_SERVICE_1))
                    .thenReturn(List.of(service1Instance1, service1Instance3));
            Mockito.when(mock.getInstances(TEST_SERVICE_2))
                    .thenReturn(List.of(service2Instance2));
            return mock;
        }

        @Bean
        public TestService1ClientFallback testService1ClientFallback() {
            return new TestService1ClientFallback();
        }
    }

    /**
     * 验证配置生效
     */
    @Test
    public void testRetry() throws InterruptedException {
        //防止断路器影响
        TimeUnit.SECONDS.sleep(5);
        callCount = 0;
        try {
            testService1Client.testGetRetryStatus500();
        } catch (Exception e) {
        }
        Assert.assertEquals(callCount, 3);
        //防止断路器影响
        TimeUnit.SECONDS.sleep(5);
        callCount = 0;
        try {
            testService1Client.testPostRetryStatus500();
        } catch (Exception e) {
        }
        Assert.assertEquals(callCount, 1);
        //防止断路器影响
        TimeUnit.SECONDS.sleep(5);
        callCount = 0;
        try {
            testService2Client.testGetRetryStatus500();
        } catch (Exception e) {
        }
        Assert.assertEquals(callCount, 2);
        //防止断路器影响
        TimeUnit.SECONDS.sleep(5);
        callCount = 0;
        try {
            testService2Client.testPostRetryStatus500();
        } catch (Exception e) {
        }
        Assert.assertEquals(callCount, 1);
        //防止断路器影响
        TimeUnit.SECONDS.sleep(5);
        callCount = 0;
        try {
            testService2Client.testPostWithAnnotationRetryStatus500();
        } catch (Exception e) {
        }
        Assert.assertEquals(callCount, 2);
    }

    @FeignClient(name = "testService2", contextId = CONTEXT_ID_2)
    public interface TestService2Client {
        @GetMapping("/anything")
        HttpBinAnythingResponse anything();

        @GetMapping("/status/500")
        String testGetRetryStatus500();

        @PostMapping("/status/500")
        String testPostRetryStatus500();

        @RetryableMethod
        @PostMapping("/status/500")
        String testPostWithAnnotationRetryStatus500();
    }

    @Test(expected = RetryableException.class)
    public void testFallback() {
        for (int i = 0; i < 10; i++) {
            String s = testService1Client.testGetRetryStatus500();
            log.info(s);
        }
        testService2Client.testGetRetryStatus500();
    }

    public static class TestService1ClientFallback implements TestService1Client {

        @Override
        public HttpBinAnythingResponse anything() {
            HttpBinAnythingResponse httpBinAnythingResponse = new HttpBinAnythingResponse();
            httpBinAnythingResponse.setData("fallback");
            return httpBinAnythingResponse;
        }

        @Override
        public String testCircuitBreakerStatus500() {
            return "fallback";
        }

        @Override
        public String testGetRetryStatus500() {
            return "fallback";
        }

        @Override
        public String testPostRetryStatus500() {
            return "fallback";
        }
    }

    @FeignClient(name = "testService1", contextId = CONTEXT_ID_1, configuration = TestService1ClientConfiguration.class)
    public interface TestService1Client {
        @GetMapping("/anything")
        HttpBinAnythingResponse anything();

        @GetMapping("/status/500")
        String testCircuitBreakerStatus500();

        @GetMapping("/status/500")
        String testGetRetryStatus500();

        @PostMapping("/status/500")
        String testPostRetryStatus500();
    }


    /**
     * 验证同一个服务不同实例处于不同线程
     *
     * @throws Exception
     */
    @Test
    public void testDifferentInstanceWithDifferentThread() throws Exception {
        Thread[] threads = new Thread[100];
        AtomicBoolean passed = new AtomicBoolean(true);
        //循环100次
        for (int i = 0; i < 100; i++) {
            threads[i] = new Thread(() -> {
                Span span = tracer.nextSpan();
                //保证两次调用处于同一个 Span 下，这样一定会调用另一个实例
                try (Tracer.SpanInScope cleared = tracer.withSpanInScope(span)) {
                    HttpBinAnythingResponse response = testService1Client.anything();
                    String threadId1 = response.getHeaders().get(THREAD_ID_HEADER);
                    response = testService1Client.anything();
                    String threadId2 = response.getHeaders().get(THREAD_ID_HEADER);
                    if (StringUtils.equalsIgnoreCase(threadId1, threadId2)) {
                        passed.set(false);
                    }
                }
            });
            threads[i].start();
        }
        for (int i = 0; i < 100; i++) {
            threads[i].join();
        }
        Assert.assertTrue(passed.get());
    }

    /**
     * 验证不同服务处于不同线程
     *
     * @throws Exception
     */
    @Test
    public void testDifferentServiceWithDifferentThread() throws Exception {
        Thread[] threads = new Thread[100];
        AtomicBoolean passed = new AtomicBoolean(true);
        //循环100次
        for (int i = 0; i < 100; i++) {
            threads[i] = new Thread(() -> {
                Span span = tracer.nextSpan();
                try (Tracer.SpanInScope cleared = tracer.withSpanInScope(span)) {
                    HttpBinAnythingResponse response = testService1Client.anything();
                    String threadId1 = response.getHeaders().get(THREAD_ID_HEADER);
                    response = testService2Client.anything();
                    String threadId2 = response.getHeaders().get(THREAD_ID_HEADER);
                    if (StringUtils.equalsIgnoreCase(threadId1, threadId2)) {
                        passed.set(false);
                    }
                }
            });
            threads[i].start();
        }
        for (int i = 0; i < 100; i++) {
            threads[i].join();
        }
        Assert.assertTrue(passed.get());
    }

    /**
     * 验证配置生效
     */
    @Test
    public void testConfigureThreadPool() {
        testService1Client.anything();
        testService2Client.anything();
        List<ThreadPoolBulkhead> threadPoolBulkheads = threadPoolBulkheadRegistry.getAllBulkheads().asJava();
        Set<String> collect = threadPoolBulkheads.stream().map(ThreadPoolBulkhead::getName)
                .filter(name -> name.contains(CONTEXT_ID_1) || name.contains(CONTEXT_ID_2)).collect(Collectors.toSet());
        Assert.assertTrue(collect.size() >= 2);
        threadPoolBulkheads.forEach(threadPoolBulkhead -> {
            if (threadPoolBulkhead.getName().contains(CONTEXT_ID_1)) {
                Assert.assertEquals(threadPoolBulkhead.getBulkheadConfig().getCoreThreadPoolSize(), DEFAULT_THREAD_POOL_SIZE);
                Assert.assertEquals(threadPoolBulkhead.getBulkheadConfig().getMaxThreadPoolSize(), DEFAULT_THREAD_POOL_SIZE);
            } else if (threadPoolBulkhead.getName().contains(CONTEXT_ID_2)) {
                Assert.assertEquals(threadPoolBulkhead.getBulkheadConfig().getCoreThreadPoolSize(), TEST_SERVICE_2_THREAD_POOL_SIZE);
                Assert.assertEquals(threadPoolBulkhead.getBulkheadConfig().getMaxThreadPoolSize(), TEST_SERVICE_2_THREAD_POOL_SIZE);
            }
        });
    }

    /**
     * 验证配置生效
     */
    @Test
    public void testConfigureCircuitBreaker() {
        testService1Client.anything();
        testService2Client.anything();
        List<CircuitBreaker> circuitBreakers = circuitBreakerRegistry.getAllCircuitBreakers().asJava();
        Set<String> collect = circuitBreakers.stream().map(CircuitBreaker::getName)
                .filter(name -> {
                    try {
                        return name.contains(TestService1Client.class.getMethod("anything").toGenericString())
                                || name.contains(TestService1Client.class.getMethod("anything").toGenericString());
                    } catch (NoSuchMethodException e) {
                        return false;
                    }
                }).collect(Collectors.toSet());
        Assert.assertEquals(collect.size(), 2);
        circuitBreakers.forEach(circuitBreaker -> {
            if (circuitBreaker.getName().contains(TestService1Client.class.getName())) {
                Assert.assertEquals((int) circuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold(), (int) DEFAULT_FAILURE_RATE_THRESHOLD);
                Assert.assertEquals(circuitBreaker.getCircuitBreakerConfig().getMinimumNumberOfCalls(), DEFAULT_MINIMUM_NUMBER_OF_CALLS);
            } else if (circuitBreaker.getName().contains(TestService2Client.class.getName())) {
                Assert.assertEquals((int) circuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold(), (int) TEST_SERVICE_2_FAILURE_RATE_THRESHOLD);
                Assert.assertEquals(circuitBreaker.getCircuitBreakerConfig().getMinimumNumberOfCalls(), TEST_SERVICE_2_MINIMUM_NUMBER_OF_CALLS);
            }
        });
    }

    @Test
    public void testCircuitBreakerOpenBasedOnServiceAndMethod() {
        for (int i = 0; i < 2; i++) {
            try {
                System.out.println(testService1Client.testCircuitBreakerStatus500());
            } catch (Exception e) {
                log.error("{}", e.getMessage());
            }
        }
        circuitBreakerRegistry.getAllCircuitBreakers().asJava().forEach(circuitBreaker -> {
            try {
                if (circuitBreaker.getName().contains(TestService1Client.class.getMethod("testCircuitBreakerStatus500").toGenericString())) {
                    Assert.assertEquals(circuitBreaker.getState(), CircuitBreaker.State.OPEN);
                }
            } catch (NoSuchMethodException e) {

            }
        });
    }

    /**
     * 验证配置生效
     */
    @Test
    public void testConfigureRetry() {
        testService1Client.anything();
        testService2Client.anything();
        List<Retry> retries = retryRegistry.getAllRetries().asJava();
        Set<String> collect = retries.stream().map(Retry::getName)
                .filter(name -> name.contains(CONTEXT_ID_1)
                        || name.contains(CONTEXT_ID_2)).collect(Collectors.toSet());
        Assert.assertEquals(collect.size(), 2);
        retries.forEach(retry -> {
            if (retry.getName().contains(CONTEXT_ID_1)) {
                Assert.assertEquals(retry.getRetryConfig().getMaxAttempts(), DEFAULT_RETRY);
            } else if (retry.getName().contains(CONTEXT_ID_2)) {
                Assert.assertEquals(retry.getRetryConfig().getMaxAttempts(), TEST_SERVICE_2_RETRY);
            }
        });
    }

    @Aspect
    public static class ApacheHttpClientAop {
        //在最后一步 ApacheHttpClient 切面
        @Pointcut("execution(* com.github.hashjang.spring.cloud.iiford.service.common.feign.ApacheHttpClient.execute(..))")
        public void annotationPointcut() {
        }

        @Around("annotationPointcut()")
        public Object around(ProceedingJoinPoint pjp) throws Throwable {
            //调用次数加1
            callCount += 1;
            //设置 Header，不能通过 Feign 的 RequestInterceptor，因为我们要拿到最后调用 ApacheHttpClient 的线程上下文
            Request request = (Request) pjp.getArgs()[0];
            Field headers = ReflectionUtils.findField(Request.class, "headers");
            ReflectionUtils.makeAccessible(headers);
            Map<String, Collection<String>> map = (Map<String, Collection<String>>) ReflectionUtils.getField(headers, request);
            HashMap<String, Collection<String>> stringCollectionHashMap = new HashMap<>(map);
            stringCollectionHashMap.put(THREAD_ID_HEADER, List.of(String.valueOf(Thread.currentThread().getId())));
            ReflectionUtils.setField(headers, request, stringCollectionHashMap);
            return pjp.proceed();
        }
    }

    public static class TestService1ClientConfiguration {
        @Bean
        public FeignDecoratorBuilderInterceptor feignDecoratorBuilderInterceptor(
                TestService1ClientFallback testService1ClientFallback
        ) {
            return builder -> {
                builder.withFallback(testService1ClientFallback);
            };
        }

        @Bean
        public TestService1ClientFallback testService1ClientFallback() {
            return new TestService1ClientFallback();
        }
    }
}
