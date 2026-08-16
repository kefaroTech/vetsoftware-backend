package com.vetsoftware.app.cashregister.application.dto;

import com.vetsoftware.app.shared.pagination.PageResult;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.BASE;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.BRANCH_ID;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.SESSION_ID;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.TERMINAL_ID;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.sesionAbierta;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.sesionCerrada;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.sesionConMovimientos;
import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.cashregister.domain.CashPaymentMethod;
import com.vetsoftware.app.cashregister.domain.CashSessionStatus;
import com.vetsoftware.app.cashregister.testsupport.CashSessionMother;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Vistas de cashregister")
class CashSessionViewTest {

    @Nested
    @DisplayName("CashSessionView.from — el detalle completo")
    class Detalle {

        @Test
        @DisplayName("traslada la cabecera de la sesion")
        void traslada_la_cabecera() {
            CashSessionView vista = CashSessionView.from(sesionAbierta());

            assertThat(vista.id()).isEqualTo(SESSION_ID);
            assertThat(vista.branchId()).isEqualTo(BRANCH_ID);
            assertThat(vista.terminalId()).isEqualTo(TERMINAL_ID);
            assertThat(vista.terminal()).isEqualTo("principal");
            assertThat(vista.status()).isEqualTo(CashSessionStatus.OPEN);
            assertThat(vista.openedAt()).isEqualTo(CashSessionMother.ABIERTA);
            assertThat(vista.openingFloat()).isEqualByComparingTo(BASE);
            assertThat(vista.version()).isEqualTo(3L);
        }

        @Test
        @DisplayName("expone el total en vivo por metodo")
        void expone_el_total_en_vivo_por_metodo() {
            CashSessionView vista = CashSessionView.from(sesionConMovimientos());

            assertThat(vista.totals()).extracting(MethodTotalView::method)
                    .containsExactly(CashPaymentMethod.CASH, CashPaymentMethod.CARD);
            assertThat(vista.totals()).extracting(MethodTotalView::expectedAmount).containsExactly(
                    new java.math.BigDecimal("130000"), new java.math.BigDecimal("30000"));
        }

        @Test
        @DisplayName("una sesion abierta no tiene total de cierre")
        void una_sesion_abierta_no_tiene_total_de_cierre() {
            // Null y no cero: la caja abierta todavia no se ha contado.
            assertThat(CashSessionView.from(sesionConMovimientos()).closingTotal()).isNull();
            assertThat(CashSessionView.from(sesionConMovimientos()).counts()).isEmpty();
        }

        @Test
        @DisplayName("una sesion cerrada suma lo contado en todos los metodos")
        void una_sesion_cerrada_suma_lo_contado() {
            CashSessionView vista = CashSessionView.from(sesionCerrada());

            // 125.000 contados en efectivo + 30.000 conciliados con tarjeta.
            assertThat(vista.closingTotal()).isEqualByComparingTo("155000");
            assertThat(vista.counts()).extracting(CashSessionCountView::method)
                    .containsExactly(CashPaymentMethod.CASH, CashPaymentMethod.CARD);
        }

        @Test
        @DisplayName("los movimientos viajan proyectados, no como entidades")
        void los_movimientos_viajan_proyectados() {
            CashSessionView vista = CashSessionView.from(sesionConMovimientos());

            assertThat(vista.movements()).hasSize(3);
            assertThat(vista.movements().getFirst().amount()).isEqualByComparingTo("50000");
            assertThat(vista.movements().getFirst().createdAt())
                    .isEqualTo(CashSessionMother.MOVIDA);
        }
    }

    @Nested
    @DisplayName("CashSessionView.summary — la cabecera del listado")
    class Resumen {

        @Test
        @DisplayName("no arrastra movimientos ni conteos: el listado no los necesita")
        void no_arrastra_movimientos_ni_conteos() {
            CashSessionView resumen = CashSessionView.summary(SESSION_ID, BRANCH_ID, "Sede Centro",
                    TERMINAL_ID, "principal", CashSessionStatus.CLOSED, 7L, "Ana Ruiz",
                    CashSessionMother.ABIERTA, BASE, BASE, 8L, "Luis Paz",
                    CashSessionMother.CERRADA, null, 3L);

            // Traerlos multiplicaria por N las filas de un historial paginado.
            assertThat(resumen.movements()).isEmpty();
            assertThat(resumen.counts()).isEmpty();
            assertThat(resumen.totals()).isEmpty();
        }

        @Test
        @DisplayName("si trae los nombres que el detalle deja en null")
        void si_trae_los_nombres_que_el_detalle_deja_en_null() {
            CashSessionView resumen = CashSessionView.summary(SESSION_ID, BRANCH_ID, "Sede Centro",
                    TERMINAL_ID, "principal", CashSessionStatus.OPEN, 7L, "Ana Ruiz",
                    CashSessionMother.ABIERTA, BASE, null, null, null, null, null, 3L);

            // El listado resuelve sede y responsable en la query; el detalle los deja
            // vacios porque el agregado solo tiene ids.
            assertThat(resumen.branchName()).isEqualTo("Sede Centro");
            assertThat(resumen.openedByEmployeeName()).isEqualTo("Ana Ruiz");
            assertThat(CashSessionView.from(sesionAbierta()).branchName()).isNull();
        }
    }

    @Nested
    @DisplayName("CashSessionCountView")
    class Conteo {

        @Test
        @DisplayName("proyecta la diferencia ya calculada por el dominio")
        void proyecta_la_diferencia_ya_calculada() {
            CashSessionCountView conteo = CashSessionView.from(sesionCerrada()).counts().getFirst();

            assertThat(conteo.expectedAmount()).isEqualByComparingTo("130000");
            assertThat(conteo.countedAmount()).isEqualByComparingTo("125000");
            assertThat(conteo.difference()).isEqualByComparingTo("-5000");
        }
    }

    @Nested
    @DisplayName("PageResult")
    class Pagina {

        @Test
        @DisplayName("map transforma el contenido y conserva los metadatos de la pagina")
        void map_transforma_el_contenido_y_conserva_los_metadatos() {
            PageResult<Integer> pagina = new PageResult<>(List.of(1, 2, 3), 2, 20, 41L, 3);

            PageResult<String> mapeada = pagina.map(String::valueOf);

            assertThat(mapeada.content()).containsExactly("1", "2", "3");
            assertThat(mapeada.page()).isEqualTo(2);
            assertThat(mapeada.pageSize()).isEqualTo(20);
            assertThat(mapeada.totalElements()).isEqualTo(41L);
            assertThat(mapeada.totalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("una pagina vacia se mapea a una pagina vacia, no a null")
        void una_pagina_vacia_se_mapea_a_vacia() {
            PageResult<Integer> vacia = new PageResult<>(List.of(), 0, 20, 0L, 0);

            assertThat(vacia.map(Function.identity()).content()).isEmpty();
        }
    }
}
