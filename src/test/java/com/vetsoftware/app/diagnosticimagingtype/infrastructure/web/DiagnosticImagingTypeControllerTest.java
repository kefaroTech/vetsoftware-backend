package com.vetsoftware.app.diagnosticimagingtype.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.diagnosticimagingtype.application.command.CreateDiagnosticImagingTypeCommand;
import com.vetsoftware.app.diagnosticimagingtype.application.command.UpdateDiagnosticImagingTypeCommand;
import com.vetsoftware.app.diagnosticimagingtype.application.dto.CompanySummaryDto;
import com.vetsoftware.app.diagnosticimagingtype.application.dto.DiagnosticImagingTypeDto;
import com.vetsoftware.app.diagnosticimagingtype.application.port.in.CreateDiagnosticImagingTypeUseCase;
import com.vetsoftware.app.diagnosticimagingtype.application.port.in.DeleteDiagnosticImagingTypeUseCase;
import com.vetsoftware.app.diagnosticimagingtype.application.port.in.FindDiagnosticImagingTypeUseCase;
import com.vetsoftware.app.diagnosticimagingtype.application.port.in.ListAvailableDiagnosticImagingTypesUseCase;
import com.vetsoftware.app.diagnosticimagingtype.application.port.in.ListDiagnosticImagingTypesUseCase;
import com.vetsoftware.app.diagnosticimagingtype.application.port.in.UpdateDiagnosticImagingTypeUseCase;
import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingTypeNameAlreadyExistsException;
import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingTypeNotFoundException;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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
 * Rodaja HTTP del controller: rutas, binding, validacion del request, codigos
 * de estado y forma del JSON. Lo que hay debajo son dobles.
 *
 * <p>
 * La comprobacion que mas vale de todas es que el {@code companyId} del command
 * sale del contexto de autorizacion y no del cuerpo: el request no tiene donde
 * declararlo, y si algun dia lo tuviera este test lo delataria.
 */
@WebMvcTest(DiagnosticImagingTypeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("DiagnosticImagingTypeController — contrato HTTP")
class DiagnosticImagingTypeControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;

    private static final String CUERPO_VALIDO = """
            {"name":"Ecografia abdominal","description":"Ecografia de rutina","general":false}
            """;

    private static final String CUERPO_GENERAL = """
            {"name":"Radiografia","description":"Radiografia simple","general":true}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateDiagnosticImagingTypeUseCase createUseCase;
    @MockitoBean
    private UpdateDiagnosticImagingTypeUseCase updateUseCase;
    @MockitoBean
    private FindDiagnosticImagingTypeUseCase findUseCase;
    @MockitoBean
    private ListDiagnosticImagingTypesUseCase listUseCase;
    @MockitoBean
    private ListAvailableDiagnosticImagingTypesUseCase listAvailableUseCase;
    @MockitoBean
    private DeleteDiagnosticImagingTypeUseCase deleteUseCase;

    /**
     * El doble de {@code Authz} lo aporta {@link WebMvcSliceConfig}, que ya stubea
     * {@code currentCompanyId()} para las lecturas; se inyecta aqui para poder
     * mover la empresa del contexto en los casos que lo necesitan.
     */
    @Autowired
    private Authz authz;

    /**
     * {@code WebMvcSliceConfig} NO stubea {@code currentCompanyIdOrNull()} —lo
     * comparten 92 rodajas y varias dependen de que devuelva {@code null}—, asi que
     * la empresa del contexto para las ESCRITURAS se pone aqui. Desde el arreglo de
     * #565 el {@code create} y el {@code update} leen esa segunda, igual que ya
     * hacia el {@code delete}: sin este stub el command llegaria con
     * {@code companyId} nulo y el tipo caeria en el catalogo de plataforma en vez
     * de en la veterinaria.
     */
    @BeforeEach
    void empresaDelContexto() {
        when(authz.currentCompanyIdOrNull()).thenReturn(COMPANY_ID);
    }

    private static DiagnosticImagingTypeDto tipoDeEmpresa() {
        return new DiagnosticImagingTypeDto(501L, "Ecografia abdominal", "Ecografia de rutina",
                new CompanySummaryDto(COMPANY_ID, "Clinica Norte", "900123456"), false,
                LocalDateTime.of(2026, 1, 15, 10, 0), true);
    }

    private static DiagnosticImagingTypeDto tipoGeneral() {
        return new DiagnosticImagingTypeDto(502L, "Radiografia", "Radiografia simple", null, true,
                LocalDateTime.of(2026, 1, 15, 10, 0), true);
    }

    @Nested
    @DisplayName("POST /diagnostic-imaging-types")
    class Creacion {

        @Test
        @DisplayName("responde 201 con el recurso creado")
        void post_responde_201() throws Exception {
            when(createUseCase.execute(any())).thenReturn(tipoDeEmpresa());

            mockMvc.perform(post("/diagnostic-imaging-types")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(501))
                    .andExpect(jsonPath("$.company.id").value(COMPANY_ID))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("traduce el request al command con la empresa del contexto, no del cuerpo")
        void post_traduce_el_request_al_command_con_la_empresa_del_contexto() throws Exception {
            when(createUseCase.execute(any())).thenReturn(tipoDeEmpresa());

            mockMvc.perform(post("/diagnostic-imaging-types")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO));

            verify(createUseCase).execute(new CreateDiagnosticImagingTypeCommand(
                    "Ecografia abdominal", "Ecografia de rutina", COMPANY_ID, false));
        }

        @Test
        @DisplayName("un principal de plataforma crea un tipo global: el command va sin empresa y con general")
        void un_principal_de_plataforma_crea_un_tipo_global() throws Exception {
            // El arreglo de #565. Con currentCompanyId() ningun actor podia crear un
            // tipo global: al principal de plataforma le saltaba un AccessDeniedException
            // sin contexto y al empleado le colaban SU empresa, que choca contra el XOR
            // del dominio en cuanto general = true.
            when(authz.currentCompanyIdOrNull()).thenReturn(null);
            when(createUseCase.execute(any())).thenReturn(tipoGeneral());

            mockMvc.perform(post("/diagnostic-imaging-types")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_GENERAL))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.company").doesNotExist());

            verify(createUseCase).execute(new CreateDiagnosticImagingTypeCommand("Radiografia",
                    "Radiografia simple", null, true));
        }

        @Test
        @DisplayName("con nombre vacio responde 400 y no llega al caso de uso")
        void post_con_nombre_vacio_responde_400() throws Exception {
            mockMvc.perform(
                    post("/diagnostic-imaging-types").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"\",\"description\":\"desc\",\"general\":false}"))
                    .andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("un nombre ya usado en el ambito responde 409, no 500")
        void nombre_repetido_responde_409() throws Exception {
            when(createUseCase.execute(any())).thenThrow(
                    new DiagnosticImagingTypeNameAlreadyExistsException("Ecografia abdominal"));

            mockMvc.perform(post("/diagnostic-imaging-types")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("una empresa inexistente sale como 400, no como 500")
        void empresa_inexistente_responde_400() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new IllegalArgumentException("Company not found: " + COMPANY_ID));

            mockMvc.perform(post("/diagnostic-imaging-types")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("lecturas")
    class Lecturas {

        @Test
        @DisplayName("GET /diagnostic-imaging-types devuelve el listado global")
        void get_lista_global() throws Exception {
            when(listUseCase.listAll()).thenReturn(List.of(tipoGeneral(), tipoDeEmpresa()));

            mockMvc.perform(get("/diagnostic-imaging-types")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(502))
                    .andExpect(jsonPath("$[1].id").value(501));
        }

        @Test
        @DisplayName("GET /diagnostic-imaging-types/available acota por la empresa del contexto")
        void get_disponibles_acota_por_la_empresa() throws Exception {
            when(listAvailableUseCase.listAvailable(COMPANY_ID))
                    .thenReturn(List.of(tipoDeEmpresa()));

            mockMvc.perform(get("/diagnostic-imaging-types/available")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].company.id").value(COMPANY_ID));
        }

        @Test
        @DisplayName("GET /diagnostic-imaging-types/{id} responde 200 con el recurso encontrado")
        void get_por_id_responde_200() throws Exception {
            when(findUseCase.findById(501L, COMPANY_ID)).thenReturn(tipoDeEmpresa());

            mockMvc.perform(get("/diagnostic-imaging-types/501")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(501));
        }

        @Test
        @DisplayName("GET /diagnostic-imaging-types/{id} inexistente responde 404, no 500")
        void get_por_id_inexistente_responde_404() throws Exception {
            when(findUseCase.findById(999L, COMPANY_ID))
                    .thenThrow(new DiagnosticImagingTypeNotFoundException(999L));

            mockMvc.perform(get("/diagnostic-imaging-types/999")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("escrituras sobre un tipo existente")
    class Escrituras {

        @Test
        @DisplayName("PUT /diagnostic-imaging-types/{id} responde 200")
        void put_responde_200() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(tipoDeEmpresa());

            mockMvc.perform(put("/diagnostic-imaging-types/501")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("PUT traduce el request al command con el id de la ruta y la empresa del contexto")
        void put_traduce_el_request_al_command() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(tipoDeEmpresa());

            mockMvc.perform(put("/diagnostic-imaging-types/501")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO));

            verify(updateUseCase).execute(new UpdateDiagnosticImagingTypeCommand(501L,
                    "Ecografia abdominal", "Ecografia de rutina", COMPANY_ID, false));
        }

        @Test
        @DisplayName("PUT de un principal de plataforma edita el catalogo global: el command va sin empresa")
        void put_de_un_principal_de_plataforma_va_sin_empresa() throws Exception {
            // La otra mitad de #565: el update tambien pasa a currentCompanyIdOrNull().
            when(authz.currentCompanyIdOrNull()).thenReturn(null);
            when(updateUseCase.execute(any())).thenReturn(tipoGeneral());

            mockMvc.perform(put("/diagnostic-imaging-types/502")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_GENERAL));

            verify(updateUseCase).execute(new UpdateDiagnosticImagingTypeCommand(502L,
                    "Radiografia", "Radiografia simple", null, true));
        }

        @Test
        @DisplayName("PUT con un nombre ya usado en el ambito responde 409, no 500")
        void put_con_nombre_repetido_responde_409() throws Exception {
            when(updateUseCase.execute(any())).thenThrow(
                    new DiagnosticImagingTypeNameAlreadyExistsException("Ecografia abdominal"));

            mockMvc.perform(put("/diagnostic-imaging-types/501")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("PUT sobre un tipo de otra empresa responde 404")
        void put_de_otro_tipo_responde_404() throws Exception {
            when(updateUseCase.execute(any()))
                    .thenThrow(new DiagnosticImagingTypeNotFoundException(501L));

            mockMvc.perform(put("/diagnostic-imaging-types/501")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("DELETE /diagnostic-imaging-types/{id} responde 204 y propaga la empresa del contexto")
        void delete_responde_204() throws Exception {
            mockMvc.perform(delete("/diagnostic-imaging-types/501"))
                    .andExpect(status().isNoContent());

            verify(deleteUseCase).execute(501L, COMPANY_ID);
        }
    }
}
