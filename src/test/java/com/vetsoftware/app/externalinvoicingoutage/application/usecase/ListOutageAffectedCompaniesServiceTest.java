package com.vetsoftware.app.externalinvoicingoutage.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.externalinvoicingoutage.application.dto.OutageAffectedCompanyDto;
import com.vetsoftware.app.externalinvoicingoutage.application.port.out.ExternalInvoicingOutageCompanyRepository;
import com.vetsoftware.app.externalinvoicingoutage.application.port.out.ExternalInvoicingOutageRepository;
import com.vetsoftware.app.externalinvoicingoutage.domain.ExternalInvoicingOutageCompany;
import com.vetsoftware.app.externalinvoicingoutage.domain.ExternalInvoicingOutageNotFoundException;
import com.vetsoftware.app.externalinvoicingoutage.testsupport.ExternalInvoicingOutageMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListOutageAffectedCompaniesService")
class ListOutageAffectedCompaniesServiceTest {

    @Mock
    private ExternalInvoicingOutageCompanyRepository repository;
    @Mock
    private ExternalInvoicingOutageRepository outageRepository;

    @InjectMocks
    private ListOutageAffectedCompaniesService service;

    @Nested
    @DisplayName("caida inexistente")
    class Inexistente {

        @Test
        @DisplayName("404 en vez de una pagina vacia, y no consulta el reparto")
        void revienta_sin_consultar_el_reparto() {
            when(outageRepository.findById(ExternalInvoicingOutageMother.OUTAGE_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.listByOutage(ExternalInvoicingOutageMother.OUTAGE_ID, 0, 20))
                    .isInstanceOf(ExternalInvoicingOutageNotFoundException.class)
                    .hasMessageContaining("External invoicing outage not found: "
                            + ExternalInvoicingOutageMother.OUTAGE_ID);

            verifyNoInteractions(repository);
        }
    }

    @Nested
    @DisplayName("listado")
    class Listado {

        @Test
        @DisplayName("mapea el reparto conservando la paginacion del repositorio")
        void mapea_el_reparto_conservando_la_paginacion() {
            when(outageRepository.findById(ExternalInvoicingOutageMother.OUTAGE_ID))
                    .thenReturn(Optional.of(ExternalInvoicingOutageMother.abierta()));
            ExternalInvoicingOutageCompany afectada = ExternalInvoicingOutageMother.afectada();
            when(repository.findAllByOutageId(ExternalInvoicingOutageMother.OUTAGE_ID, 0, 20))
                    .thenReturn(new PageResult<>(List.of(afectada), 0, 20, 1L, 1));

            PageResult<OutageAffectedCompanyDto> resultado = service
                    .listByOutage(ExternalInvoicingOutageMother.OUTAGE_ID, 0, 20);

            assertThat(resultado.content()).hasSize(1);
            assertThat(resultado.content().get(0).companyId())
                    .isEqualTo(ExternalInvoicingOutageMother.COMPANY_ID);
            assertThat(resultado.content().get(0).contingencyNumbering()).isTrue();
            assertThat(resultado.totalElements()).isEqualTo(1L);
        }
    }
}
