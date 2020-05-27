package com.github.hashjang.hoxton.service.consumer.config;

import brave.Tracer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.loadbalancer.cache.LoadBalancerCacheManager;
import org.springframework.cloud.loadbalancer.config.LoadBalancerZoneConfig;
import org.springframework.cloud.loadbalancer.core.CachingServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.DiscoveryClientServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

public class CommonLoadBalancerConfig {

    private volatile boolean isValid = false;

    /**
     * 同步环境下的ServiceInstanceListSupplier
     * SameZoneOnlyServiceInstanceListSupplier限制仅返回同一个zone下的实例（注意）
     * CachingServiceInstanceListSupplier启用缓存，不每次访问eureka请求实例列表
     *
     * @param discoveryClient
     * @param env
     * @param zoneConfig
     * @param context
     * @return
     */
    @Bean
    @Order(Integer.MIN_VALUE)
    public ServiceInstanceListSupplier discoveryClientServiceInstanceListSupplier(
            DiscoveryClient discoveryClient, Environment env,
            LoadBalancerZoneConfig zoneConfig,
            ApplicationContext context) {
        isValid = true;
        ServiceInstanceListSupplier delegate = new SameZoneOnlyServiceInstanceListSupplier(
                new DiscoveryClientServiceInstanceListSupplier(discoveryClient, env),
                zoneConfig
        );
        ObjectProvider<LoadBalancerCacheManager> cacheManagerProvider = context
                .getBeanProvider(LoadBalancerCacheManager.class);
        if (cacheManagerProvider.getIfAvailable() != null) {
            return new CachingServiceInstanceListSupplier(
                    delegate,
                    cacheManagerProvider.getIfAvailable()
            );
        }
        return delegate;
    }

    @Bean
    public ReactorLoadBalancer<ServiceInstance> reactorServiceInstanceLoadBalancer(
            Environment environment,
            ServiceInstanceListSupplier serviceInstanceListSupplier,
            Tracer tracer) {
        if (!isValid) {
            throw new IllegalStateException("should use the ServiceInstanceListSupplier in this configuration, please check config");
        }
        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new RoundRobinBaseOnTraceIdLoadBalancer(
                name,
                serviceInstanceListSupplier,
                tracer
        );
    }
}
