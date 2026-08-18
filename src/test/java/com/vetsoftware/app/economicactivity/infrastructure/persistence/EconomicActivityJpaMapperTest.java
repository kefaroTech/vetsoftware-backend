package com.vetsoftware.app.economicactivity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.economicactivity.domain.EconomicActivity;
import com.vetsoftware.app.economicactivity.testsupport.EconomicActivityMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** JUnit puro, sin mocks: ida y vuelta dominio <-> entidad JPA. */
@DisplayName("EconomicActivityJpaMapper")
class EconomicActivityJpaMapperTest {

    private final EconomicActivityJpaMapper mapper = new EconomicActivityJpaMapper();

    @Test
    @DisplayName("toJpa traslada cada campo del dominio a la entidad")
    void to_jpa_traslada_cada_campo() {
        EconomicActivity actividad = EconomicActivityMother.existente();

        EconomicActivityJpaEntity entity = mapper.toJpa(actividad);

        assertThat(entity.getId()).isEqualTo(EconomicActivityMother.ECONOMIC_ACTIVITY_ID);
        assertThat(entity.getCode()).isEqualTo(EconomicActivityMother.CODIGO);
        assertThat(entity.getName()).isEqualTo(EconomicActivityMother.NOMBRE);
        assertThat(entity.getCreatedDate()).isEqualTo(EconomicActivityMother.CREADO);
        assertThat(entity.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("toJpa conserva el id nulo de una actividad nueva")
    void to_jpa_conserva_el_id_nulo() {
        EconomicActivityJpaEntity entity = mapper.toJpa(EconomicActivityMother.nueva());

        assertThat(entity.getId()).isNull();
    }

    @Test
    @DisplayName("toDomain reconstruye la actividad con cada campo de la entidad")
    void to_domain_reconstruye_cada_campo() {
        EconomicActivityJpaEntity entity = new EconomicActivityJpaEntity();
        entity.setId(70L);
        entity.setCode("0111");
        entity.setName("Cultivo de cereales");
        entity.setCreatedDate(EconomicActivityMother.CREADO);
        entity.setEnabled(false);

        EconomicActivity actividad = mapper.toDomain(entity);

        assertThat(actividad.getId()).isEqualTo(70L);
        assertThat(actividad.getCode()).isEqualTo("0111");
        assertThat(actividad.getName()).isEqualTo("Cultivo de cereales");
        assertThat(actividad.getCreatedDate()).isEqualTo(EconomicActivityMother.CREADO);
        assertThat(actividad.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("guardar y releer con el mapper conserva la identidad de cada campo")
    void ida_y_vuelta_conserva_cada_campo() {
        EconomicActivity original = EconomicActivityMother.existente();

        EconomicActivity releida = mapper.toDomain(mapper.toJpa(original));

        assertThat(releida.getId()).isEqualTo(original.getId());
        assertThat(releida.getCode()).isEqualTo(original.getCode());
        assertThat(releida.getName()).isEqualTo(original.getName());
        assertThat(releida.getCreatedDate()).isEqualTo(original.getCreatedDate());
        assertThat(releida.isEnabled()).isEqualTo(original.isEnabled());
    }
}
