package com.vetsoftware.app.servicechargeopenaccount.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother;
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

@DisplayName("ServiceChargeOpenAccount — invariantes y ciclo de vida del agregado")
class ServiceChargeOpenAccountTest {

    /**
     * Constructor de fixtures con un campo variable por caso. Evita repetir 22
     * argumentos en cada escenario invalido, que es como se cuela un test que
     * valida un campo distinto del que dice validar.
     */
    private static Builder valido() {
        return new Builder();
    }

    private static final class Builder {
        private Long id = ServiceChargeOpenAccountMother.CHARGE_ID;
        private AnimalRef animal = ServiceChargeOpenAccountMother.ANIMAL;
        private ServiceRef service = ServiceChargeOpenAccountMother.SERVICIO;
        private BigDecimal unitPrice = ServiceChargeOpenAccountMother.PRECIO;
        private OpenAccountRef openAccount = ServiceChargeOpenAccountMother.CUENTA;
        private EmployeeRef createdBy = ServiceChargeOpenAccountMother.EMPLEADO;

        private Builder animal(AnimalRef v) {
            this.animal = v;
            return this;
        }

        private Builder service(ServiceRef v) {
            this.service = v;
            return this;
        }

        private Builder unitPrice(BigDecimal v) {
            this.unitPrice = v;
            return this;
        }

        private Builder openAccount(OpenAccountRef v) {
            this.openAccount = v;
            return this;
        }

        private ServiceChargeOpenAccount build() {
            return new ServiceChargeOpenAccount(id, animal, service, unitPrice,
                    ServiceChargeOpenAccountMother.IVA_19, true, new BigDecimal("19.00"), "IVA 19%",
                    "IVA", "GRAVADO", ServiceChargeOpenAccountMother.BASE,
                    ServiceChargeOpenAccountMother.IMPUESTO, ServiceChargeOpenAccountMother.TOTAL,
                    openAccount, createdBy, ServiceChargeOpenAccountMother.CREADO, null, true,
                    false, null, null, null, null);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            ServiceChargeOpenAccount charge = valido().build();

            assertThat(charge.getId()).isEqualTo(ServiceChargeOpenAccountMother.CHARGE_ID);
            assertThat(charge.getAnimal()).isEqualTo(ServiceChargeOpenAccountMother.ANIMAL);
            assertThat(charge.getService()).isEqualTo(ServiceChargeOpenAccountMother.SERVICIO);
            assertThat(charge.getUnitPrice())
                    .isEqualByComparingTo(ServiceChargeOpenAccountMother.PRECIO);
            assertThat(charge.getTax()).isEqualTo(ServiceChargeOpenAccountMother.IVA_19);
            assertThat(charge.isHasTax()).isTrue();
            assertThat(charge.getTaxPercentage()).isEqualByComparingTo("19.00");
            assertThat(charge.getTaxName()).isEqualTo("IVA 19%");
            assertThat(charge.getTaxScheme()).isEqualTo("IVA");
            assertThat(charge.getTaxTreatment()).isEqualTo("GRAVADO");
            assertThat(charge.getBaseAmount()).isEqualByComparingTo("10000.00");
            assertThat(charge.getTaxAmount()).isEqualByComparingTo("1900.00");
            assertThat(charge.getTotalAmount()).isEqualByComparingTo("11900.00");
            assertThat(charge.getOpenAccount()).isEqualTo(ServiceChargeOpenAccountMother.CUENTA);
            assertThat(charge.getCreatedBy()).isEqualTo(ServiceChargeOpenAccountMother.EMPLEADO);
            assertThat(charge.getCreatedDate()).isEqualTo(ServiceChargeOpenAccountMother.CREADO);
            assertThat(charge.isEnabled()).isTrue();
            assertThat(charge.isVoided()).isFalse();
            assertThat(charge.getVoidedBy()).isNull();
            assertThat(charge.getVoidedAt()).isNull();
            assertThat(charge.getVoidReason()).isNull();
            assertThat(charge.getClientRequestId()).isNull();
        }

        @Test
        @DisplayName("el constructor de compatibilidad deja base = total y el impuesto en cero")
        void el_constructor_de_compatibilidad_deja_base_igual_a_total() {
            ServiceChargeOpenAccount charge = ServiceChargeOpenAccountMother.cargoSinImpuesto();

            assertThat(charge.isHasTax()).isFalse();
            assertThat(charge.getTax()).isNull();
            assertThat(charge.getTaxPercentage()).isNull();
            assertThat(charge.getTaxName()).isNull();
            assertThat(charge.getTaxScheme()).isNull();
            assertThat(charge.getBaseAmount()).isEqualByComparingTo("5000.00");
            assertThat(charge.getTaxAmount()).isEqualByComparingTo("0.00");
            assertThat(charge.getTotalAmount()).isEqualByComparingTo("5000.00");
        }
    }

    @Nested
    @DisplayName("create — congelacion del precio y del impuesto")
    class Create {

        @Test
        @DisplayName("congela el precio del catalogo y desglosa el IVA incluido")
        void congela_el_precio_y_desglosa_el_iva() {
            ServiceChargeOpenAccount charge = ServiceChargeOpenAccount.create(
                    ServiceChargeOpenAccountMother.ANIMAL, ServiceChargeOpenAccountMother.SERVICIO,
                    ServiceChargeOpenAccountMother.CUENTA, ServiceChargeOpenAccountMother.EMPLEADO,
                    "req-1");

            // 11.900 con IVA 19 % incluido = 10.000 de base + 1.900 de impuesto.
            assertThat(charge.getUnitPrice()).isEqualByComparingTo("11900");
            assertThat(charge.getBaseAmount()).isEqualByComparingTo("10000.00");
            assertThat(charge.getTaxAmount()).isEqualByComparingTo("1900.00");
            assertThat(charge.getTotalAmount()).isEqualByComparingTo("11900.00");
            assertThat(charge.getBaseAmount().add(charge.getTaxAmount()))
                    .as("base + impuesto tiene que cuadrar con el total")
                    .isEqualByComparingTo(charge.getTotalAmount());
        }

        @Test
        @DisplayName("nace sin id, habilitado y sin anular")
        void nace_sin_id_habilitado_y_sin_anular() {
            ServiceChargeOpenAccount charge = ServiceChargeOpenAccount.create(
                    ServiceChargeOpenAccountMother.ANIMAL, ServiceChargeOpenAccountMother.SERVICIO,
                    ServiceChargeOpenAccountMother.CUENTA, ServiceChargeOpenAccountMother.EMPLEADO,
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
            ServiceChargeOpenAccount charge = ServiceChargeOpenAccount.create(
                    ServiceChargeOpenAccountMother.ANIMAL, ServiceChargeOpenAccountMother.SERVICIO,
                    ServiceChargeOpenAccountMother.CUENTA, ServiceChargeOpenAccountMother.EMPLEADO,
                    "req-42");

            assertThat(charge.getClientRequestId()).isEqualTo("req-42");
        }

        @Test
        @DisplayName("un servicio sin impuesto no genera impuesto pero conserva el tratamiento")
        void un_servicio_sin_impuesto_no_genera_impuesto() {
            ServiceRef exento = new ServiceRef(6L, "Pesaje", new BigDecimal("3000"), false, null,
                    "EXENTO");

            ServiceChargeOpenAccount charge = ServiceChargeOpenAccount.create(
                    ServiceChargeOpenAccountMother.ANIMAL, exento,
                    ServiceChargeOpenAccountMother.CUENTA, ServiceChargeOpenAccountMother.EMPLEADO,
                    null);

            assertThat(charge.isHasTax()).isFalse();
            assertThat(charge.getTaxAmount()).isEqualByComparingTo("0.00");
            assertThat(charge.getBaseAmount()).isEqualByComparingTo("3000.00");
            // EXENTO (IVA 0 %) hay que poder distinguirlo de EXCLUIDO al emitir el
            // documento del cierre, aunque el importe retenido sea el mismo: cero.
            assertThat(charge.getTaxTreatment()).isEqualTo("EXENTO");
        }

        @Test
        @DisplayName("hasTax con tax null no rompe: se trata como sin impuesto")
        void has_tax_con_tax_null_se_trata_como_sin_impuesto() {
            ServiceRef incoherente = new ServiceRef(7L, "Raro", new BigDecimal("1000"), true, null,
                    "GRAVADO");

            ServiceChargeOpenAccount charge = ServiceChargeOpenAccount.create(
                    ServiceChargeOpenAccountMother.ANIMAL, incoherente,
                    ServiceChargeOpenAccountMother.CUENTA, ServiceChargeOpenAccountMother.EMPLEADO,
                    null);

            assertThat(charge.isHasTax()).isFalse();
            assertThat(charge.getTaxAmount()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("un servicio null recorre las ramas de congelacion antes de que el constructor lo rechace")
        void un_servicio_null_recorre_las_ramas_antes_de_que_el_constructor_lo_rechace() {
            // create() calcula unitPrice, hasTax y taxTreatment con ternarios que miran
            // "service == null" antes de delegar en el constructor: ese camino solo lo
            // ejercita un servicio null, aunque el constructor termine rechazandolo.
            assertThatThrownBy(
                    () -> ServiceChargeOpenAccount.create(ServiceChargeOpenAccountMother.ANIMAL,
                            null, ServiceChargeOpenAccountMother.CUENTA,
                            ServiceChargeOpenAccountMother.EMPLEADO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("service is required");
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("animal null", (ThrowingCallable) () -> valido().animal(null).build(),
                            "animal is required"),
                    arguments("service null",
                            (ThrowingCallable) () -> valido().service(null).build(),
                            "service is required"),
                    arguments("openAccount null",
                            (ThrowingCallable) () -> valido().openAccount(null).build(),
                            "openAccount is required"),
                    arguments("unitPrice null",
                            (ThrowingCallable) () -> valido().unitPrice(null).build(),
                            "unitPrice is required"),
                    arguments(
                            "unitPrice negativo", (ThrowingCallable) () -> valido()
                                    .unitPrice(new BigDecimal("-1")).build(),
                            "unitPrice cannot be negative"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("el constructor rechaza")
        void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("un precio de cero es valido: hay servicios de cortesia")
        void un_precio_de_cero_es_valido() {
            assertThatCode(() -> valido().unitPrice(BigDecimal.ZERO).build())
                    .doesNotThrowAnyException();
        }

    }

    @Nested
    @DisplayName("anulacion")
    class Anulacion {

        @Test
        @DisplayName("registra quien, cuando y por que, y deja la fila visible")
        void registra_quien_cuando_y_por_que() {
            ServiceChargeOpenAccount charge = valido().build();

            charge.voidCharge(ServiceChargeOpenAccountMother.OTRO_EMPLEADO, "Cobrado por error");

            assertThat(charge.isVoided()).isTrue();
            assertThat(charge.getVoidedBy())
                    .isEqualTo(ServiceChargeOpenAccountMother.OTRO_EMPLEADO);
            assertThat(charge.getVoidReason()).isEqualTo("Cobrado por error");
            assertThat(charge.getVoidedAt()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
            // Anular NO es borrar: la fila sigue habilitada y visible en el historico.
            assertThat(charge.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("un cargo ya anulado no se puede volver a anular")
        void un_cargo_ya_anulado_no_se_puede_volver_a_anular() {
            ServiceChargeOpenAccount charge = ServiceChargeOpenAccountMother.cargoAnulado();

            assertThatThrownBy(
                    () -> charge.voidCharge(ServiceChargeOpenAccountMother.EMPLEADO, "Otra vez"))
                    .isInstanceOf(ServiceChargeOpenAccountAlreadyVoidedException.class)
                    .hasMessageContaining(String.valueOf(ServiceChargeOpenAccountMother.CHARGE_ID));
        }

        @Test
        @DisplayName("exige el empleado que anula")
        void exige_el_empleado_que_anula() {
            ServiceChargeOpenAccount charge = valido().build();

            assertThatThrownBy(() -> charge.voidCharge(null, "Motivo"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("voidedBy is required");
            assertThat(charge.isVoided()).as("un intento fallido no puede anular").isFalse();
        }

        @ParameterizedTest(name = "motivo [{0}]")
        @MethodSource("motivosInvalidos")
        @DisplayName("exige un motivo con contenido")
        void exige_un_motivo_con_contenido(String motivo) {
            ServiceChargeOpenAccount charge = valido().build();

            assertThatThrownBy(
                    () -> charge.voidCharge(ServiceChargeOpenAccountMother.OTRO_EMPLEADO, motivo))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reason is required to void");
            assertThat(charge.isVoided()).isFalse();
        }

        static Stream<Arguments> motivosInvalidos() {
            return Stream.of(arguments((Object) null), arguments(""), arguments("   "));
        }
    }
}
