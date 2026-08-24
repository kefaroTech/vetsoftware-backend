package com.vetsoftware.app.subscription.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("SubscriptionAmendment - el papel de cada cambio")
class SubscriptionAmendmentTest {

    private static final Long EMPRESA = 42L;
    private static final Long CONTRATO = 7L;
    private static final Long EMPLEADO = 4L;
    private static final Long USUARIO_DE_PLATAFORMA = 6L;
    private static final LocalDate EFECTIVA = LocalDate.of(2026, 5, 1);

    private static SubscriptionAmendment otrosi(Long empleadoId, Long systemUserId) {
        return SubscriptionAmendment.issue(EMPRESA, CONTRATO, "AMD-2026-0001",
                AmendmentType.ADD_ITEM, EFECTIVA, "Contrato veterinaria", empleadoId, systemUserId,
                BigDecimal.ZERO, new BigDecimal("179000.00"), null, "req-1");
    }

    @Nested
    @DisplayName("Quien firma: exactamente uno")
    class QuienFirma {

        @Test
        @DisplayName("firmado por el empleado del cliente")
        void firmaElEmpleado() {
            SubscriptionAmendment enmienda = otrosi(EMPLEADO, null);

            assertThat(enmienda.getRequestedByEmployeeId()).isEqualTo(EMPLEADO);
            assertThat(enmienda.getRequestedBySystemUserId()).isNull();
        }

        @Test
        @DisplayName("firmado por alguien de la plataforma")
        void firmaLaPlataforma() {
            SubscriptionAmendment enmienda = otrosi(null, USUARIO_DE_PLATAFORMA);

            assertThat(enmienda.getRequestedBySystemUserId()).isEqualTo(USUARIO_DE_PLATAFORMA);
            assertThat(enmienda.getRequestedByEmployeeId()).isNull();
        }

        @Test
        @DisplayName("los dos rellenos a la vez se rechaza: la responsabilidad es de uno solo")
        void losDosALaVez() {
            // Son dos columnas distintas justamente porque la responsabilidad es
            // distinta. Con las dos puestas, el expediente no dice quien pidio el cambio
            // —y esa es toda la razon de que la columna exista—.
            assertThatThrownBy(() -> otrosi(EMPLEADO, USUARIO_DE_PLATAFORMA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exactly one requester");
        }

        @Test
        @DisplayName("ninguno relleno se rechaza: un cambio sin responsable es un cambio que nadie firmo")
        void ninguno() {
            assertThatThrownBy(() -> otrosi(null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exactly one requester");
        }
    }

    @Nested
    @DisplayName("Prorrateo")
    class Prorrateo {

        @Test
        @DisplayName("los dos importes se guardan tal como llegan, con su signo")
        void seGuardanTalCual() {
            SubscriptionAmendment baja = SubscriptionAmendment.issue(EMPRESA, CONTRATO,
                    "AMD-2026-0002", AmendmentType.REMOVE_ITEM, EFECTIVA, null, EMPLEADO, null,
                    new BigDecimal("-58000.00"), new BigDecimal("-179000.00"), null, "req-2");

            // Aqui no hay motor de calculo: una baja resta y el signo lo trae el command.
            assertThat(baja.getProrationAmount()).isEqualByComparingTo("-58000.00");
            assertThat(baja.getMonthlyDeltaAmount()).isEqualByComparingTo("-179000.00");
        }

        @Test
        @DisplayName("no se admiten mas de dos decimales: la columna es DECIMAL(19,2)")
        void tresDecimales() {
            assertThatThrownBy(() -> SubscriptionAmendment.issue(EMPRESA, CONTRATO, "AMD-1",
                    AmendmentType.ADD_ITEM, EFECTIVA, null, EMPLEADO, null,
                    new BigDecimal("100.123"), BigDecimal.ZERO, null, "req-3"))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("decimals");
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("sin llave antiduplicados no se emite")
        void sinLlave() {
            assertThatThrownBy(() -> SubscriptionAmendment.issue(EMPRESA, CONTRATO, "AMD-1",
                    AmendmentType.ADD_ITEM, EFECTIVA, null, EMPLEADO, null, BigDecimal.ZERO,
                    BigDecimal.ZERO, null, "  ")).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("clientRequestId");
        }

        @Test
        @DisplayName("sin fecha efectiva no se sabe desde cuando aplica el cambio")
        void sinFechaEfectiva() {
            assertThatThrownBy(() -> SubscriptionAmendment.issue(EMPRESA, CONTRATO, "AMD-1",
                    AmendmentType.ADD_ITEM, null, null, EMPLEADO, null, BigDecimal.ZERO,
                    BigDecimal.ZERO, null, "req-4")).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("effectiveDate");
        }
    }

    private static SubscriptionAmendment otrosi(String numero, Long companyId, AmendmentType tipo,
            LocalDate efectiva, String motivo, BigDecimal prorrateo, BigDecimal delta,
            String llave) {
        return SubscriptionAmendment.issue(companyId, CONTRATO, numero, tipo, efectiva, motivo,
                EMPLEADO, null, prorrateo, delta, null, llave);
    }

    @Nested
    @DisplayName("Invariantes del documento")
    class InvariantesDelDocumento {

        static Stream<Arguments> otrosisInvalidos() {
            return Stream.of(
                    Arguments.of("sin empresa",
                            (ThrowingCallable) () -> otrosi("AMD-2026-0001", null,
                                    AmendmentType.ADD_ITEM, EFECTIVA, "motivo", BigDecimal.ZERO,
                                    BigDecimal.ZERO, "req-1"),
                            "companyId"),
                    Arguments.of("numero en blanco",
                            (ThrowingCallable) () -> otrosi(" ", EMPRESA, AmendmentType.ADD_ITEM,
                                    EFECTIVA, "motivo", BigDecimal.ZERO, BigDecimal.ZERO, "req-1"),
                            "amendmentNumber is required"),
                    Arguments.of("numero mas largo que la columna",
                            (ThrowingCallable) () -> otrosi("A".repeat(31), EMPRESA,
                                    AmendmentType.ADD_ITEM, EFECTIVA, "motivo", BigDecimal.ZERO,
                                    BigDecimal.ZERO, "req-1"),
                            "amendmentNumber must be 30"),
                    Arguments.of("sin tipo de cambio",
                            (ThrowingCallable) () -> otrosi("AMD-2026-0001", EMPRESA, null,
                                    EFECTIVA, "motivo", BigDecimal.ZERO, BigDecimal.ZERO, "req-1"),
                            "amendmentType"),
                    Arguments.of("motivo mas largo que la columna",
                            (ThrowingCallable) () -> otrosi("AMD-2026-0001", EMPRESA,
                                    AmendmentType.ADD_ITEM, EFECTIVA, "M".repeat(256),
                                    BigDecimal.ZERO, BigDecimal.ZERO, "req-1"),
                            "reason must be 255"),
                    Arguments.of("sin importe de prorrateo",
                            (ThrowingCallable) () -> otrosi("AMD-2026-0001", EMPRESA,
                                    AmendmentType.ADD_ITEM, EFECTIVA, "motivo", null,
                                    BigDecimal.ZERO, "req-1"),
                            "prorationAmount is required"),
                    Arguments.of("sin delta mensual",
                            (ThrowingCallable) () -> otrosi("AMD-2026-0001", EMPRESA,
                                    AmendmentType.ADD_ITEM, EFECTIVA, "motivo", BigDecimal.ZERO,
                                    null, "req-1"),
                            "monthlyDeltaAmount is required"),
                    Arguments.of("llave mas larga que la columna",
                            (ThrowingCallable) () -> otrosi("AMD-2026-0001", EMPRESA,
                                    AmendmentType.ADD_ITEM, EFECTIVA, "motivo", BigDecimal.ZERO,
                                    BigDecimal.ZERO, "K".repeat(65)),
                            "clientRequestId must be 64"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("otrosisInvalidos")
        @DisplayName("el otrosi rechaza lo mismo que rechazaria la base")
        void otrosisInvalidos(String caso, ThrowingCallable creacion, String fragmento) {
            assertThatThrownBy(creacion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(fragmento);
        }

        @Test
        @DisplayName("un otrosi recien emitido no lleva id ni fecha: los pone la base")
        void recienEmitidoNoLlevaIdNiFecha() {
            SubscriptionAmendment enmienda = otrosi(EMPLEADO, null);

            assertThat(enmienda.getId()).isNull();
            assertThat(enmienda.getCreatedDate()).isNull();
            assertThat(enmienda.getSubscriptionId()).isEqualTo(CONTRATO);
            assertThat(enmienda.getAmendmentNumber()).isEqualTo("AMD-2026-0001");
            assertThat(enmienda.getAmendmentType()).isEqualTo(AmendmentType.ADD_ITEM);
            assertThat(enmienda.getEffectiveDate()).isEqualTo(EFECTIVA);
            assertThat(enmienda.getClientRequestId()).isEqualTo("req-1");
            assertThat(enmienda.getQuoteId()).isNull();
        }

        @ParameterizedTest
        @EnumSource(AmendmentType.class)
        @DisplayName("todos los tipos de cambio se pueden documentar")
        void todosLosTiposSeDocumentan(AmendmentType tipo) {
            // @EnumSource y no una lista escrita a mano: es lo que hace que un tipo
            // nuevo -una migracion de tarifa, una renovacion- no pueda entrar sin que
            // alguien compruebe que el otrosi sabe representarlo.
            SubscriptionAmendment enmienda = otrosi("AMD-2026-0001", EMPRESA, tipo, EFECTIVA,
                    "motivo", BigDecimal.ZERO, BigDecimal.ZERO, "req-1");

            assertThat(enmienda.getAmendmentType()).isEqualTo(tipo);
        }
    }
}
