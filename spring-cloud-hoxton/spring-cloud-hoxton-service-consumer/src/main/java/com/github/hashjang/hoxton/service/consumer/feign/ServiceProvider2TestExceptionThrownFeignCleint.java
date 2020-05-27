package com.github.hashjang.hoxton.service.consumer.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient(value = "service-provider2")
public interface ServiceProvider2TestExceptionThrownFeignCleint {
    @RequestMapping(value = "/test-exception-thrown", method = RequestMethod.GET)
    String testExceptionThrownGet();

    @RequestMapping(value = "/test-exception-thrown", method = RequestMethod.POST)
    String testExceptionThrownPost();

    @RequestMapping(value = "/test-exception-thrown", method = RequestMethod.PUT)
    String testExceptionThrownPut();

    @RequestMapping(value = "/test-exception-thrown", method = RequestMethod.DELETE)
    String testExceptionThrownDelete();
}
