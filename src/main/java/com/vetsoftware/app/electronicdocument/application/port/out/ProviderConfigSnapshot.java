package com.vetsoftware.app.electronicdocument.application.port.out;

/**
 * Vista de la configuración del proveedor DIAN de una empresa, con credenciales ya descifradas.
 * `provider` es el nombre del proveedor (hoy MATIAS) en texto, para no acoplar esta feature al enum
 * de la feature de configuración (vertical slicing). El adaptador correcto se elige por este
 * nombre.
 */
public record ProviderConfigSnapshot(
    String provider,
    String baseUrl,
    String clientId,
    String clientSecret,
    String username,
    String password,
    String apiToken,
    String webhookSecret,
    String numberingProviderRef) {}
