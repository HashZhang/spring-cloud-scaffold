package com.github.hashjang.spring.cloud.iiford.service.common.auto;

import com.github.hashjang.spring.cloud.iiford.service.common.undertow.WebServerConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import(WebServerConfiguration.class)
public class UndertowAutoConfiguration {
}
