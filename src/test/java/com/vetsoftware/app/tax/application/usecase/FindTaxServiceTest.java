package com.vetsoftware.app.tax.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.tax.application.dto.TaxDto;
import com.vetsoftware.app.tax.application.port.out.TaxRepository;
import com.vetsoftware.app.tax.domain.TaxNotFoundException;
import com.vetsoftware.app.tax.testsupport.TaxMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindTaxService")
class FindTaxServiceTest {

    @Mock
    private TaxRepository repository;

    @InjectMocks
    private FindTaxService service;

    @Nested
    @DisplayName("Consulta")
    class Consulta {

        @Test
        @DisplayName("devuelve el DTO del impuesto cuando existe en la empresa")
        void devuelve_el_dto_del_impuesto() {
            when(repository.findByIdAndCompanyId(TaxMother.TAX_ID, TaxMother.COMPANY_ID))
                    .thenReturn(Optional.of(TaxMother.activo()));

            TaxDto dto = service.findById(TaxMother.TAX_ID, TaxMother.COMPANY_ID);

            assertThat(dto.id()).isEqualTo(TaxMother.TAX_ID);
        }
    }

    @Nested
    @DisplayName("Fallos")
    class Fallos {

        @Test
        @DisplayName("lanza TaxNotFoundException si no existe en la empresa")
        void lanza_excepcion_si_no_existe() {
            when(repository.findByIdAndCompanyId(TaxMother.TAX_ID, TaxMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(TaxMother.TAX_ID, TaxMother.COMPANY_ID))
                    .isInstanceOf(TaxNotFoundException.class)
                    .hasMessageContaining("Tax not found: " + TaxMother.TAX_ID);
        }
    }
}
