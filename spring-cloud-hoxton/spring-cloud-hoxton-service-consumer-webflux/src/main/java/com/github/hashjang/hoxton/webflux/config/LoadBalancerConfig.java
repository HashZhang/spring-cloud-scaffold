package com.github.hashjang.hoxton.webflux.config;

import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@LoadBalancerClients(defaultConfiguration = {CommonLoadBalancerConfig.class})
@EnableFeignClients(basePackages = "com.github.hashjang.hoxton")
public class LoadBalancerConfig {
}
