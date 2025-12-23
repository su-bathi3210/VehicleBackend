package com.VehicleMIS.VehicleMIS.Repository;

import com.VehicleMIS.VehicleMIS.Model.VehicleRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRequestRepository extends MongoRepository<VehicleRequest, String> {
    Optional<VehicleRequest> findByRequestId(String requestId);
    List<VehicleRequest> findByTravelerNameAndPhoneNumber(String travelerName, String phoneNumber);
}