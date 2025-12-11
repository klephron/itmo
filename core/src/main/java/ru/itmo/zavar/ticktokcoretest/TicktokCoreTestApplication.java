package ru.itmo.zavar.ticktokcoretest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableDiscoveryClient
@EnableFeignClients(basePackages = "ru.itmo.zavar.ticktokcoretest")
@SpringBootApplication(scanBasePackages = "ru.itmo.zavar.ticktokcoretest")
public class TicktokCoreTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicktokCoreTestApplication.class, args);
    }

}
