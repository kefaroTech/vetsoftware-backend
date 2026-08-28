package com.vetsoftware.app.accountingaccount.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("AccountingAccount - invariantes y ciclo de vida del agregado")
class AccountingAccountTest {

    /**
     * Constructor de fixtures con un campo variable por caso. Los defaults son un
     * nivel 6 postable con padre: evita repetir 13 argumentos en cada escenario
     * invalido, que es como se cuela un test que valida un campo distinto del que
     * dice validar.
     */
    private static Builder valido() {
        return new Builder();
    }

    private static final class Builder {
        private Long id = 10L;
        private String code = "110501";
        private String name = "Caja general";
        private AccountClass accountClass = AccountClass.ASSET;
        private String parentCode = "1105";
        private int accountLevel = 6;
        private boolean postable = true;
        private boolean requiresThirdParty;
        private LocalDate validFrom = LocalDate.of(2026, 1, 1);
        private LocalDate validTo;
        private LocalDateTime createdDate = LocalDateTime.of(2026, 1, 1, 9, 0);
        private boolean enabled = true;
        private Long version = 3L;

        private Builder code(String v) {
            this.code = v;
            return this;
        }

        private Builder name(String v) {
            this.name = v;
            return this;
        }

        private Builder accountClass(AccountClass v) {
            this.accountClass = v;
            return this;
        }

        private Builder parentCode(String v) {
            this.parentCode = v;
            return this;
        }

        private Builder accountLevel(int v) {
            this.accountLevel = v;
            return this;
        }

        private Builder postable(boolean v) {
            this.postable = v;
            return this;
        }

        private Builder validFrom(LocalDate v) {
            this.validFrom = v;
            return this;
        }

        private Builder validTo(LocalDate v) {
            this.validTo = v;
            return this;
        }

        private Builder createdDate(LocalDateTime v) {
            this.createdDate = v;
            return this;
        }

        private AccountingAccount build() {
            return new AccountingAccount(id, code, name, accountClass, parentCode, accountLevel,
                    postable, requiresThirdParty, validFrom, validTo, createdDate, enabled,
                    version);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            AccountingAccount cuenta = valido().build();

            assertThat(cuenta.getId()).isEqualTo(10L);
            assertThat(cuenta.getCode()).isEqualTo("110501");
            assertThat(cuenta.getName()).isEqualTo("Caja general");
            assertThat(cuenta.getAccountClass()).isEqualTo(AccountClass.ASSET);
            assertThat(cuenta.getParentCode()).isEqualTo("1105");
            assertThat(cuenta.getAccountLevel()).isEqualTo(6);
            assertThat(cuenta.isPostable()).isTrue();
            assertThat(cuenta.isRequiresThirdParty()).isFalse();
            assertThat(cuenta.getValidFrom()).isEqualTo(LocalDate.of(2026, 1, 1));
            assertThat(cuenta.getValidTo()).isNull();
            assertThat(cuenta.getCreatedDate()).isEqualTo(LocalDateTime.of(2026, 1, 1, 9, 0));
            assertThat(cuenta.isEnabled()).isTrue();
            assertThat(cuenta.getVersion()).isEqualTo(3L);
        }

        @Test
        @DisplayName("create() nace sin id, habilitada y sin version")
        void create_nace_sin_id_habilitada_y_sin_version() {
            AccountingAccount cuenta = AccountingAccount.create("110501", "Caja general",
                    AccountClass.ASSET, "1105", 6, true, false, LocalDate.of(2026, 1, 1), null,
                    LocalDateTime.of(2026, 1, 1, 9, 0));

            assertThat(cuenta.getId()).isNull();
            assertThat(cuenta.isEnabled()).isTrue();
            assertThat(cuenta.getVersion()).isNull();
            // El createdDate lo sella el caller con su Clock inyectado, no un now()
            // interno: es exactamente el que se paso, sin ventana.
            assertThat(cuenta.getCreatedDate()).isEqualTo(LocalDateTime.of(2026, 1, 1, 9, 0));
        }

        @Test
        @DisplayName("create() acepta validTo para cargar un plan historico ya cerrado")
        void create_acepta_valid_to_para_cargar_plan_historico() {
            AccountingAccount cuenta = AccountingAccount.create("110501", "Caja general",
                    AccountClass.ASSET, "1105", 6, true, false, LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 12, 1), LocalDateTime.of(2026, 1, 1, 9, 0));

            assertThat(cuenta.getValidTo()).isEqualTo(LocalDate.of(2024, 12, 1));
            assertThat(cuenta.isOpen()).isFalse();
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("code null", (ThrowingCallable) () -> valido().code(null).build(),
                            "code is required"),
                    arguments("code en blanco",
                            (ThrowingCallable) () -> valido().code("   ").build(),
                            "code is required"),
                    arguments("code de 11 chars",
                            (ThrowingCallable) () -> valido().code("x".repeat(11)).build(),
                            "code must be 10 chars or less"),
                    arguments("code con espacio inicial",
                            (ThrowingCallable) () -> valido().code(" 1105").build(),
                            "code must not have leading or trailing spaces"),
                    arguments("code con espacio final",
                            (ThrowingCallable) () -> valido().code("1105 ").build(),
                            "code must not have leading or trailing spaces"),
                    arguments("name null", (ThrowingCallable) () -> valido().name(null).build(),
                            "name is required"),
                    arguments("name en blanco",
                            (ThrowingCallable) () -> valido().name("   ").build(),
                            "name is required"),
                    arguments("name de 121 chars",
                            (ThrowingCallable) () -> valido().name("x".repeat(121)).build(),
                            "name must be 120 chars or less"),
                    arguments("accountClass null",
                            (ThrowingCallable) () -> valido().accountClass(null).build(),
                            "accountClass is required"),
                    arguments("nivel invalido: 3",
                            (ThrowingCallable) () -> valido().accountLevel(3).postable(false)
                                    .build(),
                            "accountLevel must be one of 1, 2, 4 or 6"),
                    arguments("nivel invalido: 0",
                            (ThrowingCallable) () -> valido().accountLevel(0).postable(false)
                                    .build(),
                            "accountLevel must be one of 1, 2, 4 or 6"),
                    arguments("nivel 2 postable: solo nivel 6 asienta",
                            (ThrowingCallable) () -> valido().accountLevel(2).postable(true)
                                    .build(),
                            "only a level 6 account can be postable"),
                    arguments("nivel 4 postable: solo nivel 6 asienta",
                            (ThrowingCallable) () -> valido().accountLevel(4).postable(true)
                                    .build(),
                            "only a level 6 account can be postable"),
                    arguments("raiz con padre",
                            (ThrowingCallable) () -> valido().accountLevel(1).postable(false)
                                    .build(),
                            "the root account must not have a parent code"),
                    arguments("nivel 2 sin padre",
                            (ThrowingCallable) () -> valido().accountLevel(2).postable(false)
                                    .parentCode(null).build(),
                            "parentCode is required below level 1"),
                    arguments("nivel 2 con padre en blanco",
                            (ThrowingCallable) () -> valido().accountLevel(2).postable(false)
                                    .parentCode("   ").build(),
                            "parentCode is required below level 1"),
                    arguments("padre de 11 chars",
                            (ThrowingCallable) () -> valido().parentCode("x".repeat(11)).build(),
                            "parentCode must be 10 chars or less"),
                    arguments("validFrom null",
                            (ThrowingCallable) () -> valido().validFrom(null).build(),
                            "validFrom is required"),
                    arguments("validTo igual a validFrom",
                            (ThrowingCallable) () -> valido().validFrom(LocalDate.of(2026, 1, 1))
                                    .validTo(LocalDate.of(2026, 1, 1)).build(),
                            "validTo must be after validFrom"),
                    arguments("validTo anterior a validFrom",
                            (ThrowingCallable) () -> valido().validFrom(LocalDate.of(2026, 1, 1))
                                    .validTo(LocalDate.of(2025, 12, 31)).build(),
                            "validTo must be after validFrom"),
                    arguments("createdDate null",
                            (ThrowingCallable) () -> valido().createdDate(null).build(),
                            "createdDate is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("el constructor rechaza")
        void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("code de exactamente 10 chars se acepta")
        void code_de_10_chars_se_acepta() {
            assertThatCode(() -> valido().code("x".repeat(10)).build()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("name de exactamente 120 chars se acepta")
        void name_de_120_chars_se_acepta() {
            assertThatCode(() -> valido().name("x".repeat(120)).build()).doesNotThrowAnyException();
        }

        static Stream<Arguments> nivelesValidos() {
            return Stream.of(arguments(1, null, false), arguments(2, "11", false),
                    arguments(4, "1105", false), arguments(6, "110501", true));
        }

        @ParameterizedTest(name = "nivel {0}")
        @MethodSource("nivelesValidos")
        @DisplayName("los cuatro niveles del plan de cuentas se aceptan con su padre correcto")
        void los_cuatro_niveles_se_aceptan(int nivel, String padre, boolean postable) {
            assertThatCode(
                    () -> valido().accountLevel(nivel).parentCode(padre).postable(postable).build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("validTo estrictamente posterior a validFrom se acepta")
        void valid_to_posterior_se_acepta() {
            assertThatCode(() -> valido().validFrom(LocalDate.of(2026, 1, 1))
                    .validTo(LocalDate.of(2026, 1, 2)).build()).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("update cambia nombre y tercero identificado, conserva lo demas y la version")
        void update_cambia_nombre_y_tercero_conserva_lo_demas() {
            AccountingAccount original = valido().build();

            AccountingAccount actualizada = original.update("Caja general - sede norte", true);

            assertThat(actualizada.getName()).isEqualTo("Caja general - sede norte");
            assertThat(actualizada.isRequiresThirdParty()).isTrue();
            assertThat(actualizada.getCode()).isEqualTo(original.getCode());
            assertThat(actualizada.getAccountClass()).isEqualTo(original.getAccountClass());
            assertThat(actualizada.getParentCode()).isEqualTo(original.getParentCode());
            assertThat(actualizada.getAccountLevel()).isEqualTo(original.getAccountLevel());
            assertThat(actualizada.isPostable()).isEqualTo(original.isPostable());
            assertThat(actualizada.getValidFrom()).isEqualTo(original.getValidFrom());
            assertThat(actualizada.getValidTo()).isEqualTo(original.getValidTo());
            // La version viaja intacta: es lo que mantiene el save() posterior dentro
            // del ciclo leer-modificar-guardar con bloqueo optimista.
            assertThat(actualizada.getVersion()).isEqualTo(original.getVersion());
        }
    }

    @Nested
    @DisplayName("cierre de vigencia")
    class Cierre {

        @Test
        @DisplayName("close pone la fecha de fin y conserva la version para el bloqueo optimista")
        void close_pone_fecha_fin_y_conserva_version() {
            AccountingAccount abierta = valido().validTo(null).build();

            AccountingAccount cerrada = abierta.close(LocalDate.of(2026, 6, 1));

            assertThat(cerrada.getValidTo()).isEqualTo(LocalDate.of(2026, 6, 1));
            assertThat(cerrada.isOpen()).isFalse();
            assertThat(cerrada.getVersion()).isEqualTo(abierta.getVersion());
            assertThat(cerrada.getCode()).isEqualTo(abierta.getCode());
        }

        @Test
        @DisplayName("cerrar una cuenta ya cerrada lanza y no admite un segundo cierre silencioso")
        void cerrar_una_cuenta_ya_cerrada_lanza() {
            AccountingAccount cerrada = valido().validTo(LocalDate.of(2026, 3, 1)).build();

            assertThatThrownBy(() -> cerrada.close(LocalDate.of(2026, 6, 1)))
                    .isInstanceOf(AccountingAccountAlreadyClosedException.class)
                    .hasMessageContaining("already closed since 2026-03-01");
        }
    }

    @Nested
    @DisplayName("vigencia por fecha")
    class Vigencia {

        @Test
        @DisplayName("el limite inferior de una cuenta cerrada incluye el propio validFrom")
        void limite_inferior_incluye_valid_from() {
            AccountingAccount cuenta = valido().validFrom(LocalDate.of(2026, 1, 1))
                    .validTo(LocalDate.of(2026, 6, 1)).build();

            assertThat(cuenta.isEffectiveOn(LocalDate.of(2026, 1, 1))).isTrue();
            assertThat(cuenta.isEffectiveOn(LocalDate.of(2025, 12, 31))).isFalse();
        }

        @Test
        @DisplayName("el limite superior de una cuenta cerrada es estricto: validTo ya no aplica")
        void limite_superior_es_estricto() {
            AccountingAccount cuenta = valido().validFrom(LocalDate.of(2026, 1, 1))
                    .validTo(LocalDate.of(2026, 6, 1)).build();

            assertThat(cuenta.isEffectiveOn(LocalDate.of(2026, 5, 31))).isTrue();
            assertThat(cuenta.isEffectiveOn(LocalDate.of(2026, 6, 1))).isFalse();
        }

        @Test
        @DisplayName("una cuenta abierta sigue vigente en cualquier fecha posterior a validFrom")
        void cuenta_abierta_sigue_vigente() {
            AccountingAccount cuenta = valido().validFrom(LocalDate.of(2026, 1, 1)).validTo(null)
                    .build();

            assertThat(cuenta.isEffectiveOn(LocalDate.of(2099, 1, 1))).isTrue();
            assertThat(cuenta.isOpen()).isTrue();
        }

        @Test
        @DisplayName("una cuenta cerrada deja de estar abierta")
        void cuenta_cerrada_no_esta_abierta() {
            AccountingAccount cuenta = valido().validTo(LocalDate.of(2026, 6, 1)).build();

            assertThat(cuenta.isOpen()).isFalse();
        }
    }
}
