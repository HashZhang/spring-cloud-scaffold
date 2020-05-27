package com.github.hashjang.hoxton.service.consumer.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;

@FeignClient(value = "service-provider2")
public interface ServiceProvider2TestFeignCleint {
    @RequestMapping(value = "/test-simple", method = RequestMethod.GET)
    String testGet(@RequestBody Map<String, String> map);

    @RequestMapping(value = "/test-simple", method = RequestMethod.POST)
    String testPost(@RequestBody Map<String, String> map);

    @RequestMapping(value = "/test-simple", method = RequestMethod.PUT)
    String testPut(@RequestBody Map<String, String> map);

    @RequestMapping(value = "/test-simple", method = RequestMethod.DELETE)
    String testDelete(@RequestBody Map<String, String> map);
}
