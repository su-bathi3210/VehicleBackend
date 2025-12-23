package com.VehicleMIS.VehicleMIS.Model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "Vehicles")
public class Vehicle {
    @Id
    private String vehicleId;

    private String vehicleNumber;
    private String vehicleType;
    private String manufacturer;
    private String model;
    private String status;

    private String licenseNumber;
    private LocalDate licenseIssueDate;
    private LocalDate licenseExpiryDate;

    private boolean warning30Sent = false;
    private boolean warning14Sent = false;
    private boolean warning7Sent = false;
    private boolean isBlocked = false;

}
