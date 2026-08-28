package com.vetsoftware.app.companylimitoverride.application.port.out;

import com.vetsoftware.app.companylimitoverride.domain.LimitCandidates;

/**
 * Recoge los candidatos a techo que <em>no</em> son de esta rodaja: los
 * congelados en el contrato y los escalones de fábrica de lo que la empresa usa
 * gratis, más la fecha que decide D-74.
 *
 * <p>
 * <strong>Es un solo puerto y no tres a propósito.</strong> Los tres datos se
 * piden siempre juntos, para la misma empresa y el mismo eje, y solo sirven
 * para alimentar una llamada a {@code EffectiveLimitResolver}: partirlos en
 * tres puertos multiplicaría por tres los adaptadores sin que ningún llamador
 * pudiera usar uno sin los otros dos.
 *
 * <p>
 * <strong>Toda lectura lleva la empresa.</strong> No hay ninguna variante
 * ancha, ni la puede haber: los techos congelados de un eje sin filtro de
 * empresa son las cifras de todos los tenants sobre ese eje.
 */
public interface LimitCandidatesQueryPort {

    LimitCandidates findCandidates(Long companyId, Long limitDimensionId);
}
