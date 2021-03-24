package com.github.hashjang.iiford.service.consumer.feign;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class TestFeignClients implements ApplicationListener<ApplicationReadyEvent> {
    @Autowired
    private ServiceProviderClient serviceProviderClient;

    @SneakyThrows
    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
            try {
                serviceProviderClient.test(Map.of());
            } catch (Exception e) {
                log.error("error: {}", e.toString());
            }
    }
}
