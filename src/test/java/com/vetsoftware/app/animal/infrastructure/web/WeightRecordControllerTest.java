package com.vetsoftware.app.animal.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.animal.application.command.CreateWeightRecordCommand;
import com.vetsoftware.app.animal.application.dto.WeightRecordDto;
import com.vetsoftware.app.animal.application.port.in.CreateWeightRecordUseCase;
import com.vetsoftware.app.animal.application.port.in.DeleteWeightRecordUseCase;
import com.vetsoftware.app.animal.application.port.in.FindLatestWeightRecordUseCase;
import com.vetsoftware.app.animal.application.port.in.ListWeightRecordsByAnimalUseCase;
import com.vetsoftware.app.animal.domain.AnimalNotFoundException;
import com.vetsoftware.app.animal.domain.WeightRecordNotFoundException;
import com.vetsoftware.app.animal.domain.WeightType;
import com.vetsoftware.app.animal.testsupport.AnimalMother;
import com.vetsoftware.app.animal.testsupport.WeightRecordMother;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rodaja HTTP de {@link WeightRecordController}: rutas anidadas bajo
 * {@code /animals/{animalId}/weight-records}, binding, validacion y codigos de
 * estado. Lo que hay debajo son dobles.
 *
 * <p>
 * Lo que esta rodaja protege: que la empresa la sella {@code Authz} y nunca el
 * cuerpo, y que el {@code animalId} de la ruta llega intacto al command (una
 * serie de peso mal enrutada mezclaria el historial de dos mascotas).
 */
@WebMvcTest(WeightRecordController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("WeightRecordController — contrato HTTP")
class WeightRecordControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;
    private static final Long ANIMAL_ID = AnimalMother.ANIMAL_ID;

    private static final String REGISTRO_VALIDO = """
            {"value":12.50,"unit":"KILOGRAMS","measuredAt":"2026-02-01","note":"control de rutina"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateWeightRecordUseCase createUseCase;
    @MockitoBean
    private ListWeightRecordsByAnimalUseCase listUseCase;
    @MockitoBean
    private FindLatestWeightRecordUseCase findLatestUseCase;
    @MockitoBean
    private DeleteWeightRecordUseCase deleteUseCase;

    private static WeightRecordDto manual() {
        return WeightRecordDto.from(WeightRecordMother.manual());
    }

    @Nested
    @DisplayName("POST /animals/{animalId}/weight-records")
    class Creacion {

        @Test
        @DisplayName("responde 201 con el registro creado")
        void responde_201() throws Exception {
            when(createUseCase.execute(any())).thenReturn(manual());

            mockMvc.perform(post("/animals/" + ANIMAL_ID + "/weight-records")
                    .contentType(MediaType.APPLICATION_JSON).content(REGISTRO_VALIDO))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(WeightRecordMother.RECORD_ID))
                    .andExpect(jsonPath("$.animalId").value(AnimalMother.ANIMAL_ID))
                    .andExpect(jsonPath("$.value").value(12.50))
                    .andExpect(jsonPath("$.source").value("MANUAL"));
        }

        @Test
        @DisplayName("el animalId sale de la ruta y la empresa del contexto")
        void el_animal_id_sale_de_la_ruta_y_la_empresa_del_contexto() throws Exception {
            when(createUseCase.execute(any())).thenReturn(manual());

            mockMvc.perform(post("/animals/" + ANIMAL_ID + "/weight-records")
                    .contentType(MediaType.APPLICATION_JSON).content(REGISTRO_VALIDO));

            verify(createUseCase).execute(new CreateWeightRecordCommand(ANIMAL_ID,
                    new BigDecimal("12.50"), WeightType.KILOGRAMS, LocalDate.of(2026, 2, 1),
                    "control de rutina", COMPANY_ID));
        }

        @Test
        @DisplayName("un valor negativo responde 400 y no crea nada")
        void un_valor_negativo_responde_400() throws Exception {
            mockMvc.perform(post("/animals/" + ANIMAL_ID + "/weight-records")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"value":-1,"unit":"KILOGRAMS","measuredAt":"2026-02-01"}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("sin valor responde 400 y no crea nada")
        void sin_valor_responde_400() throws Exception {
            mockMvc.perform(post("/animals/" + ANIMAL_ID + "/weight-records")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"unit":"KILOGRAMS","measuredAt":"2026-02-01"}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("un animal inexistente responde 404, no 500")
        void un_animal_inexistente_responde_404() throws Exception {
            when(createUseCase.execute(any())).thenThrow(new AnimalNotFoundException(999L));

            mockMvc.perform(post("/animals/999/weight-records")
                    .contentType(MediaType.APPLICATION_JSON).content(REGISTRO_VALIDO))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /animals/{animalId}/weight-records")
    class Listado {

        @Test
        @DisplayName("devuelve la serie temporal del animal")
        void devuelve_la_serie_temporal() throws Exception {
            when(listUseCase.listByAnimal(ANIMAL_ID, COMPANY_ID)).thenReturn(List.of(manual()));

            mockMvc.perform(get("/animals/" + ANIMAL_ID + "/weight-records"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].value").value(12.50));
        }

        @Test
        @DisplayName("sin registros devuelve una lista vacia")
        void sin_registros_devuelve_lista_vacia() throws Exception {
            when(listUseCase.listByAnimal(ANIMAL_ID, COMPANY_ID)).thenReturn(List.of());

            mockMvc.perform(get("/animals/" + ANIMAL_ID + "/weight-records"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /animals/{animalId}/weight-records/latest")
    class UltimoRegistro {

        @Test
        @DisplayName("devuelve el ultimo registro de peso")
        void devuelve_el_ultimo_registro() throws Exception {
            when(findLatestUseCase.findLatest(ANIMAL_ID, COMPANY_ID)).thenReturn(manual());

            mockMvc.perform(get("/animals/" + ANIMAL_ID + "/weight-records/latest"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(WeightRecordMother.RECORD_ID));
        }

        @Test
        @DisplayName("un animal sin registros de peso responde 404, no 500")
        void un_animal_sin_registros_responde_404() throws Exception {
            when(findLatestUseCase.findLatest(ANIMAL_ID, COMPANY_ID))
                    .thenThrow(new WeightRecordNotFoundException(ANIMAL_ID));

            mockMvc.perform(get("/animals/" + ANIMAL_ID + "/weight-records/latest"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /animals/{animalId}/weight-records/{id}")
    class Borrado {

        @Test
        @DisplayName("responde 204 sin cuerpo")
        void responde_204() throws Exception {
            mockMvc.perform(delete(
                    "/animals/" + ANIMAL_ID + "/weight-records/" + WeightRecordMother.RECORD_ID))
                    .andExpect(status().isNoContent());

            verify(deleteUseCase).execute(WeightRecordMother.RECORD_ID, ANIMAL_ID, COMPANY_ID);
        }

        @Test
        @DisplayName("borrar un registro inexistente responde 404")
        void borrar_un_registro_inexistente_responde_404() throws Exception {
            org.mockito.Mockito.doThrow(new WeightRecordNotFoundException(999L)).when(deleteUseCase)
                    .execute(999L, ANIMAL_ID, COMPANY_ID);

            mockMvc.perform(delete("/animals/" + ANIMAL_ID + "/weight-records/999"))
                    .andExpect(status().isNotFound());
        }
    }
}
