package com.vetsoftware.app.auth.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties("vetsoftware.observability.actuator")
public class ActuatorSecurityProperties {

    private final Authentication authentication = new Authentication();

    public Authentication getAuthentication() {
        return authentication;
    }

    public static final class Authentication {

        private boolean enabled;
        private String username;
        private String password;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        void validate() {
            if (!enabled) {
                return;
            }
            if (!StringUtils.hasText(username)) {
                throw new IllegalStateException(
                        "ACTUATOR_PROMETHEUS_USERNAME is required when Actuator authentication is enabled");
            }
            if (!StringUtils.hasText(password) || password.length() < 16) {
                throw new IllegalStateException(
                        "ACTUATOR_PROMETHEUS_PASSWORD must contain at least 16 characters "
                                + "when Actuator authentication is enabled");
            }
        }
    }
}
