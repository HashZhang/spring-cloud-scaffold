package com.github.hashjang.hoxton.api.gateway.filter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * 加解密filter，解密修改requestbody，加密修改responsebody
 */
@Log4j2
@Component
public class EncryptFilter extends AbstractSpecificPathFilter {
    private final List<HttpMessageReader<?>> messageReaders = HandlerStrategies.withDefaults().messageReaders();

    @Override
    protected Mono<Void> filter0(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders headers = request.getHeaders();
        ServerHttpResponse response = exchange.getResponse();
        DataBufferFactory bufferFactory = response.bufferFactory();
        if (headers.getContentLength() == 0) {
            return FilterUtil.errorResponse(response, HttpStatus.BAD_REQUEST, bufferFactory, "");
        }
        return decrypt0(exchange).flatMap(decryptResult -> {
            log.info("decrypt data: {}", decryptResult);
            if (decryptResult.isSuccessful()) {
                String result = decryptResult.getResult();
                return chain.filter(exchange.mutate().request(
                        new ServerHttpRequestDecorator(request) {
                            @Override
                            public HttpHeaders getHeaders() {
                                HttpHeaders httpHeaders = new HttpHeaders();
                                httpHeaders.putAll(headers);

                                httpHeaders.remove(HttpHeaders.CONTENT_LENGTH);
                                httpHeaders.remove(HttpHeaders.CONTENT_TYPE);

                                httpHeaders.set(HttpHeaders.TRANSFER_ENCODING, "chunked");
                                //对于{或者是[开头的，认为是json
                                if (result.trim().startsWith("{") || result.trim().startsWith("[")) {
                                    httpHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                                } else {
                                    //否则就是表单
                                    httpHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
                                }
                                return httpHeaders;
                            }

                            @Override
                            public Flux<DataBuffer> getBody() {
                                return Flux.from(
                                        Mono.just(bufferFactory.wrap(result.getBytes()))
                                );
                            }
                        }
                ).response(new ServerHttpResponseDecorator(response) {
                    @Override
                    public HttpHeaders getHeaders() {
                        HttpHeaders responseHeaders = super.getHeaders();
                        responseHeaders.remove(HttpHeaders.CONTENT_LENGTH);
                        responseHeaders.set(HttpHeaders.TRANSFER_ENCODING, "chunked");
                        return responseHeaders;
                    }

                    @Override
                    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                        if (body instanceof Flux) {
                            //有TCP粘包拆包问题，这个body是多次写入的，一次调用拿不到完整的body，所以这里转换成fluxBody利用其中的buffer来接受完整的body
                            Flux<? extends DataBuffer> fluxBody = Flux.from(body);
                            return super.writeWith(fluxBody.buffer().map(buffers -> {
                                DataBuffer buffer = bufferFactory.join(buffers);
                                try {
                                    String s = FilterUtil.dataBufferToString(buffer);
                                    log.info("encrypt response: {}", s);
                                    byte[] uppedContent = encrypt0(exchange, s, decryptResult.getKey()).getBytes();
                                    return bufferFactory.wrap(uppedContent);
                                } catch (Exception e) {
                                    log.error("error while encrypt response: {}", e.getMessage(), e);
                                }
                                return null;
                            }));
                        }
                        // if body is not a flux. never got there.
                        return super.writeWith(body);
                    }

                    @Override
                    public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
                        return super.writeAndFlushWith(body);
                    }
                }).build());
            } else {
                return FilterUtil.errorResponse(response, HttpStatus.BAD_REQUEST, bufferFactory, "encrypt failed");
            }
        });
    }

    private Mono<DecryptResult> decrypt0(ServerWebExchange exchange) {
        ServerRequest serverRequest = ServerRequest.create(exchange,
                messageReaders);
        return serverRequest.bodyToMono(Map.class).flatMap(map -> {
            Object test = map.get("test");
            Object key = map.get("key");
            return Mono.just(DecryptResult.builder().key(key.toString())
                    .result(FilterUtil.toJsonString(Map.ofEntries(
                            Map.entry("decrypted", test)
                    )))
                    .successful(true)
                    .build()
            );
        });
    }

    private String encrypt0(ServerWebExchange exchange, String body, String key) throws Exception {
        return body + " - " + key;
    }

    @Override
    protected Map<String, List<HttpMethod>> getPaths() {
        return Map.ofEntries(
                Map.entry("/service-provider/test-simple", List.of(HttpMethod.POST))
        );
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    protected static class DecryptResult {
        private boolean successful;
        private String result;
        private String key;
    }
}
