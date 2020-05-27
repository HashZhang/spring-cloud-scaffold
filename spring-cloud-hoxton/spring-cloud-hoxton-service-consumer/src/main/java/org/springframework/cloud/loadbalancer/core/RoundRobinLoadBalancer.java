/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.loadbalancer.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.reactive.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.reactive.Request;
import org.springframework.cloud.client.loadbalancer.reactive.Response;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A Round-Robin-based implementation of {@link org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer}.
 * 同名同路径类，用于替换框架中的类
 * 框架中的RoundRobinLoadBalancer可能会导致在同一个请求重试的时候，两次重试返回的都是同一个实例
 * 参考我提的issues：https://github.com/spring-cloud/spring-cloud-commons/issues/747
 * 所以修改position为threadLocal的
 *
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 */
public class RoundRobinLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    private static final Log log = LogFactory.getLog(RoundRobinLoadBalancer.class);

    private final ConcurrentHashMap<Long, Integer> position;
    private final String serviceId;
    @Deprecated
    private ObjectProvider<ServiceInstanceSupplier> serviceInstanceSupplier;
    private ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;

    /**
     * @param serviceId               id of the service for which to choose an instance
     * @param serviceInstanceSupplier a provider of {@link org.springframework.cloud.loadbalancer.core.ServiceInstanceSupplier} that
     *                                will be used to get available instances
     * @deprecated Use {@link #RoundRobinLoadBalancer(org.springframework.beans.factory.ObjectProvider, String)}} instead.
     */
    @Deprecated
    public RoundRobinLoadBalancer(String serviceId,
                                  ObjectProvider<ServiceInstanceSupplier> serviceInstanceSupplier) {
        this(serviceId, serviceInstanceSupplier, new Random().nextInt(1000));
    }

    /**
     * @param serviceId               id of the service for which to choose an instance
     * @param serviceInstanceSupplier a provider of {@link org.springframework.cloud.loadbalancer.core.ServiceInstanceSupplier} that
     *                                will be used to get available instances
     * @param seedPosition            Round Robin element position marker
     * @deprecated Use {@link #RoundRobinLoadBalancer(org.springframework.beans.factory.ObjectProvider, String, int)}}
     * instead.
     */
    @Deprecated
    public RoundRobinLoadBalancer(String serviceId,
                                  ObjectProvider<ServiceInstanceSupplier> serviceInstanceSupplier,
                                  int seedPosition) {
        this.serviceId = serviceId;
        this.serviceInstanceSupplier = serviceInstanceSupplier;
        this.position = new ConcurrentHashMap<>();
    }

    /**
     * @param serviceInstanceListSupplierProvider a provider of
     *                                            {@link org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier} that will be used to get available instances
     * @param serviceId                           id of the service for which to choose an instance
     */
    public RoundRobinLoadBalancer(
            ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,
            String serviceId) {
        this(serviceInstanceListSupplierProvider, serviceId, new Random().nextInt(1000));
    }

    /**
     * @param serviceInstanceListSupplierProvider a provider of
     *                                            {@link org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier} that will be used to get available instances
     * @param serviceId                           id of the service for which to choose an instance
     * @param seedPosition                        Round Robin element position marker
     */
    public RoundRobinLoadBalancer(
            ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,
            String serviceId, int seedPosition) {
        this.serviceId = serviceId;
        this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
        this.position = new ConcurrentHashMap<>();
    }

    @SuppressWarnings("rawtypes")
    @Override
    // see original
    // https://github.com/Netflix/ocelli/blob/master/ocelli-core/
    // src/main/java/netflix/ocelli/loadbalancer/RoundRobinLoadBalancer.java
    public Mono<Response<ServiceInstance>> choose(Request request) {
        // TODO: move supplier to Request?
        // Temporary conditional logic till deprecated members are removed.
        long threadId = Thread.currentThread().getId();
        if (serviceInstanceListSupplierProvider != null) {
            ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider
                    .getIfAvailable(NoopServiceInstanceListSupplier::new);
            return supplier.get().next().map(serviceInstances -> getInstanceResponse(serviceInstances, threadId));
        }
        ServiceInstanceSupplier supplier = this.serviceInstanceSupplier
                .getIfAvailable(NoopServiceInstanceSupplier::new);
        return supplier.get().collectList().map(serviceInstances -> getInstanceResponse(serviceInstances, threadId));
    }

    private Response<ServiceInstance> getInstanceResponse(
            List<ServiceInstance> instances, long threadId) {
        if (instances.isEmpty()) {
            log.warn("No servers available for service: " + this.serviceId);
            return new EmptyResponse();
        }
        int pos = Math.abs(this.position.computeIfAbsent(threadId, k -> ThreadLocalRandom.current().nextInt(1000)));
        this.position.put(threadId, pos + 1);
        ServiceInstance instance = instances.get(pos % instances.size());
        return new DefaultResponse(instance);
    }

}
