package com.github.hashjang.spring.cloud.iiford.service.common.undertow;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.embedded.undertow.ConfigurableUndertowWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class WebServerConfiguration {
    @Bean
    public WebServerFactoryCustomizer<ConfigurableUndertowWebServerFactory> undertowWebServerAccessLogTimingEnabler(ServerProperties serverProperties) {
        return new DefaultWebServerFactoryCustomizer(serverProperties);
    }
}
