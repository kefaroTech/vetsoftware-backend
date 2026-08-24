package com.vetsoftware.app.subscriptionbilling.infrastructure.persistence;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import com.vetsoftware.app.subscriptionbilling.application.port.out.BillingDocumentSequenceRepository;
import com.vetsoftware.app.subscriptionbilling.domain.BillingDocumentSequence;
import com.vetsoftware.app.subscriptionbilling.domain.BillingDocumentSequenceNotFoundException;
import com.vetsoftware.app.subscriptionbilling.domain.DocumentNumber;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

/**
 * Adaptador de {@link BillingDocumentSequenceRepository}.
 *
 * <p>
 * <b>Este adaptador no declara {@code @Transactional}, y es lo importante de
 * él.</b> El consecutivo se lee bloqueado y se incrementa <b>dentro de la
 * transacción del caso de uso</b>: si el documento no llega a existir, el
 * {@code rollback} devuelve también el número y la serie no deja huecos. Un
 * {@code REQUIRES_NEW} aquí confirmaría el incremento por su cuenta y cada
 * emisión fallida se comería un número — que es lo correcto para el consecutivo
 * fiscal de la DIAN, donde el hueco es lo prohibido, y lo incorrecto para el
 * interno.
 *
 * <p>
 * Como {@link #nextNumber} ejecuta una consulta {@code @Modifying}, un caller
 * sin transacción falla en el acto en vez de dejar el incremento sin bloqueo.
 * Es un modo de fallo deseable: obliga a que el número se consuma donde se usa.
 */
@Repository
public class JpaBillingDocumentSequenceRepository implements BillingDocumentSequenceRepository {

    private final BillingDocumentSequenceJpaRepository jpaRepository;
    private final BillingDocumentSequenceJpaMapper mapper;

    public JpaBillingDocumentSequenceRepository(BillingDocumentSequenceJpaRepository jpaRepository,
            BillingDocumentSequenceJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public DocumentNumber nextNumber(String prefix) {
        // Primero el SELECT ... FOR UPDATE: es lo que serializa el read-then-write.
        // Un "maximo mas uno" no puede hacerlo -dos procesos leen el mismo maximo-, y
        // por eso el consecutivo es una tabla y no una funcion de agregado.
        long value = jpaRepository.lockNextValue(prefix)
                .orElseThrow(() -> new BillingDocumentSequenceNotFoundException(prefix));
        // Y despues el incremento, sobre la fila que este proceso ya tiene bloqueada.
        // Las dos sentencias son una sola operacion logica y van pegadas.
        jpaRepository.advance(prefix);
        return new DocumentNumber(prefix, value);
    }

    @Override
    public BillingDocumentSequence save(BillingDocumentSequence sequence) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(sequence)));
    }

    @Override
    public Optional<BillingDocumentSequence> findByPrefix(String prefix) {
        return jpaRepository.findByPrefix(prefix).map(mapper::toDomain);
    }

    @Override
    public PageResult<BillingDocumentSequence> findAll(int page, int pageSize) {
        Sort order = Sort.by(Sort.Direction.ASC, "prefix").and(Sort.by(Sort.Direction.ASC, "id"));
        return Pages.result(jpaRepository.findAll(Pages.request(page, pageSize, order)),
                mapper::toDomain);
    }
}
