package com.vetsoftware.app.specie.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.specie.domain.Specie;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SpecieJpaMapper")
class SpecieJpaMapperTest {

    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private final SpecieJpaMapper mapper = new SpecieJpaMapper();

    @Test
    @DisplayName("toJpa copia cada campo del dominio a la entidad")
    void to_jpa_copia_cada_campo() {
        Specie specie = new Specie(1L, "Perro", CREADO, true);

        SpecieJpaEntity entity = mapper.toJpa(specie);

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getName()).isEqualTo("Perro");
        assertThat(entity.getCreatedDate()).isEqualTo(CREADO);
        assertThat(entity.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("toDomain copia cada campo de la entidad al dominio")
    void to_domain_copia_cada_campo() {
        SpecieJpaEntity entity = new SpecieJpaEntity();
        entity.setId(2L);
        entity.setName("Gato");
        entity.setCreatedDate(CREADO);
        entity.setEnabled(false);

        Specie specie = mapper.toDomain(entity);

        assertThat(specie.getId()).isEqualTo(2L);
        assertThat(specie.getName()).isEqualTo("Gato");
        assertThat(specie.getCreatedDate()).isEqualTo(CREADO);
        assertThat(specie.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("ida y vuelta conserva cada campo del agregado")
    void ida_y_vuelta_conserva_cada_campo() {
        Specie original = new Specie(3L, "Ave", CREADO, true);

        Specie reconstruida = mapper.toDomain(mapper.toJpa(original));

        assertThat(reconstruida.getId()).isEqualTo(original.getId());
        assertThat(reconstruida.getName()).isEqualTo(original.getName());
        assertThat(reconstruida.getCreatedDate()).isEqualTo(original.getCreatedDate());
        assertThat(reconstruida.isEnabled()).isEqualTo(original.isEnabled());
    }
}
