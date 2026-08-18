package com.vetsoftware.app.companytaxprofile.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.companytaxprofile.application.command.CreateCompanyTaxProfileCommand;
import com.vetsoftware.app.companytaxprofile.application.command.UpdateCompanyTaxProfileCommand;
import com.vetsoftware.app.companytaxprofile.application.dto.CompanyTaxProfileDto;
import com.vetsoftware.app.companytaxprofile.application.port.in.CreateCompanyTaxProfileUseCase;
import com.vetsoftware.app.companytaxprofile.application.port.in.DeleteCompanyTaxProfileUseCase;
import com.vetsoftware.app.companytaxprofile.application.port.in.FindCompanyTaxProfileUseCase;
import com.vetsoftware.app.companytaxprofile.application.port.in.ReactivateCompanyTaxProfileUseCase;
import com.vetsoftware.app.companytaxprofile.application.port.in.UpdateCompanyTaxProfileUseCase;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfileAlreadyExistsException;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfileNotFoundException;
import com.vetsoftware.app.companytaxprofile.testsupport.CompanyTaxProfileMother;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
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
 * Rodaja HTTP del perfil fiscal: rutas, binding, validacion del request,
 * codigos de estado y forma del JSON. Lo que hay debajo son dobles.
 *
 * <p>
 * <b>La empresa nunca viaja en el cuerpo.</b> Ninguno de los cinco endpoints
 * recibe {@code companyId}: lo pone {@code Authz.currentCompanyId()}, que en la
 * rodaja siempre resuelve a {@link WebMvcSliceConfig#COMPANY_ID}. Cada
 * traduccion request→command se comprueba capturando el command real.
 */
@WebMvcTest(CompanyTaxProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("CompanyTaxProfileController — contrato HTTP")
class CompanyTaxProfileControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateCompanyTaxProfileUseCase createUseCase;
    @MockitoBean
    private UpdateCompanyTaxProfileUseCase updateUseCase;
    @MockitoBean
    private FindCompanyTaxProfileUseCase findUseCase;
    @MockitoBean
    private DeleteCompanyTaxProfileUseCase deleteUseCase;
    @MockitoBean
    private ReactivateCompanyTaxProfileUseCase reactivateUseCase;

    private static final String CUERPO_CREAR = """
            {"documentType":"NIT","companyDocumentId":"900123456",
             "companyDocumentVerificationDigit":"1","legalName":"Clinica Veterinaria Norte S.A.S.",
             "taxRegime":"RESPONSABLE_IVA","fiscalEmail":"facturacion@vetnorte.com",
             "commercialName":"Vet Norte","economicActivityId":5,
             "responsibilities":["O-13","O-15"]}
            """;

    private static final String CUERPO_CREAR_SIN_LEGAL_NAME = """
            {"documentType":"NIT","companyDocumentId":"900123456","legalName":"   ",
             "taxRegime":"RESPONSABLE_IVA","fiscalEmail":"facturacion@vetnorte.com"}
            """;

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("POST /company-tax-profile responde 201 con la empresa y la actividad economica")
        void post_crea_y_devuelve_201() throws Exception {
            when(createUseCase.execute(any()))
                    .thenReturn(CompanyTaxProfileDto.from(CompanyTaxProfileMother.perfilNit()));

            mockMvc.perform(post("/company-tax-profile").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_CREAR)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(CompanyTaxProfileMother.PROFILE_ID))
                    .andExpect(jsonPath("$.company.name").value("Clinica Norte"))
                    .andExpect(jsonPath("$.companyDocumentType").value("NIT"))
                    .andExpect(jsonPath("$.companyDocumentId").value(CompanyTaxProfileMother.NIT))
                    .andExpect(jsonPath("$.companyDocumentVerificationDigit")
                            .value(CompanyTaxProfileMother.NIT_DV))
                    .andExpect(jsonPath("$.economicActivity.code").value("7500"))
                    .andExpect(jsonPath("$.responsibilities[0]").value("O-13"))
                    .andExpect(jsonPath("$.responsibilities[1]").value("O-15"))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("POST sella el companyId del contexto, no el del cuerpo")
        void post_sella_el_companyid_del_contexto() throws Exception {
            when(createUseCase.execute(any()))
                    .thenReturn(CompanyTaxProfileDto.from(CompanyTaxProfileMother.perfilNit()));

            mockMvc.perform(post("/company-tax-profile").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_CREAR)).andExpect(status().isCreated());

            verify(createUseCase).execute(new CreateCompanyTaxProfileCommand(
                    com.vetsoftware.app.companytaxprofile.domain.CompanyDocumentType.NIT,
                    "900123456", "1", "Clinica Veterinaria Norte S.A.S.",
                    com.vetsoftware.app.companytaxprofile.domain.TaxRegime.RESPONSABLE_IVA,
                    "facturacion@vetnorte.com", "Vet Norte", 5L, java.util.List.of("O-13", "O-15"),
                    COMPANY_ID));
        }

        @Test
        @DisplayName("POST sin legalName responde 400 y no llama al caso de uso")
        void post_sin_legal_name_responde_400() throws Exception {
            mockMvc.perform(post("/company-tax-profile").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_CREAR_SIN_LEGAL_NAME)).andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("POST con perfil ya existente responde 409")
        void post_con_perfil_existente_responde_409() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new CompanyTaxProfileAlreadyExistsException(COMPANY_ID));

            mockMvc.perform(post("/company-tax-profile").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_CREAR)).andExpect(status().isConflict());
        }

        @Test
        @DisplayName("una persona natural sin actividad economica sale con economicActivity null")
        void persona_natural_sin_actividad_sale_con_economic_activity_null() throws Exception {
            when(createUseCase.execute(any()))
                    .thenReturn(CompanyTaxProfileDto.from(CompanyTaxProfileMother.perfilCedula()));

            mockMvc.perform(
                    post("/company-tax-profile").contentType(MediaType.APPLICATION_JSON).content("""
                            {"documentType":"CEDULA_CIUDADANIA","companyDocumentId":"1020304050",
                             "legalName":"Ana Ruiz","taxRegime":"NO_RESPONSABLE_IVA",
                             "fiscalEmail":"ana@ruiz.com"}
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.economicActivity").doesNotExist())
                    .andExpect(jsonPath("$.companyDocumentVerificationDigit").doesNotExist())
                    .andExpect(jsonPath("$.responsibilities").isEmpty());
        }
    }

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("PUT /company-tax-profile responde 200 y sella el companyId del contexto")
        void put_actualiza_y_sella_el_companyid() throws Exception {
            when(updateUseCase.execute(any()))
                    .thenReturn(CompanyTaxProfileDto.from(CompanyTaxProfileMother.perfilNit()));

            mockMvc.perform(put("/company-tax-profile").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_CREAR)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.legalName").value(CompanyTaxProfileMother.RAZON_SOCIAL));

            verify(updateUseCase).execute(new UpdateCompanyTaxProfileCommand(
                    com.vetsoftware.app.companytaxprofile.domain.CompanyDocumentType.NIT,
                    "900123456", "1", "Clinica Veterinaria Norte S.A.S.",
                    com.vetsoftware.app.companytaxprofile.domain.TaxRegime.RESPONSABLE_IVA,
                    "facturacion@vetnorte.com", "Vet Norte", 5L, java.util.List.of("O-13", "O-15"),
                    COMPANY_ID));
        }

        @Test
        @DisplayName("PUT sin legalName responde 400 y no llama al caso de uso")
        void put_sin_legal_name_responde_400() throws Exception {
            mockMvc.perform(put("/company-tax-profile").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_CREAR_SIN_LEGAL_NAME)).andExpect(status().isBadRequest());

            verifyNoInteractions(updateUseCase);
        }

        @Test
        @DisplayName("PUT sobre un perfil inexistente responde 404")
        void put_sobre_perfil_inexistente_responde_404() throws Exception {
            when(updateUseCase.execute(any()))
                    .thenThrow(new CompanyTaxProfileNotFoundException(COMPANY_ID));

            mockMvc.perform(put("/company-tax-profile").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_CREAR)).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("consulta")
    class Consulta {

        @Test
        @DisplayName("GET /company-tax-profile consulta el companyId del contexto")
        void get_consulta_el_companyid_del_contexto() throws Exception {
            when(findUseCase.findByCompanyId(COMPANY_ID))
                    .thenReturn(CompanyTaxProfileDto.from(CompanyTaxProfileMother.perfilNit()));

            mockMvc.perform(get("/company-tax-profile")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.legalName").value(CompanyTaxProfileMother.RAZON_SOCIAL));

            verify(findUseCase).findByCompanyId(COMPANY_ID);
        }

        @Test
        @DisplayName("GET sin perfil configurado responde 404, no 500")
        void get_sin_perfil_responde_404() throws Exception {
            when(findUseCase.findByCompanyId(COMPANY_ID))
                    .thenThrow(new CompanyTaxProfileNotFoundException(COMPANY_ID));

            mockMvc.perform(get("/company-tax-profile")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("borrado")
    class Borrado {

        @Test
        @DisplayName("DELETE /company-tax-profile responde 204 y borra el del contexto")
        void delete_responde_204() throws Exception {
            mockMvc.perform(delete("/company-tax-profile")).andExpect(status().isNoContent());

            verify(deleteUseCase).execute(COMPANY_ID);
        }

        @Test
        @DisplayName("DELETE sobre un perfil inexistente responde 404")
        void delete_sobre_perfil_inexistente_responde_404() throws Exception {
            org.mockito.Mockito.doThrow(new CompanyTaxProfileNotFoundException(COMPANY_ID))
                    .when(deleteUseCase).execute(COMPANY_ID);

            mockMvc.perform(delete("/company-tax-profile")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("POST /company-tax-profile/reactivate responde 200 con el perfil habilitado")
        void post_reactivar_responde_200() throws Exception {
            when(reactivateUseCase.execute(COMPANY_ID))
                    .thenReturn(CompanyTaxProfileDto.from(CompanyTaxProfileMother.perfilNit()));

            mockMvc.perform(post("/company-tax-profile/reactivate")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled").value(true));

            verify(reactivateUseCase).execute(COMPANY_ID);
        }

        @Test
        @DisplayName("reactivar un perfil inexistente responde 404 y no llama a los demas casos de uso")
        void post_reactivar_perfil_inexistente_responde_404() throws Exception {
            when(reactivateUseCase.execute(COMPANY_ID))
                    .thenThrow(new CompanyTaxProfileNotFoundException(COMPANY_ID));

            mockMvc.perform(post("/company-tax-profile/reactivate"))
                    .andExpect(status().isNotFound());

            verifyNoInteractions(createUseCase, updateUseCase, deleteUseCase, findUseCase);
        }
    }
}
