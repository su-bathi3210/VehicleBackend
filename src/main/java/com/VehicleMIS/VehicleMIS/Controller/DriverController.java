package com.VehicleMIS.VehicleMIS.Controller;

import com.VehicleMIS.VehicleMIS.Model.Driver;
import com.VehicleMIS.VehicleMIS.Repository.DriverRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/drivers")
@CrossOrigin(origins = "http://localhost:5173")
public class DriverController {

    @Autowired
    private DriverRepository driverRepo;

    private void applyExpiryStatus(Driver driver) {
        if (driver.getLicenseExpiryDate() != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiryStartOfDay = driver.getLicenseExpiryDate().atStartOfDay();

            if (now.isAfter(expiryStartOfDay) || now.isEqual(expiryStartOfDay)) {
                driver.setBlocked(true);
                driver.setStatus("Expired");
            } else {
                if (!"Assigned".equalsIgnoreCase(driver.getStatus()) &&
                        !"On_Trip".equalsIgnoreCase(driver.getStatus())) {

                    if (driver.isBlocked() && "Expired".equalsIgnoreCase(driver.getStatus())) {
                        driver.setBlocked(false);
                    }

                    driver.setStatus("Available");
                }
            }
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllDrivers() {
        List<Driver> drivers = driverRepo.findAll();
        drivers.forEach(this::applyExpiryStatus);
        driverRepo.saveAll(drivers);
        return ResponseEntity.ok(drivers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDriverById(@PathVariable String id) {
        Optional<Driver> driverOpt = driverRepo.findById(id);
        if (driverOpt.isEmpty()) {
            return ResponseEntity.status(404).body("❌ Driver not found");
        }

        Driver driver = driverOpt.get();
        applyExpiryStatus(driver);
        driverRepo.save(driver);

        return ResponseEntity.ok(driver);
    }

    @GetMapping("/available")
    public ResponseEntity<List<Driver>> getAvailableDrivers() {
        List<Driver> drivers = driverRepo.findAll()
                .stream()
                .peek(this::applyExpiryStatus)
                .filter(d -> "Available".equalsIgnoreCase(d.getStatus()))
                .toList();
        driverRepo.saveAll(drivers);
        return ResponseEntity.ok(drivers);
    }

    @GetMapping("/count-available")
    public ResponseEntity<Long> getAvailableDriversCount() {
        List<Driver> drivers = driverRepo.findAll();
        drivers.forEach(this::applyExpiryStatus);
        driverRepo.saveAll(drivers);
        long count = drivers.stream().filter(d -> "Available".equalsIgnoreCase(d.getStatus())).count();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/expired")
    public ResponseEntity<List<Driver>> getExpiredDrivers() {
        List<Driver> drivers = driverRepo.findAll();
        drivers.forEach(this::applyExpiryStatus);
        driverRepo.saveAll(drivers);
        List<Driver> expiredDrivers = drivers.stream()
                .filter(d -> d.isBlocked())
                .toList();
        return ResponseEntity.ok(expiredDrivers);
    }

    @GetMapping("/count-expired")
    public ResponseEntity<Long> getExpiredDriversCount() {
        List<Driver> drivers = driverRepo.findAll();
        drivers.forEach(this::applyExpiryStatus);
        driverRepo.saveAll(drivers);
        long count = drivers.stream().filter(Driver::isBlocked).count();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/count-assigned")
    public ResponseEntity<Long> getAssignedDriversCount() {
        List<Driver> drivers = driverRepo.findAll();
        long count = drivers.stream()
                .filter(d -> "Assigned".equalsIgnoreCase(d.getStatus()))
                .count();
        return ResponseEntity.ok(count);
    }

    @PostMapping
    public ResponseEntity<?> addDriver(@RequestBody Driver driver) {
        applyExpiryStatus(driver);
        driverRepo.save(driver);
        return ResponseEntity.ok("✅ Driver added successfully");
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDriver(@PathVariable String id, @RequestBody Driver updatedDriver) {
        Optional<Driver> driverOpt = driverRepo.findById(id);
        if (driverOpt.isPresent()) {
            Driver driver = driverOpt.get();
            driver.setName(updatedDriver.getName());
            driver.setPhoneNumber(updatedDriver.getPhoneNumber());
            driver.setLicenseNumber(updatedDriver.getLicenseNumber());
            driver.setNic(updatedDriver.getNic());
            driver.setAddress(updatedDriver.getAddress());
            driver.setEmail(updatedDriver.getEmail());
            driver.setEmergencyContact(updatedDriver.getEmergencyContact());
            driver.setLicenseExpiryDate(updatedDriver.getLicenseExpiryDate());

            applyExpiryStatus(driver);
            driverRepo.save(driver);
            return ResponseEntity.ok("✅ Driver updated successfully");
        }
        return ResponseEntity.status(404).body("❌ Driver not found");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDriver(@PathVariable String id) {
        if (driverRepo.existsById(id)) {
            driverRepo.deleteById(id);
            return ResponseEntity.ok("✅ Driver deleted successfully");
        }
        return ResponseEntity.status(404).body("❌ Driver not found");
    }
}
