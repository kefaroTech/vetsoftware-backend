package com.vetsoftware.app.company.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.application.dto.CompanyDto;
import com.vetsoftware.app.company.application.port.out.CompanyRepository;
import com.vetsoftware.app.company.domain.CompanyNotFoundException;
import com.vetsoftware.app.company.testsupport.CompanyMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivateCompanyService")
class ReactivateCompanyServiceTest {

    @Mock
    private CompanyRepository repository;

    @InjectMocks
    private ReactivateCompanyService service;

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("reactiva y devuelve el DTO de la empresa ya habilitada")
        void reactiva_y_devuelve_el_dto() {
            when(repository.reactivate(CompanyMother.COMPANY_ID)).thenReturn(1);
            when(repository.findById(CompanyMother.COMPANY_ID))
                    .thenReturn(Optional.of(CompanyMother.clinicaNorte()));

            CompanyDto dto = service.execute(CompanyMother.COMPANY_ID);

            assertThat(dto.id()).isEqualTo(CompanyMother.COMPANY_ID);
            assertThat(dto.enabled()).isTrue();
            verify(repository).reactivate(CompanyMother.COMPANY_ID);
        }
    }

    @Nested
    @DisplayName("caminos de error")
    class Errores {

        @Test
        @DisplayName("ninguna fila afectada: no existe, no consulta el detalle")
        void ninguna_fila_afectada_no_consulta_el_detalle() {
            when(repository.reactivate(CompanyMother.COMPANY_ID)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(CompanyMother.COMPANY_ID))
                    .isInstanceOf(CompanyNotFoundException.class)
                    .hasMessageContaining("Company not found: 9");

            verify(repository, never()).findById(any());
        }

        @Test
        @DisplayName("fila afectada pero el detalle ya no existe: tambien es empresa no encontrada")
        void fila_afectada_pero_sin_detalle_lanza_no_encontrada() {
            when(repository.reactivate(CompanyMother.COMPANY_ID)).thenReturn(1);
            when(repository.findById(CompanyMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(CompanyMother.COMPANY_ID))
                    .isInstanceOf(CompanyNotFoundException.class)
                    .hasMessageContaining("Company not found: 9");
        }
    }
}
