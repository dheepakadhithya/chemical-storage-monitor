package com.college.chemical_storage_monitor.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            if (request.getRequestURI().startsWith("/api/")) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"error\":\"Forbidden: Insufficient privileges for role\"}");
            } else {
                response.sendRedirect("/index.html?error=access_denied");
            }
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // 1. Crucial IoT / Microcontroller endpoints: DHT11 sensor posting, active threshold, and actuator controls
                        .requestMatchers(HttpMethod.POST, "/api/readings").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/chemicals/active").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/controls").permitAll()

                        // 2. Public assets: Login page, static stylesheets, scripts, media, and icons
                        .requestMatchers(
                                "/login.html", "/login.css", "/login.js",
                                "/images/**", "/users/**", "/chemical/**", "/backgrounds/**", "/favicon.ico"
                        ).permitAll()

                        // 3. User Identity Endpoint: Required by ANY authenticated user (ADMIN, OPERATOR, VIEWER) for top header profile
                        .requestMatchers(HttpMethod.GET, "/api/users/current").authenticated()

                        // 4. User Management: Strictly ADMIN ONLY
                        .requestMatchers("/users.html", "/users.js").hasRole("ADMIN")
                        .requestMatchers("/api/users/**").hasRole("ADMIN")

                        // 5. Chemical & Actuator Modifying Operations: ADMIN & OPERATOR ONLY
                        .requestMatchers(HttpMethod.POST, "/api/controls/**").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.POST, "/api/chemicals/active/**").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.POST, "/api/chemicals").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.PUT, "/api/chemicals/**").hasAnyRole("ADMIN", "OPERATOR")
                        // Chemical Deletion: Strictly ADMIN ONLY
                        .requestMatchers(HttpMethod.DELETE, "/api/chemicals/**").hasRole("ADMIN")

                        // 6. Chemical Viewing: All authenticated roles (ADMIN, OPERATOR, VIEWER)
                        .requestMatchers(HttpMethod.GET, "/api/chemicals/**").authenticated()

                        // 7. Telemetry & Sensor Readings: All authenticated roles (ADMIN, OPERATOR, VIEWER)
                        .requestMatchers(HttpMethod.GET, "/api/readings/**", "/api/alerts", "/api/history").authenticated()

                        // 8. AI Voice/Chat Assistant: All authenticated roles
                        .requestMatchers("/api/ai/**").authenticated()

                        // 9. Dashboard & General Web Pages
                        .requestMatchers("/", "/index.html", "/dashboard.html", "/chemicals.html", "/style.css", "/app.js", "/chemicals.js", "/header.js").authenticated()

                        // 9. All other requests must be authenticated
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login.html")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/index.html", true)
                        .failureUrl("/login.html?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login.html?logout=true")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login.html"))
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(1)
                )
                .build();
    }
}

