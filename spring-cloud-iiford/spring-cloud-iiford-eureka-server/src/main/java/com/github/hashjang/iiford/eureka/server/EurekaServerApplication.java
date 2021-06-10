package com.github.hashjang.iiford.eureka.server;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.ClientFilter;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.cloud.netflix.eureka.server.ReplicationClientAdditionalFilters;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
@EnableEurekaServer
@Log4j2
public class EurekaServerApplication {
    @Bean
    public ReplicationClientAdditionalFilters replicationClientAdditionalFilters() {
        return new ReplicationClientAdditionalFilters(List.of(new ClientFilter() {
            @Override
            public ClientResponse handle(ClientRequest clientRequest) throws ClientHandlerException {
                log.info("client req: {}", clientRequest.getURI());
                ClientResponse response = this.getNext().handle(clientRequest);
                log.info("client res: {}", response);
                return response;
            }
        }));
    }

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
