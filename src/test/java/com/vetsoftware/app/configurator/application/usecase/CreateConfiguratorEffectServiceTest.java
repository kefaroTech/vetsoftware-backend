package com.vetsoftware.app.configurator.application.usecase;

import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.CREADA_EL;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.ITEM_POS;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O11_SI_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q1_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q3_CUANTAS_CAJAS;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.opcion;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.pregunta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.configurator.application.command.CreateConfiguratorEffectCommand;
import com.vetsoftware.app.configurator.application.dto.ConfiguratorEffectDto;
import com.vetsoftware.app.configurator.application.port.out.CatalogItemValidationPort;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorEffectRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorOptionRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorQuestionRepository;
import com.vetsoftware.app.configurator.domain.AnswerType;
import com.vetsoftware.app.configurator.domain.ConfiguratorEffect;
import com.vetsoftware.app.configurator.domain.ConfiguratorOptionNotFoundException;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestionNotFoundException;
import com.vetsoftware.app.configurator.domain.EffectType;
import com.vetsoftware.app.configurator.domain.QuantityFromAnswerRequiresNumberQuestionException;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El alta del efecto es la puerta por la que un artículo entra en el carrito
 * del prospecto, así que las tres comprobaciones previas —artículo existente,
 * disparador existente y coherencia de {@code QUANTITY_FROM_ANSWER}— tienen que
 * cortar antes de escribir.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateConfiguratorEffectService — alta de un efecto del configurador")
class CreateConfiguratorEffectServiceTest {

    @Mock
    private ConfiguratorEffectRepository repository;
    @Mock
    private ConfiguratorQuestionRepository questionRepository;
    @Mock
    private ConfiguratorOptionRepository optionRepository;
    @Mock
    private CatalogItemValidationPort catalogItemValidationPort;

    private CreateConfiguratorEffectService service;

    @BeforeEach
    void montarConRelojFijo() {
        service = new CreateConfiguratorEffectService(repository, questionRepository,
                optionRepository, catalogItemValidationPort,
                Clock.fixed(CREADA_EL.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    }

    private static CreateConfiguratorEffectCommand porOpcion(EffectType tipo, Integer cantidad) {
        return new CreateConfiguratorEffectCommand(O11_SI_VENDE, null, ITEM_POS, tipo, cantidad);
    }

    @Nested
    @DisplayName("alta valida")
    class Creacion {

        @Test
        @DisplayName("guarda el efecto con el disparador, el articulo y la fecha del reloj inyectado")
        void guarda_el_efecto_con_lo_que_traia_el_comando() {
            when(catalogItemValidationPort.existsById(ITEM_POS)).thenReturn(true);
            when(optionRepository.findById(O11_SI_VENDE))
                    .thenReturn(Optional.of(opcion(O11_SI_VENDE, Q1_VENDE, "YES")));
            when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

            ConfiguratorEffectDto dto = service.execute(porOpcion(EffectType.SET_QUANTITY, 3));

            ArgumentCaptor<ConfiguratorEffect> guardado = ArgumentCaptor
                    .forClass(ConfiguratorEffect.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getOptionId()).isEqualTo(O11_SI_VENDE);
            assertThat(guardado.getValue().getQuestionId()).isNull();
            assertThat(guardado.getValue().getCatalogItemId()).isEqualTo(ITEM_POS);
            assertThat(guardado.getValue().getEffect()).isEqualTo(EffectType.SET_QUANTITY);
            assertThat(guardado.getValue().getQuantity()).isEqualTo(3);
            assertThat(guardado.getValue().getCreatedDate()).isEqualTo(CREADA_EL);
            assertThat(guardado.getValue().isEnabled()).isTrue();
            assertThat(dto.effect()).isEqualTo(EffectType.SET_QUANTITY);
            assertThat(dto.catalogItemId()).isEqualTo(ITEM_POS);
        }

        @Test
        @DisplayName("un QUANTITY_FROM_ANSWER colgado de una pregunta NUMBER se guarda")
        void quantity_from_answer_sobre_una_number_se_guarda() {
            when(catalogItemValidationPort.existsById(ITEM_POS)).thenReturn(true);
            when(questionRepository.findById(Q3_CUANTAS_CAJAS)).thenReturn(Optional
                    .of(pregunta(Q3_CUANTAS_CAJAS, "HOW_MANY", AnswerType.NUMBER, null, false)));
            when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

            ConfiguratorEffectDto dto = service.execute(new CreateConfiguratorEffectCommand(null,
                    Q3_CUANTAS_CAJAS, ITEM_POS, EffectType.QUANTITY_FROM_ANSWER, null));

            assertThat(dto.questionId()).isEqualTo(Q3_CUANTAS_CAJAS);
            assertThat(dto.quantity()).isNull();
        }
    }

    @Nested
    @DisplayName("lo que corta antes de escribir")
    class Validaciones {

        @Test
        @DisplayName("si el articulo de catalogo no existe no consulta el disparador ni guarda")
        void si_el_articulo_no_existe_no_guarda() {
            when(catalogItemValidationPort.existsById(ITEM_POS)).thenReturn(false);

            assertThatThrownBy(() -> service.execute(porOpcion(EffectType.ADD, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Catalog item not found: " + ITEM_POS);

            verifyNoInteractions(repository, optionRepository, questionRepository);
        }

        @Test
        @DisplayName("si la opcion disparadora no existe no guarda")
        void si_la_opcion_disparadora_no_existe_no_guarda() {
            when(catalogItemValidationPort.existsById(ITEM_POS)).thenReturn(true);
            when(optionRepository.findById(O11_SI_VENDE)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(porOpcion(EffectType.ADD, null)))
                    .isInstanceOf(ConfiguratorOptionNotFoundException.class)
                    .hasMessageContaining("ConfiguratorOption not found: " + O11_SI_VENDE);

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("si la pregunta disparadora no existe no guarda")
        void si_la_pregunta_disparadora_no_existe_no_guarda() {
            when(catalogItemValidationPort.existsById(ITEM_POS)).thenReturn(true);
            when(questionRepository.findById(Q3_CUANTAS_CAJAS)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(new CreateConfiguratorEffectCommand(null,
                    Q3_CUANTAS_CAJAS, ITEM_POS, EffectType.ADD, null)))
                    .isInstanceOf(ConfiguratorQuestionNotFoundException.class)
                    .hasMessageContaining("ConfiguratorQuestion not found: 3");

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("un QUANTITY_FROM_ANSWER colgado de una pregunta que no es NUMBER no se guarda")
        void quantity_from_answer_sobre_una_no_number_no_se_guarda() {
            when(catalogItemValidationPort.existsById(ITEM_POS)).thenReturn(true);
            when(questionRepository.findById(Q1_VENDE)).thenReturn(
                    Optional.of(pregunta(Q1_VENDE, "SELLS", AnswerType.SINGLE, null, true)));

            assertThatThrownBy(() -> service.execute(new CreateConfiguratorEffectCommand(null,
                    Q1_VENDE, ITEM_POS, EffectType.QUANTITY_FROM_ANSWER, null)))
                    .isInstanceOf(QuantityFromAnswerRequiresNumberQuestionException.class)
                    .hasMessageContaining("requires a NUMBER question");

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("un efecto con los dos disparadores no llega a guardarse: lo para la entidad")
        void un_efecto_con_los_dos_disparadores_no_se_guarda() {
            when(catalogItemValidationPort.existsById(ITEM_POS)).thenReturn(true);
            when(optionRepository.findById(O11_SI_VENDE))
                    .thenReturn(Optional.of(opcion(O11_SI_VENDE, Q1_VENDE, "YES")));

            assertThatThrownBy(
                    () -> service.execute(new CreateConfiguratorEffectCommand(O11_SI_VENDE,
                            Q3_CUANTAS_CAJAS, ITEM_POS, EffectType.ADD, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exactly one trigger is required");

            verifyNoInteractions(repository);
        }
    }
}
