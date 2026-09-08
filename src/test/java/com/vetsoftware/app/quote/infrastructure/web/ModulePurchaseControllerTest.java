package com.vetsoftware.app.quote.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.quote.application.command.PurchaseModulesCommand;
import com.vetsoftware.app.quote.application.dto.PurchaseModulesResultDto;
import com.vetsoftware.app.quote.application.dto.PurchasedModuleLineDto;
import com.vetsoftware.app.quote.application.port.in.PurchaseModulesUseCase;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDate;
import java.util.List;
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

@WebMvcTest(ModulePurchaseController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("ModulePurchaseController — contrato HTTP")
class ModulePurchaseControllerTest {

    private static final String CUERPO_VALIDO = """
            {"catalogItemCodes":["GROOMING"],"billingCycle":"MONTHLY","paymentSourceId":12,
             "clientRequestId":"req-purchase-1"}
            """;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Authz authz;
    @MockitoBean
    private PurchaseModulesUseCase purchaseUseCase;

    @Nested
    @DisplayName("Camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("la empresa y la IP salen del contexto/petición, nunca del cuerpo")
        void empresa_e_ip_salen_del_contexto() throws Exception {
            Long employeeId = 55L;
            when(authz.currentEmployeeId()).thenReturn(employeeId);
            when(purchaseUseCase.execute(any())).thenReturn(new PurchaseModulesResultDto(900L, List
                    .of(new PurchasedModuleLineDto("GROOMING", LocalDate.of(2026, 9, 30), false))));

            mockMvc.perform(post("/subscriptions/modules/purchase")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.quoteId").value(900))
                    .andExpect(jsonPath("$.lines[0].catalogItemCode").value("GROOMING"))
                    .andExpect(jsonPath("$.lines[0].chargedNow").value(false));

            ArgumentCaptor<PurchaseModulesCommand> captor = ArgumentCaptor
                    .forClass(PurchaseModulesCommand.class);
            verify(purchaseUseCase).execute(captor.capture());
            org.assertj.core.api.Assertions.assertThat(captor.getValue().companyId())
                    .isEqualTo(WebMvcSliceConfig.COMPANY_ID);
            org.assertj.core.api.Assertions.assertThat(captor.getValue().employeeId())
                    .isEqualTo(employeeId);
            org.assertj.core.api.Assertions.assertThat(captor.getValue().acceptedIp()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("Validación del cuerpo")
    class Validacion {

        @Test
        @DisplayName("sin códigos de módulo, 400 y nada de llegar al caso de uso")
        void sin_codigos_es_400() throws Exception {
            String sinCodigos = """
                    {"catalogItemCodes":[],"billingCycle":"MONTHLY","paymentSourceId":12,
                     "clientRequestId":"req-purchase-1"}
                    """;

            mockMvc.perform(post("/subscriptions/modules/purchase")
                    .contentType(MediaType.APPLICATION_JSON).content(sinCodigos))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(purchaseUseCase);
        }

        @Test
        @DisplayName("sin medio de pago, 400 y nada de llegar al caso de uso")
        void sin_medio_de_pago_es_400() throws Exception {
            String sinPago = """
                    {"catalogItemCodes":["GROOMING"],"billingCycle":"MONTHLY",
                     "clientRequestId":"req-purchase-1"}
                    """;

            mockMvc.perform(post("/subscriptions/modules/purchase")
                    .contentType(MediaType.APPLICATION_JSON).content(sinPago))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(purchaseUseCase);
        }
    }
}
