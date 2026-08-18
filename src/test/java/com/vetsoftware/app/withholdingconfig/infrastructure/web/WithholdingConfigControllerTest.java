package com.vetsoftware.app.withholdingconfig.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.withholdingconfig.application.command.SetWithholdingConfigCommand;
import com.vetsoftware.app.withholdingconfig.application.dto.WithholdingConfigDto;
import com.vetsoftware.app.withholdingconfig.application.port.in.FindWithholdingConfigUseCase;
import com.vetsoftware.app.withholdingconfig.application.port.in.SetWithholdingConfigUseCase;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDateTime;
import java.math.BigDecimal;
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

/**
 * Rodaja HTTP de {@link WithholdingConfigController}: rutas, binding y forma
 * del JSON. La company nunca viaja en el cuerpo — la pone {@code Authz}
 * ({@link WebMvcSliceConfig#COMPANY_ID}) y el command se captura para
 * comprobarlo.
 */
@WebMvcTest(WithholdingConfigController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("WithholdingConfigController — contrato HTTP")
class WithholdingConfigControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SetWithholdingConfigUseCase setUseCase;
    @MockitoBean
    private FindWithholdingConfigUseCase findUseCase;

    private static WithholdingConfigDto dto() {
        return new WithholdingConfigDto(1L, COMPANY_ID, new BigDecimal("2.5"),
                new BigDecimal("15.0"), new BigDecimal("1.0"), LocalDateTime.of(2026, 1, 15, 8, 30),
                true);
    }

    @Nested
    @DisplayName("PUT /withholding-configs")
    class Set {

        @Test
        @DisplayName("sella el command con la company del contexto, nunca del cuerpo")
        void sella_el_command_con_la_company_del_contexto() throws Exception {
            when(setUseCase.execute(any())).thenReturn(dto());

            mockMvc.perform(put("/withholding-configs").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"reteFuenteRate\":2.5,\"reteIvaRate\":15.0,\"reteIcaRate\":1.0}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.companyId").value(COMPANY_ID))
                    .andExpect(jsonPath("$.reteFuenteRate").value(2.5));

            ArgumentCaptor<SetWithholdingConfigCommand> captor = ArgumentCaptor
                    .forClass(SetWithholdingConfigCommand.class);
            verify(setUseCase).execute(captor.capture());
            org.assertj.core.api.Assertions.assertThat(captor.getValue().companyId())
                    .isEqualTo(COMPANY_ID);
        }

        @Test
        @DisplayName("una tasa negativa en el request se rechaza con 400")
        void una_tasa_negativa_se_rechaza_con_400() throws Exception {
            mockMvc.perform(put("/withholding-configs").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"reteFuenteRate\":-1,\"reteIvaRate\":15.0,\"reteIcaRate\":1.0}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /withholding-configs")
    class Find {

        @Test
        @DisplayName("devuelve la configuracion de la company del contexto")
        void devuelve_la_configuracion_de_la_company_del_contexto() throws Exception {
            when(findUseCase.findByCompany(COMPANY_ID)).thenReturn(dto());

            mockMvc.perform(get("/withholding-configs")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.companyId").value(COMPANY_ID));
        }
    }
}
