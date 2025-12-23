package com.VehicleMIS.VehicleMIS.Controller;

import com.VehicleMIS.VehicleMIS.Model.VehicleRequest;
import com.VehicleMIS.VehicleMIS.Model.Vehicle;
import com.VehicleMIS.VehicleMIS.Model.Driver;
import com.VehicleMIS.VehicleMIS.Repository.VehicleRequestRepository;
import com.VehicleMIS.VehicleMIS.Repository.VehicleRepository;
import com.VehicleMIS.VehicleMIS.Repository.DriverRepository;
import com.VehicleMIS.VehicleMIS.Service.EmailService;
import com.VehicleMIS.VehicleMIS.Service.NotificationService;
import com.VehicleMIS.VehicleMIS.Service.SmsService;
import com.VehicleMIS.VehicleMIS.Service.TripResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/vehicle-requests")
@CrossOrigin(origins = "http://localhost:5173")
public class VehicleRequestController {

    @Autowired
    private VehicleRequestRepository requestRepo;

    @Autowired
    private VehicleRepository vehicleRepo;

    @Autowired
    private DriverRepository driverRepo;

    @Autowired
    private EmailService emailService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SmsService smsService;

    @Autowired
    private TripResetService tripResetService;

    @PostMapping
    public ResponseEntity<?> createRequest(@RequestBody VehicleRequest request) {
        request.setStatus("PENDING");
        request.setSubmittedAt(LocalDateTime.now());
        VehicleRequest savedRequest = requestRepo.save(request);

        if (savedRequest.getPhoneNumber() != null && !savedRequest.getPhoneNumber().isEmpty()) {
            boolean smsSent = smsService.sendRequestConfirmationSms(
                    savedRequest.getPhoneNumber(),
                    savedRequest.getRequestId()
            );
            if (!smsSent) {
                System.out.println("⚠️ SMS confirmation failed for " + savedRequest.getPhoneNumber());
            }
        }
        return ResponseEntity.ok(savedRequest);
    }

    @PostMapping("/employee-login")
    public ResponseEntity<?> employeeLogin(@RequestBody Map<String, String> body) {
        String requestId = body.get("requestId");
        String travelerName = body.get("travelerName");
        String phoneNumber = body.get("phoneNumber");

        Optional<VehicleRequest> opt = requestRepo.findByRequestId(requestId);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body("❌ Request not found");
        }

        VehicleRequest req = opt.get();
        if (!req.getTravelerName().equalsIgnoreCase(travelerName)
                || !req.getPhoneNumber().equals(phoneNumber)) {
            return ResponseEntity.status(401).body("❌ Invalid credentials");
        }

        return ResponseEntity.ok(req);
    }

    @GetMapping("/my-requests")
    public ResponseEntity<?> getMyRequests(@RequestParam String travelerName, @RequestParam String phoneNumber) {
        List<VehicleRequest> list = requestRepo.findAll()
                .stream()
                .filter(r -> r.getTravelerName() != null && r.getTravelerName().equalsIgnoreCase(travelerName))
                .filter(r -> r.getPhoneNumber() != null && r.getPhoneNumber().equals(phoneNumber))
                .toList();
        return ResponseEntity.ok(list);
    }

    @PutMapping("/{requestId}/cancel")
    public ResponseEntity<?> cancelRequest(@PathVariable String requestId, @RequestBody Map<String, String> body) {
        String travelerName = body.get("travelerName");
        String phoneNumber = body.get("phoneNumber");
        String reason = body.getOrDefault("reason", "No reason provided");

        Optional<VehicleRequest> opt = requestRepo.findByRequestId(requestId);
        if (opt.isEmpty()) return ResponseEntity.status(404).body("❌ Request not found");

        VehicleRequest req = opt.get();

        if (!req.getTravelerName().equalsIgnoreCase(travelerName)
                || !req.getPhoneNumber().equals(phoneNumber)) {
            return ResponseEntity.status(401).body("❌ Unauthorized cancellation");
        }

        if (List.of("COMPLETED", "CANCELLED").contains(req.getStatus()))
            return ResponseEntity.status(400).body("⚠️ Cannot cancel this request now.");

        switch (req.getStatus().toUpperCase()) {
            case "PENDING" -> {
                req.setStatus("CANCELLED");
                req.setCancelledAt(LocalDateTime.now());
                req.setCancellationReason(reason);
                requestRepo.save(req);
                return ResponseEntity.ok("✅ Request cancelled successfully (admin review not required).");
            }
            case "APPROVED_BY_ADMIN", "APPROVED_BY_OFFICER", "DRIVER_ASSIGNED" -> {
                req.setStatus("CANCELLATION_REQUESTED");
                req.setCancellationReason(reason);
                req.setCancellationPending(true);
                req.setCancelledAt(LocalDateTime.now());
                requestRepo.save(req);

                notificationService.createNotification(
                        "ksuba3210@gmail.com",
                        "Employee requested cancellation for vehicle request: " + req.getRequestId()
                );
                smsService.sendSms(
                        "94714341777",
                        "Employee requested cancellation for request " + req.getRequestId() + ". Please review.",
                        true
                );
                return ResponseEntity.ok("🕓 Cancellation request sent for admin approval.");
            }
            default -> {
                return ResponseEntity.status(400).body("⚠️ Cancellation not allowed in this status.");
            }
        }
    }

    @PutMapping("/{requestId}/cancel/approve")
    public ResponseEntity<?> approveCancellation(@PathVariable String requestId) {
        Optional<VehicleRequest> opt = requestRepo.findByRequestId(requestId);
        if (opt.isEmpty()) return ResponseEntity.status(404).body("❌ Request not found");

        VehicleRequest req = opt.get();

        if (!"CANCELLATION_REQUESTED".equalsIgnoreCase(req.getStatus())) {
            return ResponseEntity.status(400).body("⚠️ Cancellation not pending approval.");
        }

        req.setStatus("CANCELLED");
        req.setCancellationPending(false);
        req.setCancellationDecision("APPROVED");
        req.setCancellationReviewedAt(LocalDateTime.now());
        requestRepo.save(req);

        if (req.getAssignedVehicleId() != null) {
            vehicleRepo.findById(req.getAssignedVehicleId()).ifPresent(vehicle -> {
                vehicle.setStatus("Available");
                vehicleRepo.save(vehicle);
            });
        }
        if (req.getAssignedDriverId() != null) {
            driverRepo.findById(req.getAssignedDriverId()).ifPresent(driver -> {
                driver.setStatus("Available");
                driverRepo.save(driver);
            });
        }

        smsService.sendSms(req.getPhoneNumber(),
                "✅ Your cancellation request (" + req.getRequestId() + ") has been approved by Admin.",
                true);

        return ResponseEntity.ok("✅ Cancellation approved successfully.");
    }

    @PutMapping("/{requestId}/cancel/reject")
    public ResponseEntity<?> rejectCancellation(@PathVariable String requestId, @RequestBody Map<String, String> body) {
        Optional<VehicleRequest> opt = requestRepo.findByRequestId(requestId);
        if (opt.isEmpty()) return ResponseEntity.status(404).body("❌ Request not found");

        VehicleRequest req = opt.get();

        if (!"CANCELLATION_REQUESTED".equalsIgnoreCase(req.getStatus())) {
            return ResponseEntity.status(400).body("⚠️ Cancellation not pending approval.");
        }

        String adminReason = body.getOrDefault("adminReason", "Not specified");

        req.setCancellationPending(false);
        req.setCancellationDecision("REJECTED");
        req.setCancellationReviewedAt(LocalDateTime.now());
        req.setStatus("APPROVED_BY_ADMIN");
        requestRepo.save(req);

        smsService.sendSms(req.getPhoneNumber(),
                "❌ Your cancellation request (" + req.getRequestId() + ") has been rejected by Admin. Reason: " + adminReason,
                true);

        return ResponseEntity.ok("❌ Cancellation rejected successfully.");
    }


    @GetMapping("/test-trip-reset")
    public ResponseEntity<String> testTripReset() {
        tripResetService.resetTripStatus();
        return ResponseEntity.ok("Trip reset executed manually!");
    }

    @GetMapping("/myrequests/{requesterName}")
    public ResponseEntity<List<VehicleRequest>> getRequestsByRequester(@PathVariable String requesterName) {
        List<VehicleRequest> requests = requestRepo.findAll()
                .stream()
                .filter(r -> r.getRequesterName().equalsIgnoreCase(requesterName))
                .toList();
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/admin")
    public ResponseEntity<List<VehicleRequest>> getAllRequestsForAdmin() {
        return ResponseEntity.ok(requestRepo.findAll());
    }

    @GetMapping("/officer")
    public ResponseEntity<List<VehicleRequest>> getRequestsForOfficer() {
        List<VehicleRequest> requests = requestRepo.findAll()
                .stream()
                .filter(r -> "APPROVED_BY_ADMIN".equalsIgnoreCase(r.getStatus()))
                .toList();
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getRequestCount() {
        return ResponseEntity.ok(requestRepo.count());
    }

    @GetMapping("/count-assigned")
    public ResponseEntity<Long> getAssignedRequestCount() {
        long count = requestRepo.findAll()
                .stream()
                .filter(r -> "APPROVED_BY_ADMIN".equalsIgnoreCase(r.getStatus())
                        || "APPROVED_BY_OFFICER".equalsIgnoreCase(r.getStatus())
                        || "ON_GOING_TRIP".equalsIgnoreCase(r.getStatus()))
                .count();
        return ResponseEntity.ok(count);
    }

    @PutMapping("/assign/{id}/{vehicleId}")
    public ResponseEntity<?> assignVehicle(@PathVariable String id, @PathVariable String vehicleId) {
        Optional<VehicleRequest> reqOpt = requestRepo.findById(id);
        Optional<Vehicle> vehicleOpt = vehicleRepo.findById(vehicleId);

        if (reqOpt.isEmpty()) return ResponseEntity.status(404).body("❌ Request not found");
        if (vehicleOpt.isEmpty()) return ResponseEntity.status(404).body("❌ Vehicle not found");

        VehicleRequest request = reqOpt.get();
        Vehicle vehicle = vehicleOpt.get();

        if (vehicle.isBlocked() || "Expired".equalsIgnoreCase(vehicle.getStatus())) {
            return ResponseEntity.status(400).body("❌ Vehicle license is expired. Cannot assign this vehicle!");
        }

        if (!"Available".equalsIgnoreCase(vehicle.getStatus())) {
            return ResponseEntity.status(400).body("❌ Vehicle is not available");
        }

        request.setAssignedVehicleId(vehicleId);
        request.setStatus("APPROVED_BY_ADMIN");
        request.setVehicleAssignedAt(LocalDateTime.now());
        vehicle.setStatus("Assigned");

        requestRepo.save(request);
        vehicleRepo.save(vehicle);

        emailService.sendApprovalEmail(request.getRequestId());
        notificationService.createNotification(
                "enukaenubro@gmail.com",
                "New vehicle request (" + request.getRequestId() + ") requires your approval."
        );

        String approvalOfficerPhone = "94703464165";
        String smsMessage = "New vehicle request (" + request.getRequestId() + ") requires your approval.";
        boolean smsSent = smsService.sendSms(approvalOfficerPhone, smsMessage, false);
        if (!smsSent) {
            System.out.println("⚠️ SMS failed to send to " + approvalOfficerPhone);
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
            String empMessage = "✅ Your vehicle request (" + request.getRequestId() +
                    ") has been assigned a vehicle and is pending officer approval.";
            boolean empSmsSent = smsService.sendSms(request.getPhoneNumber(), empMessage, true);
            if (!empSmsSent) {
                System.out.println("⚠️ SMS failed to send to employee " + request.getPhoneNumber());
            }
        }

        return ResponseEntity.ok("✅ Vehicle assigned successfully, notification sent to Approval Officer and Employee");
    }

    @PutMapping("/approve/{id}")
    public ResponseEntity<?> approveRequest(@PathVariable String id) {
        Optional<VehicleRequest> reqOpt = requestRepo.findById(id);
        if (reqOpt.isEmpty()) return ResponseEntity.status(404).body("❌ Request not found");

        VehicleRequest request = reqOpt.get();
        request.setStatus("APPROVED_BY_OFFICER");
        request.setOfficerApprovedAt(LocalDateTime.now());
        requestRepo.save(request);

        String adminEmail = "ksuba3210@gmail.com";
        String adminPhone = "94714341777";
        String message = "Request " + id + " has been APPROVED by Approval Officer.";

        emailService.sendAdminEmail(message);
        notificationService.createNotification(adminEmail, message);

        boolean smsSent = smsService.sendSms(adminPhone, message, false);
        if (!smsSent) {
            System.out.println("⚠️ SMS failed to send to " + adminPhone);
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
            String empMsg = "✅ Your vehicle request (" + request.getRequestId() +
                    ") has been APPROVED by the Approval Officer. Please wait for driver assignment.";
            boolean empSmsSent = smsService.sendSms(request.getPhoneNumber(), empMsg, true);
            if (!empSmsSent) {
                System.out.println("⚠️ SMS failed to send to employee " + request.getPhoneNumber());
            }
        }

        return ResponseEntity.ok("✅ Request approved by Approval Officer, notifications sent to Admin and Employee");
    }

    @PutMapping("/reject/{id}")
    public ResponseEntity<?> rejectRequest(@PathVariable String id) {
        Optional<VehicleRequest> reqOpt = requestRepo.findById(id);
        if (reqOpt.isEmpty()) return ResponseEntity.status(404).body("❌ Request not found");

        VehicleRequest request = reqOpt.get();
        request.setStatus("REJECTED");

        if (request.getAssignedVehicleId() != null) {
            Optional<Vehicle> vehicleOpt = vehicleRepo.findById(request.getAssignedVehicleId());
            vehicleOpt.ifPresent(vehicle -> {
                vehicle.setStatus("Available");
                vehicleRepo.save(vehicle);
            });
        }
        requestRepo.save(request);

        emailService.sendAdminEmail("Request " + id + " Rejected By Approval Officer.");
        notificationService.createNotification(
                "admin@vehicle.com",
                "Request " + id + " Has Been REJECTED by Approval Officer."
        );

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
            String empMsg = "❌ Your vehicle request (" + request.getRequestId() +
                    ") has been REJECTED by the Approval Officer.";
            boolean empSmsSent = smsService.sendSms(request.getPhoneNumber(), empMsg, true);
            if (!empSmsSent) {
                System.out.println("⚠️ SMS failed to send to employee " + request.getPhoneNumber());
            }
        }

        return ResponseEntity.ok("❌ Request Rejected By Approval Officer, notifications sent to Admin and Employee");
    }

    @GetMapping("/count-per-year")
    public ResponseEntity<Map<Integer, Long>> getRequestCountPerYear() {
        List<VehicleRequest> requests = requestRepo.findAll();
        Map<Integer, Long> countsPerYear = requests.stream()
                .filter(r -> r.getSubmittedAt() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getSubmittedAt().getYear(),
                        Collectors.counting()
                ));
        return ResponseEntity.ok(countsPerYear);
    }

    @PutMapping("/assign-driver/{requestId}/{driverId}")
    public ResponseEntity<?> assignDriver(
            @PathVariable String requestId,
            @PathVariable String driverId) {

        Optional<VehicleRequest> reqOpt = requestRepo.findById(requestId);
        Optional<Driver> driverOpt = driverRepo.findById(driverId);

        if (reqOpt.isEmpty()) return ResponseEntity.status(404).body("❌ Request not found");
        if (driverOpt.isEmpty()) return ResponseEntity.status(404).body("❌ Driver not found");

        VehicleRequest request = reqOpt.get();
        Driver driver = driverOpt.get();

        if (driver.isBlocked() || "Expired".equalsIgnoreCase(driver.getStatus())) {
            return ResponseEntity.status(400).body("❌ Driver license is expired. Cannot assign this driver!");
        }

        if (!"APPROVED_BY_OFFICER".equalsIgnoreCase(request.getStatus())) {
            return ResponseEntity.status(400).body("❌ Request is not approved by officer yet");
        }

        if ("Assigned".equalsIgnoreCase(driver.getStatus())) {
            return ResponseEntity.status(400).body("⚠️ Driver is already assigned to another trip!");
        }

        request.setAssignedDriverId(driverId);
        request.setStatus("DRIVER_ASSIGNED");
        request.setDriverAssignedAt(LocalDateTime.now());
        requestRepo.save(request);

        driver.setStatus("Assigned");
        driverRepo.save(driver);

        Vehicle vehicle = null;
        if (request.getAssignedVehicleId() != null) {
            Optional<Vehicle> vehicleOpt = vehicleRepo.findById(request.getAssignedVehicleId());
            if (vehicleOpt.isPresent()) {
                vehicle = vehicleOpt.get();
                vehicle.setStatus("Assigned");
                vehicleRepo.save(vehicle);
            }
        }

        String vehicleDetails = (vehicle != null)
                ? "Vehicle: " + vehicle.getVehicleNumber() + " (" + vehicle.getModel() + ")"
                : "Vehicle details not available";

        boolean smsSentDriver = smsService.sendSms(
                driver.getPhoneNumber(),
                "✅ You have been assigned to request " + requestId + ". " + vehicleDetails + ". Please contact admin if needed.",
                true
        );

        if (!smsSentDriver) System.out.println("⚠️ SMS failed to send to driver " + driver.getPhoneNumber());

        String employeePhone = request.getPhoneNumber();
        if (employeePhone != null && !employeePhone.isEmpty()) {
            boolean smsSentEmployee = smsService.sendSms(
                    employeePhone,
                    "✅ For the vehicle number " +
                            (vehicle != null ? vehicle.getVehicleNumber() : "N/A") +
                            " you requested, along with driver " + driver.getName() +
                            ", the vehicle has been successfully assigned for " +
                            (request.getTravelDateTime() != null ? request.getTravelDateTime().toString() : "the requested date") + ".",
                    true
            );
            if (!smsSentEmployee) System.out.println("⚠️ SMS failed to send to employee " + employeePhone);
        } else {
            System.out.println("⚠️ No phone number found for requester of request " + requestId);
        }

        return ResponseEntity.ok("✅ Driver assigned successfully, statuses updated, SMS sent to driver and employee.");
    }

    @PutMapping("/complete/{id}")
    public ResponseEntity<?> completeTrip(@PathVariable String id) {
        Optional<VehicleRequest> reqOpt = requestRepo.findById(id);
        if (reqOpt.isEmpty()) return ResponseEntity.status(404).body("❌ Request not found");

        VehicleRequest request = reqOpt.get();
        request.setStatus("COMPLETED");
        request.setTripCompletedAt(LocalDateTime.now());

        if (request.getAssignedVehicleId() != null) {
            vehicleRepo.findById(request.getAssignedVehicleId()).ifPresent(vehicle -> {
                vehicle.setStatus("Available");
                vehicleRepo.save(vehicle);
            });
        }

        if (request.getAssignedDriverId() != null) {
            driverRepo.findById(request.getAssignedDriverId()).ifPresent(driver -> {
                driver.setStatus("Available");
                driverRepo.save(driver);
            });
        }

        requestRepo.save(request);

        return ResponseEntity.ok("✅ Trip marked as COMPLETED and resources freed.");
    }

    @GetMapping("/{id}/assigned-details")
    public ResponseEntity<Map<String, Object>> getAssignedDetails(@PathVariable String id) {
        Optional<VehicleRequest> requestOpt = requestRepo.findById(id);
        if (requestOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        VehicleRequest req = requestOpt.get();
        Map<String, Object> response = new HashMap<>();

        if (req.getAssignedDriverId() != null) {
            driverRepo.findById(req.getAssignedDriverId()).ifPresent(driver -> {
                response.put("assignedDriver", driver);
            });
        } else {
            response.put("assignedDriver", null);
        }

        if (req.getAssignedVehicleId() != null) {
            vehicleRepo.findById(req.getAssignedVehicleId()).ifPresent(vehicle -> {
                response.put("assignedVehicle", vehicle);
            });
        } else {
            response.put("assignedVehicle", null);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/count-ongoing")
    public ResponseEntity<Long> getOngoingTripCount() {
        long count = requestRepo.findAll()
                .stream()
                .filter(r -> "ON_GOING_TRIP".equalsIgnoreCase(r.getStatus()))
                .count();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/ongoing")
    public ResponseEntity<List<VehicleRequest>> getOngoingTrips() {
        List<VehicleRequest> ongoingTrips = requestRepo.findAll()
                .stream()
                .filter(r -> "ON_GOING_TRIP".equalsIgnoreCase(r.getStatus()))
                .toList();
        return ResponseEntity.ok(ongoingTrips);
    }
}