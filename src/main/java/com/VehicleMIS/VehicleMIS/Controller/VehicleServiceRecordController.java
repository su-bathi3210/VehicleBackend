package com.VehicleMIS.VehicleMIS.Controller;

import com.VehicleMIS.VehicleMIS.Model.Vehicle;
import com.VehicleMIS.VehicleMIS.Model.VehicleServiceRecord;
import com.VehicleMIS.VehicleMIS.Repository.VehicleRepository;
import com.VehicleMIS.VehicleMIS.Repository.VehicleServiceRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/vehicle-services")
@CrossOrigin(origins = "http://localhost:5173")
public class VehicleServiceRecordController {

    @Autowired
    private VehicleServiceRecordRepository recordRepo;

    @Autowired
    private VehicleRepository vehicleRepo;

    private static final Map<String, Integer> SERVICE_INTERVALS = Map.of(
            "Car", 6000,
            "Van", 7000,
            "Jeep", 8000,
            "Truck", 12000,
            "Cab", 9000
    );

    private int getIntervalForType(String type) {
        return SERVICE_INTERVALS.getOrDefault(type, 6000);
    }

    private String generateNextServiceRecordId(String vehicleNumber) {
        List<VehicleServiceRecord> records = recordRepo.findByVehicleNumber(vehicleNumber);
        int nextNumber = records.size() + 1;
        return String.format("SER-REC-%03d", nextNumber);
    }

    private void applyMileageUpdate(VehicleServiceRecord record, double newCurrentMileage, String vehicleType) {
        if (record.getMileageHistory() == null) record.setMileageHistory(new ArrayList<>());

        List<Double> hist = record.getMileageHistory();
        if (hist.isEmpty() || !Objects.equals(hist.get(hist.size() - 1), newCurrentMileage)) {
            hist.add(newCurrentMileage);
        }

        double interval = record.getServiceInterval();
        if (interval <= 0) interval = getIntervalForType(vehicleType);

        double prevNext = record.getNextServiceMileage();
        if (prevNext <= 0) prevNext = record.getCurrentMileage() + interval;

        if (prevNext > 0 && newCurrentMileage >= prevNext) {
            long extra = (long) Math.floor((newCurrentMileage - prevNext) / interval) + 1L;
            record.setServiceCount(record.getServiceCount() + (int) extra);
        }

        record.setCurrentMileage(newCurrentMileage);
        record.setNextServiceMileage(newCurrentMileage + interval);
    }

    @PostMapping
    public ResponseEntity<?> addServiceRecord(@RequestBody VehicleServiceRecord record) {
        Optional<Vehicle> optionalVehicle = vehicleRepo.findById(record.getVehicleId());
        if (optionalVehicle.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("❌ Vehicle not found!");
        }

        Vehicle vehicle = optionalVehicle.get();
        int interval = getIntervalForType(vehicle.getVehicleType());
        record.setServiceInterval(interval);

        record.setVehicleNumber(vehicle.getVehicleNumber());
        String newServiceRecordId = generateNextServiceRecordId(vehicle.getVehicleNumber());
        record.setServiceRecordId(newServiceRecordId);

        if (record.getMileageHistory() == null)
            record.setMileageHistory(new ArrayList<>());

        if (record.getCurrentMileage() <= 0) {
            record.setCurrentMileage(0.0);
            record.setNextServiceMileage(interval);
        } else {
            record.setNextServiceMileage(record.getCurrentMileage() + interval);
        }

        record.setServiceDate(
                record.getServiceDate() == null ? java.time.LocalDate.now() : record.getServiceDate()
        );

        recordRepo.save(record);
        return ResponseEntity.ok("✅ Service Record Added Successfully (" + newServiceRecordId + ") for " + vehicle.getVehicleNumber());
    }

    @GetMapping
    public ResponseEntity<List<VehicleServiceRecord>> getAllServiceRecords() {
        return ResponseEntity.ok(recordRepo.findAll());
    }

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<?> getRecordsByVehicleId(@PathVariable String vehicleId) {
        List<VehicleServiceRecord> records = recordRepo.findByVehicleId(vehicleId);
        if (records.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        return ResponseEntity.ok(records);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteServiceRecord(@PathVariable String id) {
        if (!recordRepo.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("❌ Service record not found!");
        }
        recordRepo.deleteById(id);
        return ResponseEntity.ok("✅ Service record deleted");
    }

    @GetMapping("/service-intervals")
    public ResponseEntity<Map<String, Integer>> getServiceIntervals() {
        return ResponseEntity.ok(SERVICE_INTERVALS);
    }

    @PutMapping("/{id}/mileage")
    public ResponseEntity<String> updateMileage(@PathVariable String id, @RequestBody Map<String, Object> payload) {
        Optional<VehicleServiceRecord> opt = recordRepo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.status(404).body("❌ Service Record Not Found");

        VehicleServiceRecord record = opt.get();
        double currentMileage = Double.parseDouble(payload.get("currentMileage").toString());
        String vehicleType = payload.getOrDefault("vehicleType", "Car").toString();

        if (currentMileage < 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("❌ Invalid Mileage Value");
        }

        applyMileageUpdate(record, currentMileage, vehicleType);
        recordRepo.save(record);

        return ResponseEntity.ok("✅ Mileage updated. Current: " + currentMileage +
                " km | Next service: " + record.getNextServiceMileage() +
                " km | Service count: " + record.getServiceCount());
    }

    @PutMapping("/{id}/service-count")
    public ResponseEntity<String> updateServiceCount(@PathVariable String id, @RequestBody Map<String, Object> payload) {
        Optional<VehicleServiceRecord> opt = recordRepo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.status(404).body("❌ Service Record Not Found");

        VehicleServiceRecord record = opt.get();
        int newCount = Integer.parseInt(payload.get("serviceCount").toString());
        record.setServiceCount(newCount);
        recordRepo.save(record);
        return ResponseEntity.ok("✅ Service count manually updated to " + newCount);
    }
}