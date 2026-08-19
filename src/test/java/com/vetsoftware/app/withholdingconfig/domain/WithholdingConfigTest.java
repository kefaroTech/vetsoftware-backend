package com.vetsoftware.app.withholdingconfig.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("WithholdingConfig — invariantes de dominio")
class WithholdingConfigTest {

    private static final CompanyRef COMPANY = new CompanyRef(1L, "Veterinaria Central",
            "900123456-1");
    private static final BigDecimal RATE = new BigDecimal("2.5");

    private static WithholdingConfig configConTasas(BigDecimal reteFuente, BigDecimal reteIva,
            BigDecimal reteIca) {
        return new WithholdingConfig(10L, COMPANY, reteFuente, reteIva, reteIca,
                LocalDateTime.now(), null, true);
    }

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("create() arranca habilitado, sin id y con fecha de creacion")
        void create_arranca_habilitado_sin_id() {
            WithholdingConfig config = WithholdingConfig.create(COMPANY, RATE, RATE, RATE);

            assertThat(config.getId()).isNull();
            assertThat(config.isEnabled()).isTrue();
            assertThat(config.getCreatedDate()).isNotNull();
            assertThat(config.getCompany()).isEqualTo(COMPANY);
        }

        @Test
        @DisplayName("las tasas en cero son validas")
        void las_tasas_en_cero_son_validas() {
            WithholdingConfig config = configConTasas(BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO);

            assertThat(config.getReteFuenteRate()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(config.getReteIvaRate()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(config.getReteIcaRate()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("company nulo se rechaza")
        void company_nulo_se_rechaza() {
            assertThatThrownBy(() -> new WithholdingConfig(10L, null, RATE, RATE, RATE,
                    LocalDateTime.now(), null, true)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company is required");
        }

        @ParameterizedTest(name = "reteFuenteRate=[{0}]")
        @NullSource
        @ValueSource(strings = {"-0.01", "-5"})
        @DisplayName("reteFuenteRate nulo o negativo se rechaza")
        void rete_fuente_rate_invalida_se_rechaza(String raw) {
            BigDecimal invalido = raw == null ? null : new BigDecimal(raw);

            assertThatThrownBy(() -> configConTasas(invalido, RATE, RATE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reteFuenteRate");
        }

        @ParameterizedTest(name = "reteIvaRate=[{0}]")
        @NullSource
        @ValueSource(strings = {"-0.01", "-5"})
        @DisplayName("reteIvaRate nulo o negativo se rechaza")
        void rete_iva_rate_invalida_se_rechaza(String raw) {
            BigDecimal invalido = raw == null ? null : new BigDecimal(raw);

            assertThatThrownBy(() -> configConTasas(RATE, invalido, RATE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reteIvaRate");
        }

        @ParameterizedTest(name = "reteIcaRate=[{0}]")
        @NullSource
        @ValueSource(strings = {"-0.01", "-5"})
        @DisplayName("reteIcaRate nulo o negativo se rechaza")
        void rete_ica_rate_invalida_se_rechaza(String raw) {
            BigDecimal invalido = raw == null ? null : new BigDecimal(raw);

            assertThatThrownBy(() -> configConTasas(RATE, RATE, invalido))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reteIcaRate");
        }
    }

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("update() cambia las tres tasas sin tocar la company")
        void update_cambia_las_tasas_sin_tocar_la_company() {
            WithholdingConfig config = configConTasas(RATE, RATE, RATE);
            BigDecimal nueva = new BigDecimal("4.0");

            config.update(nueva, nueva, nueva);

            assertThat(config.getReteFuenteRate()).isEqualByComparingTo(nueva);
            assertThat(config.getReteIvaRate()).isEqualByComparingTo(nueva);
            assertThat(config.getReteIcaRate()).isEqualByComparingTo(nueva);
            assertThat(config.getCompany()).isEqualTo(COMPANY);
        }

        @Test
        @DisplayName("update() revalida: una tasa negativa se rechaza")
        void update_revalida_una_tasa_negativa() {
            WithholdingConfig config = configConTasas(RATE, RATE, RATE);

            assertThatThrownBy(() -> config.update(new BigDecimal("-1"), RATE, RATE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be negative");
        }
    }

    @Nested
    @DisplayName("activacion")
    class Activacion {

        @Test
        @DisplayName("disable() y enable() cambian el flag")
        void disable_y_enable_cambian_el_flag() {
            WithholdingConfig config = configConTasas(RATE, RATE, RATE);

            config.disable();
            assertThat(config.isEnabled()).isFalse();

            config.enable();
            assertThat(config.isEnabled()).isTrue();
        }
    }
}
