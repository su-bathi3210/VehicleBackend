package com.VehicleMIS.VehicleMIS.Config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "notifylk")
public class NotifyConfig {
    private String userId;
    private String apiKey;
    private String senderId;

}
