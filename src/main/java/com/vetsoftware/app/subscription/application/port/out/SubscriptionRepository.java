package com.vetsoftware.app.subscription.application.port.out;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscription.domain.Subscription;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida del contrato.
 *
 * <p>
 * <strong>No declara ningun {@code findById(id)} ancho, y es
 * deliberado.</strong> {@code subscriptions} lleva {@code company_id NOT NULL},
 * asi que toda carga por id de este slice tiene que ir acotada: no existiendo
 * la variante ancha, no hay nada que el proximo copy-paste pueda llamar por
 * error, y {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA} se satisface por
 * construccion en vez de por disciplina. La consola de plataforma tambien pasa
 * empresa: un principal SYSTEM la lleva en la cabecera {@code X-Company-Id}.
 */
public interface SubscriptionRepository {

    Subscription save(Subscription subscription);

    Optional<Subscription> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * El contrato vigente de una empresa, con el criterio de
     * {@code SubscriptionStatus.CURRENT} —el mismo que alimenta
     * {@code active_marker}—. Como maximo hay uno: lo garantiza
     * {@code uq_subscriptions_active_company}.
     */
    Optional<Subscription> findCurrentByCompanyId(Long companyId);

    /**
     * Toma la fila del contrato con bloqueo pesimista dentro de la transaccion en
     * curso. Es lo que serializa el <em>leer-y-luego-escribir</em> de la
     * comprobacion de solape: sin el, dos transacciones concurrentes pasan las dos
     * la comprobacion y las dos insertan. Mismo patron que el solape de citas
     * (changeset {@code 226}), donde el indice unico es la ultima linea de defensa
     * del caso exacto y el bloqueo es la primera para todo lo demas.
     */
    Optional<Subscription> lockByIdAndCompanyId(Long id, Long companyId);

    /**
     * Reclama una pagina global del barrido interno con bloqueo no bloqueante. Solo
     * el worker SYSTEM de lifecycle puede consumir esta operacion.
     */
    List<Subscription> lockLifecycleBatchAfter(long afterId, int batchSize);

    PageResult<Subscription> findAllByCompanyId(Long companyId, int page, int pageSize);

    /**
     * Barrido de plataforma sin filtro de empresa. Solo lo puede consumir un caso
     * de uso cerrado a {@code hasRole('SYSTEM')} a secas (BE-29).
     */
    PageResult<Subscription> findAll(int page, int pageSize);
}
