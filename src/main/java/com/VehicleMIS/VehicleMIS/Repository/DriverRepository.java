package com.VehicleMIS.VehicleMIS.Repository;

import com.VehicleMIS.VehicleMIS.Model.Driver;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DriverRepository extends MongoRepository<Driver, String> {
    long countByStatusIgnoreCase(String status);
}
