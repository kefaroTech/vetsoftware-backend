package com.vetsoftware.app.surgerytype.infrastructure.web;

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

import com.vetsoftware.app.surgerytype.application.command.CreateSurgeryTypeCommand;
import com.vetsoftware.app.surgerytype.application.command.UpdateSurgeryTypeCommand;
import com.vetsoftware.app.surgerytype.application.dto.CompanySummaryDto;
import com.vetsoftware.app.surgerytype.application.dto.SurgeryTypeDto;
import com.vetsoftware.app.surgerytype.application.port.in.CreateSurgeryTypeUseCase;
import com.vetsoftware.app.surgerytype.application.port.in.DeleteSurgeryTypeUseCase;
import com.vetsoftware.app.surgerytype.application.port.in.FindSurgeryTypeUseCase;
import com.vetsoftware.app.surgerytype.application.port.in.ListAvailableSurgeryTypesUseCase;
import com.vetsoftware.app.surgerytype.application.port.in.ListSurgeryTypesUseCase;
import com.vetsoftware.app.surgerytype.application.port.in.ReactivateSurgeryTypeUseCase;
import com.vetsoftware.app.surgerytype.application.port.in.UpdateSurgeryTypeUseCase;
import com.vetsoftware.app.surgerytype.domain.SurgeryTypeHasActiveChildrenException;
import com.vetsoftware.app.surgerytype.domain.SurgeryTypeNotFoundException;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDateTime;
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
 * Rodaja HTTP de {@link SurgeryTypeController}.
 *
 * <p>
 * Lo que decide el controller por su cuenta y que por eso se afirma aqui: la
 * empresa nunca viaja en el cuerpo de la peticion —la pone {@code Authz}— ni en
 * create/update ni en borrado/reactivacion. Los cuatro casos de uso reciben el
 * companyId del contexto y lo revalidan con {@code @authz.isMyCompany(...)}
 * (ver {@code DeleteSurgeryTypeUseCase}/{@code ReactivateSurgeryTypeUseCase}).
 */
@WebMvcTest(SurgeryTypeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SurgeryTypeController — contrato HTTP")
class SurgeryTypeControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;
    private static final Long SURGERY_TYPE_ID = 700L;

    private static final String CUERPO_VALIDO = """
            {"name":"Castracion","description":"Cirugia de esterilizacion","general":false}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateSurgeryTypeUseCase createUseCase;
    @MockitoBean
    private UpdateSurgeryTypeUseCase updateUseCase;
    @MockitoBean
    private FindSurgeryTypeUseCase findUseCase;
    @MockitoBean
    private ListSurgeryTypesUseCase listUseCase;
    @MockitoBean
    private ListAvailableSurgeryTypesUseCase listAvailableUseCase;
    @MockitoBean
    private DeleteSurgeryTypeUseCase deleteUseCase;
    @MockitoBean
    private ReactivateSurgeryTypeUseCase reactivateUseCase;

    /**
     * El doble de {@code Authz} lo aporta {@link WebMvcSliceConfig}; se inyecta
     * aqui para afirmar que el borrado propaga la empresa del contexto, que es la
     * mitad del arreglo de aislamiento (la otra mitad vive en el service).
     */
    @Autowired
    private com.vetsoftware.app.auth.infrastructure.security.Authz authz;

    private static SurgeryTypeDto tipo(boolean general) {
        return new SurgeryTypeDto(SURGERY_TYPE_ID, "Castracion", "Cirugia de esterilizacion",
                general ? null : new CompanySummaryDto(COMPANY_ID, "Clinica Norte", "900123456"),
                general, LocalDateTime.of(2026, 3, 12, 9, 15), true);
    }

    private static SurgeryTypeDto propio() {
        return tipo(false);
    }

    @Nested
    @DisplayName("POST /surgery-types")
    class Creacion {

        @Test
        @DisplayName("responde 201 con el tipo creado")
        void responde_201_con_el_tipo_creado() throws Exception {
            when(createUseCase.execute(any())).thenReturn(propio());

            mockMvc.perform(post("/surgery-types").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(700))
                    .andExpect(jsonPath("$.name").value("Castracion"))
                    .andExpect(jsonPath("$.company.identifier").value("900123456"))
                    .andExpect(jsonPath("$.general").value(false))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("traduce el request al command tomando la empresa del contexto")
        void traduce_el_request_al_command() throws Exception {
            when(createUseCase.execute(any())).thenReturn(propio());

            mockMvc.perform(post("/surgery-types").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO));

            // El cuerpo no lleva companyId: si el controller lo aceptara del cliente,
            // cualquiera podria crear un tipo en otra empresa.
            verify(createUseCase).execute(new CreateSurgeryTypeCommand("Castracion",
                    "Cirugia de esterilizacion", COMPANY_ID, false));
        }

        @Test
        @DisplayName("un tipo general responde con company null en el JSON")
        void un_tipo_general_responde_con_company_null() throws Exception {
            when(createUseCase.execute(any())).thenReturn(tipo(true));

            mockMvc.perform(post("/surgery-types").contentType(MediaType.APPLICATION_JSON).content(
                    """
                            {"name":"Cirugia general","description":"Procedimiento estandar","general":true}
                            """))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.company").doesNotExist())
                    .andExpect(jsonPath("$.general").value(true));
        }

        @Test
        @DisplayName("un nombre en blanco responde 400 y no llega al caso de uso")
        void nombre_en_blanco_responde_400() throws Exception {
            mockMvc.perform(
                    post("/surgery-types").contentType(MediaType.APPLICATION_JSON).content("""
                            {"name":"","description":"desc","general":false}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("una empresa inexistente sale como 400, no 500")
        void empresa_inexistente_responde_400() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new IllegalArgumentException("Company not found: " + COMPANY_ID));

            mockMvc.perform(post("/surgery-types").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("lecturas")
    class Lecturas {

        @Test
        @DisplayName("GET /surgery-types lista TODOS los tipos, sin acotar por empresa (SYSTEM)")
        void get_lista_todos_los_tipos() throws Exception {
            when(listUseCase.listAll()).thenReturn(List.of(propio()));

            mockMvc.perform(get("/surgery-types")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(700));
        }

        @Test
        @DisplayName("GET /surgery-types/available lista los disponibles para la empresa del contexto")
        void get_available_lista_los_disponibles_de_la_empresa() throws Exception {
            when(listAvailableUseCase.listAvailable(COMPANY_ID)).thenReturn(List.of(propio()));

            mockMvc.perform(get("/surgery-types/available")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(700));

            verify(listAvailableUseCase).listAvailable(COMPANY_ID);
        }

        @Test
        @DisplayName("GET /surgery-types/{id} devuelve el recurso disponible para la empresa")
        void get_por_id_devuelve_el_recurso() throws Exception {
            when(findUseCase.findById(SURGERY_TYPE_ID, COMPANY_ID)).thenReturn(propio());

            mockMvc.perform(get("/surgery-types/700")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(700));
        }

        @Test
        @DisplayName("GET /surgery-types/{id} no disponible para la empresa responde 404, no 500")
        void get_por_id_inexistente_responde_404() throws Exception {
            when(findUseCase.findById(99L, COMPANY_ID))
                    .thenThrow(new SurgeryTypeNotFoundException(99L));

            mockMvc.perform(get("/surgery-types/99")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("escrituras sobre un tipo existente")
    class Escrituras {

        @Test
        @DisplayName("PUT /surgery-types/{id} responde 200 con el recurso actualizado")
        void put_responde_200() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(propio());

            mockMvc.perform(put("/surgery-types/700").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(700));
        }

        @Test
        @DisplayName("PUT traduce el request al command con el id de la ruta y la empresa del contexto")
        void put_traduce_el_request_con_el_id_de_la_ruta() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(propio());

            mockMvc.perform(put("/surgery-types/700").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO));

            verify(updateUseCase).execute(new UpdateSurgeryTypeCommand(SURGERY_TYPE_ID,
                    "Castracion", "Cirugia de esterilizacion", COMPANY_ID, false));
        }

        @Test
        @DisplayName("PUT sobre un tipo que no existe responde 404")
        void put_inexistente_responde_404() throws Exception {
            when(updateUseCase.execute(any()))
                    .thenThrow(new SurgeryTypeNotFoundException(SURGERY_TYPE_ID));

            mockMvc.perform(put("/surgery-types/700").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("DELETE /surgery-types/{id} responde 204 y propaga la empresa del contexto")
        void delete_responde_204() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(COMPANY_ID);

            mockMvc.perform(delete("/surgery-types/700")).andExpect(status().isNoContent());

            // Igual que create/update: la empresa la pone el controller desde el
            // contexto, nunca el cliente. Es lo que permite al service acotar la lectura
            // previa al borrado.
            verify(deleteUseCase).execute(SURGERY_TYPE_ID, COMPANY_ID);
        }

        @Test
        @DisplayName("DELETE de un tipo inexistente responde 404")
        void delete_inexistente_responde_404() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(COMPANY_ID);
            doThrow(new SurgeryTypeNotFoundException(99L)).when(deleteUseCase).execute(99L,
                    COMPANY_ID);

            mockMvc.perform(delete("/surgery-types/99")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("DELETE de un tipo con cirugias activas responde 409")
        void delete_con_hijos_activos_responde_409() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(COMPANY_ID);
            doThrow(new SurgeryTypeHasActiveChildrenException(SURGERY_TYPE_ID, "surgery"))
                    .when(deleteUseCase).execute(SURGERY_TYPE_ID, COMPANY_ID);

            mockMvc.perform(delete("/surgery-types/700")).andExpect(status().isConflict());
        }

        @Test
        @DisplayName("PATCH /surgery-types/{id}/enable responde 200 y propaga la empresa del contexto")
        void enable_responde_200() throws Exception {
            when(reactivateUseCase.execute(SURGERY_TYPE_ID, COMPANY_ID)).thenReturn(propio());

            mockMvc.perform(patch("/surgery-types/700/enable")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled").value(true));

            verify(reactivateUseCase).execute(SURGERY_TYPE_ID, COMPANY_ID);
        }

        @Test
        @DisplayName("PATCH enable de un tipo inexistente responde 404")
        void enable_inexistente_responde_404() throws Exception {
            when(reactivateUseCase.execute(99L, COMPANY_ID))
                    .thenThrow(new SurgeryTypeNotFoundException(99L));

            mockMvc.perform(patch("/surgery-types/99/enable")).andExpect(status().isNotFound());
        }
    }
}
