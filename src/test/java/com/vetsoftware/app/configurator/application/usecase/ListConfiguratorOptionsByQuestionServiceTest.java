package com.vetsoftware.app.configurator.application.usecase;

import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O11_SI_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O12_NO_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q1_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.opcion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.configurator.application.dto.ConfiguratorOptionDto;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorOptionRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Las opciones de una pregunta, en el orden que fijó la consulta. */
@ExtendWith(MockitoExtension.class)
@DisplayName("ListConfiguratorOptionsByQuestionService — opciones de una pregunta")
class ListConfiguratorOptionsByQuestionServiceTest {

    @Mock
    private ConfiguratorOptionRepository repository;
    @InjectMocks
    private ListConfiguratorOptionsByQuestionService service;

    @Test
    @DisplayName("traduce a DTO respetando el orden que dio la consulta")
    void traduce_a_dto_respetando_el_orden() {
        when(repository.findByQuestionId(Q1_VENDE)).thenReturn(List
                .of(opcion(O11_SI_VENDE, Q1_VENDE, "YES"), opcion(O12_NO_VENDE, Q1_VENDE, "NO")));

        List<ConfiguratorOptionDto> opciones = service.listByQuestion(Q1_VENDE);

        assertThat(opciones).extracting(ConfiguratorOptionDto::id, ConfiguratorOptionDto::code)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(O11_SI_VENDE, "YES"),
                        org.assertj.core.groups.Tuple.tuple(O12_NO_VENDE, "NO"));
        assertThat(opciones).allSatisfy(dto -> assertThat(dto.questionId()).isEqualTo(Q1_VENDE));
    }

    @Test
    @DisplayName("una pregunta numerica no tiene opciones y devuelve lista vacia, no null")
    void una_pregunta_sin_opciones_devuelve_lista_vacia() {
        when(repository.findByQuestionId(Q1_VENDE)).thenReturn(List.of());

        assertThat(service.listByQuestion(Q1_VENDE)).isEmpty();
    }
}
