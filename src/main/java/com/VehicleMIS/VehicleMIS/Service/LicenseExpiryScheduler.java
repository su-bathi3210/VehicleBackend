package com.VehicleMIS.VehicleMIS.Service;

import com.VehicleMIS.VehicleMIS.Model.Vehicle;
import com.VehicleMIS.VehicleMIS.Repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class LicenseExpiryScheduler {

    private static final Logger logger = LoggerFactory.getLogger(LicenseExpiryScheduler.class);

    @Autowired
    private VehicleRepository vehicleRepo;

    @Autowired
    private SmsService smsService;

    @Autowired
    private NotificationService notificationService;

    @Value("${admin.phone}")
    private String adminPhone;

    @Value("${admin.email}")
    private String adminEmail;

    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Colombo")
    public void checkLicenseExpiry() {
        logger.info("🌙 Running license expiry check at midnight: {}", LocalDate.now(ZoneId.of("Asia/Colombo")));

        List<Vehicle> vehicles = vehicleRepo.findAll();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Colombo"));
        List<Vehicle> toSave = new ArrayList<>();

        for (Vehicle vehicle : vehicles) {
            if (vehicle.getLicenseExpiryDate() == null) continue;

            long daysLeft = ChronoUnit.DAYS.between(today, vehicle.getLicenseExpiryDate());

            if (daysLeft == 30 && !vehicle.isWarning30Sent()) {
                sendWarning(vehicle, 30);
                vehicle.setWarning30Sent(true);
                toSave.add(vehicle);
            }

            if (daysLeft == 14 && !vehicle.isWarning14Sent()) {
                sendWarning(vehicle, 14);
                vehicle.setWarning14Sent(true);
                toSave.add(vehicle);
            }

            if (daysLeft == 7 && !vehicle.isWarning7Sent()) {
                sendWarning(vehicle, 7);
                vehicle.setWarning7Sent(true);
                toSave.add(vehicle);
            }

            if (daysLeft <= 0) { // changed from daysLeft < 0 to daysLeft <= 0
                if (!"Expired".equalsIgnoreCase(vehicle.getStatus())) {
                    vehicle.setStatus("Expired");
                    vehicle.setBlocked(true);
                    logger.warn("❌ Vehicle {} license expired on {}. Status updated to 'Expired'.", vehicle.getVehicleNumber(), vehicle.getLicenseExpiryDate());
                    toSave.add(vehicle);
                }

                if (!vehicle.isBlocked()) {
                    vehicle.setBlocked(true);

                    smsService.sendLicenseExpirySms(
                            adminPhone,
                            vehicle.getVehicleNumber(),
                            vehicle.getLicenseExpiryDate().toString(),
                            daysLeft
                    );

                    notificationService.sendLicenseExpiryNotification(
                            adminEmail,
                            vehicle.getVehicleNumber(),
                            vehicle.getLicenseExpiryDate().toString(),
                            daysLeft
                    );

                    logger.warn("❌ Vehicle {} blocked. License expired {} days ago.",
                            vehicle.getVehicleNumber(), -daysLeft);
                    toSave.add(vehicle);
                }
            }
        }

        if (!toSave.isEmpty()) {
            vehicleRepo.saveAll(toSave);
            logger.info("✅ Saved {} updated vehicles after license check.", toSave.size());
        } else {
            logger.info("No vehicle updates required during this run.");
        }
    }

    private void sendWarning(Vehicle vehicle, int days) {
        smsService.sendLicenseExpirySms(
                adminPhone,
                vehicle.getVehicleNumber(),
                vehicle.getLicenseExpiryDate().toString(),
                days
        );
        notificationService.sendLicenseExpiryNotification(
                adminEmail,
                vehicle.getVehicleNumber(),
                vehicle.getLicenseExpiryDate().toString(),
                days
        );
        logger.info("⚠️ Vehicle {} license will expire in {} days. SMS + Notification sent.",
                vehicle.getVehicleNumber(), days);
    }
}
