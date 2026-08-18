package com.vetsoftware.app.registration.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import com.vetsoftware.app.registration.application.command.RegisterUserCommand;
import com.vetsoftware.app.registration.application.command.VerifyEmailCommand;
import com.vetsoftware.app.registration.application.dto.RegistrationDto;
import com.vetsoftware.app.registration.application.port.in.RegisterUserUseCase;
import com.vetsoftware.app.registration.application.port.in.VerifyEmailUseCase;
import com.vetsoftware.app.registration.domain.EmployeeCodeAlreadyExistsException;
import com.vetsoftware.app.registration.domain.InvalidVerificationTokenException;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Contrato HTTP del auto-registro público. {@code AuditLogger} lo provee
 * {@link WebMvcSliceConfig} como bean mockeado —se reutiliza vía
 * {@code @Autowired} para verificar la auditoría del alta— y no como
 * {@code @MockitoBean} propio, para no duplicar el bean en el contexto.
 */
@WebMvcTest(RegistrationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("RegistrationController — contrato HTTP")
class RegistrationControllerTest {

    private static final String REGISTRO_VALIDO = """
            {"companyName":"Veterinaria Vetrina","documentType":"NIT","companyIdentifier":"900123456",
             "companyAddress":"Calle 1 # 2-3","companyContactNumber":"3001234567","cityId":11001,
             "employeeName":"Orlando Velásquez","employeeEmail":"orlando@vetrina.co",
             "password":"Orlando1997*","taxRegime":"RESPONSABLE_IVA","fiscalEmail":"fiscal@vetrina.co",
             "recaptchaToken":"captcha-token"}
            """;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AuditLogger auditLogger;

    @MockitoBean
    private RegisterUserUseCase registerUserUseCase;
    @MockitoBean
    private VerifyEmailUseCase verifyEmailUseCase;

    private static RegistrationDto dto() {
        return new RegistrationDto(9L, 55L, "orlando@vetrina.co", "PENDING_VERIFICATION");
    }

    @Nested
    @DisplayName("POST /register")
    class Registro {

        @Test
        @DisplayName("con datos válidos responde 201 y audita el alta de la empresa")
        void con_datos_validos_responde_201_y_audita() throws Exception {
            when(registerUserUseCase.execute(any(RegisterUserCommand.class))).thenReturn(dto());

            mockMvc.perform(post("/register").contentType(MediaType.APPLICATION_JSON)
                    .content(REGISTRO_VALIDO)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.companyId").value(9))
                    .andExpect(jsonPath("$.employeeId").value(55))
                    .andExpect(jsonPath("$.email").value("orlando@vetrina.co"))
                    .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"));

            verify(auditLogger).companyRegistered(9L, "Veterinaria Vetrina", "900123456", 55L,
                    "orlando@vetrina.co");
        }

        @Test
        @DisplayName("sin nombre de empresa responde 400 y no llega al caso de uso")
        void sin_nombre_de_empresa_responde_400() throws Exception {
            mockMvc.perform(post("/register").contentType(MediaType.APPLICATION_JSON).content("""
                    {"companyName":"","documentType":"NIT","companyIdentifier":"900123456",
                     "cityId":11001,"employeeName":"Orlando","employeeEmail":"orlando@vetrina.co",
                     "password":"Orlando1997*","taxRegime":"RESPONSABLE_IVA",
                     "fiscalEmail":"fiscal@vetrina.co"}
                    """)).andExpect(status().isBadRequest());

            verifyNoInteractions(registerUserUseCase);
        }

        @Test
        @DisplayName("con el correo de acceso ya registrado responde 409")
        void correo_ya_registrado_responde_409() throws Exception {
            when(registerUserUseCase.execute(any(RegisterUserCommand.class)))
                    .thenThrow(new EmployeeCodeAlreadyExistsException("orlando@vetrina.co"));

            mockMvc.perform(post("/register").contentType(MediaType.APPLICATION_JSON)
                    .content(REGISTRO_VALIDO)).andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("POST /register/verify")
    class Verificacion {

        @Test
        @DisplayName("con un token válido responde 204")
        void con_token_valido_responde_204() throws Exception {
            mockMvc.perform(
                    post("/register/verify").contentType(MediaType.APPLICATION_JSON).content("""
                            {"token":"raw-token-value"}
                            """)).andExpect(status().isNoContent());

            verify(verifyEmailUseCase).execute(new VerifyEmailCommand("raw-token-value"));
        }

        @Test
        @DisplayName("con token en blanco responde 400 y no llega al caso de uso")
        void con_token_en_blanco_responde_400() throws Exception {
            mockMvc.perform(
                    post("/register/verify").contentType(MediaType.APPLICATION_JSON).content("""
                            {"token":""}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(verifyEmailUseCase);
        }

        @Test
        @DisplayName("con token inválido o expirado responde 400")
        void con_token_invalido_responde_400() throws Exception {
            Mockito.doThrow(new InvalidVerificationTokenException("Invalid verification token"))
                    .when(verifyEmailUseCase).execute(any(VerifyEmailCommand.class));

            mockMvc.perform(
                    post("/register/verify").contentType(MediaType.APPLICATION_JSON).content("""
                            {"token":"expired-token"}
                            """)).andExpect(status().isBadRequest());
        }
    }
}
