package com.VehicleMIS.VehicleMIS.Service;

import com.VehicleMIS.VehicleMIS.Model.Notification;
import com.VehicleMIS.VehicleMIS.Repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepo;

    public Notification createNotification(String recipientEmail, String message) {
        Notification n = Notification.builder()
                .recipientEmail(recipientEmail)
                .message(message)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();
        return notificationRepo.save(n);
    }

    public List<Notification> getUnreadNotifications(String recipientEmail) {
        return notificationRepo.findByRecipientEmailAndReadFalseOrderByCreatedAtDesc(recipientEmail);
    }

    public void markAsRead(String id) {
        notificationRepo.findById(id).ifPresent(n -> {
            n.setRead(true);
            notificationRepo.save(n);
        });
    }

    public void deleteNotification(String id) {
        notificationRepo.deleteById(id);
    }

    public Notification sendLicenseExpiryNotification(String recipientEmail, String vehicleNumber, String expiryDate, long daysLeft) {
        String msg;

        if (daysLeft >= 0) {
            msg = "⚠️ Reminder: Vehicle " + vehicleNumber +
                    " license will expire in " + daysLeft + " day(s) (" + expiryDate + ").";
        } else {
            msg = "❌ ALERT: Vehicle " + vehicleNumber +
                    " license expired on " + expiryDate +
                    ". Vehicle is now blocked until renewal.";
        }

        return createNotification(recipientEmail, msg);
    }

}
