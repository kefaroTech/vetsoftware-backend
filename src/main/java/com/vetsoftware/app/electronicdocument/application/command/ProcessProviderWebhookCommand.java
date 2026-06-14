package com.vetsoftware.app.electronicdocument.application.command;

/** Webhook entrante de un proveedor async. rawBody se conserva crudo para verificar la firma HMAC. */
public record ProcessProviderWebhookCommand(
        String provider,
        String rawBody,
        String signatureHeader
) {}
