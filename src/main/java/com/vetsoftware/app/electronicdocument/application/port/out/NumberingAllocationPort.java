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

    /**
     * Lee la resolución activa SIN consumir consecutivo (no incrementa {@code current_number}): devuelve solo
     * resolución + prefijo ({@code consecutive} = null). Para documentos cuyo consecutivo lo asigna el
     * proveedor DIAN (POS auto-increment), que igual exige {@code resolution_number}+{@code prefix} en el
     * request. Vacío si la empresa no tiene resolución activa para el tipo.
     */
    Optional<AllocatedNumber> peekActive(Long companyId, ElectronicDocumentType documentType);

    /**
     * Libera un consecutivo asignado ante un rechazo, para evitar huecos en la secuencia fiscal. Solo lo
     * recupera (decrementa {@code current_number} de vuelta al {@code consecutive}) si éste fue el último
     * número entregado y nadie tomó el siguiente — bajo el mismo bloqueo pesimista de {@code allocate}.
     * Devuelve {@code true} si lo recuperó; {@code false} si ya no es seguro (el hueco permanece).
     */
    boolean release(Long companyId, ElectronicDocumentType documentType, Long consecutive);

    record AllocatedNumber(String resolutionNumber, String prefix, Long consecutive) {}
}
