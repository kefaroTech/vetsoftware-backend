package com.vetsoftware.app.registration.infrastructure.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.company.application.command.CreateCompanyCommand;
import com.vetsoftware.app.company.application.dto.CitySummaryDto;
import com.vetsoftware.app.company.application.dto.CompanyDto;
import com.vetsoftware.app.company.application.port.in.CreateCompanyUseCase;
import com.vetsoftware.app.registration.application.port.out.CompanyCreator.CompanyResult;
import java.time.LocalDateTime;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Único punto donde se orquesta la creación de la empresa durante el signup
 * público. Aquí vive el caso "NIT ya registrado": el adaptador no lo valida —
 * eso pasa dentro de {@code CreateCompanyUseCase}, mockeado aquí — pero debe
 * dejar propagar la excepción sin envolverla.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateCompanyAdapter")
class CreateCompanyAdapterTest {

    @Mock
    private CreateCompanyUseCase createCompanyUseCase;
    @Mock
    private SystemAuthRunner systemAuthRunner;
    @InjectMocks
    private CreateCompanyAdapter adapter;

    @BeforeEach
    void setUp() {
        when(systemAuthRunner.call(any()))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get());
    }

    private static CompanyDto dto() {
        return new CompanyDto(9L, "Veterinaria Vetrina", "900123456", "Calle 1 # 2-3", "3001234567",
                new CitySummaryDto(11001L, "Bogotá"), LocalDateTime.of(2026, 1, 15, 10, 30), true);
    }

    @Test
    @DisplayName("crea la empresa bajo el contexto de sistema con los datos mapeados")
    void crea_la_empresa_bajo_contexto_de_sistema() {
        when(createCompanyUseCase.execute(any(CreateCompanyCommand.class))).thenReturn(dto());

        CompanyResult result = adapter.create("Veterinaria Vetrina", "900123456", "Calle 1 # 2-3",
                "3001234567", 11001L);

        ArgumentCaptor<CreateCompanyCommand> captor = ArgumentCaptor
                .forClass(CreateCompanyCommand.class);
        verify(createCompanyUseCase).execute(captor.capture());
        CreateCompanyCommand command = captor.getValue();
        assertThat(command.name()).isEqualTo("Veterinaria Vetrina");
        assertThat(command.identifier()).isEqualTo("900123456");
        assertThat(command.address()).isEqualTo("Calle 1 # 2-3");
        assertThat(command.contactNumber()).isEqualTo("3001234567");
        assertThat(command.cityId()).isEqualTo(11001L);

        assertThat(result.id()).isEqualTo(9L);
        assertThat(result.name()).isEqualTo("Veterinaria Vetrina");
        assertThat(result.identifier()).isEqualTo("900123456");
        verify(systemAuthRunner).call(any());
    }

    @Test
    @DisplayName("un NIT ya registrado propaga la excepción del caso de uso sin envolverla")
    void un_nit_duplicado_propaga_la_excepcion() {
        when(createCompanyUseCase.execute(any(CreateCompanyCommand.class))).thenThrow(
                new IllegalArgumentException("Company identifier already in use: 900123456"));

        assertThatThrownBy(() -> adapter.create("Veterinaria Vetrina", "900123456", "Calle 1 # 2-3",
                "3001234567", 11001L)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already in use");
    }
}
