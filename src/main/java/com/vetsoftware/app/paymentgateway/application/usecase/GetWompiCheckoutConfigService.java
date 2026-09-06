package com.vetsoftware.app.paymentgateway.application.usecase;

import com.vetsoftware.app.paymentgateway.application.dto.WompiCheckoutConfigDto;
import com.vetsoftware.app.paymentgateway.application.dto.WompiCheckoutConfigDto.Acceptance;
import com.vetsoftware.app.paymentgateway.application.port.in.GetWompiCheckoutConfigUseCase;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentGatewayPort;
import com.vetsoftware.app.paymentgateway.domain.MerchantAcceptance;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/**
 * Sin {@code @Transactional}: {@link PaymentGatewayPort#fetchAcceptance()} es
 * I/O HTTP.
 */
@Observed(name = "payment.gateway.checkout.config")
@Service
public class GetWompiCheckoutConfigService implements GetWompiCheckoutConfigUseCase {

    private static final String PRODUCTION_KEY_PREFIX = "pub_prod_";

    private final PaymentGatewayPort paymentGatewayPort;

    public GetWompiCheckoutConfigService(PaymentGatewayPort paymentGatewayPort) {
        this.paymentGatewayPort = paymentGatewayPort;
    }

    @Override
    public WompiCheckoutConfigDto execute() {
        MerchantAcceptance acceptance = paymentGatewayPort.fetchAcceptance();
        String publicKey = paymentGatewayPort.publicKey();
        String environment = publicKey != null && publicKey.startsWith(PRODUCTION_KEY_PREFIX)
                ? "PRODUCTION"
                : "SANDBOX";
        return new WompiCheckoutConfigDto(environment, paymentGatewayPort.apiBaseUrl(), publicKey,
                new Acceptance(acceptance.acceptanceToken(), acceptance.acceptancePermalink()),
                new Acceptance(acceptance.personalDataAuthToken(),
                        acceptance.personalDataAuthPermalink()));
    }
}
