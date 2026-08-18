package com.vetsoftware.app.tax.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.tax.application.dto.TaxDto;
import com.vetsoftware.app.tax.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.tax.application.port.out.TaxRepository;
import com.vetsoftware.app.tax.domain.Tax;
import com.vetsoftware.app.tax.domain.TaxNameAlreadyExistsException;
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
@DisplayName("UpdateTaxService")
class UpdateTaxServiceTest {

    @Mock
    private TaxRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;

    @InjectMocks
    private UpdateTaxService service;

    private void impuestoExiste(Tax tax) {
        when(repository.findByIdAndCompanyId(TaxMother.TAX_ID, TaxMother.COMPANY_ID))
                .thenReturn(Optional.of(tax));
    }

    @Nested
    @DisplayName("Actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("actualiza el impuesto con la empresa resuelta por el puerto")
        void actualiza_el_impuesto_con_la_empresa_resuelta() {
            impuestoExiste(TaxMother.activo());
            when(companyQueryPort.findById(TaxMother.COMPANY_ID))
                    .thenReturn(Optional.of(TaxMother.EMPRESA));
            when(repository.existsByCompanyIdAndNameExcludingId(TaxMother.COMPANY_ID,
                    "IVA Reducido", TaxMother.TAX_ID)).thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TaxDto dto = service.execute(TaxMother.comandoActualizar());

            assertThat(dto.name()).isEqualTo("IVA Reducido");
            assertThat(dto.percentage()).isEqualByComparingTo("5.00");
        }
    }

    @Nested
    @DisplayName("Fallos")
    class Fallos {

        @Test
        @DisplayName("no actualiza un impuesto que no existe en la empresa")
        void no_actualiza_un_impuesto_inexistente() {
            when(repository.findByIdAndCompanyId(TaxMother.TAX_ID, TaxMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(TaxMother.comandoActualizar()))
                    .isInstanceOf(TaxNotFoundException.class)
                    .hasMessageContaining("Tax not found: " + TaxMother.TAX_ID);

            verifyNoInteractions(companyQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("no actualiza si la empresa no existe")
        void no_actualiza_si_la_empresa_no_existe() {
            impuestoExiste(TaxMother.activo());
            when(companyQueryPort.findById(TaxMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(TaxMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + TaxMother.COMPANY_ID);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("no actualiza si el nuevo nombre ya existe activo en otro impuesto")
        void no_actualiza_si_el_nombre_ya_existe() {
            impuestoExiste(TaxMother.activo());
            when(companyQueryPort.findById(TaxMother.COMPANY_ID))
                    .thenReturn(Optional.of(TaxMother.EMPRESA));
            when(repository.existsByCompanyIdAndNameExcludingId(TaxMother.COMPANY_ID,
                    "IVA Reducido", TaxMother.TAX_ID)).thenReturn(true);

            assertThatThrownBy(() -> service.execute(TaxMother.comandoActualizar()))
                    .isInstanceOf(TaxNameAlreadyExistsException.class)
                    .hasMessageContaining("IVA Reducido");

            verify(repository, never()).save(any());
        }
    }
}
