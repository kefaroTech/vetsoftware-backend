package com.vetsoftware.app.configurator.application.usecase;

import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.ITEM_CAJA;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.ITEM_POS;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O11_SI_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q3_CUANTAS_CAJAS;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.efectoPorOpcion;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.efectoPorPregunta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.configurator.application.dto.ConfiguratorEffectDto;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorEffectRepository;
import com.vetsoftware.app.configurator.domain.ConfiguratorEffect;
import com.vetsoftware.app.configurator.domain.EffectType;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Listado paginado de efectos para la consola de plataforma. */
@ExtendWith(MockitoExtension.class)
@DisplayName("ListConfiguratorEffectsService — listado paginado de efectos")
class ListConfiguratorEffectsServiceTest {

    @Mock
    private ConfiguratorEffectRepository repository;
    @InjectMocks
    private ListConfiguratorEffectsService service;

    @Test
    @DisplayName("traduce cada efecto a DTO conservando su disparador y su cantidad")
    void traduce_cada_efecto_conservando_disparador_y_cantidad() {
        PageResult<ConfiguratorEffect> pagina = new PageResult<>(
                List.of(efectoPorOpcion(1L, O11_SI_VENDE, ITEM_POS, EffectType.SET_QUANTITY, 2),
                        efectoPorPregunta(2L, Q3_CUANTAS_CAJAS, ITEM_CAJA,
                                EffectType.QUANTITY_FROM_ANSWER, null)),
                0, 20, 2L, 1);
        when(repository.findAll(0, 20)).thenReturn(pagina);

        PageResult<ConfiguratorEffectDto> resultado = service.listAll(0, 20);

        assertThat(resultado.content())
                .extracting(ConfiguratorEffectDto::optionId, ConfiguratorEffectDto::questionId,
                        ConfiguratorEffectDto::effect, ConfiguratorEffectDto::quantity)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(O11_SI_VENDE, null,
                                EffectType.SET_QUANTITY, 2),
                        org.assertj.core.groups.Tuple.tuple(null, Q3_CUANTAS_CAJAS,
                                EffectType.QUANTITY_FROM_ANSWER, null));
        assertThat(resultado.totalElements()).isEqualTo(2L);
    }

    @Test
    @DisplayName("un cuestionario sin efectos devuelve una pagina vacia, no null")
    void sin_efectos_devuelve_pagina_vacia() {
        when(repository.findAll(0, 20)).thenReturn(PageResult.empty(0, 20));

        assertThat(service.listAll(0, 20).content()).isEmpty();
    }
}
