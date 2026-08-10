package com.vetsoftware.app.country.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.country.domain.Country;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CountryJpaMapper — ida y vuelta dominio/entidad")
class CountryJpaMapperTest {

    private static final LocalDateTime CREACION = LocalDateTime.of(2026, 1, 15, 10, 30);

    private final CountryJpaMapper mapper = new CountryJpaMapper();

    @Test
    @DisplayName("toJpa copia los cuatro campos del dominio")
    void to_jpa_copia_los_cuatro_campos() {
        CountryJpaEntity entity = mapper.toJpa(new Country(7L, "Colombia", CREACION, true));

        assertThat(entity.getId()).isEqualTo(7L);
        assertThat(entity.getName()).isEqualTo("Colombia");
        assertThat(entity.getCreatedDate()).isEqualTo(CREACION);
        assertThat(entity.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("toJpa de un pais nuevo deja el id nulo para que lo asigne la base")
    void to_jpa_deja_el_id_nulo() {
        assertThat(mapper.toJpa(Country.create("Colombia")).getId()).isNull();
    }

    @Test
    @DisplayName("toDomain reconstruye el pais con los mismos valores")
    void to_domain_reconstruye_el_pais() {
        CountryJpaEntity entity = mapper.toJpa(new Country(7L, "Colombia", CREACION, false));

        Country country = mapper.toDomain(entity);

        assertThat(country.getId()).isEqualTo(7L);
        assertThat(country.getName()).isEqualTo("Colombia");
        assertThat(country.getCreatedDate()).isEqualTo(CREACION);
        assertThat(country.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("la ida y vuelta no pierde ni altera ningun campo")
    void ida_y_vuelta_no_pierde_campos() {
        Country original = new Country(7L, "Colombia", CREACION, true);

        Country vuelta = mapper.toDomain(mapper.toJpa(original));

        assertThat(vuelta).usingRecursiveComparison().isEqualTo(original);
    }
}
