package com.vetsoftware.app.configurator.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.configurator.application.command.ResolveConfiguratorSelectionCommand;
import com.vetsoftware.app.configurator.application.dto.ConfiguratorSelectionDto;
import com.vetsoftware.app.configurator.application.dto.QuestionnaireOptionDto;
import com.vetsoftware.app.configurator.application.dto.QuestionnaireQuestionDto;
import com.vetsoftware.app.configurator.application.dto.SelectedItemDto;
import com.vetsoftware.app.configurator.application.port.in.GetPublicQuestionnaireUseCase;
import com.vetsoftware.app.configurator.application.port.in.ResolveConfiguratorSelectionUseCase;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ConfiguratorController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("ConfiguratorController — contrato HTTP")
class ConfiguratorControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private GetPublicQuestionnaireUseCase questionnaireUseCase;
    @MockitoBean
    private ResolveConfiguratorSelectionUseCase resolveUseCase;

    @Test
    @DisplayName("serializa como arreglo el cuestionario público vacío")
    void serializa_como_arreglo_el_cuestionario_publico_vacio() throws Exception {
        when(questionnaireUseCase.get()).thenReturn(List.of());

        mockMvc.perform(get("/configurator/questionnaire")).andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("anida las opciones dentro de su pregunta y no repite el questionId en el JSON publico")
    void anida_las_opciones_dentro_de_su_pregunta() throws Exception {
        when(questionnaireUseCase.get()).thenReturn(List.of(new QuestionnaireQuestionDto(1L,
                "SELLS_PRODUCTS", "¿Vende productos?", "ayuda", "SINGLE", null, true, 0,
                List.of(new QuestionnaireOptionDto(11L, "YES", "Sí, vendo", null, 0)))));

        mockMvc.perform(get("/configurator/questionnaire")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].code").value("SELLS_PRODUCTS"))
                .andExpect(jsonPath("$[0].answerType").value("SINGLE"))
                .andExpect(jsonPath("$[0].required").value(true))
                .andExpect(jsonPath("$[0].options[0].id").value(11))
                .andExpect(jsonPath("$[0].options[0].code").value("YES"))
                .andExpect(jsonPath("$[0].options[0].questionId").doesNotExist());
    }

    @Test
    @DisplayName("resolver traslada opciones marcadas y respuestas numericas tal como llegan")
    void resolver_traslada_las_respuestas_tal_como_llegan() throws Exception {
        when(resolveUseCase.resolve(any())).thenReturn(new ConfiguratorSelectionDto(
                List.of(new SelectedItemDto(100L, 1), new SelectedItemDto(200L, 4))));

        mockMvc.perform(post("/configurator/resolve").contentType(APPLICATION_JSON).content("""
                {"selectedOptionIds":[11,21],"numericAnswers":{"3":4}}
                """)).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].catalogItemId").value(100))
                .andExpect(jsonPath("$.items[1].catalogItemId").value(200))
                .andExpect(jsonPath("$.items[1].quantity").value(4));

        ArgumentCaptor<ResolveConfiguratorSelectionCommand> comando = ArgumentCaptor
                .forClass(ResolveConfiguratorSelectionCommand.class);
        verify(resolveUseCase).resolve(comando.capture());
        assertThat(comando.getValue().selectedOptionIds()).containsExactlyInAnyOrder(11L, 21L);
        assertThat(comando.getValue().numericAnswers()).containsEntry(3L, 4);
    }

    @Test
    @DisplayName("un cuerpo sin respuestas resuelve igual: la seleccion vacia es una respuesta valida")
    void un_cuerpo_sin_respuestas_resuelve_igual() throws Exception {
        when(resolveUseCase.resolve(any())).thenReturn(new ConfiguratorSelectionDto(List.of()));

        mockMvc.perform(post("/configurator/resolve").contentType(APPLICATION_JSON).content("""
                {"selectedOptionIds":[],"numericAnswers":{}}
                """)).andExpect(status().isOk()).andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isEmpty());
    }
}
