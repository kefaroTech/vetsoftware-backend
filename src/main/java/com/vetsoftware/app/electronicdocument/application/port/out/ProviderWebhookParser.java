package com.vetsoftware.app.electronicdocument.application.port.out;

/**
 * Interpreta el webhook de un proveedor async (p. ej. MATIAS). Cada proveedor lo implementa con su
 * formato de payload y su esquema de firma. El servicio genérico enruta por {@link
 * #providerName()}.
 */
public interface ProviderWebhookParser {
  String providerName();

  /** Verifica la autenticidad del webhook (p. ej. HMAC-SHA256 del cuerpo crudo con el secret). */
  boolean verifySignature(String rawBody, String signatureHeader, String secret);

  /** Normaliza el payload: a qué documento aplica y qué sellos/estado trae. */
  ParsedWebhook parse(String rawBody);
}
