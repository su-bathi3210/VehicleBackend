package com.VehicleMIS.VehicleMIS.Service;

import com.VehicleMIS.VehicleMIS.Model.Driver;
import com.VehicleMIS.VehicleMIS.Repository.DriverRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
public class DriverLicenseExpiryScheduler {

    private final DriverRepository driverRepository;

    public DriverLicenseExpiryScheduler(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void updateExpiredLicenses() {
        List<Driver> drivers = driverRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Driver driver : drivers) {
            if (driver.getLicenseExpiryDate() != null) {
                LocalDateTime expiryEndOfDay = driver.getLicenseExpiryDate().atTime(LocalTime.MAX);

                if (now.isAfter(expiryEndOfDay)) {
                    if (!driver.isBlocked() || !"Expired".equals(driver.getStatus())) {
                        driver.setStatus("Expired");
                        driver.setBlocked(true);
                        driverRepository.save(driver);
                        System.out.println("🚫 Driver expired and blocked: " + driver.getName());
                    }
                } else {
                    if (driver.isBlocked() && "Expired".equals(driver.getStatus())) {
                        driver.setStatus("Available");
                        driver.setBlocked(false);
                        driverRepository.save(driver);
                        System.out.println("✅ Driver unblocked after renewal: " + driver.getName());
                    }
                }
            }
        }

        System.out.println("✅ License expiry check completed at: " + now);
    }
}
