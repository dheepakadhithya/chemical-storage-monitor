package com.college.chemical_storage_monitor.controller;

import com.college.chemical_storage_monitor.model.AppUser;
import com.college.chemical_storage_monitor.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 1. Get current logged-in user profile (Any authenticated role: Admin, Operator, Viewer)
    @GetMapping("/current")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCurrentUser(java.security.Principal principal) {
        String username = null;
        if (principal != null) {
            username = principal.getName();
        } else {
            org.springframework.security.core.Authentication auth =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                username = auth.getName();
            }
        }

        if (username == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        Optional<AppUser> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        AppUser u = userOpt.get();
        Map<String, Object> map = new HashMap<>();
        map.put("id", u.getId());
        map.put("username", u.getUsername());
        map.put("role", u.getRole());
        map.put("imageUrl", u.getImageUrl());
        return ResponseEntity.ok(map);
    }

    // 2. Get all users (Admin only)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<AppUser> users = userRepository.findAll();
        List<Map<String, Object>> response = new ArrayList<>();
        for (AppUser u : users) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("username", u.getUsername());
            map.put("role", u.getRole());
            map.put("imageUrl", u.getImageUrl());
            response.add(map);
        }
        return ResponseEntity.ok(response);
    }

    // 3. Add a new user (Admin only)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addUser(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String password = payload.get("password");
        String role = payload.getOrDefault("role", "OPERATOR");
        String imageUrl = payload.get("imageUrl");

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username and password are required."));
        }

        if (userRepository.findByUsername(username.trim()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username already exists."));
        }

        AppUser newUser = new AppUser();
        newUser.setUsername(username.trim());
        newUser.setPassword(passwordEncoder.encode(password.trim()));
        newUser.setRole(role.toUpperCase());
        newUser.setImageUrl(imageUrl != null && !imageUrl.trim().isEmpty() ? imageUrl.trim() : null);
        AppUser saved = userRepository.save(newUser);

        Map<String, Object> res = new HashMap<>();
        res.put("id", saved.getId());
        res.put("username", saved.getUsername());
        res.put("role", saved.getRole());
        res.put("imageUrl", saved.getImageUrl());
        return ResponseEntity.ok(res);
    }

    // 4. Delete a user (Admin only)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        Optional<AppUser> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AppUser user = userOpt.get();
        if ("admin".equalsIgnoreCase(user.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of("error", "The primary admin account cannot be deleted."));
        }

        userRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully."));
    }

    // 5. Update a user (Admin only)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        Optional<AppUser> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AppUser user = userOpt.get();
        String username = payload.get("username");
        String password = payload.get("password");
        String role = payload.get("role");

        if (username != null && !username.trim().isEmpty()) {
            if (!user.getUsername().equalsIgnoreCase(username.trim()) && userRepository.findByUsername(username.trim()).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username already taken."));
            }
            user.setUsername(username.trim());
        }

        if (password != null && !password.trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(password.trim()));
        }

        if (role != null && !role.trim().isEmpty()) {
            if ("admin".equalsIgnoreCase(user.getUsername()) && !"ADMIN".equalsIgnoreCase(role.trim())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Primary admin must retain ADMIN role."));
            }
            user.setRole(role.trim().toUpperCase());
        }

        if (payload.containsKey("imageUrl")) {
            String img = payload.get("imageUrl");
            user.setImageUrl(img != null && !img.trim().isEmpty() ? img.trim() : null);
        }

        AppUser updated = userRepository.save(user);
        Map<String, Object> res = new HashMap<>();
        res.put("id", updated.getId());
        res.put("username", updated.getUsername());
        res.put("role", updated.getRole());
        res.put("imageUrl", updated.getImageUrl());
        return ResponseEntity.ok(res);
    }
}
