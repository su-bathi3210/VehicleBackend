package com.VehicleMIS.VehicleMIS.Service;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final String ADMIN_EMAIL = "admin1@vehicle.com";
    private final String ADMIN_PASSWORD = "admin123";

    private final String APPROVAL_EMAIL = "approver@vehicle.com";
    private final String APPROVAL_PASSWORD = "approver123";

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        if (ADMIN_EMAIL.equals(username)) {
            return new org.springframework.security.core.userdetails.User(
                    ADMIN_EMAIL,
                    ADMIN_PASSWORD,
                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
            );
        }

        if (APPROVAL_EMAIL.equals(username)) {
            return new org.springframework.security.core.userdetails.User(
                    APPROVAL_EMAIL,
                    APPROVAL_PASSWORD,
                    List.of(new SimpleGrantedAuthority("ROLE_APPROVAL_OFFICER"))
            );
        }

        throw new UsernameNotFoundException("User not found: " + username);
    }
}
