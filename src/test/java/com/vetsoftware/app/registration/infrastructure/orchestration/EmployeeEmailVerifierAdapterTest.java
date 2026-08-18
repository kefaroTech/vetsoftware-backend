package com.vetsoftware.app.registration.infrastructure.orchestration;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.vetsoftware.app.employee.application.port.in.VerifyEmployeeEmailUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeEmailVerifierAdapter")
class EmployeeEmailVerifierAdapterTest {

    @Mock
    private VerifyEmployeeEmailUseCase verifyEmployeeEmailUseCase;
    @InjectMocks
    private EmployeeEmailVerifierAdapter adapter;

    @Test
    @DisplayName("delega la verificación del correo en el caso de uso de employee")
    void delega_la_verificacion_en_el_caso_de_uso() {
        adapter.verify(55L);

        verify(verifyEmployeeEmailUseCase).execute(55L);
        verifyNoMoreInteractions(verifyEmployeeEmailUseCase);
    }
}
