package com.vetsoftware.app.platformtaxprofile.infrastructure.persistence;

import com.vetsoftware.app.economicactivity.infrastructure.persistence.EconomicActivityJpaEntity;
import com.vetsoftware.app.platformtaxprofile.domain.EconomicActivityRef;
import com.vetsoftware.app.platformtaxprofile.domain.PlatformTaxProfile;
import org.springframework.stereotype.Component;

/**
 * El unico sitio que conoce a la vez el modelo de dominio y la entidad JPA.
 *
 * <p>
 * <strong>Dos {@code toDomain}, y la diferencia no es cosmetica.</strong> El de
 * lectura extrae el {@link EconomicActivityRef} del {@code @ManyToOne} que el
 * {@code @EntityGraph} ya hidrato; el de escritura recibe el ref que el caso de
 * uso ya tenia en la mano. Sin el segundo, reconstruir la ficha despues de un
 * {@code save} leeria {@code entity.getEconomicActivity().getName()} sobre el
 * proxy que devolvio {@code getReferenceById} y dispararia una consulta de
 * hidratacion por cada escritura.
 *
 * <p>
 * <strong>La {@code version} viaja en los dos sentidos.</strong> Sin llevarla
 * en la ida, cada {@code save} de una ficha ya persistida le pasaria a
 * Hibernate una version nula, la tomaria por transitoria y la operacion se
 * convertiria en un {@code INSERT}: el bloqueo optimista dejaria de proteger
 * nada justo en la operacion que cierra la ficha vigente, que es la unica que
 * muta — y el {@code INSERT} chocaria ademas contra
 * {@code uq_platform_tax_profiles_validity}.
 *
 * <p>
 * <strong>No toca {@code current_profile_marker}</strong>: la calcula MySQL y
 * no esta mapeada. Escribirla desde aqui haria que el motor rechazara el
 * {@code INSERT} con el error 3105.
 *
 * <p>
 * <strong>La actividad economica es opcional en los dos sentidos</strong>,
 * porque {@code economic_activity_id} es nulable: {@code null} entra y
 * {@code null} sale, sin construir un {@code EconomicActivityRef} vacio que sus
 * propias invariantes rechazarian.
 */
@Component
public class PlatformTaxProfileJpaMapper {

    /**
     * @param economicActivity
     *            el proxy que devuelve {@code getReferenceById}, sin
     *            {@code SELECT}, o {@code null} si la ficha no declara actividad.
     *            El adaptador lo resuelve por el id que trae el
     *            {@code EconomicActivityRef} del dominio
     */
    public PlatformTaxProfileJpaEntity toJpa(PlatformTaxProfile profile,
            EconomicActivityJpaEntity economicActivity) {
        PlatformTaxProfileJpaEntity entity = new PlatformTaxProfileJpaEntity();
        entity.setId(profile.getId());
        entity.setDocumentType(profile.getDocumentType());
        entity.setDocumentId(profile.getDocumentId());
        entity.setVerificationDigit(profile.getVerificationDigit());
        entity.setLegalName(profile.getLegalName());
        entity.setTaxRegime(profile.getTaxRegime());
        entity.setFiscalEmail(profile.getFiscalEmail());
        entity.setCommercialName(profile.getCommercialName());
        entity.setEconomicActivity(economicActivity);
        entity.setSelfWithholder(profile.isSelfWithholder());
        entity.setValidFrom(profile.getValidFrom());
        entity.setValidTo(profile.getValidTo());
        entity.setCreatedDate(profile.getCreatedDate());
        entity.setVersion(profile.getVersion());
        return entity;
    }

    /**
     * Camino de lectura: el {@code @EntityGraph} ya hidrato
     * {@code entity.getEconomicActivity()}.
     */
    public PlatformTaxProfile toDomain(PlatformTaxProfileJpaEntity entity) {
        EconomicActivityJpaEntity activity = entity.getEconomicActivity();
        EconomicActivityRef ref = activity == null
                ? null
                : new EconomicActivityRef(activity.getId(), activity.getCode(), activity.getName());
        return toDomain(entity, ref);
    }

    /** Camino de escritura: reusa el ref precargado y no toca el proxy. */
    public PlatformTaxProfile toDomain(PlatformTaxProfileJpaEntity entity,
            EconomicActivityRef economicActivity) {
        return new PlatformTaxProfile(entity.getId(), entity.getDocumentType(),
                entity.getDocumentId(), entity.getVerificationDigit(), entity.getLegalName(),
                entity.getTaxRegime(), entity.getFiscalEmail(), entity.getCommercialName(),
                economicActivity, entity.isSelfWithholder(), entity.getValidFrom(),
                entity.getValidTo(), entity.getCreatedDate(), entity.getVersion());
    }
}
