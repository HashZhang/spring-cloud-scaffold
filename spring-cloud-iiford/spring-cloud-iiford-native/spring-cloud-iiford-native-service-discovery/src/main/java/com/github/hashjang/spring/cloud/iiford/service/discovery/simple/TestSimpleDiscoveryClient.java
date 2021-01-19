package com.github.hashjang.spring.cloud.iiford.service.discovery.simple;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClient;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

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
    private SimpleDiscoveryClient simpleDiscoveryClient;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        List<String> services = simpleDiscoveryClient.getServices();
        services.forEach(serviceId -> {
            log.info("{}: {}", serviceId, simpleDiscoveryClient.getInstances(serviceId));
        });
    }
}
