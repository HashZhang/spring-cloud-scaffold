package com.github.hashjang.spring.cloud.iiford.service.common.undertow;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.BDDAssertions.then;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = { "server.undertow.accesslog.pattern=%D" })
public class TestAccessLog implements ApplicationContextAware {
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        System.out.println();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    public static class SampleConfig {
    }

    @Autowired
    private DefaultWebServerFactoryCustomizer defaultWebServerFactoryCustomizer;

    @Test
    public void testLogContainsResponseTime() throws NoSuchFieldException {
        then(this.defaultWebServerFactoryCustomizer).isNotNull();
    }
}
