package com.vetsoftware.app.auth.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import org.junit.jupiter.api.Test;

class ActuatorSecurityPropertiesTest {

    @Test
    void rejectsEnabledAuthenticationWithoutStrongCredentials() {
        var authentication = new ActuatorSecurityProperties.Authentication();
        authentication.setEnabled(true);
        authentication.setUsername("prometheus");
        authentication.setPassword("short");

        assertThatIllegalStateException().isThrownBy(authentication::validate)
                .withMessageContaining("at least 16 characters");
    }

    @Test
    void doesNotRequireCredentialsWhenOnlyPublicHealthIsExposed() {
        var authentication = new ActuatorSecurityProperties.Authentication();

        assertThatCode(authentication::validate).doesNotThrowAnyException();
    }
}
