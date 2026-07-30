package com.ecvs.overrideload;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OverrideLoadApplication {

    public static void main(String[] args) {
        SpringApplication.run(OverrideLoadApplication.class, args);
    }
}
