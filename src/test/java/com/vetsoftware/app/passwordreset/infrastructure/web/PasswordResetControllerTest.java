package com.vetsoftware.app.passwordreset.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.passwordreset.application.command.RequestPasswordResetCommand;
import com.vetsoftware.app.passwordreset.application.command.ResetPasswordCommand;
import com.vetsoftware.app.passwordreset.application.port.in.RequestPasswordResetUseCase;
import com.vetsoftware.app.passwordreset.application.port.in.ResetPasswordUseCase;
import com.vetsoftware.app.passwordreset.application.port.in.ValidatePasswordResetTokenUseCase;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PasswordResetController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("PasswordResetController — contrato HTTP")
class PasswordResetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RequestPasswordResetUseCase requestUseCase;
    @MockitoBean
    private ValidatePasswordResetTokenUseCase validateUseCase;
    @MockitoBean
    private ResetPasswordUseCase resetUseCase;

    @Nested
    @DisplayName("POST /auth/forgot-password")
    class Forgot {

        @Test
        @DisplayName("responde 204 SIEMPRE y construye el command con el codigo del request")
        void responde_204_y_construye_el_command() throws Exception {
            mockMvc.perform(post("/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"employeeCode\":\"EMP001\"}")).andExpect(status().isNoContent());

            ArgumentCaptor<RequestPasswordResetCommand> captor = ArgumentCaptor
                    .forClass(RequestPasswordResetCommand.class);
            verify(requestUseCase).execute(captor.capture());
            assertThat(captor.getValue().employeeCode()).isEqualTo("EMP001");
        }

        @Test
        @DisplayName("rechaza con 400 un codigo en blanco: anti-enumeracion, no llega al use case")
        void rechaza_con_400_un_codigo_en_blanco() throws Exception {
            mockMvc.perform(post("/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"employeeCode\":\"\"}")).andExpect(status().isBadRequest());

            verifyNoInteractions(requestUseCase);
        }

        @Test
        @DisplayName("rechaza con 400 un codigo de mas de 50 caracteres")
        void rechaza_con_400_un_codigo_demasiado_largo() throws Exception {
            String codigoLargo = "x".repeat(51);

            mockMvc.perform(post("/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"employeeCode\":\"" + codigoLargo + "\"}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(requestUseCase);
        }
    }

    @Nested
    @DisplayName("GET /auth/reset-password/validate")
    class Validate {

        @Test
        @DisplayName("token usable: responde valid=true")
        void token_usable_responde_valid_true() throws Exception {
            when(validateUseCase.isValid("token-valido")).thenReturn(true);

            mockMvc.perform(get("/auth/reset-password/validate").param("token", "token-valido"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.valid").value(true));
        }

        @Test
        @DisplayName("token invalido: responde valid=false, nunca 404 (no revela nada del token)")
        void token_invalido_responde_valid_false() throws Exception {
            when(validateUseCase.isValid("token-caducado")).thenReturn(false);

            mockMvc.perform(get("/auth/reset-password/validate").param("token", "token-caducado"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.valid").value(false));
        }
    }

    @Nested
    @DisplayName("POST /auth/reset-password")
    class Reset {

        @Test
        @DisplayName("responde 204 y construye el command con token y nueva contrasena")
        void responde_204_y_construye_el_command() throws Exception {
            mockMvc.perform(post("/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"token\":\"raw-token\",\"newPassword\":\"nuevaClave123\"}"))
                    .andExpect(status().isNoContent());

            ArgumentCaptor<ResetPasswordCommand> captor = ArgumentCaptor
                    .forClass(ResetPasswordCommand.class);
            verify(resetUseCase).execute(captor.capture());
            assertThat(captor.getValue().token()).isEqualTo("raw-token");
            assertThat(captor.getValue().newPassword()).isEqualTo("nuevaClave123");
        }

        @Test
        @DisplayName("rechaza con 400 una contrasena de menos de 8 caracteres")
        void rechaza_con_400_una_contrasena_demasiado_corta() throws Exception {
            mockMvc.perform(post("/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"token\":\"raw-token\",\"newPassword\":\"1234567\"}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(resetUseCase);
        }

        @Test
        @DisplayName("rechaza con 400 un token en blanco")
        void rechaza_con_400_un_token_en_blanco() throws Exception {
            mockMvc.perform(post("/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"token\":\"\",\"newPassword\":\"nuevaClave123\"}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(resetUseCase);
        }
    }
}
