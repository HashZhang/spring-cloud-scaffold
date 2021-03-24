package com.github.hashjang.alibaba.service.provider.discovery;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 通过消费 ApplicationReadyEvent 来确保 DiscoveryClient 初始化完成并可用
 */
@Slf4j
@Component
public class TestSimpleDiscoveryClient implements ApplicationListener<ApplicationReadyEvent> {


    /**
     * 初始化的方法返回类型是 DiscoveryClient 并且不是 Primary，这里只能通过 @Resource 自动装载不能通过 @Autowired
     * 这里不排除以后返回类型修改为 SimpleDiscoveryClient 的可能性
     *
     * @see org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientAutoConfiguration
     */
    @Resource
    private DiscoveryClient discoveryClient;
    @Autowired
    private LoadBalancerClient loadBalancerClient;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        List<String> services = discoveryClient.getServices();
        services.forEach(serviceId -> {
            log.info("{}: {}", serviceId, discoveryClient.getInstances(serviceId).stream().map(serviceInstance -> serviceInstance.getHost() + ":" + serviceInstance.getPort()).collect(Collectors.joining()));
        });
    }
}
