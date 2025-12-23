package com.VehicleMIS.VehicleMIS;

import com.VehicleMIS.VehicleMIS.Config.NotifyConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(NotifyConfig.class)
@EnableScheduling
public class VehicleMisApplication {
    public static void main(String[] args) {
        SpringApplication.run(VehicleMisApplication.class, args);
    }
}