package com.vetsoftware.app.registration.application.port.out;

import com.vetsoftware.app.subscription.domain.BillingCycle;
import java.util.List;

/**
 * Los artículos {@code trial_eligibility = 'ELIGIBLE'} de la plataforma, solo
 * lo que hace falta para conceder su prueba.
 *
 * <p>
 * <strong>No es una consulta propia: delega en la de
 * {@code subscription}.</strong> Esta feature necesita conceder una
 * {@code company_trial_grants} por artículo <em>antes</em> de que
 * {@code subscription} firme la línea correspondiente
 * ({@code fk_subscription_items_trial_grant} exige que la concesión ya exista),
 * así que las dos preguntas —qué se concede, qué se firma— tienen que salir de
 * la misma fuente o un artículo podría acabar con concesión y sin línea, o al
 * revés. El adaptador de este puerto es el único sitio de {@code registration}
 * que conoce a {@code subscription}.
 */
public interface EligibleTrialCatalogItemsPort {

    List<EligibleTrialCatalogItem> findAll(BillingCycle billingCycle);

    /** Copia de solo lo que hace falta para conceder una prueba: nada de precio. */
    record EligibleTrialCatalogItem(Long catalogItemId, int defaultTrialDays, String trialOutcome) {
    }
}
