package com.github.hashjang.hoxton.api.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import reactor.core.publisher.Mono;

import java.io.IOException;

public class FilterUtil {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static String toJsonString(Object o) {
        try {
            return OBJECT_MAPPER.writeValueAsString(o);
        } catch (JsonProcessingException e) {
        }
        return null;
    }

    public static Mono<Void> errorResponse(ServerHttpResponse response, HttpStatus httpStatus, DataBufferFactory dataBufferFactory, String msg) {
        response.setStatusCode(httpStatus);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return response.writeWith(Mono.just(
                dataBufferFactory.wrap(
                        msg.getBytes()
                )
        ));
    }

    public static String dataBufferToString(DataBuffer dataBuffer) throws IOException {
        byte[] content = new byte[dataBuffer.readableByteCount()];
        try {
            dataBuffer.read(content);
        } finally {
            DataBufferUtils.release(dataBuffer);
        }
        return new String(content);
    }
}
