package com.vetsoftware.app.state.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.country.infrastructure.persistence.CountryJpaEntity;
import com.vetsoftware.app.state.domain.CountryRef;
import com.vetsoftware.app.state.domain.State;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("StateJpaMapper — ida y vuelta dominio/entidad")
class StateJpaMapperTest {

    private static final LocalDateTime CREACION = LocalDateTime.of(2026, 1, 15, 10, 30);
    private static final CountryRef COLOMBIA = new CountryRef(1L, "Colombia");

    private final StateJpaMapper mapper = new StateJpaMapper();

    /**
     * {@code CountryJpaEntity} tiene constructor protegido: se dobla en vez de
     * construirla, igual que el resto de entidades JPA cruzadas entre features.
     */
    private static CountryJpaEntity countryEntity(Long id, String name) {
        CountryJpaEntity entity = mock(CountryJpaEntity.class);
        when(entity.getId()).thenReturn(id);
        when(entity.getName()).thenReturn(name);
        return entity;
    }

    @Test
    @DisplayName("toJpa copia los campos propios y usa la entidad de pais recibida")
    void to_jpa_copia_los_campos_propios() {
        CountryJpaEntity country = countryEntity(1L, "Colombia");

        StateJpaEntity entity = mapper
                .toJpa(new State(7L, "Antioquia", COLOMBIA, "05", CREACION, true), country);

        assertThat(entity.getId()).isEqualTo(7L);
        assertThat(entity.getName()).isEqualTo("Antioquia");
        assertThat(entity.getCountry()).isSameAs(country);
        assertThat(entity.getDaneCode()).isEqualTo("05");
        assertThat(entity.getCreatedDate()).isEqualTo(CREACION);
        assertThat(entity.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("toJpa de un departamento nuevo deja el id nulo para que lo asigne la base")
    void to_jpa_deja_el_id_nulo() {
        State nuevo = State.create("Antioquia", COLOMBIA, "05");

        assertThat(mapper.toJpa(nuevo, countryEntity(1L, "Colombia")).getId()).isNull();
    }

    @Test
    @DisplayName("toDomain(entity) reconstruye el pais a partir de la entidad hidratada")
    void to_domain_con_una_sola_entidad_reconstruye_el_pais() {
        CountryJpaEntity country = countryEntity(1L, "Colombia");
        StateJpaEntity entity = mapper
                .toJpa(new State(7L, "Antioquia", COLOMBIA, "05", CREACION, false), country);

        State state = mapper.toDomain(entity);

        assertThat(state.getId()).isEqualTo(7L);
        assertThat(state.getName()).isEqualTo("Antioquia");
        assertThat(state.getCountry()).isEqualTo(new CountryRef(1L, "Colombia"));
        assertThat(state.getDaneCode()).isEqualTo("05");
        assertThat(state.getCreatedDate()).isEqualTo(CREACION);
        assertThat(state.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("toDomain(entity, ref) reusa el ref precargado sin leer la entidad de pais")
    void to_domain_con_ref_precargado_reusa_el_ref() {
        StateJpaEntity entity = mapper.toJpa(
                new State(7L, "Antioquia", COLOMBIA, "05", CREACION, true),
                countryEntity(1L, "Colombia"));
        CountryRef otroRef = new CountryRef(9L, "Otro Pais");

        State state = mapper.toDomain(entity, otroRef);

        assertThat(state.getCountry()).isEqualTo(otroRef);
    }

    @Test
    @DisplayName("la ida y vuelta no pierde ni altera ningun campo")
    void ida_y_vuelta_no_pierde_campos() {
        State original = new State(7L, "Antioquia", COLOMBIA, "05", CREACION, true);

        State vuelta = mapper.toDomain(mapper.toJpa(original, countryEntity(1L, "Colombia")));

        assertThat(vuelta).usingRecursiveComparison().isEqualTo(original);
    }
}
