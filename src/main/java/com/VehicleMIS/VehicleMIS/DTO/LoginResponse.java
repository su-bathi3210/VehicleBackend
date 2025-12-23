package com.VehicleMIS.VehicleMIS.DTO;

import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse {
    private final String token;
    private final Set<String> roles;

    public LoginResponse(String token, Set<String> roles) {
        this.token = token;
        this.roles = roles;
    }
}
