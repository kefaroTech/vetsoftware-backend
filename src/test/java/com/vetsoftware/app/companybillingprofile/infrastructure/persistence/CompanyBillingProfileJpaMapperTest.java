package com.vetsoftware.app.companybillingprofile.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.city.infrastructure.persistence.CityJpaEntity;
import com.vetsoftware.app.companybillingprofile.domain.CityRef;
import com.vetsoftware.app.companybillingprofile.domain.CompanyBillingProfile;
import com.vetsoftware.app.companybillingprofile.domain.PersonKind;
import com.vetsoftware.app.companybillingprofile.domain.TaxIdKind;
import com.vetsoftware.app.companybillingprofile.domain.TaxRegime;
import com.vetsoftware.app.companybillingprofile.testsupport.CompanyBillingProfileMother;
import com.vetsoftware.app.companybillingprofile.testsupport.ReflectionEntities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El unico sitio que conoce a la vez el dominio y la entidad JPA.
 *
 * <p>
 * <b>Lo que esta clase vigila y la rodaja de persistencia no puede</b>: la
 * sobrecarga de escritura de {@code toDomain}. Contra MySQL real las dos
 * sobrecargas producen el mismo resultado —el {@code @EntityGraph} hidrata el
 * municipio igual— y la diferencia es una consulta de mas que ninguna asercion
 * ve. Aqui la ciudad se deja deliberadamente <em>sin nombre</em> en la entidad
 * para que usar la sobrecarga equivocada rompa la asercion.
 */
@DisplayName("CompanyBillingProfileJpaMapper")
class CompanyBillingProfileJpaMapperTest {

    private final CompanyBillingProfileJpaMapper mapper = new CompanyBillingProfileJpaMapper();

    private CityJpaEntity medellin;

    @BeforeEach
    void municipio() throws ReflectiveOperationException {
        medellin = ReflectionEntities.newInstance(CityJpaEntity.class);
        medellin.setId(900L);
        medellin.setName("Medellin");
    }

    @Test
    @DisplayName("ida y vuelta de una sociedad: cada campo vuelve a su sitio")
    void ida_y_vuelta_de_una_sociedad() {
        CompanyBillingProfile original = CompanyBillingProfileMother.persistida(42L);

        CompanyBillingProfile vuelta = mapper.toDomain(mapper.toJpa(original, medellin));

        assertThat(vuelta.getId()).isEqualTo(42L);
        assertThat(vuelta.getCompanyId()).isEqualTo(CompanyBillingProfileMother.COMPANY_ID);
        assertThat(vuelta.getPersonKind()).isEqualTo(PersonKind.LEGAL);
        assertThat(vuelta.getTaxIdKind()).isEqualTo(TaxIdKind.NIT);
        assertThat(vuelta.getTaxId()).isEqualTo(CompanyBillingProfileMother.NIT);
        assertThat(vuelta.getVerificationDigit())
                .isEqualTo(CompanyBillingProfileMother.DIGITO_VERIFICACION);
        assertThat(vuelta.getLegalName()).isEqualTo(CompanyBillingProfileMother.RAZON_SOCIAL);
        assertThat(vuelta.getAddress()).isEqualTo(CompanyBillingProfileMother.DIRECCION);
        assertThat(vuelta.getCity()).isEqualTo(CompanyBillingProfileMother.MEDELLIN);
        assertThat(vuelta.getBillingEmail()).isEqualTo(CompanyBillingProfileMother.CORREO);
        assertThat(vuelta.getTaxRegime()).isEqualTo(TaxRegime.COMMON);
        assertThat(vuelta.isWithholdingAgent()).isTrue();
        assertThat(vuelta.getValidFrom()).isEqualTo(CompanyBillingProfileMother.RIGE_DESDE);
        assertThat(vuelta.getValidTo()).isNull();
        assertThat(vuelta.getCreatedDate()).isEqualTo(CompanyBillingProfileMother.CREADA_EL);
        assertThat(vuelta.getVersion()).isZero();
    }

    @Test
    @DisplayName("ida y vuelta de una persona natural: los cuatro campos de nombre no se cruzan")
    void ida_y_vuelta_de_una_persona_natural() {
        // Los cuatro valores son distintos entre si a proposito: cruzar firstName con
        // middleName en el mapper es un error que un fixture con valores repetidos no
        // detectaria.
        CompanyBillingProfile original = CompanyBillingProfileMother.personaNatural();

        CompanyBillingProfile vuelta = mapper.toDomain(mapper.toJpa(original, medellin));

        assertThat(vuelta.getFirstName()).isEqualTo(CompanyBillingProfileMother.PRIMER_NOMBRE);
        assertThat(vuelta.getMiddleName()).isEqualTo(CompanyBillingProfileMother.OTROS_NOMBRES);
        assertThat(vuelta.getLastName()).isEqualTo(CompanyBillingProfileMother.PRIMER_APELLIDO);
        assertThat(vuelta.getSecondLastName())
                .isEqualTo(CompanyBillingProfileMother.SEGUNDO_APELLIDO);
        assertThat(vuelta.getLegalName()).isNull();
    }

    @Test
    @DisplayName("la version viaja en la IDA: sin ella cada guardado de una ficha ya persistida seria un INSERT")
    void la_version_viaja_en_la_ida() {
        // Es el defecto silencioso que rompe el bloqueo optimista justo en la unica
        // operacion que muta: el cierre de la ficha vigente.
        CompanyBillingProfileJpaEntity entity = mapper
                .toJpa(CompanyBillingProfileMother.persistida(42L), medellin);

        assertThat(entity.getVersion()).isZero();
        assertThat(entity.getId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("la ficha cerrada lleva su valid_to a la entidad, que es lo que apaga la columna generada")
    void la_ficha_cerrada_lleva_su_valid_to() {
        CompanyBillingProfile cerrada = CompanyBillingProfileMother.persistida(41L,
                CompanyBillingProfileMother.COMPANY_ID, CompanyBillingProfileMother.RIGE_DESDE,
                CompanyBillingProfileMother.SUCEDE_DESDE);

        CompanyBillingProfileJpaEntity entity = mapper.toJpa(cerrada, medellin);

        assertThat(entity.getValidTo()).isEqualTo(CompanyBillingProfileMother.SUCEDE_DESDE);
    }

    @Test
    @DisplayName("la entidad nace con enabled=true y el dominio no lo toca: la baja de una ficha es valid_to")
    void la_entidad_nace_habilitada_y_el_dominio_no_lo_toca() {
        // enabled no esta en el dominio a proposito. Tener las dos bajas visibles en el
        // modelo es la forma segura de que alguien acabe cerrando una ficha con la que
        // no es.
        CompanyBillingProfileJpaEntity entity = mapper.toJpa(CompanyBillingProfileMother.sociedad(),
                medellin);

        assertThat(entity.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("el camino de ESCRITURA reusa el ref precargado y no lee el proxy del municipio")
    void el_camino_de_escritura_reusa_el_ref_precargado() throws ReflectiveOperationException {
        // La ciudad de la entidad se deja sin nombre —es lo que devuelve
        // getReferenceById antes de hidratarse—: si la sobrecarga equivocada leyera
        // entity.getCity().getName(), el CityRef no se podria ni construir.
        CompanyBillingProfileJpaEntity entity = mapper
                .toJpa(CompanyBillingProfileMother.persistida(42L), sinHidratar());

        CompanyBillingProfile vuelta = mapper.toDomain(entity,
                CompanyBillingProfileMother.MEDELLIN);

        assertThat(vuelta.getCity()).isEqualTo(new CityRef(900L, "Medellin"));
    }

    private CityJpaEntity sinHidratar() throws ReflectiveOperationException {
        CityJpaEntity proxy = ReflectionEntities.newInstance(CityJpaEntity.class);
        proxy.setId(900L);
        return proxy;
    }
}
