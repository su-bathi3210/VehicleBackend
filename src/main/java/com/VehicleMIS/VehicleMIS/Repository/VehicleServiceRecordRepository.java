package com.VehicleMIS.VehicleMIS.Repository;

import com.VehicleMIS.VehicleMIS.Model.VehicleServiceRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VehicleServiceRecordRepository extends MongoRepository<VehicleServiceRecord, String> {
    List<VehicleServiceRecord> findByVehicleId(String vehicleId);
    List<VehicleServiceRecord> findByVehicleNumber(String vehicleNumber);

}
