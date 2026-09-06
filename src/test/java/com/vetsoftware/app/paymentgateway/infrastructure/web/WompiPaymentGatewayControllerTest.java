package com.vetsoftware.app.paymentgateway.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.paymentgateway.application.command.CreateWompiPaymentSourceCommand;
import com.vetsoftware.app.paymentgateway.application.dto.FirstPeriodPaymentDto;
import com.vetsoftware.app.paymentgateway.application.dto.WompiCheckoutConfigDto;
import com.vetsoftware.app.paymentgateway.application.dto.WompiPaymentMethodDto;
import com.vetsoftware.app.paymentgateway.application.port.in.CreateWompiPaymentSourceUseCase;
import com.vetsoftware.app.paymentgateway.application.port.in.FindFirstPeriodPaymentUseCase;
import com.vetsoftware.app.paymentgateway.application.port.in.GetWompiCheckoutConfigUseCase;
import com.vetsoftware.app.paymentgateway.domain.FirstPeriodPaymentStatus;
import com.vetsoftware.app.paymentgateway.domain.PaymentGatewayNotConfiguredException;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDate;
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

@WebMvcTest(WompiPaymentGatewayController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("WompiPaymentGatewayController — contrato HTTP")
class WompiPaymentGatewayControllerTest {

    private static final Long EMPRESA_DEL_CONTEXTO = WebMvcSliceConfig.COMPANY_ID;

    private static final String ALTA_VALIDA = """
            {
              "cardToken": "tok_test_7f3a",
              "acceptanceToken": "acc-token",
              "personalDataAuthToken": "pda-token",
              "brand": "VISA",
              "lastFour": "4242",
              "expMonth": 9,
              "expYear": 2027
            }
            """;

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private GetWompiCheckoutConfigUseCase checkoutConfigUseCase;
    @MockitoBean
    private CreateWompiPaymentSourceUseCase createPaymentSourceUseCase;
    @MockitoBean
    private FindFirstPeriodPaymentUseCase findFirstPeriodPaymentUseCase;
    @MockitoBean
    private Authz authz;

    @Nested
    @DisplayName("checkout-config")
    class CheckoutConfig {

        @Test
        @DisplayName("devuelve la configuracion con la llave publica, nunca la privada")
        void devuelve_la_configuracion() throws Exception {
            when(checkoutConfigUseCase.execute()).thenReturn(new WompiCheckoutConfigDto("SANDBOX",
                    "https://sandbox.wompi.co/v1", "pub_test_abc",
                    new WompiCheckoutConfigDto.Acceptance("acc-jwt", "https://wompi.com/acc"),
                    new WompiCheckoutConfigDto.Acceptance("pda-jwt", "https://wompi.com/pda")));

            mockMvc.perform(get("/payment-gateway/wompi/checkout-config"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.environment").value("SANDBOX"))
                    .andExpect(jsonPath("$.publicKey").value("pub_test_abc"))
                    .andExpect(jsonPath("$.acceptance.token").value("acc-jwt"))
                    .andExpect(jsonPath("$.privateKey").doesNotExist());
        }

        @Test
        @DisplayName("pasarela no configurada responde 409")
        void pasarela_no_configurada_responde_409() throws Exception {
            when(checkoutConfigUseCase.execute())
                    .thenThrow(new PaymentGatewayNotConfiguredException("Wompi deshabilitado"));

            mockMvc.perform(get("/payment-gateway/wompi/checkout-config"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("PAYMENT_GATEWAY_NOT_CONFIGURED"));
        }
    }

    @Nested
    @DisplayName("alta de la fuente de pago")
    class AltaDeLaFuenteDePago {

        @Test
        @DisplayName("registra el medio con la empresa del contexto, no con una del cliente")
        void registra_con_la_empresa_del_contexto() throws Exception {
            when(authz.currentCompanyId()).thenReturn(EMPRESA_DEL_CONTEXTO);
            when(createPaymentSourceUseCase.execute(any())).thenReturn(new WompiPaymentMethodDto(
                    31L, "VISA", "4242", LocalDate.of(2027, 9, 30), true));

            mockMvc.perform(post("/payment-gateway/wompi/payment-sources")
                    .contentType(MediaType.APPLICATION_JSON).content(ALTA_VALIDA))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.paymentMethodId").value(31))
                    .andExpect(jsonPath("$.brand").value("VISA"));

            ArgumentCaptor<CreateWompiPaymentSourceCommand> captor = ArgumentCaptor
                    .forClass(CreateWompiPaymentSourceCommand.class);
            verify(createPaymentSourceUseCase).execute(captor.capture());
            org.assertj.core.api.Assertions.assertThat(captor.getValue().companyId())
                    .isEqualTo(EMPRESA_DEL_CONTEXTO);
            org.assertj.core.api.Assertions.assertThat(captor.getValue().cardToken())
                    .isEqualTo("tok_test_7f3a");
        }

        @Test
        @DisplayName("rechaza un lastFour que no son 4 digitos")
        void rechaza_last_four_invalido() throws Exception {
            String invalido = ALTA_VALIDA.replace("\"4242\"", "\"abcd\"");

            mockMvc.perform(post("/payment-gateway/wompi/payment-sources")
                    .contentType(MediaType.APPLICATION_JSON).content(invalido))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("rechaza un cardToken vacio")
        void rechaza_card_token_vacio() throws Exception {
            String invalido = ALTA_VALIDA.replace("\"tok_test_7f3a\"", "\"\"");

            mockMvc.perform(post("/payment-gateway/wompi/payment-sources")
                    .contentType(MediaType.APPLICATION_JSON).content(invalido))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("first-period-payment")
    class FirstPeriodPayment {

        @Test
        @DisplayName("devuelve el estado del cobro del primer periodo de la empresa del contexto")
        void devuelve_el_estado() throws Exception {
            when(authz.currentCompanyId()).thenReturn(EMPRESA_DEL_CONTEXTO);
            when(findFirstPeriodPaymentUseCase.execute(EMPRESA_DEL_CONTEXTO))
                    .thenReturn(new FirstPeriodPaymentDto(FirstPeriodPaymentStatus.APPROVED,
                            new java.math.BigDecimal("45000"), "COP", "tx-1", null));

            mockMvc.perform(get("/payment-gateway/wompi/first-period-payment"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"))
                    .andExpect(jsonPath("$.gatewayReference").value("tx-1"));
        }
    }
}
