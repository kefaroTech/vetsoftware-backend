package com.vetsoftware.app.withholdingcertificate.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.withholdingcertificate.application.port.out.WithholdingCertificateRepository;
import com.vetsoftware.app.withholdingcertificate.testsupport.WithholdingCertificateMother;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El hermano acotado del barrido de vencimientos: lo que el tenant necesita ver
 * sin que haya que relajar el gate del listado que sirve a todas las empresas.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ListMissingWithholdingCertificatesByCompanyService — el aviso de la clinica")
class ListMissingWithholdingCertificatesByCompanyServiceTest {

    private static final LocalDate CORTE = LocalDate.of(2026, 3, 31);

    @Mock
    private WithholdingCertificateRepository repository;
    @InjectMocks
    private ListMissingWithholdingCertificatesByCompanyService service;

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("devuelve lo que le falta a esa empresa con los totales de la consulta")
        void devuelve_lo_que_le_falta_a_esa_empresa() {
            when(repository.findAllMissingByCompanyId(WithholdingCertificateMother.COMPANY_ID,
                    CORTE, 1, 4))
                    .thenReturn(PageResult.of(List.of(WithholdingCertificateMother.conId(41L)), 1,
                            4, 9L));

            PageResult<?> pagina = service
                    .listMissingByCompany(WithholdingCertificateMother.COMPANY_ID, CORTE, 1, 4);

            assertThat(pagina.content()).hasSize(1);
            assertThat(pagina.totalElements()).isEqualTo(9L);
            assertThat(pagina.totalPages()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("traslada los cuatro argumentos en su orden y nunca barre sin empresa")
        void traslada_los_cuatro_argumentos_y_nunca_barre_sin_empresa() {
            // Los cuatro con valores distintos entre si: cruzar page con pageSize
            // compila sin una queja y solo se ve paginando de verdad.
            when(repository.findAllMissingByCompanyId(any(), any(), anyInt(), anyInt()))
                    .thenReturn(PageResult.empty(1, 4));

            service.listMissingByCompany(WithholdingCertificateMother.COMPANY_ID, CORTE, 1, 4);

            verify(repository).findAllMissingByCompanyId(WithholdingCertificateMother.COMPANY_ID,
                    CORTE, 1, 4);
            verify(repository, never()).findAllMissing(any(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("una empresa sin pendientes no ve las de las demas")
        void una_empresa_sin_pendientes_no_ve_las_de_las_demas() {
            when(repository.findAllMissingByCompanyId(WithholdingCertificateMother.OTRA_COMPANY_ID,
                    CORTE, 0, 20)).thenReturn(PageResult.empty(0, 20));

            assertThat(service.listMissingByCompany(WithholdingCertificateMother.OTRA_COMPANY_ID,
                    CORTE, 0, 20).content()).isEmpty();
        }
    }
}
