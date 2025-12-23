package com.VehicleMIS.VehicleMIS.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
public class LicenseDto {
    @NotBlank
    private String licenseNumber;
    @NotNull
    private LocalDate licenseIssueDate;
    @NotNull
    private LocalDate licenseExpiryDate;

}
