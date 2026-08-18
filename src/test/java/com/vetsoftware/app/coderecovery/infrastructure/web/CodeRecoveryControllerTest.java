package com.vetsoftware.app.coderecovery.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.coderecovery.application.command.RecoverEmployeeCodeCommand;
import com.vetsoftware.app.coderecovery.application.port.in.RecoverEmployeeCodeUseCase;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.ArgumentCaptor;

@WebMvcTest(CodeRecoveryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("CodeRecoveryController — contrato HTTP")
class CodeRecoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecoverEmployeeCodeUseCase recoverUseCase;

    @Test
    @DisplayName("responde 204 y construye el command con el email del request")
    void responde_204_y_construye_el_command_con_el_email() throws Exception {
        mockMvc.perform(post("/auth/recover-code").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"empleado@vetrina.co\"}")).andExpect(status().isNoContent());

        ArgumentCaptor<RecoverEmployeeCodeCommand> captor = ArgumentCaptor
                .forClass(RecoverEmployeeCodeCommand.class);
        verify(recoverUseCase).execute(captor.capture());
        assertThat(captor.getValue().email()).isEqualTo("empleado@vetrina.co");
    }

    @Test
    @DisplayName("rechaza con 400 un email en blanco, anti-enumeración: no llega al use case")
    void rechaza_con_400_un_email_en_blanco() throws Exception {
        mockMvc.perform(post("/auth/recover-code").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"\"}")).andExpect(status().isBadRequest());

        verifyNoInteractions(recoverUseCase);
    }

    @Test
    @DisplayName("rechaza con 400 un email con formato inválido")
    void rechaza_con_400_un_email_con_formato_invalido() throws Exception {
        mockMvc.perform(post("/auth/recover-code").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"no-es-un-correo\"}")).andExpect(status().isBadRequest());

        verifyNoInteractions(recoverUseCase);
    }
}
