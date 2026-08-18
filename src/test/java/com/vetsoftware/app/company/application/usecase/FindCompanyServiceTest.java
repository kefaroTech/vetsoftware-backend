package com.vetsoftware.app.company.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
@DisplayName("FindCompanyService")
class FindCompanyServiceTest {

    @Mock
    private CompanyRepository repository;

    @InjectMocks
    private FindCompanyService service;

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("devuelve el DTO de la empresa encontrada")
        void devuelve_el_dto_de_la_empresa_encontrada() {
            when(repository.findById(CompanyMother.COMPANY_ID))
                    .thenReturn(Optional.of(CompanyMother.clinicaNorte()));

            CompanyDto dto = service.findById(CompanyMother.COMPANY_ID);

            assertThat(dto.id()).isEqualTo(CompanyMother.COMPANY_ID);
            assertThat(dto.name()).isEqualTo("Clinica Norte");
            assertThat(dto.city().name()).isEqualTo("Bogota");
            assertThat(dto.membership().status()).isEqualTo("ACTIVE");
        }
    }

    @Nested
    @DisplayName("empresa inexistente")
    class EmpresaInexistente {

        @Test
        @DisplayName("lanza CompanyNotFoundException con el id")
        void lanza_company_not_found_exception() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(99L))
                    .isInstanceOf(CompanyNotFoundException.class)
                    .hasMessageContaining("Company not found: 99");
        }
    }
}
