package com.vetsoftware.app.paymentgateway.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.paymentgateway.application.command.CreateWompiPaymentSourceCommand;
import com.vetsoftware.app.paymentgateway.application.dto.WompiPaymentMethodDto;
import com.vetsoftware.app.paymentgateway.application.port.out.CompanyBillingEmailQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentGatewayPort;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentMethodRegistrarPort;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentSourceRateLimitPort;
import com.vetsoftware.app.paymentgateway.domain.CreatePaymentSourceRequest;
import com.vetsoftware.app.paymentgateway.domain.GatewayPaymentSource;
import com.vetsoftware.app.paymentgateway.domain.MerchantAcceptance;
import com.vetsoftware.app.paymentgateway.domain.PaymentSourceRateLimitExceededException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateWompiPaymentSourceService")
class CreateWompiPaymentSourceServiceTest {

    private static final Long EMPRESA = 42L;
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-03-04T08:15:30Z"),
            ZoneOffset.UTC);

    @Mock
    private PaymentGatewayPort paymentGatewayPort;
    @Mock
    private CompanyBillingEmailQueryPort companyBillingEmailQueryPort;
    @Mock
    private PaymentMethodRegistrarPort paymentMethodRegistrarPort;
    @Mock
    private PaymentSourceRateLimitPort paymentSourceRateLimitPort;

    private CreateWompiPaymentSourceService service;

    @BeforeEach
    void setUp() {
        service = new CreateWompiPaymentSourceService(paymentGatewayPort,
                companyBillingEmailQueryPort, paymentMethodRegistrarPort,
                paymentSourceRateLimitPort, RELOJ);
    }

    private CreateWompiPaymentSourceCommand comando() {
        return new CreateWompiPaymentSourceCommand(EMPRESA, "tok_test_7f3a", "acc-token",
                "pda-token", "VISA", "4242", 9, 2027);
    }

    @Test
    @DisplayName("sin perfil fiscal vigente lanza y no llama a la pasarela")
    void sin_perfil_fiscal_lanza() {
        when(companyBillingEmailQueryPort.findFiscalEmail(EMPRESA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(comando()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("con el limite de tasa agotado no llama a la pasarela")
    void limite_de_tasa_agotado_no_llama_a_la_pasarela() {
        doThrow(new PaymentSourceRateLimitExceededException(EMPRESA))
                .when(paymentSourceRateLimitPort).checkAndConsume(EMPRESA);

        assertThatThrownBy(() -> service.execute(comando()))
                .isInstanceOf(PaymentSourceRateLimitExceededException.class);

        verifyNoInteractions(companyBillingEmailQueryPort, paymentGatewayPort,
                paymentMethodRegistrarPort);
    }

    @Test
    @DisplayName("da de alta la fuente de pago con el correo fiscal y guarda la evidencia del mandato")
    void alta_correcta() {
        when(companyBillingEmailQueryPort.findFiscalEmail(EMPRESA))
                .thenReturn(Optional.of("facturacion@clinica.co"));
        when(paymentGatewayPort.fetchAcceptance()).thenReturn(
                new MerchantAcceptance("acc-jwt", "https://wompi.com/policies/acceptance-2026",
                        "pda-jwt", "https://wompi.com/policies/personal-data-2026"));
        when(paymentGatewayPort.createPaymentSource(any()))
                .thenReturn(new GatewayPaymentSource(778899L, "AVAILABLE"));
        when(paymentMethodRegistrarPort.registerDefaultCard(eq(EMPRESA), eq("778899"), eq("VISA"),
                eq("4242"), eq(LocalDate.of(2027, 9, 30)), any(), any()))
                .thenReturn(new WompiPaymentMethodDto(31L, "VISA", "4242",
                        LocalDate.of(2027, 9, 30), true));

        WompiPaymentMethodDto result = service.execute(comando());

        assertThat(result.paymentMethodId()).isEqualTo(31L);

        ArgumentCaptor<CreatePaymentSourceRequest> requestCaptor = ArgumentCaptor
                .forClass(CreatePaymentSourceRequest.class);
        verify(paymentGatewayPort).createPaymentSource(requestCaptor.capture());
        assertThat(requestCaptor.getValue().cardToken()).isEqualTo("tok_test_7f3a");
        assertThat(requestCaptor.getValue().customerEmail()).isEqualTo("facturacion@clinica.co");
        assertThat(requestCaptor.getValue().acceptanceToken()).isEqualTo("acc-token");
        assertThat(requestCaptor.getValue().acceptPersonalAuth()).isTrue();

        ArgumentCaptor<String> evidenceCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDateTime> authorizedAtCaptor = ArgumentCaptor
                .forClass(LocalDateTime.class);
        verify(paymentMethodRegistrarPort).registerDefaultCard(eq(EMPRESA), eq("778899"),
                eq("VISA"), eq("4242"), eq(LocalDate.of(2027, 9, 30)), evidenceCaptor.capture(),
                authorizedAtCaptor.capture());
        // El ultimo segmento de cada permalink, no la URL completa: el permalink
        // entero deja mandate_evidence a solo 4 caracteres del tope de 255.
        assertThat(evidenceCaptor.getValue())
                .isEqualTo("wompi:ps=778899;acc=acceptance-2026;pda=personal-data-2026;at="
                        + "2026-03-04T08:15:30Z");
        assertThat(authorizedAtCaptor.getValue())
                .isEqualTo(LocalDateTime.ofInstant(RELOJ.instant(), RELOJ.getZone()));
    }
}
