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
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El barrido de la consola de plataforma. El puerto esta cerrado a
 * {@code hasRole('SYSTEM')} a secas y un principal SYSTEM no tiene empresa
 * propia, asi que el filtro es opcional: con el acota, sin el barre.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ListAllWithholdingCertificatesService — el barrido cross-tenant")
class ListAllWithholdingCertificatesServiceTest {

    @Mock
    private WithholdingCertificateRepository repository;
    @InjectMocks
    private ListAllWithholdingCertificatesService service;

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("sin empresa barre todas y devuelve la pagina con sus totales")
        void sin_empresa_barre_todas() {
            when(repository.findAll(0, 20)).thenReturn(
                    PageResult.of(List.of(WithholdingCertificateMother.conId(41L)), 0, 20, 1L));

            assertThat(service.listAll(null, 0, 20).content()).singleElement()
                    .satisfies(dto -> assertThat(dto.id()).isEqualTo(41L));
        }

        @Test
        @DisplayName("con empresa acota el barrido a esa empresa")
        void con_empresa_acota_el_barrido() {
            when(repository.findAllByCompanyId(WithholdingCertificateMother.COMPANY_ID, 3, 7))
                    .thenReturn(PageResult.empty(3, 7));

            assertThat(service.listAll(WithholdingCertificateMother.COMPANY_ID, 3, 7).page())
                    .isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("el nulo elige el barrido completo y no una empresa inexistente")
        void el_nulo_elige_el_barrido_completo() {
            // Convertir el nulo en un 0L filtraria por una empresa que no existe y el
            // barrido saldria vacio sin que nadie lo notara.
            when(repository.findAll(anyInt(), anyInt())).thenReturn(PageResult.empty(0, 20));

            service.listAll(null, 0, 20);

            verify(repository).findAll(0, 20);
            verify(repository, never()).findAllByCompanyId(any(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("con empresa no llama nunca al barrido completo")
        void con_empresa_no_llama_al_barrido_completo() {
            when(repository.findAllByCompanyId(any(), anyInt(), anyInt()))
                    .thenReturn(PageResult.empty(0, 20));

            service.listAll(WithholdingCertificateMother.COMPANY_ID, 0, 20);

            verify(repository, never()).findAll(anyInt(), anyInt());
        }
    }
}
