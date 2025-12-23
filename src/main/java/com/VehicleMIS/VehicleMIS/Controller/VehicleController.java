package com.VehicleMIS.VehicleMIS.Controller;

import com.VehicleMIS.VehicleMIS.DTO.LicenseDto;
import com.VehicleMIS.VehicleMIS.Model.Vehicle;
import com.VehicleMIS.VehicleMIS.Repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/vehicles")
@CrossOrigin(origins = "http://localhost:5173")
public class VehicleController {

    @Autowired
    private VehicleRepository vehicleRepo;

    private void applyExpiryStatus(Vehicle vehicle) {
        if (vehicle.getLicenseExpiryDate() != null) {
            LocalDate today = LocalDate.now(ZoneId.of("Asia/Colombo"));
            if (!vehicle.getLicenseExpiryDate().isAfter(today)) {
                if (!"Expired".equalsIgnoreCase(vehicle.getStatus())) {
                    vehicle.setStatus("Expired");
                    vehicle.setBlocked(true);
                    vehicleRepo.save(vehicle);
                }
            } else {
                if ("Expired".equalsIgnoreCase(vehicle.getStatus())) {
                    vehicle.setBlocked(false);
                    vehicle.setStatus("Available");
                    vehicleRepo.save(vehicle);
                }
            }
        }
    }

    @GetMapping
    public ResponseEntity<List<Vehicle>> getAllVehicles() {
        List<Vehicle> vehicles = vehicleRepo.findAll();
        vehicles.forEach(this::applyExpiryStatus);
        return ResponseEntity.ok(vehicles);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getVehicleById(@PathVariable String id) {
        Optional<Vehicle> vehicleOpt = vehicleRepo.findById(id);
        if (vehicleOpt.isEmpty()) {
            return ResponseEntity.status(404).body("❌ Driver not found");
        }

        Vehicle vehicle = vehicleOpt.get();
        applyExpiryStatus(vehicle);
        vehicleRepo.save(vehicle);

        return ResponseEntity.ok(vehicle);
    }

    @GetMapping("/available")
    public ResponseEntity<List<Vehicle>> getAvailableVehicles() {
        List<Vehicle> vehicles = vehicleRepo.findByStatusIgnoreCaseAndIsBlockedFalse("Available");
        vehicles.forEach(this::applyExpiryStatus);
        return ResponseEntity.ok(vehicles);
    }

    @GetMapping("/count-available")
    public ResponseEntity<Long> getAvailableVehiclesCount() {
        List<Vehicle> vehicles = vehicleRepo.findByStatusIgnoreCaseAndIsBlockedFalse("Available");
        vehicles.forEach(this::applyExpiryStatus);
        long count = vehicles.stream()
                .filter(v -> !"Expired – This Vehicle".equalsIgnoreCase(v.getStatus()))
                .count();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/count-expired")
    public ResponseEntity<Long> getExpiredVehiclesCount() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Colombo"));
        long count = vehicleRepo.findAll()
                .stream()
                .filter(v -> v.getLicenseExpiryDate() != null &&
                        !v.getLicenseExpiryDate().isAfter(today) )
                .count();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/count-assigned")
    public ResponseEntity<Long> getAssignedVehiclesCount() {
        long count = vehicleRepo.findAll()
                .stream()
                .filter(v -> "Assigned".equalsIgnoreCase(v.getStatus()))
                .count();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/expired")
    public ResponseEntity<List<Vehicle>> getExpiredVehicles() {
        LocalDate today = LocalDate.now();
        List<Vehicle> expiredVehicles = vehicleRepo.findAll()
                .stream()
                .filter(v -> v.getLicenseExpiryDate() != null && v.getLicenseExpiryDate().isBefore(today))
                .toList();
        return ResponseEntity.ok(expiredVehicles);
    }

    @PostMapping
    public ResponseEntity<String> addVehicle(@RequestBody Vehicle vehicle) {
        applyExpiryStatus(vehicle);
        vehicleRepo.save(vehicle);
        return ResponseEntity.ok("✅ Vehicle added successfully");
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateVehicle(@PathVariable String id, @RequestBody Vehicle updatedVehicle) {
        Optional<Vehicle> vehicleOpt = vehicleRepo.findById(id);
        if (vehicleOpt.isPresent()) {
            Vehicle vehicle = vehicleOpt.get();
            vehicle.setVehicleNumber(updatedVehicle.getVehicleNumber());
            vehicle.setVehicleType(updatedVehicle.getVehicleType());
            vehicle.setManufacturer(updatedVehicle.getManufacturer());
            vehicle.setModel(updatedVehicle.getModel());
            vehicle.setStatus(updatedVehicle.getStatus());
            vehicle.setLicenseNumber(updatedVehicle.getLicenseNumber());
            vehicle.setLicenseIssueDate(updatedVehicle.getLicenseIssueDate());
            vehicle.setLicenseExpiryDate(updatedVehicle.getLicenseExpiryDate());
            applyExpiryStatus(vehicle);

            vehicleRepo.save(vehicle);
            return ResponseEntity.ok("✅ Vehicle updated successfully");
        }
        return ResponseEntity.status(404).body("❌ Vehicle not found");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteVehicle(@PathVariable String id) {
        if (vehicleRepo.existsById(id)) {
            vehicleRepo.deleteById(id);
            return ResponseEntity.ok("✅ Vehicle deleted successfully");
        }
        return ResponseEntity.status(404).body("❌ Vehicle not found");
    }

    @PutMapping("/{id}/license")
    public ResponseEntity<String> addOrUpdateLicense(
            @PathVariable String id,
            @Valid @RequestBody LicenseDto licenseDetails) {

        Optional<Vehicle> vehicleOpt = vehicleRepo.findById(id);
        if (vehicleOpt.isPresent()) {
            Vehicle vehicle = vehicleOpt.get();
            vehicle.setLicenseNumber(licenseDetails.getLicenseNumber());
            vehicle.setLicenseIssueDate(licenseDetails.getLicenseIssueDate());
            vehicle.setLicenseExpiryDate(licenseDetails.getLicenseExpiryDate());

            vehicle.setWarning30Sent(false);
            vehicle.setWarning14Sent(false);
            vehicle.setWarning7Sent(false);
            vehicle.setBlocked(false);

            applyExpiryStatus(vehicle);
            vehicleRepo.save(vehicle);
            return ResponseEntity.ok("✅ License details updated successfully");
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("❌ Vehicle not found");
    }
}
