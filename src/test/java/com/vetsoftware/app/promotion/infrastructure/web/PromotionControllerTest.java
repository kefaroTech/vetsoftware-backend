package com.vetsoftware.app.promotion.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.promotion.application.command.CreatePromotionCommand;
import com.vetsoftware.app.promotion.application.command.UpdatePromotionCommand;
import com.vetsoftware.app.promotion.application.dto.CompanySummaryDto;
import com.vetsoftware.app.promotion.application.dto.PromotionDto;
import com.vetsoftware.app.promotion.application.port.in.CreatePromotionUseCase;
import com.vetsoftware.app.promotion.application.port.in.DeletePromotionUseCase;
import com.vetsoftware.app.promotion.application.port.in.FindPromotionUseCase;
import com.vetsoftware.app.promotion.application.port.in.ListPromotionsUseCase;
import com.vetsoftware.app.promotion.application.port.in.ReactivatePromotionUseCase;
import com.vetsoftware.app.promotion.application.port.in.UpdatePromotionUseCase;
import com.vetsoftware.app.promotion.domain.ApplicationType;
import com.vetsoftware.app.promotion.domain.PromotionNotFoundException;
import com.vetsoftware.app.promotion.domain.PromotionStatus;
import com.vetsoftware.app.promotion.domain.PromotionType;
import com.vetsoftware.app.promotion.domain.ValueType;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rodaja HTTP de las promociones.
 *
 * <p>
 * Este controller es el unico del bloque que <b>traduce enums a mano</b>: los
 * cuatro campos de tipo llegan como texto libre y pasan por un
 * {@code Enum.valueOf} envuelto. Esa traduccion es la que se prueba aqui —que
 * acepta minusculas y espacios, y que un valor desconocido sale como 400 con el
 * nombre del campo, no como un 500 con un {@code IllegalArgumentException}
 * crudo—, mas el sello de empresa: el {@code companyId} no viaja en el cuerpo,
 * lo pone {@code Authz}.
 */
@WebMvcTest(PromotionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("PromotionController — contrato HTTP")
class PromotionControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;
    private static final Long PROMOTION_ID = 44L;

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 1, 1, 0, 0);
    private static final LocalDateTime FIN = LocalDateTime.of(2026, 1, 31, 23, 59);

    private static final String CUERPO_VALIDO = """
            {"name":"Enero perruno","promotionType":"DISCOUNT","applicationType":"CATEGORY",
             "applicationItem":3,"valueType":"PERCENTAGE","value":15.00,
             "startDate":"2026-01-01T00:00:00","endDate":"2026-01-31T23:59:00",
             "promotionStatus":"ACTIVE"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreatePromotionUseCase createUseCase;
    @MockitoBean
    private UpdatePromotionUseCase updateUseCase;
    @MockitoBean
    private FindPromotionUseCase findUseCase;
    @MockitoBean
    private ListPromotionsUseCase listUseCase;
    @MockitoBean
    private DeletePromotionUseCase deleteUseCase;
    @MockitoBean
    private ReactivatePromotionUseCase reactivateUseCase;

    private static PromotionDto eneroPerruno() {
        return new PromotionDto(PROMOTION_ID, "Enero perruno", PromotionType.DISCOUNT,
                ApplicationType.CATEGORY, 3L, ValueType.PERCENTAGE, new BigDecimal("15.00"), INICIO,
                FIN, PromotionStatus.ACTIVE,
                new CompanySummaryDto(COMPANY_ID, "Clinica Norte", "NIT-900"),
                LocalDateTime.of(2025, 12, 20, 9, 0), true);
    }

    private static CreatePromotionCommand comandoEsperado() {
        return new CreatePromotionCommand("Enero perruno", PromotionType.DISCOUNT,
                ApplicationType.CATEGORY, 3L, ValueType.PERCENTAGE, new BigDecimal("15.00"), INICIO,
                FIN, PromotionStatus.ACTIVE, COMPANY_ID);
    }

    @Nested
    @DisplayName("POST /promotions")
    class Creacion {

        @Test
        @DisplayName("responde 201 con la promocion creada y sus enums como texto")
        void responde_201() throws Exception {
            when(createUseCase.execute(any())).thenReturn(eneroPerruno());

            mockMvc.perform(post("/promotions").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(PROMOTION_ID))
                    .andExpect(jsonPath("$.promotionType").value("DISCOUNT"))
                    .andExpect(jsonPath("$.applicationType").value("CATEGORY"))
                    .andExpect(jsonPath("$.valueType").value("PERCENTAGE"))
                    .andExpect(jsonPath("$.promotionStatus").value("ACTIVE"))
                    .andExpect(jsonPath("$.company.identifier").value("NIT-900"))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("traduce el request al command tomando la empresa del contexto")
        void traduce_el_request_al_command() throws Exception {
            when(createUseCase.execute(any())).thenReturn(eneroPerruno());

            mockMvc.perform(post("/promotions").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO));

            verify(createUseCase).execute(comandoEsperado());
        }

        @Test
        @DisplayName("los tipos llegan normalizados: minusculas y espacios se aceptan igual")
        void normaliza_los_tipos() throws Exception {
            when(createUseCase.execute(any())).thenReturn(eneroPerruno());

            mockMvc.perform(post("/promotions").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"Enero perruno","promotionType":" discount ",
                     "applicationType":"category","applicationItem":3,
                     "valueType":"percentage","value":15.00,
                     "startDate":"2026-01-01T00:00:00","endDate":"2026-01-31T23:59:00",
                     "promotionStatus":" Active "}
                    """)).andExpect(status().isCreated());

            verify(createUseCase).execute(comandoEsperado());
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource({"\"promotionType\":\"DISCOUNT\", \"promotionType\":\"NO_EXISTE\"",
                "\"applicationType\":\"CATEGORY\", \"applicationType\":\"NO_EXISTE\"",
                "\"valueType\":\"PERCENTAGE\", \"valueType\":\"NO_EXISTE\"",
                "\"promotionStatus\":\"ACTIVE\", \"promotionStatus\":\"NO_EXISTE\""})
        @DisplayName("un tipo desconocido responde 400 y no crea nada")
        void tipo_desconocido_responde_400(String original, String invalido) throws Exception {
            // El controller envuelve el Enum.valueOf: sin ese catch, un enum invalido
            // saldria como un 500 y el front no sabria que campo corregir. Los cuatro
            // campos pasan por el mismo helper, y una rama nueva sin cubrir se veria aqui.
            mockMvc.perform(post("/promotions").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO.replace(original, invalido)))
                    .andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("sin nombre responde 400 y no llega al caso de uso")
        void sin_nombre_responde_400() throws Exception {
            mockMvc.perform(post("/promotions").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO.replace("\"name\":\"Enero perruno\"", "\"name\":\"\"")))
                    .andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("sin fecha de fin responde 400: una promocion sin vigencia no caduca nunca")
        void sin_fecha_de_fin_responde_400() throws Exception {
            mockMvc.perform(post("/promotions").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"Enero perruno","promotionType":"DISCOUNT",
                     "applicationType":"CATEGORY","applicationItem":3,
                     "valueType":"PERCENTAGE","value":15.00,
                     "startDate":"2026-01-01T00:00:00","promotionStatus":"ACTIVE"}
                    """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("con un valor negativo responde 400")
        void valor_negativo_responde_400() throws Exception {
            mockMvc.perform(post("/promotions").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO.replace("\"value\":15.00", "\"value\":-1")))
                    .andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("con un item de aplicacion no positivo responde 400")
        void item_no_positivo_responde_400() throws Exception {
            mockMvc.perform(post("/promotions").contentType(MediaType.APPLICATION_JSON).content(
                    CUERPO_VALIDO.replace("\"applicationItem\":3", "\"applicationItem\":0")))
                    .andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("una referencia inexistente rechazada por el dominio sale como 400")
        void referencia_inexistente_responde_400() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new IllegalArgumentException("ProductCategory not found: 3"));

            mockMvc.perform(post("/promotions").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("lecturas")
    class Lecturas {

        @Test
        @DisplayName("GET /promotions lista solo las de la empresa del contexto")
        void get_lista() throws Exception {
            when(listUseCase.listByCompany(COMPANY_ID)).thenReturn(List.of(eneroPerruno()));

            // El stub esta atado a COMPANY_ID: si el controller no sellara la empresa,
            // el doble devolveria null y la respuesta no seria esta lista.
            mockMvc.perform(get("/promotions")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].company.id").value(COMPANY_ID));
        }

        @Test
        @DisplayName("GET /promotions/{id} devuelve la promocion con su vigencia")
        void get_por_id() throws Exception {
            when(findUseCase.findById(PROMOTION_ID, COMPANY_ID)).thenReturn(eneroPerruno());

            mockMvc.perform(get("/promotions/44")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Enero perruno"))
                    .andExpect(jsonPath("$.value").value(15.00));
        }

        @Test
        @DisplayName("GET /promotions/{id} de otra empresa responde 404, no 500")
        void get_inexistente_responde_404() throws Exception {
            when(findUseCase.findById(99L, COMPANY_ID))
                    .thenThrow(new PromotionNotFoundException(99L));

            mockMvc.perform(get("/promotions/99")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("escrituras sobre una promocion existente")
    class Escrituras {

        @Test
        @DisplayName("PUT /promotions/{id} responde 200 y arma el command con el id de la ruta")
        void put_responde_200() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(eneroPerruno());

            mockMvc.perform(put("/promotions/44").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(PROMOTION_ID));

            verify(updateUseCase).execute(new UpdatePromotionCommand(PROMOTION_ID, "Enero perruno",
                    PromotionType.DISCOUNT, ApplicationType.CATEGORY, 3L, ValueType.PERCENTAGE,
                    new BigDecimal("15.00"), INICIO, FIN, PromotionStatus.ACTIVE, COMPANY_ID));
        }

        @Test
        @DisplayName("PUT con un tipo desconocido responde 400 y no actualiza")
        void put_con_tipo_desconocido_responde_400() throws Exception {
            mockMvc.perform(
                    put("/promotions/44").contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO.replace("\"valueType\":\"PERCENTAGE\"",
                                    "\"valueType\":\"REGALO\"")))
                    .andExpect(status().isBadRequest());

            verify(updateUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("PUT de una promocion inexistente responde 404")
        void put_inexistente_responde_404() throws Exception {
            when(updateUseCase.execute(any())).thenThrow(new PromotionNotFoundException(44L));

            mockMvc.perform(put("/promotions/44").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("DELETE /promotions/{id} responde 204 sin cuerpo")
        void delete_responde_204() throws Exception {
            mockMvc.perform(delete("/promotions/44")).andExpect(status().isNoContent());

            // OJO: este endpoint no pasa la empresa al caso de uso —el gate es solo
            // hasAuthority('promotion.delete')—, asi que el aislamiento entre tenants
            // depende por completo del service. Se afirma tal cual esta hoy.
            verify(deleteUseCase).execute(PROMOTION_ID);
        }

        @Test
        @DisplayName("DELETE de una promocion inexistente responde 404")
        void delete_inexistente_responde_404() throws Exception {
            doThrow(new PromotionNotFoundException(99L)).when(deleteUseCase).execute(99L);

            mockMvc.perform(delete("/promotions/99")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("PATCH /promotions/{id}/enable reactiva y responde 200")
        void patch_enable_responde_200() throws Exception {
            when(reactivateUseCase.execute(PROMOTION_ID)).thenReturn(eneroPerruno());

            mockMvc.perform(patch("/promotions/44/enable")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("PATCH enable de una promocion inexistente responde 404")
        void patch_enable_inexistente_responde_404() throws Exception {
            when(reactivateUseCase.execute(99L)).thenThrow(new PromotionNotFoundException(99L));

            mockMvc.perform(patch("/promotions/99/enable")).andExpect(status().isNotFound());
        }
    }
}
