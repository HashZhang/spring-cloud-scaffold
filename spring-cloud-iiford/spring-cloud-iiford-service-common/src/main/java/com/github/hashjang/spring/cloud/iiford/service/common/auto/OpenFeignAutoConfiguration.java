package com.github.hashjang.spring.cloud.iiford.service.common.auto;

import com.github.hashjang.spring.cloud.iiford.service.common.config.CommonOpenFeignConfiguration;
import com.github.hashjang.spring.cloud.iiford.service.common.config.DefaultOpenFeignConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import(CommonOpenFeignConfiguration.class)
@EnableFeignClients(value = "com.github.hashjang", defaultConfiguration = DefaultOpenFeignConfiguration.class)
public class OpenFeignAutoConfiguration {
}
