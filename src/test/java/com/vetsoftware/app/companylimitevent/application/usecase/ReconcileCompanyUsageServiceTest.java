package com.vetsoftware.app.companylimitevent.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.companylimitevent.application.command.RecordLimitEventCommand;
import com.vetsoftware.app.companylimitevent.application.dto.UsageReconciliationDto;
import com.vetsoftware.app.companylimitevent.application.port.in.RecordLimitEventUseCase;
import com.vetsoftware.app.companylimitevent.application.port.out.CapacityCounterPort;
import com.vetsoftware.app.companylimitevent.application.port.out.CapacityCounterPort.CapacityCounter;
import com.vetsoftware.app.companylimitevent.application.port.out.RealUsageCountPort;
import com.vetsoftware.app.companylimitevent.domain.LimitEventType;
import com.vetsoftware.app.companylimitevent.domain.LimitSource;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReconcileCompanyUsageService — R-LIMIT-30: el contador coincide con las filas reales")
class ReconcileCompanyUsageServiceTest {

    private static final Long EMPRESA = 10L;
    private static final Long EJE_MASCOTAS = 43L;
    private static final Long EJE_USUARIOS = 41L;
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 3, 15, 4, 10);
    private static final LocalDateTime HACE_UNA_SEMANA = AHORA.minusDays(7);
    private static final String CENTINELA = "ALLTIME";

    @Mock
    private CapacityCounterPort capacityCounterPort;
    @Mock
    private RealUsageCountPort realUsageCountPort;
    @Mock
    private RecordLimitEventUseCase recordLimitEvent;

    private ReconcileCompanyUsageService service;

    @BeforeEach
    void cablearServicio() {
        service = new ReconcileCompanyUsageService(capacityCounterPort, realUsageCountPort,
                recordLimitEvent, Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    }

    private static CapacityCounter contador(long id, Long ejeId, String codigo, int techo,
            int usado) {
        return new CapacityCounter(id, EMPRESA, ejeId, codigo, "CUMULATIVE", CENTINELA, techo,
                usado);
    }

    @Nested
    @DisplayName("El desvío se escribe, no se sobrescribe")
    class ElDesvioSeEscribe {

        /**
         * El caso violador que escribe el catalogo de reglas.
         *
         * <p>
         * El escenario real: una migracion inserta mascotas saltandose el caso de uso,
         * o un movimiento se pierde a mitad de transaccion. El contador dice una cosa y
         * las filas dicen otra, y <strong>ninguna restriccion del motor puede
         * detectarlo</strong>. Sin este recuento nadie se entera nunca — y cuando se
         * empiece a facturar excedente, la discrepancia es dinero mal cobrado.
         *
         * <p>
         * Lo que <em>no</em> hace es igual de importante: no toca el contador. La
         * correccion es otra operacion, la firma una persona de plataforma y tiene su
         * propio caso de uso (D-12, R-LIMIT-19). Un barrido nocturno moviendo cifras
         * que acaban en una factura no lo puede firmar nadie.
         */
        @Test
        @DisplayName("el recuento nocturno detecta un desvío de 12 mascotas y escribe el hecho en vez de sobrescribir")
        void el_recuento_nocturno_detecta_un_desvio_de_12_mascotas_y_escribe_el_hecho_en_vez_de_sobrescribir() {
            when(capacityCounterPort.findUnreconciled(HACE_UNA_SEMANA, 0L, 200))
                    .thenReturn(List.of(contador(700L, EJE_MASCOTAS, "ANIMAL", 100, 41)));
            when(realUsageCountPort.countFor(EMPRESA, "ANIMAL")).thenReturn(OptionalInt.of(53));

            UsageReconciliationDto resultado = service.execute(HACE_UNA_SEMANA, 0L, 200);

            ArgumentCaptor<RecordLimitEventCommand> hecho = ArgumentCaptor
                    .forClass(RecordLimitEventCommand.class);
            verify(recordLimitEvent).execute(hecho.capture());
            assertThat(hecho.getValue().eventType()).isEqualTo(LimitEventType.USAGE_RECONCILED);
            assertThat(hecho.getValue().requestedDelta()).isEqualTo(12);
            assertThat(hecho.getValue().usedQuantity()).isEqualTo(41);
            assertThat(hecho.getValue().limitQuantity()).isEqualTo(100);
            assertThat(hecho.getValue().limitSource()).isEqualTo(LimitSource.NONE);
            assertThat(hecho.getValue().actor().process()).isTrue();
            assertThat(resultado.drifted()).isEqualTo(1);

            // Ni sello ni correccion: el contador se queda como estaba, y sin marcar
            // como comprobado, para que la pasada siguiente lo vuelva a mirar.
            verify(capacityCounterPort, never()).markReconciled(anyLong(), anyLong(), anyString(),
                    any());
        }

        /**
         * El desvio en la otra direccion tambien es un desvio. Un contador que cuenta
         * de mas bloquea al cliente antes de tiempo, y ese daño no es menor que el de
         * contar de menos.
         */
        @Test
        @DisplayName("un contador que cuenta de más también escribe el hecho, con desvío negativo")
        void un_contador_que_cuenta_de_mas_escribe_el_hecho_con_desvio_negativo() {
            when(capacityCounterPort.findUnreconciled(HACE_UNA_SEMANA, 0L, 200))
                    .thenReturn(List.of(contador(700L, EJE_MASCOTAS, "ANIMAL", 100, 60)));
            when(realUsageCountPort.countFor(EMPRESA, "ANIMAL")).thenReturn(OptionalInt.of(48));

            service.execute(HACE_UNA_SEMANA, 0L, 200);

            ArgumentCaptor<RecordLimitEventCommand> hecho = ArgumentCaptor
                    .forClass(RecordLimitEventCommand.class);
            verify(recordLimitEvent).execute(hecho.capture());
            assertThat(hecho.getValue().requestedDelta()).isEqualTo(-12);
        }
    }

    @Nested
    @DisplayName("El sello del consumo (R-ENT-13)")
    class SelloDelConsumo {

        /**
         * La mitad que faltaba: {@code usage_reconciled_at} existia desde el changeset
         * 314 y no lo escribia nadie. El recalculo no lo toca a proposito --no mira el
         * consumo-- y no habia otro escritor, asi que su valor iba a ser nulo para
         * siempre.
         */
        @Test
        @DisplayName("cuando el contador cuadra, se sella el consumo y no se escribe ningún hecho")
        void cuando_cuadra_se_sella_el_consumo_y_no_se_escribe_hecho() {
            when(capacityCounterPort.findUnreconciled(HACE_UNA_SEMANA, 0L, 200))
                    .thenReturn(List.of(contador(701L, EJE_USUARIOS, "USER", 5, 3)));
            when(realUsageCountPort.countFor(EMPRESA, "USER")).thenReturn(OptionalInt.of(3));

            UsageReconciliationDto resultado = service.execute(HACE_UNA_SEMANA, 0L, 200);

            verify(capacityCounterPort).markReconciled(EMPRESA, EJE_USUARIOS, CENTINELA, AHORA);
            verifyNoInteractions(recordLimitEvent);
            assertThat(resultado.matched()).isEqualTo(1);
            assertThat(resultado.drifted()).isZero();
        }

        /**
         * Y la regla que le da sentido: sellar un contador que se sabe desviado dejaria
         * el indicador de salud diciendo «sano» justo sobre el dato que se acaba de
         * demostrar malo. Eso es peor que no tener indicador.
         */
        @Test
        @DisplayName("un contador con desvío se queda sin sello, para que la pasada siguiente lo mire")
        void un_contador_con_desvio_se_queda_sin_sello() {
            when(capacityCounterPort.findUnreconciled(HACE_UNA_SEMANA, 0L, 200))
                    .thenReturn(List.of(contador(702L, EJE_USUARIOS, "USER", 5, 3)));
            when(realUsageCountPort.countFor(EMPRESA, "USER")).thenReturn(OptionalInt.of(4));

            service.execute(HACE_UNA_SEMANA, 0L, 200);

            verify(capacityCounterPort, never()).markReconciled(anyLong(), anyLong(), anyString(),
                    any());
        }
    }

    @Nested
    @DisplayName("Ejes sin fuente de verdad computable")
    class EjesSinFuente {

        /**
         * De los ocho ejes sembrados, cinco no se pueden contar hoy con la verdad que
         * el modelo les exige --acumulativos sin fecha de borrado, flujo, y el
         * almacenamiento que no guarda tamaño--. Tratarlos como cero escribiria un
         * desvio catastrofico contra un contador correcto <strong>y le pondria el
         * sello</strong>, declarando comprobado lo que nadie comprobo.
         */
        @Test
        @DisplayName("un eje que hoy no se puede contar se salta: ni hecho ni sello")
        void un_eje_que_no_se_puede_contar_se_salta() {
            when(capacityCounterPort.findUnreconciled(HACE_UNA_SEMANA, 0L, 200))
                    .thenReturn(List.of(contador(703L, EJE_MASCOTAS, "ANIMAL", 100, 41)));
            when(realUsageCountPort.countFor(EMPRESA, "ANIMAL")).thenReturn(OptionalInt.empty());

            UsageReconciliationDto resultado = service.execute(HACE_UNA_SEMANA, 0L, 200);

            verifyNoInteractions(recordLimitEvent);
            verify(capacityCounterPort, never()).markReconciled(anyLong(), anyLong(), anyString(),
                    any());
            assertThat(resultado.skipped()).isEqualTo(1);
            assertThat(resultado.examined()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("El cursor")
    class ElCursor {

        /**
         * El cursor avanza aunque el contador se salte o quede con desvio. Si se
         * quedara quieto en esos casos, el barrido volveria a leer las mismas filas y
         * no terminaria nunca: los ejes no contables son cinco de los ocho.
         */
        @Test
        @DisplayName("el cursor avanza también sobre los contadores saltados y los desviados")
        void el_cursor_avanza_sobre_saltados_y_desviados() {
            when(capacityCounterPort.findUnreconciled(HACE_UNA_SEMANA, 0L, 200))
                    .thenReturn(List.of(contador(710L, EJE_MASCOTAS, "ANIMAL", 100, 41),
                            contador(711L, EJE_USUARIOS, "USER", 5, 3)));
            when(realUsageCountPort.countFor(EMPRESA, "ANIMAL")).thenReturn(OptionalInt.empty());
            when(realUsageCountPort.countFor(EMPRESA, "USER")).thenReturn(OptionalInt.of(4));

            UsageReconciliationDto resultado = service.execute(HACE_UNA_SEMANA, 0L, 200);

            assertThat(resultado.lastId()).isEqualTo(711L);
        }

        @Test
        @DisplayName("un lote vacío deja el cursor donde estaba")
        void un_lote_vacio_deja_el_cursor_donde_estaba() {
            when(capacityCounterPort.findUnreconciled(HACE_UNA_SEMANA, 55L, 200))
                    .thenReturn(List.of());

            UsageReconciliationDto resultado = service.execute(HACE_UNA_SEMANA, 55L, 200);

            assertThat(resultado.lastId()).isEqualTo(55L);
            assertThat(resultado.examined()).isZero();
        }
    }

    @Test
    @DisplayName("un tamaño de lote no positivo se rechaza antes de tocar nada")
    void un_tamano_de_lote_no_positivo_se_rechaza() {
        assertThatThrownBy(() -> service.execute(HACE_UNA_SEMANA, 0L, 0))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("batch size");

        verifyNoInteractions(capacityCounterPort, realUsageCountPort, recordLimitEvent);
    }
}
