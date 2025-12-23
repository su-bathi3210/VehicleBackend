package com.VehicleMIS.VehicleMIS.Model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "VehicleRequests")
public class VehicleRequest {

    @Id
    private String requestId;

    private String requesterName;
    private String requesterPosition;
    private String travelerName;
    private String travelerPosition;
    private String department;

    private String phoneNumber;
    private String dutyNature;

    private String fromLocation;
    private String toLocation;
    private Double distanceKm;

    private Date travelDateTime;
    private String reason;

    private LocalDateTime submittedAt;
    private LocalDateTime vehicleAssignedAt;
    private LocalDateTime officerApprovedAt;
    private LocalDateTime driverAssignedAt;
    private LocalDateTime tripCompletedAt;

    private String assignedVehicleId;
    private String assignedDriverId;

    private String status;

    private LocalDateTime cancelledAt;
    private boolean cancelled = false;
    private String cancellationReason;

    private boolean cancellationPending = false; // employee requested cancellation
    private String cancellationDecision; // "APPROVED" or "REJECTED"
    private LocalDateTime cancellationReviewedAt; // admin's decision time

}
