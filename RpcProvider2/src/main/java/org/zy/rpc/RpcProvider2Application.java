package org.zy.rpc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.zy.rpc.annotation.EnableProviderRpc;

@SpringBootApplication
@EnableProviderRpc
public class RpcProvider2Application {

    public static void main(String[] args) {
        SpringApplication.run(RpcProvider2Application.class, args);
    }

}
