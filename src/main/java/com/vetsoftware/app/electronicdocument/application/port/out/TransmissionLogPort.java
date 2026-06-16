package com.vetsoftware.app.electronicdocument.application.port.out;

import com.vetsoftware.app.electronicdocument.domain.TransmissionResult;
import java.util.Optional;

/** Registra cada intento de transmisión en la bitácora (electronic_document_transmissions). */
public interface TransmissionLogPort {
    /** El número de intento se calcula automáticamente (intentos previos + 1). */
    void record(Long electronicDocumentId, String provider, Integer httpStatus,
                String providerDocumentKey, TransmissionResult result, String errorMessage);

    /** Resuelve a qué documento corresponde una clave del proveedor (para enrutar webhooks async). */
    Optional<Long> findDocumentIdByProviderKey(String providerDocumentKey);

    /** Número de intentos de transmisión ya registrados (para el cap de reintentos de contingencia). */
    int countAttempts(Long electronicDocumentId);

    /** Última clave de proveedor registrada para un documento (para reconciliar PENDIENTE por polling). */
    Optional<String> findLatestProviderKey(Long electronicDocumentId);
}
