package ru.itmo.zavar.account.consul;

import com.orbitz.consul.Consul;
import com.orbitz.consul.model.agent.ImmutableRegistration;

public class ConsulClient {
    private final Consul consul;

    private final String publicAddress;
    private final int port;

    private final String serviceName;
    private final String replicaId;

    public ConsulClient(String consulUrl, String publicAddress, int port, String serviceName, String replicaId) {
        this.consul = Consul.builder().withUrl(consulUrl).build();
        this.publicAddress = publicAddress;
        this.port = port;
        this.serviceName = serviceName;
        this.replicaId = replicaId;
    }

    public void register() {
        this.consul.agentClient().register(ImmutableRegistration.builder()
                .id(this.getServiceId())
                .name(this.serviceName)
                .address(this.publicAddress)
                .port(port)
                .addChecks(ImmutableRegistration.RegCheck
                        .http("http://" + publicAddress + ":" + port + "/actuator/prometheus", 10, 5))
                .build());
    }

    public void deregister() {
        this.consul.agentClient().deregister(this.getServiceId());
    }

    private String getServiceId() {
        return this.serviceName + "-" + this.replicaId;
    }

}
