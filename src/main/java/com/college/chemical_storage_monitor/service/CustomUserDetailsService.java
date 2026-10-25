package com.college.chemical_storage_monitor.service;

import com.college.chemical_storage_monitor.model.AppUser;
import com.college.chemical_storage_monitor.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser appUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        String role = appUser.getRole() != null && !appUser.getRole().trim().isEmpty() 
                ? appUser.getRole().toUpperCase().replace("ROLE_", "") 
                : "USER";

        return User.builder()
                .username(appUser.getUsername())
                .password(appUser.getPassword())
                .roles(role)
                .build();
    }
}
