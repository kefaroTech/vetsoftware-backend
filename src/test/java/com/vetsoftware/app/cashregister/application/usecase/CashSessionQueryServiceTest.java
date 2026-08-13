package com.vetsoftware.app.cashregister.application.usecase;

import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.BRANCH_ID;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.COMPANY_ID;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.EMPLEADO_ID;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.OTRA_BRANCH_ID;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.OTRA_COMPANY_ID;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.SESSION_ID;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.sesionCerrada;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.sesionConMovimientos;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.cashregister.application.command.SearchCashSessionsQuery;
import com.vetsoftware.app.cashregister.application.dto.CashSessionView;
import com.vetsoftware.app.cashregister.application.dto.PageResult;
import com.vetsoftware.app.cashregister.application.port.out.CashSessionRepository;
import com.vetsoftware.app.cashregister.domain.CashSessionNotFoundException;
import com.vetsoftware.app.cashregister.domain.CashSessionStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Lecturas de caja. Modernizado a Mockito el 2026-08-12 al tocar la feature (la
 * version anterior usaba el fake in-memory y terminaba afirmando sobre el
 * propio fake en vez de sobre el servicio).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CashSessionQueryService — lecturas de caja")
class CashSessionQueryServiceTest {

    @Mock
    private CashSessionRepository repository;

    @InjectMocks
    private CashSessionQueryService service;

    @Captor
    private ArgumentCaptor<Set<Long>> sedesCaptor;

    private static CashSessionView resumen(Long id, Long branchId) {
        return CashSessionView.summary(id, branchId, "Sede Centro", 100L, "principal",
                CashSessionStatus.OPEN, EMPLEADO_ID, "Ana Ruiz", null, BigDecimal.TEN, null, null,
                null, null, null, 1L);
    }

    @Nested
    @DisplayName("current — la caja abierta del empleado")
    class Actual {

        @Test
        @DisplayName("devuelve la sesion abierta del empleado con sus totales en vivo")
        void devuelve_la_sesion_abierta_del_empleado() {
            when(repository.findOpenByEmployee(COMPANY_ID, EMPLEADO_ID))
                    .thenReturn(Optional.of(sesionConMovimientos()));

            CashSessionView vista = service.current(COMPANY_ID, EMPLEADO_ID);

            assertThat(vista.id()).isEqualTo(SESSION_ID);
            assertThat(vista.status()).isEqualTo(CashSessionStatus.OPEN);
            assertThat(vista.totals().getFirst().expectedAmount()).isEqualByComparingTo("130000");
        }

        @Test
        @DisplayName("sin caja abierta devuelve null, no un error")
        void sin_caja_abierta_devuelve_null() {
            when(repository.findOpenByEmployee(COMPANY_ID, EMPLEADO_ID))
                    .thenReturn(Optional.empty());

            // El front pregunta esto al entrar a cobrar: "todavia no has abierto caja"
            // es una respuesta normal, no un 404.
            assertThat(service.current(COMPANY_ID, EMPLEADO_ID)).isNull();
        }
    }

    @Nested
    @DisplayName("get — detalle por id")
    class Detalle {

        @Test
        @DisplayName("devuelve el detalle completo de la sesion")
        void devuelve_el_detalle_completo() {
            when(repository.findByIdAndCompany(SESSION_ID, COMPANY_ID))
                    .thenReturn(Optional.of(sesionCerrada()));

            CashSessionView vista = service.get(COMPANY_ID, SESSION_ID);

            assertThat(vista.id()).isEqualTo(SESSION_ID);
            assertThat(vista.closingTotal()).isEqualByComparingTo("155000");
            assertThat(vista.counts()).hasSize(2);
        }

        @Test
        @DisplayName("una sesion de otra empresa se ve como inexistente, no como prohibida")
        void una_sesion_de_otra_empresa_se_ve_como_inexistente() {
            when(repository.findByIdAndCompany(SESSION_ID, OTRA_COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.get(OTRA_COMPANY_ID, SESSION_ID))
                    .isInstanceOf(CashSessionNotFoundException.class)
                    .hasMessageContaining(String.valueOf(SESSION_ID));
        }
    }

    @Nested
    @DisplayName("list — historial paginado")
    class Historial {

        @Test
        @DisplayName("traslada la consulta al repositorio y devuelve su pagina")
        void traslada_la_consulta_y_devuelve_su_pagina() {
            SearchCashSessionsQuery consulta = new SearchCashSessionsQuery(COMPANY_ID, BRANCH_ID,
                    EMPLEADO_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), 2, 20);
            when(repository.search(consulta))
                    .thenReturn(new PageResult<>(List.of(resumen(1L, BRANCH_ID)), 2, 20, 41L, 3));

            PageResult<CashSessionView> pagina = service.list(consulta);

            assertThat(pagina.content()).extracting(CashSessionView::id).containsExactly(1L);
            assertThat(pagina.page()).isEqualTo(2);
            assertThat(pagina.totalElements()).isEqualTo(41L);
        }
    }

    @Nested
    @DisplayName("listOpen — cajas abiertas visibles")
    class Abiertas {

        @Test
        @DisplayName("pasa al repositorio exactamente las sedes que el empleado puede ver")
        void pasa_exactamente_las_sedes_que_el_empleado_puede_ver() {
            when(repository.findOpenSummaries(COMPANY_ID, Set.of(BRANCH_ID)))
                    .thenReturn(List.of(resumen(1L, BRANCH_ID)));

            List<CashSessionView> abiertas = service.listOpen(COMPANY_ID, Set.of(BRANCH_ID));

            // El scope viene del JWT y tiene que llegar intacto: ampliarlo aqui enseñaria
            // la caja de una sede que el empleado no tiene asignada.
            verify(repository).findOpenSummaries(eq(COMPANY_ID), sedesCaptor.capture());
            assertThat(sedesCaptor.getValue()).containsExactly(BRANCH_ID);
            assertThat(abiertas).extracting(CashSessionView::branchId).containsExactly(BRANCH_ID);
        }

        @Test
        @DisplayName("un scope null significa todas las sedes de la empresa: solo admin")
        void un_scope_null_significa_todas_las_sedes() {
            when(repository.findOpenSummaries(COMPANY_ID, null))
                    .thenReturn(List.of(resumen(1L, BRANCH_ID), resumen(2L, OTRA_BRANCH_ID)));

            List<CashSessionView> abiertas = service.listOpen(COMPANY_ID, null);

            // El null tiene que viajar tal cual: convertirlo en un set vacio dejaria al
            // admin sin ver ninguna caja.
            assertThat(abiertas).extracting(CashSessionView::branchId).containsExactly(BRANCH_ID,
                    OTRA_BRANCH_ID);
        }
    }
}
