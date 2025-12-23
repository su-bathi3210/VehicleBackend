package com.VehicleMIS.VehicleMIS.Controller;

import com.VehicleMIS.VehicleMIS.Config.JwtUtil;
import com.VehicleMIS.VehicleMIS.DTO.LoginRequest;
import com.VehicleMIS.VehicleMIS.DTO.LoginResponse;
import com.VehicleMIS.VehicleMIS.Service.LicenseExpiryScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

    private final LicenseExpiryScheduler scheduler;

    public AuthController(LicenseExpiryScheduler scheduler) {
        this.scheduler = scheduler;
    }

    private final String ADMIN_EMAIL = "admin1@vehicle.com";
    private final String ADMIN_PASSWORD = "admin123";

    private final String APPROVAL_EMAIL = "approver@vehicle.com";
    private final String APPROVAL_PASSWORD = "approver123";

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        String email = request.getEmail();
        String password = request.getPassword();

        Set<String> roles;

        if (ADMIN_EMAIL.equals(email) && ADMIN_PASSWORD.equals(password)) {
            roles = Set.of("ADMIN");
        } else if (APPROVAL_EMAIL.equals(email) && APPROVAL_PASSWORD.equals(password)) {
            roles = Set.of("APPROVAL_OFFICER");
        } else {
            return ResponseEntity.status(401).body("❌ Invalid email or password");
        }

        String token = jwtUtil.generateToken(email, roles);

        return ResponseEntity.ok(new LoginResponse(token, roles));
    }

    @PostMapping("/run-license-check")
    public ResponseEntity<String> runLicenseCheckNow() {
        scheduler.checkLicenseExpiry();
        return ResponseEntity.ok("License check executed");
    }
}
