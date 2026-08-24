package com.vetsoftware.app.configurator.domain;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Lo que respondió el prospecto, en la única forma que la resolución necesita:
 * qué opciones marcó y qué número escribió en cada pregunta numérica.
 *
 * <p>
 * Son dos colecciones y no una porque los dos disparadores de
 * {@code configurator_effects} son excluyentes: un efecto lo dispara una opción
 * <em>o</em> una pregunta, nunca las dos
 * ({@code chk_configurator_effects_trigger}). Mezclarlas obligaría a la
 * resolución a adivinar de cuál viene cada clave.
 *
 * @param selectedOptionIds
 *            opciones marcadas; una pregunta {@code SINGLE} aporta una y una
 *            {@code MULTI} varias
 * @param numericAnswers
 *            de cada pregunta {@code NUMBER}, el número respondido
 */
public record ConfiguratorAnswers(Set<Long> selectedOptionIds, Map<Long, Integer> numericAnswers) {

    public ConfiguratorAnswers {
        // Se valida ANTES de copiar: Set.copyOf y Map.copyOf lanzan
        // NullPointerException con un null dentro, y ese mensaje no dice cuál de las
        // dos colecciones venía mal ni por qué.
        //
        // El sondeo NO puede ser opciones.contains(null): los Set inmutables de la
        // JDK —Set.of(...), Set.copyOf(...), el propio Set.of() vacío— lanzan
        // NullPointerException cuando se les pregunta por null, así que preguntar
        // producía exactamente la NPE sin diagnóstico que este bloque existe para
        // evitar, y la producía SIEMPRE: hasta ConfiguratorAnswers.empty() moría al
        // construirse. Recorrer el conjunto funciona con cualquier implementación.
        Set<Long> opciones = selectedOptionIds == null ? Set.of() : selectedOptionIds;
        Map<Long, Integer> numeros = numericAnswers == null ? Map.of() : numericAnswers;
        if (opciones.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("selectedOptionIds cannot contain null");
        }
        for (Map.Entry<Long, Integer> entrada : numeros.entrySet()) {
            if (entrada.getKey() == null || entrada.getValue() == null) {
                throw new IllegalArgumentException("numericAnswers cannot contain nulls");
            }
            if (entrada.getValue() < 0) {
                throw new IllegalArgumentException(
                        "numeric answer for question " + entrada.getKey() + " cannot be negative");
            }
        }
        selectedOptionIds = Set.copyOf(new LinkedHashSet<>(opciones));
        numericAnswers = Map.copyOf(new LinkedHashMap<>(numeros));
    }

    /** Sin ninguna respuesta todavía: el estado con el que arranca el asistente. */
    public static ConfiguratorAnswers empty() {
        return new ConfiguratorAnswers(Set.of(), Map.of());
    }
}
