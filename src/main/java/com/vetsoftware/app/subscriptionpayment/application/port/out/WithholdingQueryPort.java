package com.vetsoftware.app.subscriptionpayment.application.port.out;

import com.vetsoftware.app.subscriptionpayment.domain.WithholdingRef;
import java.util.Optional;

/**
 * Resuelve la retencion con la que se salda una factura.
 *
 * <p>
 * <b>Solo declara la variante acotada por empresa</b>
 * ({@code REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA}). Una forma ancha
 * —{@code findById(withholdingId)}— no se apropiaria de nada ajeno, pero
 * permitiria saldar la factura de una clinica con la retencion practicada a
 * otra: el saldo bajaria y el certificado que lo respalda estaria a nombre de
 * un tercero. Aqui ni siquiera existe el metodo con el que equivocarse.
 */
public interface WithholdingQueryPort {

    Optional<WithholdingRef> findByIdAndCompanyId(Long withholdingId, Long companyId);
}
