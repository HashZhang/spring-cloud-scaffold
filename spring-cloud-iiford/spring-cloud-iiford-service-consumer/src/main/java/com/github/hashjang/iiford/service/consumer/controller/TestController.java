package com.github.hashjang.iiford.service.consumer.controller;

import com.github.hashjang.iiford.service.consumer.feign.ServiceProviderClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
public class TestController {
    @Autowired
    private ServiceProviderClient serviceProviderClient;

    @GetMapping("/test")
    public void test() {
        serviceProviderClient.test(Map.of());
    }
}
