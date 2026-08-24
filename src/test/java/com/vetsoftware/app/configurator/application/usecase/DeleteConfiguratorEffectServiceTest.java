package com.vetsoftware.app.configurator.application.usecase;

import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.ITEM_POS;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O11_SI_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.efectoPorOpcion;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.configurator.application.port.out.ConfiguratorEffectRepository;
import com.vetsoftware.app.configurator.domain.ConfiguratorEffectNotFoundException;
import com.vetsoftware.app.configurator.domain.EffectType;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Baja lógica de un efecto: nada cuelga de él, así que no hay hijos que mirar.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteConfiguratorEffectService — baja de un efecto")
class DeleteConfiguratorEffectServiceTest {

    @Mock
    private ConfiguratorEffectRepository repository;
    @InjectMocks
    private DeleteConfiguratorEffectService service;

    @Test
    @DisplayName("borra el efecto que existe")
    void borra_el_efecto_que_existe() {
        when(repository.findById(1L)).thenReturn(
                Optional.of(efectoPorOpcion(1L, O11_SI_VENDE, ITEM_POS, EffectType.ADD, null)));

        service.execute(1L);

        verify(repository).delete(1L);
    }

    @Test
    @DisplayName("un efecto inexistente se rechaza y no borra nada")
    void un_efecto_inexistente_no_borra() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(99L))
                .isInstanceOf(ConfiguratorEffectNotFoundException.class)
                .hasMessageContaining("ConfiguratorEffect not found: 99");

        verify(repository, never()).delete(any());
    }
}
