package com.vetsoftware.app.tax.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.tax.application.port.out.TaxChildrenQueryPort;
import com.vetsoftware.app.tax.application.port.out.TaxRepository;
import com.vetsoftware.app.tax.domain.TaxHasActiveChildrenException;
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
@DisplayName("DeleteTaxService")
class DeleteTaxServiceTest {

    @Mock
    private TaxRepository repository;
    @Mock
    private TaxChildrenQueryPort taxChildrenQueryPort;

    @InjectMocks
    private DeleteTaxService service;

    private void impuestoExiste() {
        when(repository.findByIdAndCompanyId(TaxMother.TAX_ID, TaxMother.COMPANY_ID))
                .thenReturn(Optional.of(TaxMother.activo()));
    }

    @Nested
    @DisplayName("Eliminacion")
    class Eliminacion {

        @Test
        @DisplayName("borra el impuesto sin hijos activos")
        void borra_el_impuesto_sin_hijos_activos() {
            impuestoExiste();
            when(taxChildrenQueryPort.existsActiveByTaxId(TaxMother.TAX_ID)).thenReturn(false);

            service.execute(TaxMother.TAX_ID, TaxMother.COMPANY_ID);

            verify(repository).delete(TaxMother.TAX_ID);
        }
    }

    @Nested
    @DisplayName("Fallos")
    class Fallos {

        @Test
        @DisplayName("no borra un impuesto que no existe en la empresa")
        void no_borra_un_impuesto_inexistente() {
            when(repository.findByIdAndCompanyId(TaxMother.TAX_ID, TaxMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(TaxMother.TAX_ID, TaxMother.COMPANY_ID))
                    .isInstanceOf(TaxNotFoundException.class)
                    .hasMessageContaining("Tax not found: " + TaxMother.TAX_ID);

            verifyNoInteractions(taxChildrenQueryPort);
            verify(repository, never()).delete(TaxMother.TAX_ID);
        }

        @Test
        @DisplayName("no borra un impuesto con productos o servicios activos")
        void no_borra_un_impuesto_con_hijos_activos() {
            impuestoExiste();
            when(taxChildrenQueryPort.existsActiveByTaxId(TaxMother.TAX_ID)).thenReturn(true);

            assertThatThrownBy(() -> service.execute(TaxMother.TAX_ID, TaxMother.COMPANY_ID))
                    .isInstanceOf(TaxHasActiveChildrenException.class)
                    .hasMessageContaining("has active product/service children");

            verify(repository, never()).delete(TaxMother.TAX_ID);
        }
    }
}
