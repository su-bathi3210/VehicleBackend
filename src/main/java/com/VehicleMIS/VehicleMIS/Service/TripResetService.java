package com.VehicleMIS.VehicleMIS.Service;

import com.VehicleMIS.VehicleMIS.Model.Vehicle;
import com.VehicleMIS.VehicleMIS.Model.Driver;
import com.VehicleMIS.VehicleMIS.Model.VehicleRequest;
import com.VehicleMIS.VehicleMIS.Repository.VehicleRepository;
import com.VehicleMIS.VehicleMIS.Repository.DriverRepository;
import com.VehicleMIS.VehicleMIS.Repository.VehicleRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class TripResetService {

    @Autowired
    private VehicleRequestRepository requestRepo;

    @Autowired
    private VehicleRepository vehicleRepo;

    @Autowired
    private DriverRepository driverRepo;

    @Scheduled(cron = "0 * * * * ?")
    public void resetTripStatus() {
        ZoneId zone = ZoneId.of("Asia/Colombo");
        LocalDate today = LocalDate.now(zone);
        LocalDate yesterday = today.minusDays(1);

        List<VehicleRequest> allRequests = requestRepo.findAll();

        for (VehicleRequest request : allRequests) {
            Date travelDateTime = request.getTravelDateTime();
            if (travelDateTime != null) {
                LocalDate travelDate = Instant.ofEpochMilli(travelDateTime.getTime())
                        .atZone(zone)
                        .toLocalDate();

                System.out.println("🔍 Checking: " + request.getRequestId() +
                        " | TravelDate: " + travelDate +
                        " | Today: " + today +
                        " | Status: " + request.getStatus());

                if (travelDate.isEqual(today)) {
                    markTripAsOngoing(request);
                }

                else if (travelDate.isEqual(yesterday)) {
                    completeTrip(request);
                }
            }
        }

        System.out.println("✅ TripResetService executed at: " + new Date());
    }

    private void markTripAsOngoing(VehicleRequest request) {
        if (!"ON_GOING_TRIP".equalsIgnoreCase(request.getStatus())) {
            request.setStatus("ON_GOING_TRIP");
            requestRepo.save(request);
            System.out.println("🟢 Trip " + request.getRequestId() + " marked as ON_GOING_TRIP");
        }
    }

    private void completeTrip(VehicleRequest request) {
        if (!"COMPLETED".equalsIgnoreCase(request.getStatus())) {

            if (request.getAssignedVehicleId() != null) {
                Optional<Vehicle> vehicleOpt = vehicleRepo.findById(request.getAssignedVehicleId());
                vehicleOpt.ifPresent(vehicle -> {
                    vehicle.setStatus("Available");
                    vehicleRepo.save(vehicle);
                });
            }

            if (request.getAssignedDriverId() != null) {
                Optional<Driver> driverOpt = driverRepo.findById(request.getAssignedDriverId());
                driverOpt.ifPresent(driver -> {
                    driver.setStatus("Available");
                    driverRepo.save(driver);
                });
            }

            request.setStatus("COMPLETED");
            requestRepo.save(request);
            System.out.println("✅ Trip " + request.getRequestId() + " marked as COMPLETED");
        }
    }
}
