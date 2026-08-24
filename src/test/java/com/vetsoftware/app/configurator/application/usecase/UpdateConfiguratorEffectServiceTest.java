package com.vetsoftware.app.configurator.application.usecase;

import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.ITEM_CAJA;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.ITEM_POS;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O11_SI_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q1_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q3_CUANTAS_CAJAS;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.efectoPorOpcion;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.efectoPorPregunta;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.pregunta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.configurator.application.command.UpdateConfiguratorEffectCommand;
import com.vetsoftware.app.configurator.application.dto.ConfiguratorEffectDto;
import com.vetsoftware.app.configurator.application.port.out.CatalogItemValidationPort;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorEffectRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorOptionRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorQuestionRepository;
import com.vetsoftware.app.configurator.domain.AnswerType;
import com.vetsoftware.app.configurator.domain.ConfiguratorEffect;
import com.vetsoftware.app.configurator.domain.ConfiguratorEffectNotFoundException;
import com.vetsoftware.app.configurator.domain.EffectType;
import com.vetsoftware.app.configurator.domain.QuantityFromAnswerRequiresNumberQuestionException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * La edición no puede mover el disparador —eso es otro efecto distinto— pero sí
 * puede cambiar el tipo, y ahí es donde puede colarse un
 * {@code QUANTITY_FROM_ANSWER} sobre una pregunta que no es {@code NUMBER}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateConfiguratorEffectService — edicion de un efecto")
class UpdateConfiguratorEffectServiceTest {

    @Mock
    private ConfiguratorEffectRepository repository;
    @Mock
    private ConfiguratorQuestionRepository questionRepository;
    @Mock
    private ConfiguratorOptionRepository optionRepository;
    @Mock
    private CatalogItemValidationPort catalogItemValidationPort;
    @InjectMocks
    private UpdateConfiguratorEffectService service;

    @Nested
    @DisplayName("edicion valida")
    class Edicion {

        @Test
        @DisplayName("cambia articulo, tipo y cantidad conservando el disparador original")
        void cambia_lo_editable_y_conserva_el_disparador() {
            when(repository.findById(1L)).thenReturn(
                    Optional.of(efectoPorOpcion(1L, O11_SI_VENDE, ITEM_POS, EffectType.ADD, null)));
            when(catalogItemValidationPort.existsById(ITEM_CAJA)).thenReturn(true);
            when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

            ConfiguratorEffectDto dto = service.execute(
                    new UpdateConfiguratorEffectCommand(1L, ITEM_CAJA, EffectType.SET_QUANTITY, 5));

            ArgumentCaptor<ConfiguratorEffect> guardado = ArgumentCaptor
                    .forClass(ConfiguratorEffect.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getCatalogItemId()).isEqualTo(ITEM_CAJA);
            assertThat(guardado.getValue().getEffect()).isEqualTo(EffectType.SET_QUANTITY);
            assertThat(guardado.getValue().getQuantity()).isEqualTo(5);
            assertThat(guardado.getValue().getOptionId()).isEqualTo(O11_SI_VENDE);
            assertThat(dto.id()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("lo que corta antes de escribir")
    class Validaciones {

        @Test
        @DisplayName("un efecto inexistente no consulta el catalogo ni guarda")
        void un_efecto_inexistente_no_guarda() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(
                    new UpdateConfiguratorEffectCommand(99L, ITEM_CAJA, EffectType.ADD, null)))
                    .isInstanceOf(ConfiguratorEffectNotFoundException.class)
                    .hasMessageContaining("ConfiguratorEffect not found: 99");

            verify(repository, never()).save(any());
            verifyNoInteractions(catalogItemValidationPort, questionRepository, optionRepository);
        }

        @Test
        @DisplayName("un articulo de catalogo inexistente no guarda")
        void un_articulo_inexistente_no_guarda() {
            when(repository.findById(1L)).thenReturn(
                    Optional.of(efectoPorOpcion(1L, O11_SI_VENDE, ITEM_POS, EffectType.ADD, null)));
            when(catalogItemValidationPort.existsById(ITEM_CAJA)).thenReturn(false);

            assertThatThrownBy(() -> service.execute(
                    new UpdateConfiguratorEffectCommand(1L, ITEM_CAJA, EffectType.ADD, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Catalog item not found: " + ITEM_CAJA);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("pasar un efecto de opcion a QUANTITY_FROM_ANSWER se rechaza y no lo guarda a medias")
        void pasar_a_quantity_from_answer_sobre_una_no_number_se_rechaza() {
            ConfiguratorEffect existente = efectoPorPregunta(1L, Q1_VENDE, ITEM_POS, EffectType.ADD,
                    null);
            when(repository.findById(1L)).thenReturn(Optional.of(existente));
            when(catalogItemValidationPort.existsById(ITEM_CAJA)).thenReturn(true);
            when(questionRepository.findById(Q1_VENDE)).thenReturn(
                    Optional.of(pregunta(Q1_VENDE, "SELLS", AnswerType.SINGLE, null, true)));

            assertThatThrownBy(() -> service.execute(new UpdateConfiguratorEffectCommand(1L,
                    ITEM_CAJA, EffectType.QUANTITY_FROM_ANSWER, null)))
                    .isInstanceOf(QuantityFromAnswerRequiresNumberQuestionException.class)
                    .hasMessageContaining("requires a NUMBER question");

            verify(repository, never()).save(any());
            assertThat(existente.getEffect()).isEqualTo(EffectType.ADD);
            assertThat(existente.getCatalogItemId()).isEqualTo(ITEM_POS);
        }

        @Test
        @DisplayName("dejar SET_QUANTITY sin cantidad se rechaza y no guarda")
        void set_quantity_sin_cantidad_se_rechaza() {
            when(repository.findById(1L)).thenReturn(
                    Optional.of(efectoPorOpcion(1L, O11_SI_VENDE, ITEM_POS, EffectType.ADD, null)));
            when(catalogItemValidationPort.existsById(ITEM_CAJA)).thenReturn(true);

            assertThatThrownBy(() -> service.execute(new UpdateConfiguratorEffectCommand(1L,
                    ITEM_CAJA, EffectType.SET_QUANTITY, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SET_QUANTITY requires a quantity");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un QUANTITY_FROM_ANSWER sobre una pregunta NUMBER si se guarda")
        void quantity_from_answer_sobre_una_number_se_guarda() {
            when(repository.findById(1L)).thenReturn(Optional
                    .of(efectoPorPregunta(1L, Q3_CUANTAS_CAJAS, ITEM_POS, EffectType.ADD, null)));
            when(catalogItemValidationPort.existsById(ITEM_CAJA)).thenReturn(true);
            when(questionRepository.findById(Q3_CUANTAS_CAJAS)).thenReturn(Optional
                    .of(pregunta(Q3_CUANTAS_CAJAS, "HOW_MANY", AnswerType.NUMBER, null, false)));
            when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

            ConfiguratorEffectDto dto = service.execute(new UpdateConfiguratorEffectCommand(1L,
                    ITEM_CAJA, EffectType.QUANTITY_FROM_ANSWER, null));

            assertThat(dto.effect()).isEqualTo(EffectType.QUANTITY_FROM_ANSWER);
            assertThat(dto.quantity()).isNull();
        }
    }
}
