package com.share.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.share")
@SpringBootApplication
public class SharePaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(SharePaymentApplication.class, args);
    }

}
