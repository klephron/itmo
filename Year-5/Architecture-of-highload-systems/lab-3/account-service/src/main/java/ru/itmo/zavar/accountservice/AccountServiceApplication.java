package ru.itmo.zavar.accountservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.itmo.zavar.accountservice.consul.ConsulClient;

@SpringBootApplication
public class AccountServiceApplication {

    private static String consulHost = "localhost";
    private static int consulPort = 8500;

    public static void main(String[] args) {
        String replicaId = args[args.length - 1];
        String publicAddress = args[args.length - 2];
        if (System.getenv("CONSUL_HOST") != null) {
            consulHost = System.getenv("CONSUL_HOST");
        }
        if (System.getenv("CONSUL_PORT") != null) {
            consulPort = Integer.parseInt(System.getenv("CONSUL_PORT"));
        }
        String consulUrl = "http://" + consulHost + ":" + consulPort;
        ConsulClient consulClient = new ConsulClient(consulUrl, publicAddress, 8082, "account-service", replicaId);
        consulClient.register();

        Runtime.getRuntime().addShutdownHook(new Thread(consulClient::deregister));

        SpringApplication.run(AccountServiceApplication.class, args);
    }

}
