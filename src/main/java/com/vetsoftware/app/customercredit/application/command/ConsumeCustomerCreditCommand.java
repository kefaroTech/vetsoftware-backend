package com.vetsoftware.app.customercredit.application.command;

import java.math.BigDecimal;

/**
 * Aplicacion de saldo a favor a un documento de cobro.
 *
 * <p>
 * El importe viaja <strong>en positivo</strong> —es lo que se quiere gastar— y
 * el dominio lo guarda negado: el saldo es una suma de asientos, no un contador
 * con reglas aparte.
 *
 * <p>
 * <strong>Un consumo casi nunca es una sola fila.</strong> Se reparte entre los
 * lotes vivos empezando por el que antes caduca, y cada fila anota de que lote
 * salio. Por eso {@code clientRequestId} es la llave <em>de la operacion</em> y
 * no la de una fila: el servicio deriva de ella una llave por lote, porque
 * {@code uq_cce_idempotency} es {@code (company_id, client_request_id)} y N
 * filas con la misma llave chocarian entre si.
 *
 * @param clientRequestId
 *            llave de idempotencia de la operacion completa, obligatoria. Se
 *            acota a 56 caracteres en la frontera para dejar sitio al sufijo
 *            por lote sin pasarse de los 64 de la columna
 */
public record ConsumeCustomerCreditCommand(Long companyId, BigDecimal amount, Long originDocumentId,
        String clientRequestId) {
}
