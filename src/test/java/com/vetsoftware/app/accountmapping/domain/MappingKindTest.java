package com.vetsoftware.app.accountmapping.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("MappingKind - que clases admiten afinado")
class MappingKindTest {

    @ParameterizedTest
    @EnumSource(value = MappingKind.class, names = {"REVENUE", "DEFERRED_REVENUE"})
    @DisplayName("REVENUE y DEFERRED_REVENUE, unicas que vienen de algo vendido, admiten afinado")
    void revenue_y_deferred_revenue_admiten_afinado(MappingKind kind) {
        assertThat(kind.acceptsRefinement()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = MappingKind.class, mode = EnumSource.Mode.EXCLUDE, names = {"REVENUE",
            "DEFERRED_REVENUE"})
    @DisplayName("ninguna otra clase admite afinado por articulo, cargo o tratamiento fiscal")
    void ninguna_otra_clase_admite_afinado(MappingKind kind) {
        assertThat(kind.acceptsRefinement()).isFalse();
    }
}
