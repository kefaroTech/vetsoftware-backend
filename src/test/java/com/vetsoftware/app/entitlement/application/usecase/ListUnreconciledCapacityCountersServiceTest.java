package com.vetsoftware.app.entitlement.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.entitlement.application.dto.CompanyCapacityDto;
import com.vetsoftware.app.entitlement.application.port.out.UnreconciledCapacityQueryPort;
import com.vetsoftware.app.entitlement.domain.CompanyCapacity;
import com.vetsoftware.app.entitlement.testsupport.EntitlementMother;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El lote pendiente de recuento, tal como lo ve la rodaja que cuenta.
 *
 * <p>
 * <b>Este servicio no tenia ningun test.</b> Es uno de los dos destinos de
 * {@code EntitlementCapacityCounterAdapter}, y que el cable y su destino
 * estuvieran los dos sin red es lo que convertia esa costura en un agujero.
 *
 * <p>
 * <b>Lo unico que hace es traducir, y ahi es donde puede mentir.</b> La
 * traduccion no es una copia: {@code exhausted} se <em>calcula</em>
 * ({@code used &gt;= limit}) y {@code uncapped} se fija a {@code false}. Ese
 * booleano calculado es el que decide si al cliente se le ofrece ampliar, asi
 * que su frontera —el instante exacto en que el contador se llena— se prueba
 * aqui con {@code @ParameterizedTest} y no con un caso feliz.
 *
 * <p>
 * <b>El reenvio de los tres argumentos lo sujeta {@code STRICT_STUBS}</b>: el
 * metodo devuelve un valor, asi que la asercion es lo devuelto; si el servicio
 * cruzara {@code afterId} con {@code limit} el stub no casaria y Mockito
 * lanzaria {@code PotentialStubbingProblem}. Por eso los dos son numeros
 * distintos.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ListUnreconciledCapacityCountersService — el lote pendiente de recontar")
class ListUnreconciledCapacityCountersServiceTest {

    private static final LocalDateTime RANCIO_ANTES_DE = LocalDateTime.of(2026, 8, 1, 3, 0);
    private static final long AFTER_ID = 5000L;
    private static final int LIMITE = 200;

    @Mock
    private UnreconciledCapacityQueryPort queryPort;
    @InjectMocks
    private ListUnreconciledCapacityCountersService service;

    @Nested
    @DisplayName("Traduccion del contador")
    class Traduccion {

        @Test
        @DisplayName("cada campo del contador llega a su componente del DTO")
        void cada_campo_del_contador_llega_a_su_componente_del_dto() {
            CompanyCapacity contador = EntitlementMother.contadorRecontado(5001L,
                    EntitlementMother.MASCOTAS, 7, 3, LocalDateTime.of(2026, 7, 15, 2, 0));
            when(queryPort.findUnreconciled(RANCIO_ANTES_DE, AFTER_ID, LIMITE))
                    .thenReturn(List.of(contador));

            List<CompanyCapacityDto> lote = service.list(RANCIO_ANTES_DE, AFTER_ID, LIMITE);

            assertThat(lote).singleElement().satisfies(dto -> {
                assertThat(dto.id()).isEqualTo(5001L);
                assertThat(dto.companyId()).isEqualTo(EntitlementMother.COMPANY_ID);
                assertThat(dto.limitDimensionId()).isEqualTo(EntitlementMother.MASCOTAS.id());
                assertThat(dto.dimensionCode()).isEqualTo("ANIMAL");
                assertThat(dto.measureKind()).isEqualTo("CUMULATIVE");
                assertThat(dto.periodKey()).isEqualTo("ALLTIME");
                assertThat(dto.limitQuantity()).isEqualTo(7);
                assertThat(dto.usedQuantity()).isEqualTo(3);
                assertThat(dto.subscriptionId()).isEqualTo(EntitlementMother.SUBSCRIPTION_ID);
                assertThat(dto.usageReconciledAt()).isEqualTo(LocalDateTime.of(2026, 7, 15, 2, 0));
            });
        }

        /**
         * <b>El sello nulo es el dato, no la ausencia de dato.</b> Un contador que
         * nadie ha recontado nunca tiene {@code usage_reconciled_at} a nulo, y eso es
         * precisamente lo que lo mete en este lote: convertirlo en una fecha inventada
         * —la de ahora, la de creacion— dejaria el indicador de salud diciendo «sano»
         * sobre el contador que nadie ha mirado.
         */
        @Test
        @DisplayName("un contador que nadie reconto nunca conserva el sello a nulo")
        void un_contador_que_nadie_reconto_nunca_conserva_el_sello_a_nulo() {
            when(queryPort.findUnreconciled(RANCIO_ANTES_DE, AFTER_ID, LIMITE)).thenReturn(List
                    .of(EntitlementMother.contadorExistente(5001L, EntitlementMother.SEDES, 2, 1)));

            assertThat(service.list(RANCIO_ANTES_DE, AFTER_ID, LIMITE)).singleElement()
                    .satisfies(dto -> assertThat(dto.usageReconciledAt()).isNull());
        }

        /**
         * {@code uncapped} solo lo pone la fabrica {@code CompanyCapacityDto.uncapped},
         * que responde a D-74 —eje nacido despues de la firma—. Un contador que existe
         * en la tabla nunca es «sin techo»: si saliera {@code true} desde aqui, el
         * barrido leeria «esta empresa no tiene limite» sobre una fila que si lo tiene.
         */
        @Test
        @DisplayName("un contador que existe nunca se marca como sin techo")
        void un_contador_que_existe_nunca_se_marca_como_sin_techo() {
            when(queryPort.findUnreconciled(RANCIO_ANTES_DE, AFTER_ID, LIMITE)).thenReturn(List
                    .of(EntitlementMother.contadorExistente(5001L, EntitlementMother.SEDES, 2, 1)));

            assertThat(service.list(RANCIO_ANTES_DE, AFTER_ID, LIMITE)).singleElement()
                    .satisfies(dto -> assertThat(dto.uncapped()).isFalse());
        }
    }

    @Nested
    @DisplayName("La frontera del contador lleno")
    class Agotamiento {

        /**
         * <b>El caso del techo cero esta aqui a proposito.</b> Un eje contratado con
         * techo {@code 0} y consumo {@code 0} esta agotado desde el primer instante
         * ({@code 0 &gt;= 0}): es la empresa que no compro ese eje, y tiene que leerse
         * como «no puedes crear ninguno», no como «te queda margen». Un {@code &gt;} en
         * vez de {@code &gt;=} lo invertiria y le dejaria crear el primero gratis.
         */
        @ParameterizedTest(name = "techo {0}, usado {1} → agotado {2}")
        @CsvSource({"7, 3, false", "7, 6, false", "7, 7, true", "7, 9, true", "0, 0, true",
                "1, 0, false"})
        @DisplayName("agotado es usado mayor o igual que techo, tambien en los bordes")
        void agotado_es_usado_mayor_o_igual_que_techo(int techo, int usado, boolean agotado) {
            when(queryPort.findUnreconciled(RANCIO_ANTES_DE, AFTER_ID, LIMITE))
                    .thenReturn(List.of(EntitlementMother.contadorExistente(5001L,
                            EntitlementMother.SEDES, techo, usado)));

            assertThat(service.list(RANCIO_ANTES_DE, AFTER_ID, LIMITE)).singleElement()
                    .satisfies(dto -> assertThat(dto.exhausted()).isEqualTo(agotado));
        }
    }

    @Nested
    @DisplayName("Final del barrido")
    class FinDelCursor {

        /**
         * El vacio aqui es una consecuencia, no el estado inicial: el puerto respondio
         * que ya no queda nada por encima del cursor. Lo que se afirma es que el
         * servicio lo propaga en vez de reventar con un {@code null}.
         */
        @Test
        @DisplayName("cuando el cursor llego al final devuelve una lista vacia")
        void cuando_el_cursor_llego_al_final_devuelve_una_lista_vacia() {
            when(queryPort.findUnreconciled(RANCIO_ANTES_DE, AFTER_ID, LIMITE))
                    .thenReturn(List.of());

            assertThat(service.list(RANCIO_ANTES_DE, AFTER_ID, LIMITE)).isEmpty();
        }

        /**
         * Los contadores llegan del puerto ordenados por id ascendente —es lo que hace
         * avanzar el cursor— y el servicio no puede reordenarlos: hacerlo dejaria el
         * barrido leyendo las mismas filas para siempre.
         */
        @Test
        @DisplayName("respeta el orden del cursor que impone el puerto")
        void respeta_el_orden_del_cursor_que_impone_el_puerto() {
            when(queryPort.findUnreconciled(RANCIO_ANTES_DE, AFTER_ID, LIMITE)).thenReturn(List.of(
                    EntitlementMother.contadorExistente(5001L, EntitlementMother.SEDES, 2, 1),
                    EntitlementMother.contadorExistente(5002L, EntitlementMother.USUARIOS, 5, 5),
                    EntitlementMother.contadorExistente(5003L, EntitlementMother.MASCOTAS, 9, 0)));

            assertThat(service.list(RANCIO_ANTES_DE, AFTER_ID, LIMITE))
                    .extracting(CompanyCapacityDto::id).containsExactly(5001L, 5002L, 5003L);
        }
    }
}
