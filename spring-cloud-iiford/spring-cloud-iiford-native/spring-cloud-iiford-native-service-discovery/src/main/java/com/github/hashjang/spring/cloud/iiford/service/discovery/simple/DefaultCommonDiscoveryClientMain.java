package com.github.hashjang.spring.cloud.iiford.service.discovery.simple;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClient;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClient;
import org.springframework.cloud.client.discovery.simple.reactive.SimpleReactiveDiscoveryClient;
import org.springframework.context.ApplicationListener;

import javax.annotation.Resource;
import java.util.List;

@SpringBootApplication
public class DefaultCommonDiscoveryClientMain implements ApplicationListener<ApplicationReadyEvent> {
    /**
     * 初始化的方法返回类型是 DiscoveryClient 并且不是 Primary，这里只能通过 @Resource 自动装载不能通过 @Autowired
     *
     * @see org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientAutoConfiguration
     */
    @Resource
    private SimpleDiscoveryClient simpleDiscoveryClient;
    @Autowired
    private CompositeDiscoveryClient compositeDiscoveryClient;
    @Autowired
    private DiscoveryClient discoveryClient;
    @Autowired
    private List<DiscoveryClient> discoveryClients;
    @Autowired
    private SimpleReactiveDiscoveryClient simpleReactiveDiscoveryClient;
    @Autowired
    private ReactiveDiscoveryClient reactiveDiscoveryClient;
    @Autowired
    private List<ReactiveDiscoveryClient> reactiveDiscoveryClients;

    public static void main(String[] args) {
        new SpringApplicationBuilder();
        SpringApplication.run(DefaultCommonDiscoveryClientMain.class, args);
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        List<String> services = simpleDiscoveryClient.getServices();
        services.forEach(serviceId -> {
            System.out.println(serviceId + ": " + simpleDiscoveryClient.getInstances(serviceId));
        });
    }
}
