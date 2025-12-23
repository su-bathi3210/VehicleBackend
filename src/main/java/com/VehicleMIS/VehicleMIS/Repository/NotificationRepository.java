package com.VehicleMIS.VehicleMIS.Repository;

import com.VehicleMIS.VehicleMIS.Model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByRecipientEmailAndReadFalseOrderByCreatedAtDesc(String recipientEmail);

}
