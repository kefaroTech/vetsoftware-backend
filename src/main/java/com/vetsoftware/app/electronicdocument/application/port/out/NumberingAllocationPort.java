package com.vetsoftware.app.electronicdocument.application.port.out;

import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import java.util.Optional;

/**
 * Asigna el siguiente consecutivo fiscal de la {@code NumberingResolution} activa de una empresa para un
 * tipo de documento, incrementándolo de forma ATÓMICA (la implementación serializa con bloqueo pesimista).
 * Cruza a la feature {@code numberingresolution} sin acoplar su dominio.
 */
public interface NumberingAllocationPort {

    /** Devuelve el número fiscal asignado, o vacío si la empresa no tiene resolución activa para el tipo. */
    Optional<AllocatedNumber> allocate(Long companyId, ElectronicDocumentType documentType);

    record AllocatedNumber(String resolutionNumber, String prefix, Long consecutive) {}
}
