package com.VehicleMIS.VehicleMIS.Model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "VehicleServiceRecords")
public class VehicleServiceRecord {

    @Id
    private String id;
    private String serviceRecordId;
    private String vehicleId;
    private String vehicleNumber;
    private String serviceType;
    private String garageName;
    private double cost;
    private LocalDate serviceDate;
    private String remarks;

    private double currentMileage = 0.0;
    private double nextServiceMileage = 0.0;
    private double serviceInterval = 6000.0;
    private int serviceCount = 0;

    private List<Double> mileageHistory = new ArrayList<>();

}
