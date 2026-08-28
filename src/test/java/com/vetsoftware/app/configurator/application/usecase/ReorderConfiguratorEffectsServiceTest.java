package com.vetsoftware.app.configurator.application.usecase;

import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.ITEM_CAJA;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.ITEM_POS;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O11_SI_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O21_SI_MOSTRADOR;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.efectoPorOpcion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.configurator.application.command.ReorderConfiguratorEffectsCommand;
import com.vetsoftware.app.configurator.application.command.ReorderConfiguratorEffectsCommand.EffectPriority;
import com.vetsoftware.app.configurator.application.dto.ConfiguratorEffectDto;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorEffectRepository;
import com.vetsoftware.app.configurator.domain.ConfiguratorEffect;
import com.vetsoftware.app.configurator.domain.ConfiguratorEffectNotFoundException;
import com.vetsoftware.app.configurator.domain.EffectType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El caso de uso que hacía falta para poder arreglar el orden sin romperlo.
 *
 * <p>
 * Hasta hoy la única forma de mover un efecto de sitio era borrarlo y volver a
 * crearlo, lo que le cambia el {@code id} —y con él el desempate— y reordena de
 * paso todo lo demás: la herramienta para arreglar el orden era la que volvía a
 * romperlo.
 *
 * <p>
 * Lo que estos casos defienden no es que se guarde, sino que <strong>no se
 * guarde nada cuando el reparto no es aplicable entero</strong>. Un reparto a
 * medias es peor que no haber reordenado: deja el {@code REMOVE} movido y el
 * {@code ADD} en su sitio viejo, que es justo la combinación que produce el
 * carrito equivocado.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReorderConfiguratorEffectsService — reparto de prioridades")
class ReorderConfiguratorEffectsServiceTest {

    @Mock
    private ConfiguratorEffectRepository repository;

    @InjectMocks
    private ReorderConfiguratorEffectsService service;

    private static ReorderConfiguratorEffectsCommand reparto(EffectPriority... pares) {
        return new ReorderConfiguratorEffectsCommand(List.of(pares));
    }

    @Nested
    @DisplayName("Reparto")
    class Reparto {

        @Test
        @DisplayName("mueve cada efecto a la prioridad que le toca y guarda los dos")
        void mueve_cada_efecto_a_su_prioridad() {
            ConfiguratorEffect add = efectoPorOpcion(1L, O11_SI_VENDE, ITEM_POS, EffectType.ADD,
                    null, 0);
            ConfiguratorEffect remove = efectoPorOpcion(2L, O21_SI_MOSTRADOR, ITEM_POS,
                    EffectType.REMOVE, null, 0);
            when(repository.findAllByIds(anyList())).thenReturn(List.of(add, remove));
            when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

            service.execute(reparto(new EffectPriority(1L, 20), new EffectPriority(2L, 10)));

            ArgumentCaptor<ConfiguratorEffect> guardados = ArgumentCaptor
                    .forClass(ConfiguratorEffect.class);
            verify(repository, times(2)).save(guardados.capture());
            assertThat(guardados.getAllValues())
                    .extracting(ConfiguratorEffect::getId, ConfiguratorEffect::getPriority)
                    .containsExactlyInAnyOrder(tuple(1L, 20), tuple(2L, 10));
        }

        @Test
        @DisplayName("devuelve los efectos ya en el orden nuevo, no en el orden en que llegaron")
        void devuelve_los_efectos_en_el_orden_nuevo() {
            // La pantalla que reordena repinta con lo devuelto. Si saliera en el
            // orden del cuerpo, el usuario veria su propio arrastre en vez de lo que
            // de verdad quedo guardado.
            ConfiguratorEffect add = efectoPorOpcion(1L, O11_SI_VENDE, ITEM_POS, EffectType.ADD,
                    null, 0);
            ConfiguratorEffect remove = efectoPorOpcion(2L, O21_SI_MOSTRADOR, ITEM_CAJA,
                    EffectType.REMOVE, null, 0);
            when(repository.findAllByIds(anyList())).thenReturn(List.of(add, remove));
            when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

            List<ConfiguratorEffectDto> resultado = service
                    .execute(reparto(new EffectPriority(1L, 90), new EffectPriority(2L, 10)));

            assertThat(resultado).extracting(ConfiguratorEffectDto::id).containsExactly(2L, 1L);
            assertThat(resultado).extracting(ConfiguratorEffectDto::priority).containsExactly(10,
                    90);
        }

        @Test
        @DisplayName("lo que no se nombra no se toca: el reparto es parcial a proposito")
        void lo_que_no_se_nombra_no_se_toca() {
            ConfiguratorEffect add = efectoPorOpcion(1L, O11_SI_VENDE, ITEM_POS, EffectType.ADD,
                    null, 0);
            when(repository.findAllByIds(List.of(1L))).thenReturn(List.of(add));
            when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

            assertThat(service.execute(reparto(new EffectPriority(1L, 15)))).hasSize(1);

            verify(repository, times(1)).save(any());
        }
    }

    @Nested
    @DisplayName("Nada se escribe si el reparto no es aplicable entero")
    class NadaSeEscribe {

        @Test
        @DisplayName("un efecto inexistente o de baja da 404 y no guarda ninguno de los otros")
        void un_efecto_inexistente_no_guarda_ninguno() {
            // findAllByIds respeta el borrado logico, asi que un efecto dado de baja
            // tampoco vuelve: para quien reordena las dos cosas son un 404.
            ConfiguratorEffect add = efectoPorOpcion(1L, O11_SI_VENDE, ITEM_POS, EffectType.ADD,
                    null, 0);
            when(repository.findAllByIds(anyList())).thenReturn(List.of(add));

            assertThatThrownBy(() -> service
                    .execute(reparto(new EffectPriority(1L, 20), new EffectPriority(404L, 10))))
                    .isInstanceOf(ConfiguratorEffectNotFoundException.class)
                    .hasMessageContaining("404");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("el mismo efecto dos veces se rechaza: seria el orden del JSON el que decide")
        void el_mismo_efecto_dos_veces_se_rechaza() {
            assertThatThrownBy(() -> service
                    .execute(reparto(new EffectPriority(7L, 20), new EffectPriority(7L, 30))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Duplicated effect in reorder: 7");

            // Ni siquiera llega a leer: el reparto es incoherente por si mismo.
            verify(repository, never()).findAllByIds(anyList());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("una prioridad fuera de 0..9999 la para la entidad y no se guarda")
        void una_prioridad_fuera_de_rango_no_se_guarda() {
            ConfiguratorEffect add = efectoPorOpcion(1L, O11_SI_VENDE, ITEM_POS, EffectType.ADD,
                    null, 0);
            when(repository.findAllByIds(anyList())).thenReturn(List.of(add));

            assertThatThrownBy(() -> service.execute(reparto(new EffectPriority(1L, 10_000))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("priority must be between");

            verify(repository, never()).save(any());
        }
    }
}
