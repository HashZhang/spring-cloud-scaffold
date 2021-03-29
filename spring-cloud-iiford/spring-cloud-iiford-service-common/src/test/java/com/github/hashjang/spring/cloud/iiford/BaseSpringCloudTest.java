package com.github.hashjang.spring.cloud.iiford;

import com.github.hashjang.spring.cloud.iiford.service.common.undertow.DefaultWebServerFactoryCustomizer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.embedded.undertow.ConfigurableUndertowWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = { "" })
public class BaseSpringCloudTest {
    @EnableAutoConfiguration
    @Configuration(proxyBeanMethods = false)
    public static class App {
    }

    @Autowired
    private List<WebServerFactoryCustomizer<ConfigurableUndertowWebServerFactory>> webServerFactoryCustomizers;

    @Test
    public void veryifyBeans() {
        then(this.webServerFactoryCustomizers).isNull();
    }
}
