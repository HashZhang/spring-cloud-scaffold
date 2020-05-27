package com.github.hashjang.hoxton.service.consumer.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient(value = "service-provider2")
public interface ServiceProvider2TestReadTimeoutFeignCleint {
    @RequestMapping(value = "/test-read-time-out", method = RequestMethod.GET)
    String testTimeoutGet();

    @RequestMapping(value = "/test-read-time-out", method = RequestMethod.POST)
    String testTimeoutPost();

    @RequestMapping(value = "/test-read-time-out", method = RequestMethod.PUT)
    String testTimeoutPut();

    @RequestMapping(value = "/test-read-time-out", method = RequestMethod.DELETE)
    String testTimeoutDelete();
}
