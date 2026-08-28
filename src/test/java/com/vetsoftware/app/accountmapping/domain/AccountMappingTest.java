package com.vetsoftware.app.accountmapping.domain;

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
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("AccountMapping - invariantes y ciclo de vida del agregado")
class AccountMappingTest {

    /**
     * Constructor de fixtures con un campo variable por caso. El default es BANK,
     * la clase mas simple: no admite afinado ni cuenta diferida, asi que los casos
     * de las otras invariantes no arrastran ruido de esas dos.
     */
    private static Builder valido() {
        return new Builder();
    }

    private static final class Builder {
        private Long id = 1L;
        private MappingKind mappingKind = MappingKind.BANK;
        private String mappingKey = "001";
        private Long catalogItemId;
        private String chargeType;
        private String taxTreatment;
        private String debitAccountCode = "110501";
        private String creditAccountCode = "220501";
        private String deferredAccountCode;
        private LocalDate validFrom = LocalDate.of(2026, 1, 1);
        private LocalDate validTo;
        private LocalDateTime createdDate = LocalDateTime.of(2026, 1, 1, 9, 0);
        private boolean enabled = true;
        private Long version = 2L;

        private Builder mappingKind(MappingKind v) {
            this.mappingKind = v;
            return this;
        }

        private Builder mappingKey(String v) {
            this.mappingKey = v;
            return this;
        }

        private Builder catalogItemId(Long v) {
            this.catalogItemId = v;
            return this;
        }

        private Builder chargeType(String v) {
            this.chargeType = v;
            return this;
        }

        private Builder taxTreatment(String v) {
            this.taxTreatment = v;
            return this;
        }

        private Builder debitAccountCode(String v) {
            this.debitAccountCode = v;
            return this;
        }

        private Builder creditAccountCode(String v) {
            this.creditAccountCode = v;
            return this;
        }

        private Builder deferredAccountCode(String v) {
            this.deferredAccountCode = v;
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

        private AccountMapping build() {
            return new AccountMapping(id, mappingKind, mappingKey, catalogItemId, chargeType,
                    taxTreatment, debitAccountCode, creditAccountCode, deferredAccountCode,
                    validFrom, validTo, createdDate, enabled, version);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            AccountMapping mapping = valido().build();

            assertThat(mapping.getId()).isEqualTo(1L);
            assertThat(mapping.getMappingKind()).isEqualTo(MappingKind.BANK);
            assertThat(mapping.getMappingKey()).isEqualTo("001");
            assertThat(mapping.getCatalogItemId()).isNull();
            assertThat(mapping.getChargeType()).isNull();
            assertThat(mapping.getTaxTreatment()).isNull();
            assertThat(mapping.getDebitAccountCode()).isEqualTo("110501");
            assertThat(mapping.getCreditAccountCode()).isEqualTo("220501");
            assertThat(mapping.getDeferredAccountCode()).isNull();
            assertThat(mapping.getValidFrom()).isEqualTo(LocalDate.of(2026, 1, 1));
            assertThat(mapping.getValidTo()).isNull();
            assertThat(mapping.getCreatedDate()).isEqualTo(LocalDateTime.of(2026, 1, 1, 9, 0));
            assertThat(mapping.isEnabled()).isTrue();
            assertThat(mapping.getVersion()).isEqualTo(2L);
        }

        @Test
        @DisplayName("create() nace sin id, habilitado y sin version")
        void create_nace_sin_id_habilitado_y_sin_version() {
            AccountMapping mapping = AccountMapping.create(MappingKind.BANK, "001", null, null,
                    null, "110501", "220501", null, LocalDate.of(2026, 1, 1), null,
                    LocalDateTime.of(2026, 1, 1, 9, 0));

            assertThat(mapping.getId()).isNull();
            assertThat(mapping.isEnabled()).isTrue();
            assertThat(mapping.getVersion()).isNull();
            assertThat(mapping.getCreatedDate()).isEqualTo(LocalDateTime.of(2026, 1, 1, 9, 0));
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("mappingKind null",
                            (ThrowingCallable) () -> valido().mappingKind(null).build(),
                            "mappingKind is required"),
                    arguments("mappingKey null",
                            (ThrowingCallable) () -> valido().mappingKey(null).build(),
                            "mappingKey is required"),
                    arguments("mappingKey en blanco",
                            (ThrowingCallable) () -> valido().mappingKey("   ").build(),
                            "mappingKey is required"),
                    arguments("mappingKey de 61 chars",
                            (ThrowingCallable) () -> valido().mappingKey("x".repeat(61)).build(),
                            "mappingKey must be 60 chars or less"),
                    arguments("chargeType en blanco (no nulo)",
                            (ThrowingCallable) () -> valido().chargeType("").build(),
                            "chargeType must be 1 to 20 chars when present"),
                    arguments("chargeType de 21 chars",
                            (ThrowingCallable) () -> valido().chargeType("x".repeat(21)).build(),
                            "chargeType must be 1 to 20 chars when present"),
                    arguments("taxTreatment en blanco (no nulo)",
                            (ThrowingCallable) () -> valido().taxTreatment("").build(),
                            "taxTreatment must be 1 to 20 chars when present"),
                    arguments("taxTreatment de 21 chars",
                            (ThrowingCallable) () -> valido().taxTreatment("x".repeat(21)).build(),
                            "taxTreatment must be 1 to 20 chars when present"),
                    arguments("BANK con catalogItemId",
                            (ThrowingCallable) () -> valido().catalogItemId(5L).build(),
                            "only allowed for REVENUE and DEFERRED_REVENUE"),
                    arguments("BANK con chargeType",
                            (ThrowingCallable) () -> valido().chargeType("X").build(),
                            "only allowed for REVENUE and DEFERRED_REVENUE"),
                    arguments("BANK con taxTreatment",
                            (ThrowingCallable) () -> valido().taxTreatment("GRAVADO").build(),
                            "only allowed for REVENUE and DEFERRED_REVENUE"),
                    arguments("debitAccountCode null",
                            (ThrowingCallable) () -> valido().debitAccountCode(null).build(),
                            "debitAccountCode is required"),
                    arguments("debitAccountCode en blanco",
                            (ThrowingCallable) () -> valido().debitAccountCode("   ").build(),
                            "debitAccountCode is required"),
                    arguments("debitAccountCode de 11 chars",
                            (ThrowingCallable) () -> valido().debitAccountCode("x".repeat(11))
                                    .build(),
                            "debitAccountCode must be 10 chars or less"),
                    arguments("creditAccountCode null",
                            (ThrowingCallable) () -> valido().creditAccountCode(null).build(),
                            "creditAccountCode is required"),
                    arguments("creditAccountCode de 11 chars",
                            (ThrowingCallable) () -> valido().creditAccountCode("x".repeat(11))
                                    .build(),
                            "creditAccountCode must be 10 chars or less"),
                    arguments("deferredAccountCode en blanco (no nulo)",
                            (ThrowingCallable) () -> valido().deferredAccountCode("   ").build(),
                            "deferredAccountCode must not be blank when present"),
                    arguments("deferredAccountCode de 11 chars",
                            (ThrowingCallable) () -> valido().deferredAccountCode("x".repeat(11))
                                    .build(),
                            "deferredAccountCode must be 10 chars or less"),
                    arguments("BANK con deferredAccountCode valido",
                            (ThrowingCallable) () -> valido().deferredAccountCode("240501").build(),
                            "deferredAccountCode is only allowed for REVENUE and DEFERRED_REVENUE"),
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

        @ParameterizedTest
        @EnumSource(value = MappingKind.class, mode = EnumSource.Mode.EXCLUDE, names = {"REVENUE",
                "DEFERRED_REVENUE"})
        @DisplayName("ninguna clase salvo REVENUE y DEFERRED_REVENUE admite catalogItemId")
        void ninguna_clase_no_refinable_admite_catalog_item_id(MappingKind kind) {
            assertThatThrownBy(() -> valido().mappingKind(kind).catalogItemId(5L).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("only allowed for REVENUE and DEFERRED_REVENUE");
        }

        @ParameterizedTest
        @EnumSource(value = MappingKind.class, names = {"REVENUE", "DEFERRED_REVENUE"})
        @DisplayName("REVENUE y DEFERRED_REVENUE admiten articulo, cargo, tratamiento y diferido")
        void revenue_y_deferred_revenue_admiten_el_afinado_completo(MappingKind kind) {
            assertThatCode(() -> valido().mappingKind(kind).catalogItemId(5L).chargeType("CONSULTA")
                    .taxTreatment("GRAVADO").deferredAccountCode("240501").build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("mappingKey de exactamente 60 chars se acepta")
        void mapping_key_de_60_chars_se_acepta() {
            assertThatCode(() -> valido().mappingKey("x".repeat(60)).build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("debitAccountCode y creditAccountCode de exactamente 10 chars se aceptan")
        void codigos_de_10_chars_se_aceptan() {
            assertThatCode(() -> valido().debitAccountCode("x".repeat(10))
                    .creditAccountCode("x".repeat(10)).build()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deferredAccountCode nulo es valido incluso para una clase refinable")
        void deferred_account_code_nulo_es_valido_en_clase_refinable() {
            assertThatCode(() -> valido().mappingKind(MappingKind.REVENUE).deferredAccountCode(null)
                    .build()).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("claves con centinela para las columnas generadas")
    class ClavesDerivadas {

        @Test
        @DisplayName("catalogItemKey devuelve el id cuando el mapeo tiene articulo")
        void catalog_item_key_devuelve_el_id() {
            AccountMapping mapping = valido().mappingKind(MappingKind.REVENUE).catalogItemId(77L)
                    .build();

            assertThat(mapping.catalogItemKey()).isEqualTo(77L);
        }

        @Test
        @DisplayName("catalogItemKey devuelve el centinela 0 cuando no hay articulo")
        void catalog_item_key_devuelve_el_centinela() {
            AccountMapping mapping = valido().build();

            assertThat(mapping.catalogItemKey()).isEqualTo(AccountMapping.NO_CATALOG_ITEM_KEY);
        }

        @Test
        @DisplayName("chargeTypeKey devuelve el valor cuando existe")
        void charge_type_key_devuelve_el_valor() {
            AccountMapping mapping = valido().mappingKind(MappingKind.REVENUE)
                    .chargeType("CONSULTA").build();

            assertThat(mapping.chargeTypeKey()).isEqualTo("CONSULTA");
        }

        @Test
        @DisplayName("chargeTypeKey devuelve el centinela '-' cuando es nulo")
        void charge_type_key_devuelve_el_centinela() {
            AccountMapping mapping = valido().build();

            assertThat(mapping.chargeTypeKey()).isEqualTo(AccountMapping.NO_REFINEMENT_KEY);
        }

        @Test
        @DisplayName("taxTreatmentKey devuelve el valor cuando existe")
        void tax_treatment_key_devuelve_el_valor() {
            AccountMapping mapping = valido().mappingKind(MappingKind.REVENUE)
                    .taxTreatment("GRAVADO").build();

            assertThat(mapping.taxTreatmentKey()).isEqualTo("GRAVADO");
        }

        @Test
        @DisplayName("taxTreatmentKey devuelve el centinela '-' cuando es nulo")
        void tax_treatment_key_devuelve_el_centinela() {
            AccountMapping mapping = valido().build();

            assertThat(mapping.taxTreatmentKey()).isEqualTo(AccountMapping.NO_REFINEMENT_KEY);
        }
    }

    @Nested
    @DisplayName("cierre de vigencia")
    class Cierre {

        @Test
        @DisplayName("close pone la fecha de fin y conserva la version para el bloqueo optimista")
        void close_pone_fecha_fin_y_conserva_version() {
            AccountMapping abierto = valido().validTo(null).build();

            AccountMapping cerrado = abierto.close(LocalDate.of(2026, 6, 1));

            assertThat(cerrado.getValidTo()).isEqualTo(LocalDate.of(2026, 6, 1));
            assertThat(cerrado.isOpen()).isFalse();
            assertThat(cerrado.getVersion()).isEqualTo(abierto.getVersion());
            assertThat(cerrado.getMappingKey()).isEqualTo(abierto.getMappingKey());
        }

        @Test
        @DisplayName("cerrar un mapeo ya cerrado lanza y no admite un segundo cierre silencioso")
        void cerrar_un_mapeo_ya_cerrado_lanza() {
            AccountMapping cerrado = valido().validTo(LocalDate.of(2026, 3, 1)).build();

            assertThatThrownBy(() -> cerrado.close(LocalDate.of(2026, 6, 1)))
                    .isInstanceOf(AccountMappingAlreadyClosedException.class)
                    .hasMessageContaining("already closed since 2026-03-01");
        }
    }

    @Nested
    @DisplayName("vigencia por fecha")
    class Vigencia {

        @Test
        @DisplayName("el limite inferior de un mapeo cerrado incluye el propio validFrom")
        void limite_inferior_incluye_valid_from() {
            AccountMapping mapping = valido().validFrom(LocalDate.of(2026, 1, 1))
                    .validTo(LocalDate.of(2026, 6, 1)).build();

            assertThat(mapping.isEffectiveOn(LocalDate.of(2026, 1, 1))).isTrue();
            assertThat(mapping.isEffectiveOn(LocalDate.of(2025, 12, 31))).isFalse();
        }

        @Test
        @DisplayName("el limite superior de un mapeo cerrado es estricto: validTo ya no aplica")
        void limite_superior_es_estricto() {
            AccountMapping mapping = valido().validFrom(LocalDate.of(2026, 1, 1))
                    .validTo(LocalDate.of(2026, 6, 1)).build();

            assertThat(mapping.isEffectiveOn(LocalDate.of(2026, 5, 31))).isTrue();
            assertThat(mapping.isEffectiveOn(LocalDate.of(2026, 6, 1))).isFalse();
        }

        @Test
        @DisplayName("un mapeo abierto sigue vigente en cualquier fecha posterior a validFrom")
        void mapeo_abierto_sigue_vigente() {
            AccountMapping mapping = valido().validFrom(LocalDate.of(2026, 1, 1)).validTo(null)
                    .build();

            assertThat(mapping.isEffectiveOn(LocalDate.of(2099, 1, 1))).isTrue();
            assertThat(mapping.isOpen()).isTrue();
        }

        @Test
        @DisplayName("un mapeo cerrado deja de estar abierto")
        void mapeo_cerrado_no_esta_abierto() {
            AccountMapping mapping = valido().validTo(LocalDate.of(2026, 6, 1)).build();

            assertThat(mapping.isOpen()).isFalse();
        }
    }
}
