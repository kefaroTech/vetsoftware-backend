package com.vetsoftware.app.companylimitevent.infrastructure.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.companylimitevent.application.port.out.CapacityCounterPort.CapacityCounter;
import com.vetsoftware.app.entitlement.application.command.MarkCapacityUsageReconciledCommand;
import com.vetsoftware.app.entitlement.application.dto.CompanyCapacityDto;
import com.vetsoftware.app.entitlement.application.port.in.ListUnreconciledCapacityCountersUseCase;
import com.vetsoftware.app.entitlement.application.port.in.MarkCapacityUsageReconciledUseCase;
import java.time.LocalDateTime;
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
 * El cable entre el recuento nocturno y el contador, que vive en otra rodaja.
 *
 * <p>
 * <b>Por que existe esta clase.</b> Hasta hoy los dos extremos estaban
 * cubiertos con dobles y el cable no lo ejecutaba nadie: ni este adaptador ni
 * sus dos destinos ({@code ListUnreconciledCapacityCountersService},
 * {@code MarkCapacityUsageReconciledService}) tenian un solo test. No es una
 * costura a medias, es un agujero: el unico sitio del sistema que traduce
 * {@code CompanyCapacityDto} —trece componentes— al {@code record}
 * {@code CapacityCounter} —ocho— lo hace <b>por posicion</b>, y un cruce entre
 * dos componentes del mismo tipo compila, no avisa y no lo ve nadie.
 *
 * <p>
 * <b>Todos los valores del escenario son distintos entre si a proposito.</b>
 * Los dos {@code Long} de identidad (5001 / 900 / 43), los dos {@code String}
 * descriptivos ({@code ANIMAL} / {@code CUMULATIVE}) y los dos {@code int} de
 * cantidad (7 / 3) tienen valores que no se repiten. Si alguien intercambia
 * {@code dimensionCode} con {@code measureKind}, o —lo caro—
 * {@code limitQuantity} con {@code usedQuantity}, este test cae. Con los mismos
 * numeros repetidos en dos campos pasaria igual de verde con el codigo roto,
 * que es exactamente la clase de prueba que no vale nada. Comprobado: al
 * intercambiar los dos ultimos argumentos del {@code new CapacityCounter(...)}
 * del adaptador, este test se pone rojo en {@code limitQuantity}.
 *
 * <p>
 * <b>El reenvio del cursor lo sujeta la estrictez de Mockito, no un
 * {@code verify}.</b> {@code findUnreconciled} devuelve un valor, asi que la
 * asercion es lo devuelto (regla del CLAUDE.md). Que el adaptador pase
 * {@code staleBefore}, {@code afterId} y {@code limit} en ese orden y sin
 * tocarlos lo garantiza {@code STRICT_STUBS}: si los cruzara, el stub no
 * casaria y Mockito lanzaria {@code PotentialStubbingProblem}. Por eso
 * {@code AFTER_ID} y {@code LIMITE} tambien son numeros distintos.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EntitlementCapacityCounterAdapter — el cable entre el recuento y el contador")
class EntitlementCapacityCounterAdapterTest {

    private static final LocalDateTime RANCIO_ANTES_DE = LocalDateTime.of(2026, 8, 1, 3, 0);
    private static final LocalDateTime SELLADO_EN = LocalDateTime.of(2026, 8, 28, 3, 15);
    private static final long AFTER_ID = 5000L;
    private static final int LIMITE = 200;

    private static final Long CONTADOR_ID = 5001L;
    private static final Long EMPRESA_ID = 900L;
    private static final Long EJE_ID = 43L;
    private static final String EJE_CODIGO = "ANIMAL";
    private static final String TIPO_DE_MEDIDA = "CUMULATIVE";
    private static final String PERIODO = "ALLTIME";
    private static final int TECHO = 7;
    private static final int USADO = 3;

    @Mock
    private ListUnreconciledCapacityCountersUseCase listUnreconciled;
    @Mock
    private MarkCapacityUsageReconciledUseCase markReconciled;
    @InjectMocks
    private EntitlementCapacityCounterAdapter adapter;

    @Nested
    @DisplayName("Lectura del lote pendiente")
    class Lectura {

        @Test
        @DisplayName("traduce los ocho campos del contador sin cruzar ninguno")
        void traduce_los_ocho_campos_del_contador_sin_cruzar_ninguno() {
            when(listUnreconciled.list(RANCIO_ANTES_DE, AFTER_ID, LIMITE))
                    .thenReturn(List.of(unContadorPendiente()));

            List<CapacityCounter> contadores = adapter.findUnreconciled(RANCIO_ANTES_DE, AFTER_ID,
                    LIMITE);

            assertThat(contadores).singleElement().satisfies(contador -> {
                assertThat(contador.id()).isEqualTo(CONTADOR_ID);
                assertThat(contador.companyId()).isEqualTo(EMPRESA_ID);
                assertThat(contador.limitDimensionId()).isEqualTo(EJE_ID);
                assertThat(contador.dimensionCode()).isEqualTo(EJE_CODIGO);
                assertThat(contador.measureKind()).isEqualTo(TIPO_DE_MEDIDA);
                assertThat(contador.periodKey()).isEqualTo(PERIODO);
                assertThat(contador.limitQuantity()).isEqualTo(TECHO);
                assertThat(contador.usedQuantity()).isEqualTo(USADO);
            });
        }

        /**
         * El vacio aqui no es el estado inicial de nada: es la respuesta del puerto
         * cuando el barrido ya llego al final del cursor, y el adaptador tiene que
         * propagarlo sin inventar filas.
         */
        @Test
        @DisplayName("un lote vacio no se convierte en contadores inventados")
        void un_lote_vacio_no_se_convierte_en_contadores_inventados() {
            when(listUnreconciled.list(RANCIO_ANTES_DE, AFTER_ID, LIMITE)).thenReturn(List.of());

            assertThat(adapter.findUnreconciled(RANCIO_ANTES_DE, AFTER_ID, LIMITE)).isEmpty();
            verifyNoInteractions(markReconciled);
        }

        /**
         * Un contador que ya se conto no se sella si esta desviado, asi que vuelve a
         * salir en el lote siguiente: el orden del cursor es lo unico que impide que el
         * barrido lea las mismas filas para siempre. Aqui se afirma que el adaptador
         * <b>respeta el orden</b> que le da la otra rodaja en vez de reordenar.
         */
        @Test
        @DisplayName("conserva el orden del cursor que impone la otra rodaja")
        void conserva_el_orden_del_cursor_que_impone_la_otra_rodaja() {
            when(listUnreconciled.list(RANCIO_ANTES_DE, AFTER_ID, LIMITE)).thenReturn(
                    List.of(unContadorPendiente(), otroContadorPendiente(CONTADOR_ID + 1),
                            otroContadorPendiente(CONTADOR_ID + 2)));

            assertThat(adapter.findUnreconciled(RANCIO_ANTES_DE, AFTER_ID, LIMITE))
                    .extracting(CapacityCounter::id)
                    .containsExactly(CONTADOR_ID, CONTADOR_ID + 1, CONTADOR_ID + 2);
        }
    }

    @Nested
    @DisplayName("Sello del consumo")
    class Sello {

        @Test
        @DisplayName("arma el comando con la empresa y el eje en su sitio, sin cruzarlos")
        void arma_el_comando_con_la_empresa_y_el_eje_en_su_sitio() {
            when(markReconciled.execute(org.mockito.ArgumentMatchers.any())).thenReturn(true);

            adapter.markReconciled(EMPRESA_ID, EJE_ID, PERIODO, SELLADO_EN);

            ArgumentCaptor<MarkCapacityUsageReconciledCommand> comando = ArgumentCaptor
                    .forClass(MarkCapacityUsageReconciledCommand.class);
            verify(markReconciled).execute(comando.capture());
            assertThat(comando.getValue().companyId()).isEqualTo(EMPRESA_ID);
            assertThat(comando.getValue().limitDimensionId()).isEqualTo(EJE_ID);
            assertThat(comando.getValue().periodKey()).isEqualTo(PERIODO);
            assertThat(comando.getValue().reconciledAt()).isEqualTo(SELLADO_EN);
        }

        /**
         * <b>El falso es la mitad del valor de este metodo.</b> Quien lo llama decide
         * con el si el contador existia; tragarselo y devolver siempre {@code true}
         * dejaria al recuento afirmando que sello filas que no estaban.
         */
        @Test
        @DisplayName("devuelve falso cuando no habia contador que sellar")
        void devuelve_falso_cuando_no_habia_contador_que_sellar() {
            when(markReconciled.execute(org.mockito.ArgumentMatchers.any())).thenReturn(false);

            assertThat(adapter.markReconciled(EMPRESA_ID, EJE_ID, PERIODO, SELLADO_EN)).isFalse();
        }

        @Test
        @DisplayName("devuelve cierto cuando el contador quedo sellado")
        void devuelve_cierto_cuando_el_contador_quedo_sellado() {
            when(markReconciled.execute(org.mockito.ArgumentMatchers.any())).thenReturn(true);

            assertThat(adapter.markReconciled(EMPRESA_ID, EJE_ID, PERIODO, SELLADO_EN)).isTrue();
            verifyNoInteractions(listUnreconciled);
        }
    }

    private static CompanyCapacityDto unContadorPendiente() {
        return otroContadorPendiente(CONTADOR_ID);
    }

    /**
     * {@code exhausted} va a {@code true} y {@code uncapped} a {@code false} a
     * proposito: son los dos componentes que el {@code record} de destino <b>no</b>
     * transporta, y si alguien los colara en el constructor de
     * {@code CapacityCounter} el numero de argumentos dejaria de cuadrar.
     */
    private static CompanyCapacityDto otroContadorPendiente(Long id) {
        return new CompanyCapacityDto(id, EMPRESA_ID, EJE_ID, EJE_CODIGO, TIPO_DE_MEDIDA, PERIODO,
                TECHO, USADO, true, false, 970L, LocalDateTime.of(2026, 7, 1, 0, 0), null);
    }
}
