package com.github.hashjang.iiford.service.consumer.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@FeignClient(name = "service-provider", contextId = "ServiceProviderClient")
public interface ServiceProviderClient {
    @GetMapping(value = "/test-exception")
    String test(@RequestBody Map<String, String> body);
}
