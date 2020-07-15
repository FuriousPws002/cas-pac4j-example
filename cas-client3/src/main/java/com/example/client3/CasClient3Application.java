package com.example.client3;

import org.jasig.cas.client.boot.configuration.EnableCasClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableCasClient
@SpringBootApplication
public class CasClient3Application {

    public static void main(String[] args) {
        SpringApplication.run(CasClient3Application.class, args);
    }

}
