package com.VehicleMIS.VehicleMIS.Controller;

import com.VehicleMIS.VehicleMIS.Model.Notification;
import com.VehicleMIS.VehicleMIS.Service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@CrossOrigin(origins = "http://localhost:5173")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/{recipientEmail}")
    public ResponseEntity<List<Notification>> getNotifications(@PathVariable String recipientEmail) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(recipientEmail));
    }

    @PostMapping
    public ResponseEntity<Notification> create(@RequestBody Notification notification) {
        Notification saved = notificationService.createNotification(notification.getRecipientEmail(), notification.getMessage());
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/read/{id}")
    public ResponseEntity<?> markAsRead(@PathVariable String id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok("✅ Notification Marked As Read");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok("🗑️ Notification Deleted");
    }
}
