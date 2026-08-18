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
@DisplayName("ReactivateTaxService")
class ReactivateTaxServiceTest {

    @Mock
    private TaxRepository repository;

    @InjectMocks
    private ReactivateTaxService service;

    @Nested
    @DisplayName("Reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("reactiva y devuelve el impuesto ya activo")
        void reactiva_y_devuelve_el_impuesto_activo() {
            when(repository.reactivate(TaxMother.TAX_ID, TaxMother.COMPANY_ID)).thenReturn(1);
            when(repository.findByIdAndCompanyId(TaxMother.TAX_ID, TaxMother.COMPANY_ID))
                    .thenReturn(Optional.of(TaxMother.activo()));

            TaxDto dto = service.execute(TaxMother.TAX_ID, TaxMother.COMPANY_ID);

            assertThat(dto.enabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Fallos")
    class Fallos {

        @Test
        @DisplayName("lanza TaxNotFoundException si no reactivo ninguna fila")
        void lanza_excepcion_si_no_reactivo_ninguna_fila() {
            when(repository.reactivate(TaxMother.TAX_ID, TaxMother.COMPANY_ID)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(TaxMother.TAX_ID, TaxMother.COMPANY_ID))
                    .isInstanceOf(TaxNotFoundException.class)
                    .hasMessageContaining("Tax not found: " + TaxMother.TAX_ID);
        }
    }
}
