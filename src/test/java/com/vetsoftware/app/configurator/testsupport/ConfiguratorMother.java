package com.vetsoftware.app.configurator.testsupport;

import com.vetsoftware.app.configurator.domain.AnswerType;
import com.vetsoftware.app.configurator.domain.ConfiguratorAnswers;
import com.vetsoftware.app.configurator.domain.ConfiguratorEffect;
import com.vetsoftware.app.configurator.domain.ConfiguratorOption;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestion;
import com.vetsoftware.app.configurator.domain.EffectType;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * Object mother de {@code configurator}. No se comparte con otras features.
 *
 * <p>
 * El cuestionario de referencia es el mismo de
 * {@code ConfiguratorAnswerCoherenceTest}, con dos niveles de condición, para
 * que las pruebas de resolución y las de coherencia hablen del mismo árbol:
 *
 * <pre>
 * Q1 SELLS_PRODUCTS (SINGLE, obligatoria, raiz)
 *    O11 YES  ──▶ Q2 HAS_COUNTER (SINGLE, condicional de O11)
 *    O12 NO             O21 YES ──▶ Q3 HOW_MANY_BOXES (NUMBER, condicional de O21)
 *                       O22 NO
 * </pre>
 */
public final class ConfiguratorMother {

    public static final LocalDateTime CREADA_EL = LocalDateTime.of(2026, 8, 22, 10, 0);

    public static final Long Q1_VENDE = 1L;
    public static final Long Q2_MOSTRADOR = 2L;
    public static final Long Q3_CUANTAS_CAJAS = 3L;
    public static final Long O11_SI_VENDE = 11L;
    public static final Long O12_NO_VENDE = 12L;
    public static final Long O21_SI_MOSTRADOR = 21L;
    public static final Long O22_NO_MOSTRADOR = 22L;

    /** Artículos del catálogo que usan los efectos de estas pruebas. */
    public static final Long ITEM_POS = 100L;
    public static final Long ITEM_CAJA = 200L;
    public static final Long ITEM_BASE = 300L;

    private ConfiguratorMother() {
    }

    // --- preguntas -------------------------------------------------------

    /** Pregunta persistida: con id y versión. */
    public static ConfiguratorQuestion pregunta(Long id, String code, AnswerType tipo,
            Long parentOptionId, boolean required) {
        return new ConfiguratorQuestion(id, code, "¿" + code + "?", null, tipo, parentOptionId,
                required, 0, CREADA_EL, 0L, true);
    }

    /** La raíz del cuestionario de referencia. */
    public static ConfiguratorQuestion preguntaRaiz() {
        return pregunta(Q1_VENDE, "SELLS_PRODUCTS", AnswerType.SINGLE, null, true);
    }

    /** La pregunta numérica de segundo nivel del cuestionario de referencia. */
    public static ConfiguratorQuestion preguntaNumerica() {
        return pregunta(Q3_CUANTAS_CAJAS, "HOW_MANY_BOXES", AnswerType.NUMBER, O21_SI_MOSTRADOR,
                false);
    }

    public static ConfiguratorQuestion preguntaNueva() {
        return ConfiguratorQuestion.create("SELLS_PRODUCTS", "¿Vende productos?", null,
                AnswerType.SINGLE, null, true, 0, java.time.Clock.fixed(
                        CREADA_EL.toInstant(java.time.ZoneOffset.UTC), java.time.ZoneOffset.UTC));
    }

    // --- opciones --------------------------------------------------------

    public static ConfiguratorOption opcion(Long id, Long questionId, String code) {
        return new ConfiguratorOption(id, questionId, code, code, null, 0, CREADA_EL, 0L, true);
    }

    // --- efectos ---------------------------------------------------------

    /** Efecto disparado por una opción marcada. */
    public static ConfiguratorEffect efectoPorOpcion(Long id, Long optionId, Long catalogItemId,
            EffectType tipo, Integer quantity) {
        return new ConfiguratorEffect(id, optionId, null, catalogItemId, tipo, quantity, CREADA_EL,
                0L, true);
    }

    /** Efecto disparado por el número respondido a una pregunta. */
    public static ConfiguratorEffect efectoPorPregunta(Long id, Long questionId, Long catalogItemId,
            EffectType tipo, Integer quantity) {
        return new ConfiguratorEffect(id, null, questionId, catalogItemId, tipo, quantity,
                CREADA_EL, 0L, true);
    }

    /** El mismo efecto, dado de baja: no debe disparar nunca. */
    public static ConfiguratorEffect efectoDeshabilitado(Long id, Long optionId, Long catalogItemId,
            EffectType tipo) {
        return new ConfiguratorEffect(id, optionId, null, catalogItemId, tipo, null, CREADA_EL, 0L,
                false);
    }

    // --- respuestas ------------------------------------------------------

    public static ConfiguratorAnswers respuestas(Set<Long> opciones, Map<Long, Integer> numeros) {
        return new ConfiguratorAnswers(opciones, numeros);
    }

    /** Solo opciones marcadas, sin ninguna respuesta numérica. */
    public static ConfiguratorAnswers marcadas(Long... opciones) {
        return new ConfiguratorAnswers(Set.of(opciones), Map.of());
    }
}
