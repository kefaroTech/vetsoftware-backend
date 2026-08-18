package com.vetsoftware.app.auth.infrastructure.persistence;

import static org.mockito.Mockito.verify;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JpaEmployeeActivationPortTest {

    @Mock
    private EmployeeJpaRepository employeeJpaRepository;
    @InjectMocks
    private JpaEmployeeActivationPort port;

    @Test
    @DisplayName("activateOnLogin delega en el UPDATE que pasa de INVITED a ACTIVE")
    void activate_on_login_delega_en_el_update() {
        port.activateOnLogin(7L, 3L);

        verify(employeeJpaRepository).activateInvited(7L, 3L);
    }
}
