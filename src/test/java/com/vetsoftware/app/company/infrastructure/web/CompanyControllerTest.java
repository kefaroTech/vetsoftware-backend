package com.vetsoftware.app.company.infrastructure.web;

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

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.company.application.command.CreateCompanyCommand;
import com.vetsoftware.app.company.application.command.UpdateCompanyCommand;
import com.vetsoftware.app.company.application.dto.CitySummaryDto;
import com.vetsoftware.app.company.application.dto.CompanyDto;
import com.vetsoftware.app.company.application.dto.MembershipSummaryDto;
import com.vetsoftware.app.company.application.port.in.CreateCompanyUseCase;
import com.vetsoftware.app.company.application.port.in.DeleteCompanyUseCase;
import com.vetsoftware.app.company.application.port.in.FindCompanyUseCase;
import com.vetsoftware.app.company.application.port.in.ListCompaniesUseCase;
import com.vetsoftware.app.company.application.port.in.ReactivateCompanyUseCase;
import com.vetsoftware.app.company.application.port.in.UpdateCompanyUseCase;
import com.vetsoftware.app.company.domain.CompanyHasActiveChildrenException;
import com.vetsoftware.app.company.domain.CompanyNotFoundException;
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
 * Rodaja HTTP del controller de empresas: rutas, binding, validacion del
 * request, codigos de estado y forma del JSON (incluidos los companion summary
 * de ciudad y membresia). Lo que hay debajo son dobles; la autorizacion de
 * {@code @PreAuthorize} (SYSTEM o la authority puntual) vive en los puertos de
 * entrada y se prueba aparte, no en esta rodaja con seguridad deshabilitada.
 */
@WebMvcTest(CompanyController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("CompanyController — contrato HTTP")
class CompanyControllerTest {

    private static final String CUERPO_VALIDO = """
            {"name":"Clinica Norte","identifier":"NIT-900","address":"Calle 123 #45-67","contactNumber":"3001234567","cityId":11,"membershipId":21}
            """;

    @Autowired
    private MockMvc mockMvc;

    /**
     * El doble que {@link WebMvcSliceConfig} publica: aqui se re-stubea para
     * distinguir los dos actores de {@code GET /companies} —el principal de
     * plataforma, que no tiene empresa, y el empleado, que si—.
     */
    @Autowired
    private Authz authz;

    @MockitoBean
    private CreateCompanyUseCase createUseCase;
    @MockitoBean
    private UpdateCompanyUseCase updateUseCase;
    @MockitoBean
    private FindCompanyUseCase findUseCase;
    @MockitoBean
    private ListCompaniesUseCase listUseCase;
    @MockitoBean
    private DeleteCompanyUseCase deleteUseCase;
    @MockitoBean
    private ReactivateCompanyUseCase reactivateUseCase;

    private static CompanyDto clinicaNorte() {
        return new CompanyDto(9L, "Clinica Norte", "NIT-900", "Calle 123 #45-67", "3001234567",
                new CitySummaryDto(11L, "Bogota"),
                new MembershipSummaryDto(21L, "Premium", "ACTIVE"),
                LocalDateTime.of(2026, 1, 15, 10, 30), true);
    }

    private static CreateCompanyCommand comandoDeCreacionEsperado() {
        return new CreateCompanyCommand("Clinica Norte", "NIT-900", "Calle 123 #45-67",
                "3001234567", 11L, 21L);
    }

    @Nested
    @DisplayName("POST /companies")
    class Creacion {

        @Test
        @DisplayName("responde 201 con el recurso creado, incluidos los companion de ciudad y membresia")
        void responde_201_con_el_recurso_creado() throws Exception {
            when(createUseCase.execute(any())).thenReturn(clinicaNorte());

            mockMvc.perform(post("/companies").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(9))
                    .andExpect(jsonPath("$.name").value("Clinica Norte"))
                    .andExpect(jsonPath("$.city.id").value(11))
                    .andExpect(jsonPath("$.city.name").value("Bogota"))
                    .andExpect(jsonPath("$.membership.id").value(21))
                    .andExpect(jsonPath("$.membership.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("traduce el request al command")
        void traduce_el_request_al_command() throws Exception {
            when(createUseCase.execute(any())).thenReturn(clinicaNorte());

            mockMvc.perform(post("/companies").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO));

            verify(createUseCase).execute(comandoDeCreacionEsperado());
        }

        @Test
        @DisplayName("nombre en blanco responde 400 y no llega al caso de uso")
        void nombre_en_blanco_responde_400() throws Exception {
            mockMvc.perform(post("/companies").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"","identifier":"NIT-900","cityId":11,"membershipId":21}
                    """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("identificador de mas de 50 caracteres responde 400")
        void identificador_demasiado_largo_responde_400() throws Exception {
            mockMvc.perform(post("/companies").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Clinica Norte\",\"identifier\":\"" + "x".repeat(51)
                            + "\",\"cityId\":11,\"membershipId\":21}"))
                    .andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("cityId nulo responde 400")
        void city_id_nulo_responde_400() throws Exception {
            mockMvc.perform(post("/companies").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"Clinica Norte","identifier":"NIT-900","membershipId":21}
                    """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("membershipId nulo responde 400")
        void membership_id_nulo_responde_400() throws Exception {
            mockMvc.perform(post("/companies").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"Clinica Norte","identifier":"NIT-900","cityId":11}
                    """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("una ciudad inexistente sale como 400, no como 500")
        void ciudad_inexistente_responde_400() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new IllegalArgumentException("City not found: 11"));

            mockMvc.perform(post("/companies").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("lecturas")
    class Lecturas {

        @Test
        @DisplayName("GET /companies lista todas las empresas para un principal de plataforma")
        void get_lista_todas_las_empresas() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(null);
            when(listUseCase.listAll(null)).thenReturn(List.of(clinicaNorte()));

            mockMvc.perform(get("/companies")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(9));
        }

        /**
         * El alcance no viaja en la peticion ni se puede falsear desde fuera: el
         * controller lo toma de {@code currentCompanyIdOrNull()}. Este test fija
         * justamente eso —que el {@code companyId} que llega al puerto es el del
         * principal—, que es lo que impedia que un empleado de una veterinaria listara
         * el registro de todas.
         */
        @Test
        @DisplayName("GET /companies acota al principal: pasa su companyId, no null")
        void get_acota_al_company_id_del_principal() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(9L);
            when(listUseCase.listAll(9L)).thenReturn(List.of(clinicaNorte()));

            mockMvc.perform(get("/companies")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(9))
                    .andExpect(jsonPath("$[1]").doesNotExist());

            verify(listUseCase).listAll(9L);
            verify(listUseCase, never()).listAll(null);
        }

        @Test
        @DisplayName("GET /companies/{id} devuelve el recurso")
        void get_por_id_devuelve_el_recurso() throws Exception {
            when(findUseCase.findById(9L)).thenReturn(clinicaNorte());

            mockMvc.perform(get("/companies/9")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(9))
                    .andExpect(jsonPath("$.identifier").value("NIT-900"));
        }

        @Test
        @DisplayName("GET /companies/{id} inexistente responde 404, no 500")
        void get_por_id_inexistente_responde_404() throws Exception {
            when(findUseCase.findById(99L)).thenThrow(new CompanyNotFoundException(99L));

            mockMvc.perform(get("/companies/99")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("escrituras sobre una empresa existente")
    class Escrituras {

        @Test
        @DisplayName("PUT /companies/{id} responde 200 con el recurso actualizado")
        void put_responde_200() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(clinicaNorte());

            mockMvc.perform(put("/companies/9").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(9));
        }

        @Test
        @DisplayName("PUT traduce el request al command con el id de la ruta")
        void put_traduce_el_request_al_command() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(clinicaNorte());

            mockMvc.perform(put("/companies/9").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO));

            verify(updateUseCase).execute(new UpdateCompanyCommand(9L, "Clinica Norte", "NIT-900",
                    "Calle 123 #45-67", "3001234567", 11L, 21L));
        }

        @Test
        @DisplayName("PUT sobre una empresa inexistente responde 404")
        void put_de_empresa_inexistente_responde_404() throws Exception {
            when(updateUseCase.execute(any())).thenThrow(new CompanyNotFoundException(9L));

            mockMvc.perform(put("/companies/9").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("DELETE /companies/{id} responde 204 sin cuerpo")
        void delete_responde_204() throws Exception {
            mockMvc.perform(delete("/companies/9")).andExpect(status().isNoContent());

            verify(deleteUseCase).execute(9L);
        }

        @Test
        @DisplayName("DELETE de una empresa inexistente responde 404")
        void delete_inexistente_responde_404() throws Exception {
            doThrow(new CompanyNotFoundException(99L)).when(deleteUseCase).execute(99L);

            mockMvc.perform(delete("/companies/99")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("DELETE de una empresa con hijos activos responde 409")
        void delete_con_hijos_activos_responde_409() throws Exception {
            doThrow(new CompanyHasActiveChildrenException(9L, "animal")).when(deleteUseCase)
                    .execute(9L);

            mockMvc.perform(delete("/companies/9")).andExpect(status().isConflict());
        }

        @Test
        @DisplayName("PATCH /companies/{id}/enable reactiva y responde 200")
        void patch_enable_responde_200() throws Exception {
            when(reactivateUseCase.execute(9L)).thenReturn(clinicaNorte());

            mockMvc.perform(patch("/companies/9/enable")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(9))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("PATCH enable de una empresa inexistente responde 404")
        void patch_enable_inexistente_responde_404() throws Exception {
            when(reactivateUseCase.execute(99L)).thenThrow(new CompanyNotFoundException(99L));

            mockMvc.perform(patch("/companies/99/enable")).andExpect(status().isNotFound());
        }
    }
}
