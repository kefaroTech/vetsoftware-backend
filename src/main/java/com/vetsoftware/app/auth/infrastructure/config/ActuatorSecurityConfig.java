package com.vetsoftware.app.auth.infrastructure.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ActuatorSecurityProperties.class)
public class ActuatorSecurityConfig {

    static final String OBSERVABILITY_ROLE = "OBSERVABILITY";

    @Bean
    @Order(1)
    SecurityFilterChain actuatorSecurityFilterChain(
            HttpSecurity http,
            ActuatorSecurityProperties properties,
            @Qualifier("actuatorUserDetailsService")
            UserDetailsService userDetailsService) throws Exception {
        var authentication = properties.getAuthentication();

        http
            .securityMatcher(EndpointRequest.toAnyEndpoint())
            .csrf(csrf -> csrf.disable())
            .requestCache(cache -> cache.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(EndpointRequest.to("health")).permitAll()
                .requestMatchers(EndpointRequest.to("prometheus", "metrics", "info"))
                    .hasRole(OBSERVABILITY_ROLE)
                .anyRequest().denyAll());

        if (authentication.isEnabled()) {
            http
                .authenticationProvider(authenticationProvider(userDetailsService))
                .httpBasic(withDefaults());
        }

        return http.build();
    }

    @Bean
    UserDetailsService actuatorUserDetailsService(ActuatorSecurityProperties properties) {
        var authentication = properties.getAuthentication();
        if (!authentication.isEnabled()) {
            return new InMemoryUserDetailsManager();
        }
        authentication.validate();
        PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        UserDetails prometheusUser = User.builder()
            .username(authentication.getUsername())
            .password(passwordEncoder.encode(authentication.getPassword()))
            .roles(OBSERVABILITY_ROLE)
            .build();
        return new InMemoryUserDetailsManager(prometheusUser);
    }

    private static DaoAuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService) {
        PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }
}
