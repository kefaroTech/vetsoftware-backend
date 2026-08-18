package com.vetsoftware.app.tax.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.tax.application.dto.TaxDto;
import com.vetsoftware.app.tax.application.port.out.TaxRepository;
import com.vetsoftware.app.tax.testsupport.TaxMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListTaxesService")
class ListTaxesServiceTest {

    @Mock
    private TaxRepository repository;

    @InjectMocks
    private ListTaxesService service;

    @Nested
    @DisplayName("listByCompany")
    class ListByCompany {

        @Test
        @DisplayName("mapea los impuestos activos de la empresa")
        void mapea_los_impuestos_activos_de_la_empresa() {
            when(repository.findAllByCompanyId(TaxMother.COMPANY_ID))
                    .thenReturn(List.of(TaxMother.activo()));

            List<TaxDto> dtos = service.listByCompany(TaxMother.COMPANY_ID);

            assertThat(dtos).extracting(TaxDto::id).containsExactly(TaxMother.TAX_ID);
        }
    }

    @Nested
    @DisplayName("listDisabledByCompany")
    class ListDisabledByCompany {

        @Test
        @DisplayName("mapea los impuestos pausados de la empresa")
        void mapea_los_impuestos_pausados_de_la_empresa() {
            when(repository.findAllDisabledByCompanyId(TaxMother.COMPANY_ID))
                    .thenReturn(List.of(TaxMother.inactivo()));

            List<TaxDto> dtos = service.listDisabledByCompany(TaxMother.COMPANY_ID);

            assertThat(dtos).extracting(TaxDto::enabled).containsExactly(false);
        }
    }
}
