package org.zy.rpc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.zy.rpc.annotation.EnableProviderRpc;

@SpringBootApplication
@EnableProviderRpc
public class RpcProvider1Application {

    public static void main(String[] args) {
        SpringApplication.run(RpcProvider1Application.class, args);
    }

}
