package com.vetsoftware.app.withholdingcertificate.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.withholdingcertificate.application.dto.WithholdingCertificateDto;
import com.vetsoftware.app.withholdingcertificate.application.port.out.WithholdingCertificateRepository;
import com.vetsoftware.app.withholdingcertificate.testsupport.WithholdingCertificateMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListWithholdingCertificatesService — los certificados de una empresa")
class ListWithholdingCertificatesServiceTest {

    @Mock
    private WithholdingCertificateRepository repository;
    @InjectMocks
    private ListWithholdingCertificatesService service;

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("conserva los totales de la consulta y no los recalcula sobre la pagina")
        void conserva_los_totales_de_la_consulta() {
            // Recalcularlos sobre el contenido ya paginado es como se acaba reportando
            // «1 de 1» en un listado de treinta y siete.
            when(repository.findAllByCompanyId(WithholdingCertificateMother.COMPANY_ID, 2, 5))
                    .thenReturn(PageResult.of(List.of(WithholdingCertificateMother.conId(41L)), 2,
                            5, 37L));

            PageResult<WithholdingCertificateDto> pagina = service
                    .listByCompany(WithholdingCertificateMother.COMPANY_ID, 2, 5);

            assertThat(pagina.content()).hasSize(1);
            assertThat(pagina.content().getFirst().id()).isEqualTo(41L);
            assertThat(pagina.page()).isEqualTo(2);
            assertThat(pagina.pageSize()).isEqualTo(5);
            assertThat(pagina.totalElements()).isEqualTo(37L);
            assertThat(pagina.totalPages()).isEqualTo(8);
        }

        @Test
        @DisplayName("una empresa sin certificados devuelve la pagina vacia, no un nulo")
        void una_empresa_sin_certificados_devuelve_pagina_vacia() {
            when(repository.findAllByCompanyId(WithholdingCertificateMother.OTRA_COMPANY_ID, 0, 20))
                    .thenReturn(PageResult.empty(0, 20));

            assertThat(service.listByCompany(WithholdingCertificateMother.OTRA_COMPANY_ID, 0, 20)
                    .content()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("traslada la empresa recibida a la consulta y nunca barre sin filtro")
        void traslada_la_empresa_y_nunca_barre_sin_filtro() {
            when(repository.findAllByCompanyId(any(), anyInt(), anyInt()))
                    .thenReturn(PageResult.empty(0, 20));

            service.listByCompany(WithholdingCertificateMother.COMPANY_ID, 0, 20);

            verify(repository).findAllByCompanyId(WithholdingCertificateMother.COMPANY_ID, 0, 20);
            verify(repository, never()).findAll(anyInt(), anyInt());
        }
    }
}
