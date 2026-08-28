package com.vetsoftware.app.catalogitemlimit.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.catalogitemlimit.application.command.CreateCatalogItemLimitCommand;
import com.vetsoftware.app.catalogitemlimit.application.command.UpdateCatalogItemLimitCommand;
import com.vetsoftware.app.catalogitemlimit.application.dto.CatalogItemLimitDto;
import com.vetsoftware.app.catalogitemlimit.application.port.in.CreateCatalogItemLimitUseCase;
import com.vetsoftware.app.catalogitemlimit.application.port.in.ListCatalogItemLimitsUseCase;
import com.vetsoftware.app.catalogitemlimit.application.port.in.UpdateCatalogItemLimitUseCase;
import com.vetsoftware.app.catalogitemlimit.domain.CatalogItemLimitAlreadyExistsException;
import com.vetsoftware.app.catalogitemlimit.domain.CatalogItemLimitNotFoundException;
import com.vetsoftware.app.catalogitemlimit.domain.LimitEnforcement;
import com.vetsoftware.app.catalogitemlimit.domain.LimitMode;
import com.vetsoftware.app.catalogitemlimit.domain.MeasureKind;
import com.vetsoftware.app.catalogitemlimit.infrastructure.web.request.CreateCatalogItemLimitRequest;
import com.vetsoftware.app.catalogitemlimit.infrastructure.web.request.UpdateCatalogItemLimitRequest;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.lang.reflect.RecordComponent;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rodaja HTTP de los techos de fábrica: rutas anidadas bajo el artículo,
 * binding, validación del cuerpo y forma del JSON.
 *
 * <p>
 * Sobre lo que esta rodaja puede afirmar de la autorización, ver el javadoc de
 * {@code LimitDimensionControllerTest}: el gate no se ejecuta aquí, así que lo
 * que se comprueba es que el declarado sigue siendo el que la ficha de
 * construcción exige.
 */
@WebMvcTest(CatalogItemLimitController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("CatalogItemLimitController — contrato HTTP")
class CatalogItemLimitControllerTest {

    private static final String CUERPO_VALIDO = """
            {"limitDimensionId":4,"mode":"LIMITED","limitQuantity":100,"enforcement":"BLOCK",
             "warnThreshold":80,"trialMode":"LIMITED","trialLimitQuantity":10}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateCatalogItemLimitUseCase createUseCase;
    @MockitoBean
    private UpdateCatalogItemLimitUseCase updateUseCase;
    @MockitoBean
    private ListCatalogItemLimitsUseCase listUseCase;

    private static CatalogItemLimitDto cienMascotas() {
        return new CatalogItemLimitDto(11L, 7L, 4L, MeasureKind.CUMULATIVE, LimitMode.LIMITED, 100,
                null, LimitEnforcement.BLOCK, null, 80, LimitMode.LIMITED, 10,
                LocalDateTime.of(2026, 8, 27, 9, 0));
    }

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("POST /catalog-items/{id}/limits responde 201 con el techo declarado")
        void post_responde_201() throws Exception {
            when(createUseCase.execute(any())).thenReturn(cienMascotas());

            mockMvc.perform(post("/catalog-items/7/limits").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(11))
                    .andExpect(jsonPath("$.catalogItemId").value(7))
                    .andExpect(jsonPath("$.limitDimensionId").value(4))
                    .andExpect(jsonPath("$.measureKind").value("CUMULATIVE"))
                    .andExpect(jsonPath("$.limitQuantity").value(100))
                    .andExpect(jsonPath("$.enforcement").value("BLOCK"))
                    .andExpect(jsonPath("$.warnThreshold").value(80));
        }

        /**
         * El artículo lo pone la ruta y el tipo de medida no viaja: se resuelve desde
         * el eje dentro del caso de uso. Aceptarlo del cliente permitiría declarar un
         * tipo distinto del real y el error saldría del motor, a mitad de una operación
         * de catálogo, sin decir qué corregir.
         */
        @Test
        @DisplayName("POST toma el articulo de la ruta y no acepta el tipo de medida del cuerpo")
        void post_toma_el_articulo_de_la_ruta() throws Exception {
            when(createUseCase.execute(any())).thenReturn(cienMascotas());

            mockMvc.perform(post("/catalog-items/7/limits").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO));

            ArgumentCaptor<CreateCatalogItemLimitCommand> command = ArgumentCaptor
                    .forClass(CreateCatalogItemLimitCommand.class);
            verify(createUseCase).execute(command.capture());
            assertThat(command.getValue().catalogItemId()).isEqualTo(7L);
            assertThat(command.getValue().limitDimensionId()).isEqualTo(4L);
            assertThat(command.getValue().mode()).isEqualTo(LimitMode.LIMITED);
            assertThat(command.getValue().warnThreshold()).isEqualTo(80);
            assertThat(CreateCatalogItemLimitRequest.class.getRecordComponents())
                    .extracting(RecordComponent::getName).doesNotContain("measureKind");
        }

        @Test
        @DisplayName("POST de un techo ya declarado sobre el mismo eje responde 409")
        void post_repetido_responde_409() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new CatalogItemLimitAlreadyExistsException(7L, 4L));

            mockMvc.perform(post("/catalog-items/7/limits").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("POST sin eje responde 400 y no llega al caso de uso")
        void post_sin_eje_responde_400() throws Exception {
            mockMvc.perform(post("/catalog-items/7/limits").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"mode":"FULL","enforcement":"BLOCK","warnThreshold":80,
                             "trialMode":"FULL"}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        /**
         * {@code warnThreshold} es {@code Integer} y no {@code int} justamente para
         * esto: omitirlo tiene que ser un error de campo, no un cero que muere después
         * contra la invariante «entre 1 y 100» con un mensaje que no señala a nada.
         */
        @Test
        @DisplayName("POST sin porcentaje de aviso responde 400: omitirlo no cae a cero")
        void post_sin_warn_threshold_responde_400() throws Exception {
            mockMvc.perform(post("/catalog-items/7/limits").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"limitDimensionId":4,"mode":"FULL","enforcement":"BLOCK",
                             "trialMode":"FULL"}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("POST con un porcentaje de aviso de 101 responde 400")
        void post_con_warn_threshold_fuera_de_rango_responde_400() throws Exception {
            mockMvc.perform(post("/catalog-items/7/limits").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"limitDimensionId":4,"mode":"FULL","enforcement":"BLOCK",
                             "warnThreshold":101,"trialMode":"FULL"}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("POST con un precio de excedente negativo responde 400")
        void post_con_excedente_negativo_responde_400() throws Exception {
            mockMvc.perform(post("/catalog-items/7/limits").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"limitDimensionId":4,"mode":"FULL","enforcement":"OVERAGE",
                             "overageUnitAmount":-2.50,"warnThreshold":80,"trialMode":"FULL"}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("PUT sin modo de prueba responde 400: es obligatorio al editar")
        void put_sin_trial_mode_responde_400() throws Exception {
            mockMvc.perform(put("/catalog-items/7/limits/11")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"mode":"FULL","enforcement":"BLOCK","warnThreshold":80}
                            """)).andExpect(status().isBadRequest());

            verify(updateUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("Consultas y edicion")
    class ConsultasYEdicion {

        @Test
        @DisplayName("GET /catalog-items/{id}/limits devuelve los techos del articulo")
        void get_listado_por_articulo() throws Exception {
            when(listUseCase.listByCatalogItemId(7L)).thenReturn(List.of(cienMascotas()));

            mockMvc.perform(get("/catalog-items/7/limits")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].limitQuantity").value(100));

            verify(listUseCase).listByCatalogItemId(7L);
        }

        @Test
        @DisplayName("PUT /catalog-items/{id}/limits/{limitId} toma el id del techo de la ruta")
        void put_toma_el_id_de_la_ruta() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(cienMascotas());

            mockMvc.perform(put("/catalog-items/7/limits/11")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"mode":"LIMITED","limitQuantity":200,"enforcement":"WARN",
                             "warnThreshold":90,"trialMode":"FULL"}
                            """)).andExpect(status().isOk());

            ArgumentCaptor<UpdateCatalogItemLimitCommand> command = ArgumentCaptor
                    .forClass(UpdateCatalogItemLimitCommand.class);
            verify(updateUseCase).execute(command.capture());
            assertThat(command.getValue().id()).isEqualTo(11L);
            assertThat(command.getValue().limitQuantity()).isEqualTo(200);
            assertThat(command.getValue().enforcement()).isEqualTo(LimitEnforcement.WARN);
        }

        /**
         * <strong>La ruta del {@code PUT} ya no decora: se comprueba.</strong> Esta
         * aserción estaba escrita al revés —afirmaba que el command <em>no</em> llevaba
         * {@code catalogItemId}— para dejar constancia del hueco, con la nota de que el
         * día que el campo se añadiera fallaría y sería la señal de propagarlo. Ese día
         * llegó: el artículo viaja en el command y la carga se acota por el par, así
         * que editar el techo del artículo 7 entrando por la ruta del 9 ya no funciona.
         */
        @Test
        @DisplayName("el command de edicion lleva el articulo: la ruta del PUT se comprueba")
        void el_command_de_edicion_lleva_el_articulo() {
            assertThat(UpdateCatalogItemLimitCommand.class.getRecordComponents())
                    .extracting(RecordComponent::getName).contains("catalogItemId");
        }

        /**
         * La otra mitad de lo mismo, del lado del controller: el artículo de la ruta
         * llega al caso de uso. Sin esta aserción, el campo podría existir en el
         * command y el controller seguir sin rellenarlo, que es exactamente el defecto
         * con otra cara.
         */
        @Test
        @DisplayName("PUT propaga el articulo de la ruta al command, no solo el id del techo")
        void put_propaga_el_articulo_de_la_ruta() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(cienMascotas());

            mockMvc.perform(put("/catalog-items/7/limits/11")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"mode":"LIMITED","limitQuantity":200,"enforcement":"WARN",
                             "warnThreshold":90,"trialMode":"FULL"}
                            """)).andExpect(status().isOk());

            ArgumentCaptor<UpdateCatalogItemLimitCommand> command = ArgumentCaptor
                    .forClass(UpdateCatalogItemLimitCommand.class);
            verify(updateUseCase).execute(command.capture());
            assertThat(command.getValue().catalogItemId()).isEqualTo(7L);
            assertThat(command.getValue().id()).isEqualTo(11L);
        }

        /**
         * <strong>El desajuste responde 404, no 200.</strong> Es el comportamiento que
         * cierra la URL que mentía: el techo 11 existe, pero no cuelga del artículo 9,
         * y la carga acotada no lo encuentra. Devolver 200 aquí confirmaría una
         * operación distinta de la que la URL dice.
         */
        @Test
        @DisplayName("PUT del techo de un articulo por la ruta de otro responde 404")
        void put_por_la_ruta_de_otro_articulo_responde_404() throws Exception {
            when(updateUseCase.execute(any()))
                    .thenThrow(new CatalogItemLimitNotFoundException(11L));

            mockMvc.perform(put("/catalog-items/9/limits/11")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"mode":"LIMITED","limitQuantity":200,"enforcement":"WARN",
                             "warnThreshold":90,"trialMode":"FULL"}
                            """)).andExpect(status().isNotFound());

            ArgumentCaptor<UpdateCatalogItemLimitCommand> command = ArgumentCaptor
                    .forClass(UpdateCatalogItemLimitCommand.class);
            verify(updateUseCase).execute(command.capture());
            assertThat(command.getValue().catalogItemId()).isEqualTo(9L);
        }

        @Test
        @DisplayName("PUT sobre un techo que no existe responde 404")
        void put_inexistente_responde_404() throws Exception {
            when(updateUseCase.execute(any()))
                    .thenThrow(new CatalogItemLimitNotFoundException(99L));

            mockMvc.perform(put("/catalog-items/7/limits/99")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"mode":"FULL","enforcement":"BLOCK","warnThreshold":80,
                             "trialMode":"FULL"}
                            """)).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Autorizacion")
    class Autorizacion {

        @Test
        @DisplayName("los tres puertos exigen SYSTEM a secas: el techo de fabrica no tiene empresa")
        void los_tres_puertos_exigen_system() throws Exception {
            assertThat(CreateCatalogItemLimitUseCase.class
                    .getMethod("execute", CreateCatalogItemLimitCommand.class)
                    .getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('SYSTEM')");
            assertThat(UpdateCatalogItemLimitUseCase.class
                    .getMethod("execute", UpdateCatalogItemLimitCommand.class)
                    .getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('SYSTEM')");
            assertThat(
                    ListCatalogItemLimitsUseCase.class.getMethod("listByCatalogItemId", Long.class)
                            .getAnnotation(PreAuthorize.class).value())
                    .isEqualTo("hasRole('SYSTEM')");
        }

        /**
         * Acotar por el artículo <strong>no</strong> es filtrar por empresa —el
         * criterio de BE-29—, así que abrir este listado a un permiso de tenant
         * enseñaría el catálogo comercial entero a cualquier clínica.
         */
        @Test
        @DisplayName("ningun request declara companyId")
        void ningun_request_declara_company_id() {
            assertThat(CreateCatalogItemLimitRequest.class.getRecordComponents())
                    .extracting(RecordComponent::getName).doesNotContain("companyId");
            assertThat(UpdateCatalogItemLimitRequest.class.getRecordComponents())
                    .extracting(RecordComponent::getName).doesNotContain("companyId");
        }
    }
}
