package com.github.hashjang.spring.cloud.iiford.service.discovery.simple;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.client.discovery.simple.reactive.SimpleReactiveDiscoveryClient;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * 通过消费 ApplicationReadyEvent 来确保 DiscoveryClient 初始化完成并可用
 */
@Slf4j
@Component
public class TestSimpleReactiveDiscoveryClient implements ApplicationListener<ApplicationReadyEvent> {
    @Autowired
    private SimpleReactiveDiscoveryClient simpleReactiveDiscoveryClient;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        simpleReactiveDiscoveryClient.getServices().subscribe(serviceId -> {
            simpleReactiveDiscoveryClient.getInstances(serviceId).collect(Collectors.toList()).subscribe(serviceInstances -> {
                log.info("{}: {}", serviceId, serviceInstances);
            });
        });
    }
}
