package com.vetsoftware.app.platformbillingconfig.domain;

import static com.vetsoftware.app.platformbillingconfig.testsupport.PlatformBillingConfigMother.CREADA;
import static com.vetsoftware.app.platformbillingconfig.testsupport.PlatformBillingConfigMother.TARIFA;
import static com.vetsoftware.app.platformbillingconfig.testsupport.PlatformBillingConfigMother.configurada;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("PlatformBillingConfig — políticas de facturación de la plataforma")
class PlatformBillingConfigTest {

    @Nested
    @DisplayName("Reconstitución")
    class Reconstitucion {

        @Test
        @DisplayName("conserva cada política tal como venía de la base")
        void conserva_cada_politica_tal_como_venia_de_la_base() {
            PlatformBillingConfig config = configurada();

            assertThat(config.getId()).isEqualTo(1L);
            assertThat(config.getDefaultPriceList()).isEqualTo(TARIFA);
            assertThat(config.getDefaultGraceDays()).isEqualTo(5);
            assertThat(config.getDefaultTrialDays()).isEqualTo(14);
            assertThat(config.getInvoiceDayOfMonth()).isEqualTo(1);
            assertThat(config.getDefaultPaymentTermDays()).isEqualTo(5);
            assertThat(config.getExternalBillingProvider()).isEqualTo("SIIGO");
            assertThat(config.getCreatedDate()).isEqualTo(CREADA);
            assertThat(config.getVersion()).isZero();
        }

        @Test
        @DisplayName("admite quedarse sin tarifa por defecto: la columna es nulable")
        void admite_quedarse_sin_tarifa_por_defecto() {
            assertThatCode(() -> new PlatformBillingConfig(1L, null, 5, 14, 1, 5, null, CREADA, 0L))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Singleton")
    class Singleton {

        @Test
        @DisplayName("no expone ninguna factory de alta: la fila la siembra el esquema")
        void no_expone_ninguna_factory_de_alta() {
            assertThat(PlatformBillingConfig.class.getDeclaredMethods()).extracting(Method::getName)
                    .doesNotContain("create", "of", "nueva", "newInstance");
        }

        @Test
        @DisplayName("no expone borrado lógico: la tabla no tiene enabled (choque C5)")
        void no_expone_borrado_logico() {
            assertThat(PlatformBillingConfig.class.getDeclaredMethods()).extracting(Method::getName)
                    .doesNotContain("enable", "disable", "isEnabled", "delete");
        }

        @Test
        @DisplayName("no modela ningún corte total de acceso: el máximo es solo lectura")
        void no_modela_ningun_corte_total_de_acceso() {
            assertThat(PlatformBillingConfig.class.getDeclaredFields())
                    .extracting(java.lang.reflect.Field::getName)
                    .noneMatch(name -> name.toLowerCase().contains("suspend")
                            || name.toLowerCase().contains("block")
                            || name.toLowerCase().contains("cutoff")
                            || name.toLowerCase().contains("lockout"));
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("rechaza días de gracia nulos")
        void rechaza_dias_de_gracia_nulos() {
            assertThatThrownBy(
                    () -> new PlatformBillingConfig(1L, null, null, 14, 1, 5, null, CREADA, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("defaultGraceDays is required");
        }

        @Test
        @DisplayName("rechaza días de gracia negativos")
        void rechaza_dias_de_gracia_negativos() {
            assertThatThrownBy(
                    () -> new PlatformBillingConfig(1L, null, -1, 14, 1, 5, null, CREADA, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("defaultGraceDays cannot be negative");
        }

        @Test
        @DisplayName("rechaza días de prueba negativos")
        void rechaza_dias_de_prueba_negativos() {
            assertThatThrownBy(
                    () -> new PlatformBillingConfig(1L, null, 5, -1, 1, 5, null, CREADA, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("defaultTrialDays cannot be negative");
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 29, 30, 31})
        @DisplayName("rechaza un día de emisión fuera de 1–28: 29, 30 y 31 no existen todos los meses")
        void rechaza_un_dia_de_emision_fuera_de_rango(int dia) {
            assertThatThrownBy(
                    () -> new PlatformBillingConfig(1L, null, 5, 14, dia, 5, null, CREADA, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("invoiceDayOfMonth must be between 1 and 28");
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 15, 28})
        @DisplayName("acepta los días de emisión que existen en todos los meses")
        void acepta_los_dias_de_emision_que_existen_en_todos_los_meses(int dia) {
            assertThatCode(
                    () -> new PlatformBillingConfig(1L, null, 5, 14, dia, 5, null, CREADA, 0L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("acepta plazo de pago cero: significa pago inmediato")
        void acepta_plazo_de_pago_cero() {
            PlatformBillingConfig config = new PlatformBillingConfig(1L, null, 5, 14, 1, 0, null,
                    CREADA, 0L);

            assertThat(config.getDefaultPaymentTermDays()).isZero();
        }

        @Test
        @DisplayName("rechaza plazo de pago negativo: vencería antes de emitirse")
        void rechaza_plazo_de_pago_negativo() {
            assertThatThrownBy(
                    () -> new PlatformBillingConfig(1L, null, 5, 14, 1, -1, null, CREADA, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("defaultPaymentTermDays cannot be negative");
        }

        @Test
        @DisplayName("rechaza un proveedor externo en blanco: o hay uno, o es null")
        void rechaza_un_proveedor_externo_en_blanco() {
            assertThatThrownBy(
                    () -> new PlatformBillingConfig(1L, null, 5, 14, 1, 5, "   ", CREADA, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("externalBillingProvider must be null");
        }

        @Test
        @DisplayName("rechaza un proveedor externo de más de 40 caracteres")
        void rechaza_un_proveedor_externo_demasiado_largo() {
            assertThatThrownBy(() -> new PlatformBillingConfig(1L, null, 5, 14, 1, 5,
                    "X".repeat(41), CREADA, 0L)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("externalBillingProvider must be 40 chars");
        }

        @Test
        @DisplayName("rechaza fecha de creación nula")
        void rechaza_fecha_de_creacion_nula() {
            assertThatThrownBy(
                    () -> new PlatformBillingConfig(1L, null, 5, 14, 1, 5, null, null, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("createdDate is required");
        }
    }

    @Nested
    @DisplayName("Actualización")
    class Actualizacion {

        @Test
        @DisplayName("reemplaza las políticas en bloque")
        void reemplaza_las_politicas_en_bloque() {
            PlatformBillingConfig config = configurada();

            config.update(null, 10, 30, 15, 0, "ALEGRA");

            assertThat(config.getDefaultPriceList()).isNull();
            assertThat(config.getDefaultGraceDays()).isEqualTo(10);
            assertThat(config.getDefaultTrialDays()).isEqualTo(30);
            assertThat(config.getInvoiceDayOfMonth()).isEqualTo(15);
            assertThat(config.getDefaultPaymentTermDays()).isZero();
            assertThat(config.getExternalBillingProvider()).isEqualTo("ALEGRA");
        }

        @Test
        @DisplayName("no toca el id ni la fecha de creación")
        void no_toca_el_id_ni_la_fecha_de_creacion() {
            PlatformBillingConfig config = configurada();

            config.update(TARIFA, 10, 30, 15, 0, "ALEGRA");

            assertThat(config.getId()).isEqualTo(1L);
            assertThat(config.getCreatedDate()).isEqualTo(CREADA);
        }

        @Test
        @DisplayName("valida con las mismas reglas que la reconstitución y deja el estado intacto")
        void valida_con_las_mismas_reglas_y_deja_el_estado_intacto() {
            PlatformBillingConfig config = configurada();

            assertThatThrownBy(() -> config.update(TARIFA, 5, 14, 31, 5, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("invoiceDayOfMonth must be between 1 and 28");

            assertThat(config.getInvoiceDayOfMonth()).isEqualTo(1);
        }
    }
}
