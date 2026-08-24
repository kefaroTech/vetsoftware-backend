package com.vetsoftware.app.configurator.application.usecase;

import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O11_SI_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q1_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q2_MOSTRADOR;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.pregunta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.configurator.application.dto.ConfiguratorQuestionDto;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorOptionRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorQuestionRepository;
import com.vetsoftware.app.configurator.domain.AnswerType;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestion;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Listado paginado de preguntas para la consola de plataforma. */
@ExtendWith(MockitoExtension.class)
@DisplayName("ListConfiguratorQuestionsService — listado paginado de preguntas")
class ListConfiguratorQuestionsServiceTest {

    @Mock
    private ConfiguratorQuestionRepository repository;

    // La pregunta sale con sus opciones anidadas (#448): sin este doble,
    // @InjectMocks deja el puerto a null y el caso de uso revienta con NPE.
    @Mock
    private ConfiguratorOptionRepository optionRepository;

    @InjectMocks
    private ListConfiguratorQuestionsService service;

    @Test
    @DisplayName("traduce el contenido a DTO y conserva intactos los metadatos de la pagina")
    void traduce_el_contenido_y_conserva_los_metadatos() {
        PageResult<ConfiguratorQuestion> pagina = new PageResult<>(List.of(
                pregunta(Q1_VENDE, "SELLS_PRODUCTS", AnswerType.SINGLE, null, true),
                pregunta(Q2_MOSTRADOR, "HAS_COUNTER", AnswerType.SINGLE, O11_SI_VENDE, false)), 2,
                10, 57L, 6);
        when(repository.findAll(2, 10)).thenReturn(pagina);

        PageResult<ConfiguratorQuestionDto> resultado = service.listAll(2, 10);

        assertThat(resultado.content()).extracting(ConfiguratorQuestionDto::code)
                .containsExactly("SELLS_PRODUCTS", "HAS_COUNTER");
        assertThat(resultado.page()).isEqualTo(2);
        assertThat(resultado.pageSize()).isEqualTo(10);
        assertThat(resultado.totalElements()).isEqualTo(57L);
        assertThat(resultado.totalPages()).isEqualTo(6);
    }

    @Test
    @DisplayName("una pagina vacia sigue reportando los totales de la consulta")
    void una_pagina_vacia_sigue_reportando_sus_totales() {
        when(repository.findAll(9, 10)).thenReturn(new PageResult<>(List.of(), 9, 10, 57L, 6));

        PageResult<ConfiguratorQuestionDto> resultado = service.listAll(9, 10);

        assertThat(resultado.content()).isEmpty();
        assertThat(resultado.totalElements()).isEqualTo(57L);
    }
}
