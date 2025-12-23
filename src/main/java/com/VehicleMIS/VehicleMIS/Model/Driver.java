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
@Document(collection = "Drivers")
public class Driver {
    @Id
    private String driverId;
    private String name;
    private String phoneNumber;
    private String licenseNumber;
    private String nic;
    private String address;
    private String email;
    private String status;
    private String emergencyContact;
    private LocalDate licenseExpiryDate;

    private boolean blocked = false;
}
