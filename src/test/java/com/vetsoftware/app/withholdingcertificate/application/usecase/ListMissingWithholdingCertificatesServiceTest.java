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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El barrido de vencimientos de plataforma. Es el listado por el que existe
 * {@code legal_deadline_on} como columna guardada.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ListMissingWithholdingCertificatesService — lo que falta antes de que venza")
class ListMissingWithholdingCertificatesServiceTest {

    private static final LocalDate CORTE = LocalDate.of(2026, 3, 31);

    @Mock
    private WithholdingCertificateRepository repository;
    @InjectMocks
    private ListMissingWithholdingCertificatesService service;

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("devuelve los que aun no han llegado, proyectados a DTO")
        void devuelve_los_que_aun_no_han_llegado() {
            when(repository.findAllMissing(CORTE, 0, 20)).thenReturn(
                    PageResult.of(List.of(WithholdingCertificateMother.conId(41L)), 0, 20, 1L));

            assertThat(service.listMissing(CORTE, 0, 20).content()).singleElement()
                    .satisfies(dto -> {
                        assertThat(dto.id()).isEqualTo(41L);
                        assertThat(dto.receivedOn()).isNull();
                        assertThat(dto.supported()).isFalse();
                    });
        }

        @Test
        @DisplayName("sin vencimientos pendientes devuelve la pagina vacia")
        void sin_vencimientos_pendientes_devuelve_pagina_vacia() {
            when(repository.findAllMissing(CORTE, 0, 20)).thenReturn(PageResult.empty(0, 20));

            assertThat(service.listMissing(CORTE, 0, 20).totalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("la fecha de corte la pone quien llama y el servicio no la inventa")
        void la_fecha_de_corte_la_pone_quien_llama() {
            // Un LocalDate.now() aqui dentro le quitaria a la consola la decision de
            // mirar lo que vence hoy o lo que vence en un mes, y ademas haria el caso
            // de uso imposible de probar de forma determinista.
            when(repository.findAllMissing(any(), anyInt(), anyInt()))
                    .thenReturn(PageResult.empty(0, 20));

            service.listMissing(LocalDate.of(2027, 1, 15), 4, 9);

            ArgumentCaptor<LocalDate> corte = ArgumentCaptor.forClass(LocalDate.class);
            verify(repository).findAllMissing(corte.capture(), org.mockito.ArgumentMatchers.eq(4),
                    org.mockito.ArgumentMatchers.eq(9));
            assertThat(corte.getValue()).isEqualTo(LocalDate.of(2027, 1, 15));
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("nunca acota por empresa: es el barrido que solo sirve un principal SYSTEM")
        void nunca_acota_por_empresa() {
            when(repository.findAllMissing(any(), anyInt(), anyInt()))
                    .thenReturn(PageResult.empty(0, 20));

            service.listMissing(CORTE, 0, 20);

            verify(repository, never()).findAllMissingByCompanyId(any(), any(), anyInt(), anyInt());
        }
    }
}
