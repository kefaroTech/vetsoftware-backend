package com.vetsoftware.app.generalchargeopenaccount.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("GeneralChargeOpenAccount — invariantes y ciclo de vida del agregado")
class GeneralChargeOpenAccountTest {

    /**
     * Constructor de fixtures con un campo variable por caso. Evita repetir 21
     * argumentos en cada escenario invalido, que es como se cuela un test que
     * valida un campo distinto del que dice validar.
     */
    private static Builder valido() {
        return new Builder();
    }

    private static final class Builder {
        private Long id = GeneralChargeOpenAccountMother.CHARGE_ID;
        private String name = GeneralChargeOpenAccountMother.NOMBRE;
        private BigDecimal unitAmount = GeneralChargeOpenAccountMother.UNITARIO;
        private BigDecimal quantity = GeneralChargeOpenAccountMother.CANTIDAD;
        private TaxRef tax = GeneralChargeOpenAccountMother.IVA_19;
        private OpenAccountRef openAccount = GeneralChargeOpenAccountMother.CUENTA;
        private EmployeeRef createdBy = GeneralChargeOpenAccountMother.EMPLEADO;

        private Builder name(String v) {
            this.name = v;
            return this;
        }

        private Builder unitAmount(BigDecimal v) {
            this.unitAmount = v;
            return this;
        }

        private Builder quantity(BigDecimal v) {
            this.quantity = v;
            return this;
        }

        private Builder tax(TaxRef v) {
            this.tax = v;
            return this;
        }

        private Builder openAccount(OpenAccountRef v) {
            this.openAccount = v;
            return this;
        }

        private GeneralChargeOpenAccount build() {
            return new GeneralChargeOpenAccount(id, name, unitAmount, quantity, tax, tax != null,
                    tax == null ? null : tax.percentage(), tax == null ? null : tax.name(),
                    tax == null ? null : tax.scheme(), GeneralChargeOpenAccountMother.BASE,
                    GeneralChargeOpenAccountMother.IMPUESTO, GeneralChargeOpenAccountMother.TOTAL,
                    openAccount, createdBy, GeneralChargeOpenAccountMother.CREADO, true, false,
                    null, null, null, null);
        }

        private void applyTo(GeneralChargeOpenAccount charge) {
            charge.update(name, unitAmount, quantity, tax, openAccount);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            GeneralChargeOpenAccount charge = valido().build();

            assertThat(charge.getId()).isEqualTo(GeneralChargeOpenAccountMother.CHARGE_ID);
            assertThat(charge.getName()).isEqualTo(GeneralChargeOpenAccountMother.NOMBRE);
            assertThat(charge.getUnitAmount()).isEqualByComparingTo("5950");
            assertThat(charge.getQuantity()).isEqualByComparingTo("2");
            assertThat(charge.getTax()).isEqualTo(GeneralChargeOpenAccountMother.IVA_19);
            assertThat(charge.isHasTax()).isTrue();
            assertThat(charge.getTaxPercentage()).isEqualByComparingTo("19.00");
            assertThat(charge.getTaxName()).isEqualTo("IVA 19%");
            assertThat(charge.getTaxScheme()).isEqualTo("IVA");
            assertThat(charge.getBaseAmount()).isEqualByComparingTo("10000.00");
            assertThat(charge.getTaxAmount()).isEqualByComparingTo("1900.00");
            assertThat(charge.getTotalAmount()).isEqualByComparingTo("11900.00");
            assertThat(charge.getOpenAccount()).isEqualTo(GeneralChargeOpenAccountMother.CUENTA);
            assertThat(charge.getCreatedBy()).isEqualTo(GeneralChargeOpenAccountMother.EMPLEADO);
            assertThat(charge.getCreatedDate()).isEqualTo(GeneralChargeOpenAccountMother.CREADO);
            assertThat(charge.isEnabled()).isTrue();
            assertThat(charge.isVoided()).isFalse();
            assertThat(charge.getVoidedBy()).isNull();
            assertThat(charge.getVoidedAt()).isNull();
            assertThat(charge.getVoidReason()).isNull();
            assertThat(charge.getClientRequestId()).isNull();
        }

        @Test
        @DisplayName("el monto efectivo que aporta a la cuenta es el total persistido")
        void el_monto_efectivo_es_el_total_persistido() {
            GeneralChargeOpenAccount charge = valido().build();

            // Es lo que compara la anulacion contra el saldo pendiente: si devolviera el
            // unitario, un cargo de cantidad 2 se anularia dejando el saldo negativo.
            assertThat(charge.effectiveAmount()).isEqualByComparingTo("11900.00");
        }
    }

    @Nested
    @DisplayName("create — calculo del total y congelacion del impuesto")
    class Create {

        @Test
        @DisplayName("multiplica unitario por cantidad y desglosa el IVA incluido")
        void multiplica_unitario_por_cantidad_y_desglosa_el_iva() {
            GeneralChargeOpenAccount charge = GeneralChargeOpenAccount.create(
                    GeneralChargeOpenAccountMother.NOMBRE, GeneralChargeOpenAccountMother.UNITARIO,
                    GeneralChargeOpenAccountMother.CANTIDAD, GeneralChargeOpenAccountMother.IVA_19,
                    GeneralChargeOpenAccountMother.CUENTA, GeneralChargeOpenAccountMother.EMPLEADO,
                    "req-1");

            // 5.950 x 2 = 11.900 con IVA 19 % incluido = 10.000 de base + 1.900.
            assertThat(charge.getTotalAmount()).isEqualByComparingTo("11900.00");
            assertThat(charge.getBaseAmount()).isEqualByComparingTo("10000.00");
            assertThat(charge.getTaxAmount()).isEqualByComparingTo("1900.00");
            assertThat(charge.getBaseAmount().add(charge.getTaxAmount()))
                    .as("base + impuesto tiene que cuadrar con el total")
                    .isEqualByComparingTo(charge.getTotalAmount());
        }

        @Test
        @DisplayName("congela nombre, porcentaje y esquema del impuesto del catalogo")
        void congela_el_impuesto_del_catalogo() {
            GeneralChargeOpenAccount charge = GeneralChargeOpenAccount.create(
                    GeneralChargeOpenAccountMother.NOMBRE, GeneralChargeOpenAccountMother.UNITARIO,
                    GeneralChargeOpenAccountMother.CANTIDAD, GeneralChargeOpenAccountMother.IVA_19,
                    GeneralChargeOpenAccountMother.CUENTA, GeneralChargeOpenAccountMother.EMPLEADO,
                    null);

            // El snapshot es lo que sostiene el total: editar el catalogo de impuestos no
            // puede mover el importe de un cargo ya registrado.
            assertThat(charge.isHasTax()).isTrue();
            assertThat(charge.getTaxPercentage()).isEqualByComparingTo("19.00");
            assertThat(charge.getTaxName()).isEqualTo("IVA 19%");
            assertThat(charge.getTaxScheme()).isEqualTo("IVA");
        }

        @Test
        @DisplayName("nace sin id, habilitado y sin anular")
        void nace_sin_id_habilitado_y_sin_anular() {
            GeneralChargeOpenAccount charge = GeneralChargeOpenAccount.create(
                    GeneralChargeOpenAccountMother.NOMBRE, GeneralChargeOpenAccountMother.UNITARIO,
                    GeneralChargeOpenAccountMother.CANTIDAD, GeneralChargeOpenAccountMother.IVA_19,
                    GeneralChargeOpenAccountMother.CUENTA, GeneralChargeOpenAccountMother.EMPLEADO,
                    null);

            assertThat(charge.getId()).isNull();
            assertThat(charge.isEnabled()).isTrue();
            assertThat(charge.isVoided()).isFalse();
            assertThat(charge.getVoidedBy()).isNull();
            // createdDate lo pone LocalDateTime.now() dentro del factory: no hay Clock
            // inyectable, asi que la asercion tiene que ser una ventana. Deuda anotada en
            // "Determinismo" del CLAUDE.md.
            assertThat(charge.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("arrastra la idempotency key al agregado")
        void arrastra_la_idempotency_key() {
            GeneralChargeOpenAccount charge = GeneralChargeOpenAccount.create(
                    GeneralChargeOpenAccountMother.NOMBRE, GeneralChargeOpenAccountMother.UNITARIO,
                    GeneralChargeOpenAccountMother.CANTIDAD, GeneralChargeOpenAccountMother.IVA_19,
                    GeneralChargeOpenAccountMother.CUENTA, GeneralChargeOpenAccountMother.EMPLEADO,
                    "req-42");

            assertThat(charge.getClientRequestId()).isEqualTo("req-42");
        }

        @Test
        @DisplayName("sin impuesto asignado el total entero es base y hasTax queda en false")
        void sin_impuesto_el_total_entero_es_base() {
            GeneralChargeOpenAccount charge = GeneralChargeOpenAccount.create(
                    GeneralChargeOpenAccountMother.NOMBRE, GeneralChargeOpenAccountMother.UNITARIO,
                    GeneralChargeOpenAccountMother.CANTIDAD, null,
                    GeneralChargeOpenAccountMother.CUENTA, GeneralChargeOpenAccountMother.EMPLEADO,
                    null);

            assertThat(charge.isHasTax()).isFalse();
            assertThat(charge.getTaxPercentage()).isNull();
            assertThat(charge.getTaxName()).isNull();
            assertThat(charge.getTaxScheme()).isNull();
            assertThat(charge.getBaseAmount()).isEqualByComparingTo("11900.00");
            assertThat(charge.getTaxAmount()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("un impuesto del 0 % deja base = total pero conserva el snapshot")
        void un_impuesto_del_cero_por_ciento_deja_base_igual_al_total() {
            GeneralChargeOpenAccount charge = GeneralChargeOpenAccount.create(
                    GeneralChargeOpenAccountMother.NOMBRE, GeneralChargeOpenAccountMother.UNITARIO,
                    GeneralChargeOpenAccountMother.CANTIDAD, GeneralChargeOpenAccountMother.IVA_0,
                    GeneralChargeOpenAccountMother.CUENTA, GeneralChargeOpenAccountMother.EMPLEADO,
                    null);

            // EXENTO (IVA 0 %) no es lo mismo que EXCLUIDO (sin impuesto): el documento
            // del cierre tiene que poder distinguirlos aunque el importe sea el mismo.
            assertThat(charge.isHasTax()).isTrue();
            assertThat(charge.getTaxPercentage()).isEqualByComparingTo("0");
            assertThat(charge.getTaxScheme()).isEqualTo("IVA");
            assertThat(charge.getBaseAmount()).isEqualByComparingTo("11900.00");
            assertThat(charge.getTaxAmount()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("una cantidad decimal se multiplica a escala monetaria")
        void una_cantidad_decimal_se_multiplica_a_escala_monetaria() {
            GeneralChargeOpenAccount charge = GeneralChargeOpenAccount.create("Suero",
                    new BigDecimal("1000.00"), new BigDecimal("2.5"), null,
                    GeneralChargeOpenAccountMother.CUENTA, GeneralChargeOpenAccountMother.EMPLEADO,
                    null);

            assertThat(charge.getTotalAmount()).isEqualByComparingTo("2500.00");
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("name null", (ThrowingCallable) () -> valido().name(null).build(),
                            "name is required"),
                    arguments("name en blanco",
                            (ThrowingCallable) () -> valido().name("   ").build(),
                            "name is required"),
                    arguments("name de mas de 150 caracteres",
                            (ThrowingCallable) () -> valido().name("x".repeat(151)).build(),
                            "name must be 150 chars or less"),
                    arguments("unitAmount null",
                            (ThrowingCallable) () -> valido().unitAmount(null).build(),
                            "unitAmount is required"),
                    arguments("unitAmount negativo",
                            (ThrowingCallable) () -> valido().unitAmount(new BigDecimal("-1"))
                                    .build(),
                            "unitAmount cannot be negative"),
                    arguments("quantity null",
                            (ThrowingCallable) () -> valido().quantity(null).build(),
                            "quantity is required"),
                    arguments("quantity en cero",
                            (ThrowingCallable) () -> valido().quantity(BigDecimal.ZERO).build(),
                            "quantity must be greater than zero"),
                    arguments("quantity negativa",
                            (ThrowingCallable) () -> valido().quantity(new BigDecimal("-2"))
                                    .build(),
                            "quantity must be greater than zero"),
                    arguments("openAccount null",
                            (ThrowingCallable) () -> valido().openAccount(null).build(),
                            "openAccount is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("el constructor rechaza")
        void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("un importe unitario de cero es valido: hay cargos de cortesia")
        void un_importe_unitario_de_cero_es_valido() {
            assertThatCode(() -> valido().unitAmount(BigDecimal.ZERO).build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("un nombre de exactamente 150 caracteres pasa: el limite es inclusivo")
        void un_nombre_de_exactamente_150_caracteres_pasa() {
            assertThatCode(() -> valido().name("x".repeat(150)).build()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("create aplica las mismas invariantes que el constructor")
        void create_aplica_las_mismas_invariantes() {
            assertThatThrownBy(() -> GeneralChargeOpenAccount.create(null,
                    GeneralChargeOpenAccountMother.UNITARIO,
                    GeneralChargeOpenAccountMother.CANTIDAD, null,
                    GeneralChargeOpenAccountMother.CUENTA, GeneralChargeOpenAccountMother.EMPLEADO,
                    null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("reemplaza los campos mutables y conserva id y fecha de creacion")
        void reemplaza_los_campos_mutables() {
            GeneralChargeOpenAccount charge = valido().build();

            valido().name("Traslado nocturno").unitAmount(new BigDecimal("2975"))
                    .quantity(new BigDecimal("4"))
                    .openAccount(GeneralChargeOpenAccountMother.OTRA_CUENTA).applyTo(charge);

            assertThat(charge.getName()).isEqualTo("Traslado nocturno");
            assertThat(charge.getUnitAmount()).isEqualByComparingTo("2975");
            assertThat(charge.getQuantity()).isEqualByComparingTo("4");
            assertThat(charge.getOpenAccount())
                    .isEqualTo(GeneralChargeOpenAccountMother.OTRA_CUENTA);
            assertThat(charge.getId()).isEqualTo(GeneralChargeOpenAccountMother.CHARGE_ID);
            assertThat(charge.getCreatedDate()).isEqualTo(GeneralChargeOpenAccountMother.CREADO);
        }

        @Test
        @DisplayName("SI recalcula el total: aqui el importe lo teclea el usuario")
        void si_recalcula_el_total() {
            GeneralChargeOpenAccount charge = valido().build();

            valido().unitAmount(new BigDecimal("1000")).quantity(new BigDecimal("3"))
                    .applyTo(charge);

            // A diferencia del cargo de servicio (precio congelado del catalogo), el cargo
            // general es un importe libre: corregirlo tiene que mover el total.
            assertThat(charge.getTotalAmount()).isEqualByComparingTo("3000.00");
            assertThat(charge.getBaseAmount()).isEqualByComparingTo("2521.01");
            assertThat(charge.getTaxAmount()).isEqualByComparingTo("478.99");
            assertThat(charge.getBaseAmount().add(charge.getTaxAmount()))
                    .isEqualByComparingTo(charge.getTotalAmount());
        }

        @Test
        @DisplayName("quitar el impuesto vuelve a poner el total entero como base")
        void quitar_el_impuesto_pone_el_total_entero_como_base() {
            GeneralChargeOpenAccount charge = valido().build();

            valido().tax(null).applyTo(charge);

            assertThat(charge.isHasTax()).isFalse();
            assertThat(charge.getTax()).isNull();
            assertThat(charge.getTaxPercentage()).isNull();
            assertThat(charge.getTaxName()).isNull();
            assertThat(charge.getTaxScheme()).isNull();
            assertThat(charge.getBaseAmount()).isEqualByComparingTo("11900.00");
            assertThat(charge.getTaxAmount()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("un update invalido no deja el agregado a medias")
        void un_update_invalido_no_deja_el_agregado_a_medias() {
            GeneralChargeOpenAccount charge = valido().build();

            assertThatThrownBy(() -> valido().name("Traslado nocturno").quantity(BigDecimal.ZERO)
                    .applyTo(charge)).isInstanceOf(IllegalArgumentException.class);

            assertThat(charge.getName()).isEqualTo(GeneralChargeOpenAccountMother.NOMBRE);
            assertThat(charge.getQuantity()).isEqualByComparingTo("2");
            assertThat(charge.getTotalAmount()).isEqualByComparingTo("11900.00");
        }
    }

    @Nested
    @DisplayName("anulacion")
    class Anulacion {

        @Test
        @DisplayName("registra quien, cuando y por que, y deja la fila visible")
        void registra_quien_cuando_y_por_que() {
            GeneralChargeOpenAccount charge = valido().build();

            charge.voidCharge(GeneralChargeOpenAccountMother.OTRO_EMPLEADO, "Cobrado por error");

            assertThat(charge.isVoided()).isTrue();
            assertThat(charge.getVoidedBy())
                    .isEqualTo(GeneralChargeOpenAccountMother.OTRO_EMPLEADO);
            assertThat(charge.getVoidReason()).isEqualTo("Cobrado por error");
            assertThat(charge.getVoidedAt()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
            // Anular NO es borrar: la fila sigue habilitada y visible en el historico.
            assertThat(charge.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("un cargo ya anulado no se puede volver a anular")
        void un_cargo_ya_anulado_no_se_puede_volver_a_anular() {
            GeneralChargeOpenAccount charge = GeneralChargeOpenAccountMother.cargoAnulado();

            assertThatThrownBy(
                    () -> charge.voidCharge(GeneralChargeOpenAccountMother.EMPLEADO, "Otra vez"))
                    .isInstanceOf(GeneralChargeOpenAccountAlreadyVoidedException.class)
                    .hasMessageContaining(String.valueOf(GeneralChargeOpenAccountMother.CHARGE_ID));
        }

        @Test
        @DisplayName("exige el empleado que anula")
        void exige_el_empleado_que_anula() {
            GeneralChargeOpenAccount charge = valido().build();

            assertThatThrownBy(() -> charge.voidCharge(null, "Motivo"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("voidedBy is required");
            assertThat(charge.isVoided()).as("un intento fallido no puede anular").isFalse();
        }

        @ParameterizedTest(name = "motivo [{0}]")
        @MethodSource("motivosInvalidos")
        @DisplayName("exige un motivo con contenido")
        void exige_un_motivo_con_contenido(String motivo) {
            GeneralChargeOpenAccount charge = valido().build();

            assertThatThrownBy(
                    () -> charge.voidCharge(GeneralChargeOpenAccountMother.OTRO_EMPLEADO, motivo))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reason is required to void");
            assertThat(charge.isVoided()).isFalse();
        }

        static Stream<Arguments> motivosInvalidos() {
            return Stream.of(arguments((Object) null), arguments(""), arguments("   "));
        }

        @Test
        @DisplayName("anular no cambia el total persistido: el que lo excluye es la query")
        void anular_no_cambia_el_total_persistido() {
            GeneralChargeOpenAccount charge = valido().build();

            charge.voidCharge(GeneralChargeOpenAccountMother.OTRO_EMPLEADO, "Cobrado por error");

            assertThat(charge.getTotalAmount()).isEqualByComparingTo("11900.00");
            assertThat(charge.effectiveAmount()).isEqualByComparingTo("11900.00");
        }
    }
}
