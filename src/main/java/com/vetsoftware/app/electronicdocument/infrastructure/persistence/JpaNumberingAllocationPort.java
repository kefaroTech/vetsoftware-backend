package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import com.vetsoftware.app.electronicdocument.application.port.out.NumberingAllocationPort;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import com.vetsoftware.app.numberingresolution.infrastructure.persistence.NumberingResolutionJpaEntity;
import com.vetsoftware.app.numberingresolution.infrastructure.persistence.NumberingResolutionJpaRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Adapter: asigna el consecutivo desde la {@code NumberingResolution} activa de la empresa, con bloqueo
 * pesimista (FOR UPDATE) para serializar emisiones concurrentes. Valida vigencia y rango antes de asignar.
 * Corre dentro de la transacción de emisión (el caso de uso es {@code @Transactional}), así que el lock se
 * mantiene hasta el commit. Único cruce permitido de vertical slicing: usa la persistencia de numberingresolution.
 */
@Component
public class JpaNumberingAllocationPort implements NumberingAllocationPort {
    private final NumberingResolutionJpaRepository repository;

    public JpaNumberingAllocationPort(NumberingResolutionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<AllocatedNumber> allocate(Long companyId, ElectronicDocumentType documentType) {
        NumberingResolutionJpaEntity r =
                repository.lockActiveForUpdate(companyId, documentType.name()).orElse(null);
        if (r == null) return Optional.empty();

        LocalDate today = LocalDate.now();
        if (today.isBefore(r.getValidFrom()) || today.isAfter(r.getValidTo())) {
            throw new IllegalStateException("La resolución de numeración " + r.getResolutionNumber()
                    + " no está vigente (" + r.getValidFrom() + " a " + r.getValidTo() + ").");
        }
        Long consecutive = r.getCurrentNumber();
        if (consecutive == null || consecutive > r.getRangeTo()) {
            throw new IllegalStateException("La resolución de numeración " + r.getResolutionNumber()
                    + " agotó su rango (hasta " + r.getRangeTo() + ").");
        }

        r.setCurrentNumber(consecutive + 1);
        repository.save(r);
        return Optional.of(new AllocatedNumber(r.getResolutionNumber(), r.getPrefix(), consecutive));
    }

    @Override
    public Optional<AllocatedNumber> peekActive(Long companyId, ElectronicDocumentType documentType) {
        NumberingResolutionJpaEntity r = repository.findActive(companyId, documentType.name()).orElse(null);
        if (r == null) return Optional.empty();

        LocalDate today = LocalDate.now();
        if (today.isBefore(r.getValidFrom()) || today.isAfter(r.getValidTo())) {
            throw new IllegalStateException("La resolución de numeración " + r.getResolutionNumber()
                    + " no está vigente (" + r.getValidFrom() + " a " + r.getValidTo() + ").");
        }
        // Sin consumir consecutivo: el proveedor (POS auto-increment) lo asigna. Solo resolución + prefijo.
        return Optional.of(new AllocatedNumber(r.getResolutionNumber(), r.getPrefix(), null));
    }

    @Override
    public boolean release(Long companyId, ElectronicDocumentType documentType, Long consecutive) {
        if (consecutive == null) return false;
        NumberingResolutionJpaEntity r =
                repository.lockActiveForUpdate(companyId, documentType.name()).orElse(null);
        if (r == null) return false;

        // Solo se recupera el consecutivo si fue el último entregado y nadie tomó el siguiente (recuperación
        // EN ORDEN). Bajo el FOR UPDATE de la emisión síncrona siempre se cumple; en rutas async es
        // oportunista (si ya se asignaron números posteriores, no se toca nada y el hueco permanece).
        //
        // DECISIÓN (no implementar free-list de consecutivos liberados): la doc DIAN/MATIAS exige numeración
        // ascendente y NO reutilizar números — "Continúa con el siguiente número secuencial. No repitas
        // números" (FAQ) y VAL-ID-003 "Debe ser consecutivo según la resolución; no puede haber saltos
        // (excepto por anulaciones)". Reutilizar un número liberado FUERA DE ORDEN (tras aceptar números
        // mayores) violaría esa regla y MATIAS lo rechazaría. Por eso el hueco que deja un rechazo async ya
        // pasado de turno es DEFINITIVO y justificable (documento rechazado, nunca aceptado por la DIAN): se
        // conserva el número + motivo en la bitácora de transmisión, no se rellena.
        if (!Long.valueOf(consecutive + 1).equals(r.getCurrentNumber())) return false;

        r.setCurrentNumber(consecutive);
        repository.save(r);
        return true;
    }
}
