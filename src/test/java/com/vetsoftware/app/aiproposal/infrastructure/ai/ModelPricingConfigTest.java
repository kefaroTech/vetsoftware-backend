package com.vetsoftware.app.aiproposal.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.vetsoftware.app.shared.ai.ModelPricing;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * El cableado de la tarifa: de dónde salen los números y qué pasa cuando no
 * cuadran.
 */
@DisplayName("ModelPricingConfig — la tarifa del modelo sale de configuración")
class ModelPricingConfigTest {

    private static final String MODELO = ModelPricing.MODELO_POR_DEFECTO;

    private final ModelPricingConfig config = new ModelPricingConfig();

    private Logger logger;

    private ListAppender<ILoggingEvent> eventos;

    @BeforeEach
    void capturarLog() {
        logger = (Logger) LoggerFactory.getLogger(ModelPricingConfig.class);
        eventos = new ListAppender<>();
        eventos.start();
        logger.addAppender(eventos);
        logger.setLevel(Level.INFO);
    }

    @AfterEach
    void soltarLog() {
        logger.detachAppender(eventos);
        logger.setLevel(null);
    }

    private ModelPricing tarifa(String entrada, String salida, String modeloTarifado,
            String modeloInvocado) {
        return config.modelPricing(new BigDecimal(entrada), new BigDecimal(salida), 3_800, 1_000,
                modeloTarifado, modeloInvocado);
    }

    @Nested
    @DisplayName("construcción")
    class Construccion {

        @Test
        @DisplayName("los defectos reproducen exactamente el coste por llamada de hoy")
        void los_defectos_reproducen_el_coste_de_hoy() {
            ModelPricing tarifa = config.modelPricing(
                    new BigDecimal(ModelPricing.DEFECTO_USD_POR_MILLON_ENTRADA),
                    new BigDecimal(ModelPricing.DEFECTO_USD_POR_MILLON_SALIDA),
                    Integer.parseInt(ModelPricing.DEFECTO_TOKENS_ESTIMADOS_ENTRADA),
                    Integer.parseInt(ModelPricing.DEFECTO_TOKENS_ESTIMADOS_SALIDA), MODELO, MODELO);

            assertThat(tarifa.usdPerCall()).isEqualByComparingTo(new BigDecimal("0.0088"));
        }

        @Test
        @DisplayName("una tarifa de otra familia se toma tal cual, sin caer al defecto")
        void una_tarifa_de_otra_familia_se_toma_tal_cual() {
            ModelPricing tarifa = tarifa("0.14", "0.28", "deepseek.v3", "deepseek.v3");

            assertThat(tarifa.usdPerCall()).isEqualByComparingTo(new BigDecimal("0.000812"));
            assertThat(tarifa.pricedModelId()).isEqualTo("deepseek.v3");
        }

        /**
         * ⛔ No se atrapa a propósito: un precio imposible tiene que tumbar el contexto
         * y verse en el primer despliegue. Caer al defecto en silencio es como se llega
         * a un tope que no significa lo que dice.
         */
        @Test
        @DisplayName("un precio de cero impide arrancar en vez de caer al defecto")
        void un_precio_de_cero_impide_arrancar() {
            assertThatThrownBy(() -> tarifa("0", "10", MODELO, MODELO))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("el acoplamiento con el modelo que se invoca")
    class ModeloYTarifa {

        /**
         * ⛔ La única red contra el fallo que este refactor no puede impedir por
         * construcción: cambiar {@code model-id} y olvidar las tarifas. Sin este aviso
         * el desajuste es exactamente igual de silencioso que antes.
         */
        @Test
        @DisplayName("avisa nombrando los dos modelos cuando las tarifas no son del que se invoca")
        void avisa_cuando_la_tarifa_no_es_del_modelo_invocado() {
            tarifa("2", "10", MODELO, "deepseek.v3");

            assertThat(eventos.list).filteredOn(evento -> evento.getLevel() == Level.WARN)
                    .singleElement().satisfies(evento -> assertThat(evento.getFormattedMessage())
                            .contains(MODELO).contains("deepseek.v3"));
        }

        @Test
        @DisplayName("cuando coinciden no avisa: un canal que grita siempre se deja de mirar")
        void no_avisa_cuando_coinciden() {
            tarifa("2", "10", MODELO, MODELO);

            assertThat(eventos.list).filteredOn(evento -> evento.getLevel() == Level.WARN)
                    .isEmpty();
        }

        @Test
        @DisplayName("deja escrito en el arranque el precio con el que se van a repartir los cupos")
        void deja_escrito_el_precio_en_el_arranque() {
            tarifa("0.14", "0.28", "deepseek.v3", "deepseek.v3");

            assertThat(eventos.list).filteredOn(evento -> evento.getLevel() == Level.INFO)
                    .singleElement().satisfies(evento -> assertThat(evento.getFormattedMessage())
                            .contains("deepseek.v3").contains("0.000812"));
        }
    }
}
