package com.vetsoftware.app.configurator.application.dto;

import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.CREADA_EL;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.ITEM_CAJA;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.ITEM_POS;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O11_SI_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q3_CUANTAS_CAJAS;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.efectoDeshabilitado;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.efectoPorOpcion;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.efectoPorPregunta;
import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.configurator.domain.EffectType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El DTO conserva el disparador tal cual: perder cuál de los dos venía relleno
 * convertiría un efecto numérico en uno de opción al volver a guardarlo desde
 * la consola.
 */
@DisplayName("ConfiguratorEffectDto — proyeccion de un efecto")
class ConfiguratorEffectDtoTest {

    @Test
    @DisplayName("copia campo por campo un efecto disparado por opcion")
    void copia_campo_por_campo_un_efecto_por_opcion() {
        ConfiguratorEffectDto dto = ConfiguratorEffectDto
                .from(efectoPorOpcion(5L, O11_SI_VENDE, ITEM_POS, EffectType.SET_QUANTITY, 3));

        assertThat(dto.id()).isEqualTo(5L);
        assertThat(dto.optionId()).isEqualTo(O11_SI_VENDE);
        assertThat(dto.questionId()).isNull();
        assertThat(dto.catalogItemId()).isEqualTo(ITEM_POS);
        assertThat(dto.effect()).isEqualTo(EffectType.SET_QUANTITY);
        assertThat(dto.quantity()).isEqualTo(3);
        assertThat(dto.createdDate()).isEqualTo(CREADA_EL);
        assertThat(dto.enabled()).isTrue();
        // La prioridad se publica a proposito: es el dato con el que la pantalla
        // de reordenado pinta el orden actual y manda el siguiente.
        assertThat(dto.priority()).isZero();
    }

    @Test
    @DisplayName("copia campo por campo un efecto disparado por pregunta, con cantidad nula")
    void copia_campo_por_campo_un_efecto_por_pregunta() {
        ConfiguratorEffectDto dto = ConfiguratorEffectDto.from(efectoPorPregunta(6L,
                Q3_CUANTAS_CAJAS, ITEM_CAJA, EffectType.QUANTITY_FROM_ANSWER, null));

        assertThat(dto.optionId()).isNull();
        assertThat(dto.questionId()).isEqualTo(Q3_CUANTAS_CAJAS);
        assertThat(dto.quantity()).isNull();
        assertThat(dto.effect()).isEqualTo(EffectType.QUANTITY_FROM_ANSWER);
    }

    @Test
    @DisplayName("traslada la prioridad tal cual, que es el orden de aplicacion")
    void traslada_la_prioridad_tal_cual() {
        assertThat(ConfiguratorEffectDto
                .from(efectoPorOpcion(8L, O11_SI_VENDE, ITEM_POS, EffectType.ADD, null, 40))
                .priority()).isEqualTo(40);
    }

    @Test
    @DisplayName("no se inventa que un efecto dado de baja sigue activo")
    void no_se_inventa_que_un_efecto_de_baja_sigue_activo() {
        assertThat(ConfiguratorEffectDto
                .from(efectoDeshabilitado(7L, O11_SI_VENDE, ITEM_POS, EffectType.ADD)).enabled())
                .isFalse();
    }
}
