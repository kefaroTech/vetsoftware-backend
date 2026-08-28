package com.vetsoftware.app.companybillingprofile.infrastructure.persistence;

import com.vetsoftware.app.city.infrastructure.persistence.CityJpaEntity;
import com.vetsoftware.app.companybillingprofile.domain.CityRef;
import com.vetsoftware.app.companybillingprofile.domain.CompanyBillingProfile;
import org.springframework.stereotype.Component;

/**
 * El unico sitio que conoce a la vez el modelo de dominio y la entidad JPA.
 *
 * <p>
 * <strong>Dos {@code toDomain}, y la diferencia no es cosmetica.</strong> El de
 * lectura extrae la {@link CityRef} del {@code @ManyToOne} que el
 * {@code @EntityGraph} ya hidrato; el de escritura recibe el {@code CityRef}
 * que el caso de uso ya tenia en la mano. Sin el segundo, reconstruir la ficha
 * despues de un {@code save} leeria {@code entity.getCity().getName()} sobre el
 * proxy que devolvio {@code getReferenceById} y dispararia una consulta de
 * hidratacion por cada escritura.
 *
 * <p>
 * <strong>La {@code version} viaja en los dos sentidos.</strong> Sin llevarla
 * en la ida, cada {@code save} de una ficha ya persistida le pasaria a
 * Hibernate una version nula y la operacion se convertiria en un
 * {@code INSERT}: el bloqueo optimista dejaria de proteger nada justo en la
 * operacion que cierra la ficha vigente, que es la unica que muta.
 *
 * <p>
 * <strong>{@code enabled} no se copia en ninguno de los dos sentidos.</strong>
 * No esta en el dominio a proposito —el cierre de una ficha es
 * {@code valid_to}— y la entidad JPA lo deja en su inicializador de campo.
 */
@Component
public class CompanyBillingProfileJpaMapper {

    /**
     * @param city
     *            el proxy que devuelve {@code getReferenceById}, sin
     *            {@code SELECT}. El adaptador lo resuelve por el id que trae el
     *            {@code CityRef} del dominio
     */
    public CompanyBillingProfileJpaEntity toJpa(CompanyBillingProfile profile, CityJpaEntity city) {
        CompanyBillingProfileJpaEntity entity = new CompanyBillingProfileJpaEntity();
        entity.setId(profile.getId());
        entity.setCompanyId(profile.getCompanyId());
        entity.setPersonKind(profile.getPersonKind());
        entity.setTaxIdKind(profile.getTaxIdKind());
        entity.setTaxId(profile.getTaxId());
        entity.setVerificationDigit(profile.getVerificationDigit());
        entity.setLegalName(profile.getLegalName());
        entity.setFirstName(profile.getFirstName());
        entity.setMiddleName(profile.getMiddleName());
        entity.setLastName(profile.getLastName());
        entity.setSecondLastName(profile.getSecondLastName());
        entity.setAddress(profile.getAddress());
        entity.setCity(city);
        entity.setBillingEmail(profile.getBillingEmail());
        entity.setTaxRegime(profile.getTaxRegime());
        entity.setWithholdingAgent(profile.isWithholdingAgent());
        entity.setValidFrom(profile.getValidFrom());
        entity.setValidTo(profile.getValidTo());
        entity.setCreatedDate(profile.getCreatedDate());
        entity.setVersion(profile.getVersion());
        return entity;
    }

    /**
     * Camino de lectura: el {@code @EntityGraph} ya hidrato
     * {@code entity.getCity()}.
     */
    public CompanyBillingProfile toDomain(CompanyBillingProfileJpaEntity entity) {
        CityJpaEntity city = entity.getCity();
        return toDomain(entity, new CityRef(city.getId(), city.getName()));
    }

    /** Camino de escritura: reusa el ref precargado y no toca el proxy. */
    public CompanyBillingProfile toDomain(CompanyBillingProfileJpaEntity entity, CityRef city) {
        return new CompanyBillingProfile(entity.getId(), entity.getCompanyId(),
                entity.getPersonKind(), entity.getTaxIdKind(), entity.getTaxId(),
                entity.getVerificationDigit(), entity.getLegalName(), entity.getFirstName(),
                entity.getMiddleName(), entity.getLastName(), entity.getSecondLastName(),
                entity.getAddress(), city, entity.getBillingEmail(), entity.getTaxRegime(),
                entity.isWithholdingAgent(), entity.getValidFrom(), entity.getValidTo(),
                entity.getCreatedDate(), entity.getVersion());
    }
}
