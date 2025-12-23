package com.VehicleMIS.VehicleMIS.Repository;

import com.VehicleMIS.VehicleMIS.Model.Vehicle;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends MongoRepository<Vehicle, String> {
    Optional<Vehicle> findByVehicleNumber(String vehicleNumber);
    List<Vehicle> findByStatusIgnoreCaseAndIsBlockedFalse(String status);
    long countByStatusIgnoreCaseAndIsBlockedFalse(String status);
}
