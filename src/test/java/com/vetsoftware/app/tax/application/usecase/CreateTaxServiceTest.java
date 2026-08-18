package com.vetsoftware.app.tax.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.tax.application.dto.TaxDto;
import com.vetsoftware.app.tax.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.tax.application.port.out.TaxRepository;
import com.vetsoftware.app.tax.domain.TaxNameAlreadyExistsException;
import com.vetsoftware.app.tax.testsupport.TaxMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateTaxService")
class CreateTaxServiceTest {

    @Mock
    private TaxRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;

    @InjectMocks
    private CreateTaxService service;

    @Captor
    private ArgumentCaptor<com.vetsoftware.app.tax.domain.Tax> taxCaptor;

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("crea el impuesto con la empresa resuelta por el puerto")
        void crea_el_impuesto_con_la_empresa_resuelta() {
            when(companyQueryPort.findById(TaxMother.COMPANY_ID))
                    .thenReturn(Optional.of(TaxMother.EMPRESA));
            when(repository.existsByCompanyIdAndName(TaxMother.COMPANY_ID, "IVA General"))
                    .thenReturn(false);
            when(repository.save(taxCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

            TaxDto dto = service.execute(TaxMother.comandoCrear());

            assertThat(taxCaptor.getValue().getCompany()).isEqualTo(TaxMother.EMPRESA);
            assertThat(dto.name()).isEqualTo("IVA General");
        }
    }

    @Nested
    @DisplayName("Fallos")
    class Fallos {

        @Test
        @DisplayName("no crea el impuesto si la empresa no existe")
        void no_crea_el_impuesto_si_la_empresa_no_existe() {
            when(companyQueryPort.findById(TaxMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(TaxMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + TaxMother.COMPANY_ID);

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("no crea el impuesto si el nombre ya existe activo en la empresa")
        void no_crea_el_impuesto_si_el_nombre_ya_existe() {
            when(companyQueryPort.findById(TaxMother.COMPANY_ID))
                    .thenReturn(Optional.of(TaxMother.EMPRESA));
            when(repository.existsByCompanyIdAndName(TaxMother.COMPANY_ID, "IVA General"))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.execute(TaxMother.comandoCrear()))
                    .isInstanceOf(TaxNameAlreadyExistsException.class)
                    .hasMessageContaining("IVA General");
        }
    }
}
