package com.vetsoftware.app.dunning.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.dunning.application.dto.DunningBatchResult;
import com.vetsoftware.app.dunning.application.port.in.EvaluateDunningUseCase;
import com.vetsoftware.app.dunning.application.port.out.DunningBillingDocumentPort;
import com.vetsoftware.app.dunning.domain.BillingDocumentRef;
import com.vetsoftware.app.dunning.domain.DunningBillingDocumentSnapshot;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El barrido por cursor, que hasta ahora no tenia ni un caso.
 *
 * <p>
 * <b>Es la pieza cross-tenant del motor de cobranza</b>: lee facturas vencidas
 * de todas las clinicas con {@code FOR UPDATE SKIP LOCKED} y las reparte. Lo
 * que estos casos fijan es lo que un fallo aqui rompe en silencio — que cada
 * factura se evalue con <b>su</b> empresa y no con la de la anterior del lote,
 * y que el cursor avance siempre, porque un cursor que no avanza convierte el
 * barrido nocturno en un bucle infinito sobre el mismo lote.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DunningEvaluationWorker — barrido por cursor de facturas vencidas")
class DunningEvaluationWorkerTest {

    private static final LocalDate HOY = LocalDate.of(2026, 8, 23);
    private static final Clock RELOJ = Clock.fixed(HOY.atStartOfDay().toInstant(ZoneOffset.UTC),
            ZoneOffset.UTC);
    private static final Long EMPRESA = 900L;
    private static final Long OTRA_EMPRESA = 901L;

    @Mock
    private DunningBillingDocumentPort billingDocumentPort;
    @Mock
    private EvaluateDunningUseCase evaluateDunningUseCase;
    @Captor
    private ArgumentCaptor<Long> documentoCaptor;
    @Captor
    private ArgumentCaptor<Long> empresaCaptor;

    private DunningEvaluationWorker worker;

    @BeforeEach
    void montar() {
        worker = new DunningEvaluationWorker(billingDocumentPort, evaluateDunningUseCase, RELOJ);
    }

    @Nested
    @DisplayName("Reparto del lote")
    class RepartoDelLote {

        @Test
        @DisplayName("evalua cada factura con SU empresa, no con la de la anterior del lote")
        void evalua_cada_factura_con_su_empresa() {
            when(billingDocumentPort.lockOverdueBatchAfter(HOY, 0L, 100))
                    .thenReturn(List.of(factura(11L, EMPRESA), factura(12L, OTRA_EMPRESA)));

            worker.processBatchAfter(0L, 100);

            verify(evaluateDunningUseCase, times(2)).evaluate(documentoCaptor.capture(),
                    empresaCaptor.capture());
            assertThat(documentoCaptor.getAllValues()).containsExactly(11L, 12L);
            assertThat(empresaCaptor.getAllValues()).containsExactly(EMPRESA, OTRA_EMPRESA);
        }

        @Test
        @DisplayName("devuelve cuantas proceso y el ultimo id visto, que es el cursor de la"
                + " vuelta siguiente")
        void devuelve_el_cursor_de_la_vuelta_siguiente() {
            when(billingDocumentPort.lockOverdueBatchAfter(HOY, 10L, 2))
                    .thenReturn(List.of(factura(11L, EMPRESA), factura(12L, EMPRESA)));

            DunningBatchResult resultado = worker.processBatchAfter(10L, 2);

            assertThat(resultado).isEqualTo(new DunningBatchResult(2, 12L));
        }

        @Test
        @DisplayName("un lote vacio conserva el cursor y no evalua nada: sin eso el barrido"
                + " volveria al principio cada noche")
        void un_lote_vacio_conserva_el_cursor() {
            when(billingDocumentPort.lockOverdueBatchAfter(HOY, 42L, 100)).thenReturn(List.of());

            DunningBatchResult resultado = worker.processBatchAfter(42L, 100);

            assertThat(resultado).isEqualTo(new DunningBatchResult(0, 42L));
            verifyNoInteractions(evaluateDunningUseCase);
        }

        @Test
        @DisplayName("pide las vencidas con la fecha del reloj inyectado, nunca con la del"
                + " sistema: si no, el limite de la mora depende de cuando corre la suite")
        void pide_las_vencidas_con_la_fecha_del_reloj_inyectado() {
            when(billingDocumentPort.lockOverdueBatchAfter(HOY, 0L, 5)).thenReturn(List.of());

            worker.processBatchAfter(0L, 5);

            verify(billingDocumentPort).lockOverdueBatchAfter(HOY, 0L, 5);
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("un cursor negativo se rechaza antes de tocar ningun puerto")
        void un_cursor_negativo_se_rechaza_sin_tocar_nada() {
            assertThatThrownBy(() -> worker.processBatchAfter(-1L, 100))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("afterId");

            verifyNoInteractions(billingDocumentPort, evaluateDunningUseCase);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        @DisplayName("un tamano de lote no positivo se rechaza: un lote de cero no avanza el"
                + " cursor y el job daria vueltas para siempre")
        void un_tamano_de_lote_no_positivo_se_rechaza(int batchSize) {
            assertThatThrownBy(() -> worker.processBatchAfter(0L, batchSize))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("batchSize");

            verifyNoInteractions(billingDocumentPort, evaluateDunningUseCase);
        }
    }

    @Nested
    @DisplayName("Propagacion de fallos")
    class PropagacionDeFallos {

        /**
         * <b>Este caso NO bendice un defecto: describe con exactitud la consecuencia de
         * una frontera transaccional, y esa consecuencia es real hoy.</b>
         * {@code processBatchAfter} es {@code @Transactional} sobre el lote entero, asi
         * que en cuanto un {@code evaluate} lanza, la transaccion queda marcada para
         * rollback; capturar la excepcion y seguir con el siguiente documento no
         * aislaria nada — fallaria igual al confirmar, y por el camino habria hecho
         * escrituras condenadas. Con el diseno de hoy, abortar el lote es lo correcto.
         *
         * <p>
         * <b>El defecto de #469 esta un nivel mas abajo: la transaccion es del lote y
         * deberia ser del documento.</b> Un barrido que recorre facturas ordenadas por
         * {@code id} —un orden que no guarda ninguna relacion con el tenant— no puede
         * permitir que un dato inconsistente en la factura 37 impida evaluar las de
         * otras clinicas; y como el recorrido siempre arranca del cursor 0, vuelve a
         * chocar con la misma fila todas las noches: se atasca, no se recupera solo.
         *
         * <p>
         * <b>Por que este caso se queda como esta y no se reescribe para exigir el
         * aislamiento.</b> Exigirlo aqui no es cambiar una asercion: pide
         * {@code REQUIRES_NEW} por documento en {@code src/main} <em>y</em> un campo de
         * fallos en {@code DunningBatchResult} que hoy no existe —sin el, el job no
         * puede llamar a {@code Outcome.from(attempted, failures)}, que es la mitad del
         * issue—. Un test que lo exigiera <b>ni siquiera compilaria</b>, asi que no hay
         * forma de dejar la expectativa escrita como se hizo con #468. Cuando ese
         * cambio llegue, este caso tiene que fallar: es la senal de que la frontera se
         * movio, y su sustituto debe afirmar que el lote continua y devuelve el conteo
         * de fallos.
         */
        @Test
        @DisplayName("con la transacción de hoy —una por lote— una factura que revienta aborta"
                + " el lote entero; el aislamiento por documento es #469")
        void si_una_factura_revienta_el_lote_aborta() {
            when(billingDocumentPort.lockOverdueBatchAfter(HOY, 0L, 100))
                    .thenReturn(List.of(factura(11L, EMPRESA), factura(12L, EMPRESA)));
            doThrow(new IllegalArgumentException("Subscription not found: 7"))
                    .when(evaluateDunningUseCase).evaluate(11L, EMPRESA);

            assertThatThrownBy(() -> worker.processBatchAfter(0L, 100))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Subscription not found");

            verify(evaluateDunningUseCase, times(1)).evaluate(anyLong(), anyLong());
        }
    }

    private static DunningBillingDocumentSnapshot factura(Long id, Long companyId) {
        return new DunningBillingDocumentSnapshot(
                new BillingDocumentRef(id, companyId, "DC-" + id, new BigDecimal("250000.00")), 70L,
                HOY.minusDays(10));
    }
}
