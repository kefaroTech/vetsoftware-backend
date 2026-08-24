package com.vetsoftware.app.configurator.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.configurator.application.command.CreateConfiguratorEffectCommand;
import com.vetsoftware.app.configurator.application.command.CreateConfiguratorOptionCommand;
import com.vetsoftware.app.configurator.application.command.CreateConfiguratorQuestionCommand;
import com.vetsoftware.app.configurator.application.command.UpdateConfiguratorEffectCommand;
import com.vetsoftware.app.configurator.application.command.UpdateConfiguratorOptionCommand;
import com.vetsoftware.app.configurator.application.command.UpdateConfiguratorQuestionCommand;
import com.vetsoftware.app.configurator.application.dto.ConfiguratorEffectDto;
import com.vetsoftware.app.configurator.application.dto.ConfiguratorOptionDto;
import com.vetsoftware.app.configurator.application.dto.ConfiguratorQuestionDto;
import com.vetsoftware.app.configurator.domain.AnswerType;
import com.vetsoftware.app.configurator.domain.EffectType;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.mockito.ArgumentCaptor;

import com.vetsoftware.app.configurator.application.port.in.CreateConfiguratorEffectUseCase;
import com.vetsoftware.app.configurator.application.port.in.CreateConfiguratorOptionUseCase;
import com.vetsoftware.app.configurator.application.port.in.CreateConfiguratorQuestionUseCase;
import com.vetsoftware.app.configurator.application.port.in.DeleteConfiguratorEffectUseCase;
import com.vetsoftware.app.configurator.application.port.in.DeleteConfiguratorOptionUseCase;
import com.vetsoftware.app.configurator.application.port.in.DeleteConfiguratorQuestionUseCase;
import com.vetsoftware.app.configurator.application.port.in.FindConfiguratorQuestionUseCase;
import com.vetsoftware.app.configurator.application.port.in.ListConfiguratorEffectsUseCase;
import com.vetsoftware.app.configurator.application.port.in.ListConfiguratorOptionsByQuestionUseCase;
import com.vetsoftware.app.configurator.application.port.in.ListConfiguratorQuestionsUseCase;
import com.vetsoftware.app.configurator.application.port.in.UpdateConfiguratorEffectUseCase;
import com.vetsoftware.app.configurator.application.port.in.UpdateConfiguratorOptionUseCase;
import com.vetsoftware.app.configurator.application.port.in.UpdateConfiguratorQuestionUseCase;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ConfiguratorAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("ConfiguratorAdminController — contrato HTTP")
class ConfiguratorAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private CreateConfiguratorQuestionUseCase createQuestionUseCase;
    @MockitoBean
    private UpdateConfiguratorQuestionUseCase updateQuestionUseCase;
    @MockitoBean
    private DeleteConfiguratorQuestionUseCase deleteQuestionUseCase;
    @MockitoBean
    private FindConfiguratorQuestionUseCase findQuestionUseCase;
    @MockitoBean
    private ListConfiguratorQuestionsUseCase listQuestionsUseCase;
    @MockitoBean
    private CreateConfiguratorOptionUseCase createOptionUseCase;
    @MockitoBean
    private UpdateConfiguratorOptionUseCase updateOptionUseCase;
    @MockitoBean
    private DeleteConfiguratorOptionUseCase deleteOptionUseCase;
    @MockitoBean
    private ListConfiguratorOptionsByQuestionUseCase listOptionsUseCase;
    @MockitoBean
    private CreateConfiguratorEffectUseCase createEffectUseCase;
    @MockitoBean
    private UpdateConfiguratorEffectUseCase updateEffectUseCase;
    @MockitoBean
    private DeleteConfiguratorEffectUseCase deleteEffectUseCase;
    @MockitoBean
    private ListConfiguratorEffectsUseCase listEffectsUseCase;

    private static final LocalDateTime CREADA = LocalDateTime.of(2026, 8, 22, 10, 0);

    private static final ConfiguratorQuestionDto UNA_PREGUNTA = new ConfiguratorQuestionDto(1L,
            "SELLS_PRODUCTS", "¿Vende productos?", null, AnswerType.SINGLE, null, true, 2, CREADA,
            true);

    private static final ConfiguratorOptionDto UNA_OPCION = new ConfiguratorOptionDto(11L, 1L,
            "YES", "Sí, vendo", null, 0, CREADA, true);

    private static final ConfiguratorEffectDto UN_EFECTO = new ConfiguratorEffectDto(5L, 11L, null,
            100L, EffectType.SET_QUANTITY, 3, CREADA, true);

    @Test
    @DisplayName("pagina las preguntas globales sin inventar un tenant")
    void pagina_las_preguntas_globales_sin_inventar_un_tenant() throws Exception {
        when(listQuestionsUseCase.listAll(3, 7)).thenReturn(PageResult.empty(3, 7));

        mockMvc.perform(get("/configurator/questions").param("page", "3").param("pageSize", "7"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(3)).andExpect(jsonPath("$.pageSize").value(7));

        verify(listQuestionsUseCase).listAll(3, 7);
    }

    @Nested
    @DisplayName("preguntas")
    class Preguntas {

        @Test
        @DisplayName("el alta devuelve 201 y traduce el enum del tipo de respuesta a texto")
        void el_alta_devuelve_201_y_traduce_el_enum() throws Exception {
            when(createQuestionUseCase.execute(any())).thenReturn(UNA_PREGUNTA);

            mockMvc.perform(
                    post("/configurator/questions").contentType(APPLICATION_JSON).content("""
                            {"code":"SELLS_PRODUCTS","questionText":"¿Vende productos?",
                             "helpText":null,"answerType":"SINGLE","parentOptionId":null,
                             "required":true,"sortOrder":2}
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.code").value("SELLS_PRODUCTS"))
                    .andExpect(jsonPath("$.answerType").value("SINGLE"))
                    .andExpect(jsonPath("$.required").value(true));

            ArgumentCaptor<CreateConfiguratorQuestionCommand> comando = ArgumentCaptor
                    .forClass(CreateConfiguratorQuestionCommand.class);
            verify(createQuestionUseCase).execute(comando.capture());
            assertThat(comando.getValue().code()).isEqualTo("SELLS_PRODUCTS");
            assertThat(comando.getValue().answerType()).isEqualTo(AnswerType.SINGLE);
            assertThat(comando.getValue().sortOrder()).isEqualTo(2);
        }

        @Test
        @DisplayName("el alta sin codigo se rechaza con 400 y no llega al caso de uso")
        void el_alta_sin_codigo_se_rechaza_con_400() throws Exception {
            mockMvc.perform(
                    post("/configurator/questions").contentType(APPLICATION_JSON).content("""
                            {"code":"  ","questionText":"¿Vende productos?","answerType":"SINGLE",
                             "required":true,"sortOrder":0}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(createQuestionUseCase);
        }

        @Test
        @DisplayName("la edicion toma el id de la ruta, no del cuerpo")
        void la_edicion_toma_el_id_de_la_ruta() throws Exception {
            when(updateQuestionUseCase.execute(any())).thenReturn(UNA_PREGUNTA);

            mockMvc.perform(
                    put("/configurator/questions/{id}", 1).contentType(APPLICATION_JSON).content("""
                            {"questionText":"¿Vende productos?","helpText":"ayuda",
                             "answerType":"NUMBER","parentOptionId":11,"required":false,
                             "sortOrder":9}
                            """)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));

            ArgumentCaptor<UpdateConfiguratorQuestionCommand> comando = ArgumentCaptor
                    .forClass(UpdateConfiguratorQuestionCommand.class);
            verify(updateQuestionUseCase).execute(comando.capture());
            assertThat(comando.getValue().id()).isEqualTo(1L);
            assertThat(comando.getValue().answerType()).isEqualTo(AnswerType.NUMBER);
            assertThat(comando.getValue().parentOptionId()).isEqualTo(11L);
            assertThat(comando.getValue().required()).isFalse();
        }

        @Test
        @DisplayName("la consulta por id serializa la pregunta completa")
        void la_consulta_por_id_serializa_la_pregunta() throws Exception {
            when(findQuestionUseCase.findById(1L)).thenReturn(UNA_PREGUNTA);

            mockMvc.perform(get("/configurator/questions/{id}", 1)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SELLS_PRODUCTS"))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("la baja devuelve 204 sin cuerpo")
        void la_baja_devuelve_204_sin_cuerpo() throws Exception {
            mockMvc.perform(delete("/configurator/questions/{id}", 1))
                    .andExpect(status().isNoContent()).andExpect(content().string(""));

            verify(deleteQuestionUseCase).execute(1L);
        }
    }

    @Nested
    @DisplayName("opciones")
    class Opciones {

        @Test
        @DisplayName("las opciones de una pregunta salen como arreglo, no paginadas")
        void las_opciones_salen_como_arreglo() throws Exception {
            when(listOptionsUseCase.listByQuestion(1L)).thenReturn(List.of(UNA_OPCION));

            mockMvc.perform(get("/configurator/questions/{questionId}/options", 1))
                    .andExpect(status().isOk()).andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].id").value(11))
                    .andExpect(jsonPath("$[0].questionId").value(1))
                    .andExpect(jsonPath("$[0].code").value("YES"));
        }

        @Test
        @DisplayName("el alta devuelve 201 y traslada la pregunta a la que cuelga")
        void el_alta_de_opcion_devuelve_201() throws Exception {
            when(createOptionUseCase.execute(any())).thenReturn(UNA_OPCION);

            mockMvc.perform(post("/configurator/options").contentType(APPLICATION_JSON).content("""
                    {"questionId":1,"code":"YES","label":"Sí, vendo","helpText":null,
                     "sortOrder":0}
                    """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("YES"));

            ArgumentCaptor<CreateConfiguratorOptionCommand> comando = ArgumentCaptor
                    .forClass(CreateConfiguratorOptionCommand.class);
            verify(createOptionUseCase).execute(comando.capture());
            assertThat(comando.getValue().questionId()).isEqualTo(1L);
            assertThat(comando.getValue().code()).isEqualTo("YES");
        }

        @Test
        @DisplayName("el alta sin etiqueta se rechaza con 400 y no llega al caso de uso")
        void el_alta_sin_etiqueta_se_rechaza_con_400() throws Exception {
            mockMvc.perform(post("/configurator/options").contentType(APPLICATION_JSON).content("""
                    {"questionId":1,"code":"YES","label":"","sortOrder":0}
                    """)).andExpect(status().isBadRequest());

            verifyNoInteractions(createOptionUseCase);
        }

        @Test
        @DisplayName("la edicion toma el id de la ruta y no permite mover la opcion de pregunta")
        void la_edicion_de_opcion_toma_el_id_de_la_ruta() throws Exception {
            when(updateOptionUseCase.execute(any())).thenReturn(UNA_OPCION);

            mockMvc.perform(
                    put("/configurator/options/{id}", 11).contentType(APPLICATION_JSON).content("""
                            {"label":"Sí, vendo","helpText":"ayuda","sortOrder":4}
                            """)).andExpect(status().isOk());

            ArgumentCaptor<UpdateConfiguratorOptionCommand> comando = ArgumentCaptor
                    .forClass(UpdateConfiguratorOptionCommand.class);
            verify(updateOptionUseCase).execute(comando.capture());
            assertThat(comando.getValue().id()).isEqualTo(11L);
            assertThat(comando.getValue().label()).isEqualTo("Sí, vendo");
            assertThat(comando.getValue().sortOrder()).isEqualTo(4);
        }

        @Test
        @DisplayName("la baja de una opcion devuelve 204")
        void la_baja_de_opcion_devuelve_204() throws Exception {
            mockMvc.perform(delete("/configurator/options/{id}", 11))
                    .andExpect(status().isNoContent());

            verify(deleteOptionUseCase).execute(11L);
        }
    }

    @Nested
    @DisplayName("efectos")
    class Efectos {

        @Test
        @DisplayName("el alta devuelve 201 y traduce el enum del efecto a texto")
        void el_alta_de_efecto_devuelve_201() throws Exception {
            when(createEffectUseCase.execute(any())).thenReturn(UN_EFECTO);

            mockMvc.perform(post("/configurator/effects").contentType(APPLICATION_JSON).content("""
                    {"optionId":11,"questionId":null,"catalogItemId":100,
                     "effect":"SET_QUANTITY","quantity":3}
                    """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.effect").value("SET_QUANTITY"))
                    .andExpect(jsonPath("$.quantity").value(3))
                    .andExpect(jsonPath("$.optionId").value(11));

            ArgumentCaptor<CreateConfiguratorEffectCommand> comando = ArgumentCaptor
                    .forClass(CreateConfiguratorEffectCommand.class);
            verify(createEffectUseCase).execute(comando.capture());
            assertThat(comando.getValue().optionId()).isEqualTo(11L);
            assertThat(comando.getValue().questionId()).isNull();
            assertThat(comando.getValue().effect()).isEqualTo(EffectType.SET_QUANTITY);
            assertThat(comando.getValue().quantity()).isEqualTo(3);
        }

        @Test
        @DisplayName("una cantidad de cero se rechaza con 400 antes de llegar al caso de uso")
        void una_cantidad_de_cero_se_rechaza_con_400() throws Exception {
            mockMvc.perform(post("/configurator/effects").contentType(APPLICATION_JSON).content("""
                    {"optionId":11,"catalogItemId":100,"effect":"SET_QUANTITY",
                     "quantity":0}
                    """)).andExpect(status().isBadRequest());

            verifyNoInteractions(createEffectUseCase);
        }

        @Test
        @DisplayName("un efecto que no existe en el enum se rechaza con 400")
        void un_efecto_desconocido_se_rechaza_con_400() throws Exception {
            mockMvc.perform(post("/configurator/effects").contentType(APPLICATION_JSON).content("""
                    {"optionId":11,"catalogItemId":100,"effect":"REGALAR"}
                    """)).andExpect(status().isBadRequest());

            verifyNoInteractions(createEffectUseCase);
        }

        @Test
        @DisplayName("pagina los efectos con los metadatos de la consulta")
        void pagina_los_efectos_con_sus_metadatos() throws Exception {
            when(listEffectsUseCase.listAll(0, 20))
                    .thenReturn(new PageResult<>(List.of(UN_EFECTO), 0, 20, 1L, 1));

            mockMvc.perform(get("/configurator/effects")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].catalogItemId").value(100))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("la edicion toma el id de la ruta y no toca el disparador")
        void la_edicion_de_efecto_toma_el_id_de_la_ruta() throws Exception {
            when(updateEffectUseCase.execute(any())).thenReturn(UN_EFECTO);

            mockMvc.perform(
                    put("/configurator/effects/{id}", 5).contentType(APPLICATION_JSON).content("""
                            {"catalogItemId":100,"effect":"SET_QUANTITY","quantity":3}
                            """)).andExpect(status().isOk());

            ArgumentCaptor<UpdateConfiguratorEffectCommand> comando = ArgumentCaptor
                    .forClass(UpdateConfiguratorEffectCommand.class);
            verify(updateEffectUseCase).execute(comando.capture());
            assertThat(comando.getValue().id()).isEqualTo(5L);
            assertThat(comando.getValue().catalogItemId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("la baja de un efecto devuelve 204")
        void la_baja_de_efecto_devuelve_204() throws Exception {
            mockMvc.perform(delete("/configurator/effects/{id}", 5))
                    .andExpect(status().isNoContent());

            verify(deleteEffectUseCase).execute(5L);
        }
    }
}
