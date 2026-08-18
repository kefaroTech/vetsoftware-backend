package com.vetsoftware.app.breed.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.breed.domain.Breed;
import com.vetsoftware.app.breed.domain.SpecieRef;
import com.vetsoftware.app.specie.infrastructure.persistence.SpecieJpaEntity;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El mapper es el unico punto que conoce dominio y entidad JPA a la vez, asi
 * que un campo cruzado aqui no lo detecta ninguna otra capa.
 *
 * <p>
 * {@code SpecieJpaEntity} se mockea porque su constructor sin argumentos es
 * {@code protected} y no es instanciable desde este paquete. No tiene logica:
 * es un portador de datos, y mockearlo no oculta comportamiento.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BreedJpaMapper — ida y vuelta dominio <-> entidad JPA")
class BreedJpaMapperTest {

    private final BreedJpaMapper mapper = new BreedJpaMapper();

    @Mock
    private SpecieJpaEntity specieEntity;

    @Test
    @DisplayName("toJpa copia cada campo del dominio, incluida la especie dada")
    void to_jpa_copia_cada_campo_del_dominio() {
        LocalDateTime creado = LocalDateTime.of(2026, 1, 15, 10, 30);
        Breed breed = new Breed(2L, "Labrador", new SpecieRef(1L, "Perro"), creado, true);

        BreedJpaEntity entity = mapper.toJpa(breed, specieEntity);

        assertThat(entity.getId()).isEqualTo(2L);
        assertThat(entity.getName()).isEqualTo("Labrador");
        assertThat(entity.getSpecie()).isSameAs(specieEntity);
        assertThat(entity.getCreatedDate()).isEqualTo(creado);
        assertThat(entity.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("toJpa conserva enabled=false de una raza deshabilitada")
    void to_jpa_conserva_enabled_false() {
        Breed breed = new Breed(2L, "Labrador", new SpecieRef(1L, "Perro"),
                LocalDateTime.of(2026, 1, 15, 10, 30), false);

        BreedJpaEntity entity = mapper.toJpa(breed, specieEntity);

        assertThat(entity.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("toDomain(entity) reconstruye la especie desde la relacion JPA")
    void to_domain_reconstruye_la_especie_desde_la_relacion() {
        when(specieEntity.getId()).thenReturn(1L);
        when(specieEntity.getName()).thenReturn("Perro");
        LocalDateTime creado = LocalDateTime.of(2026, 1, 15, 10, 30);
        BreedJpaEntity entity = new BreedJpaEntity();
        entity.setId(2L);
        entity.setName("Labrador");
        entity.setSpecie(specieEntity);
        entity.setCreatedDate(creado);
        entity.setEnabled(false);

        Breed breed = mapper.toDomain(entity);

        assertThat(breed.getId()).isEqualTo(2L);
        assertThat(breed.getName()).isEqualTo("Labrador");
        assertThat(breed.getSpecie()).isEqualTo(new SpecieRef(1L, "Perro"));
        assertThat(breed.getCreatedDate()).isEqualTo(creado);
        assertThat(breed.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("toDomain(entity, ref) usa la referencia dada sin tocar la relacion JPA")
    void to_domain_con_ref_usa_la_referencia_dada() {
        LocalDateTime creado = LocalDateTime.of(2026, 1, 15, 10, 30);
        BreedJpaEntity entity = new BreedJpaEntity();
        entity.setId(2L);
        entity.setName("Labrador");
        entity.setCreatedDate(creado);
        entity.setEnabled(true);
        SpecieRef ref = new SpecieRef(9L, "Gato");

        Breed breed = mapper.toDomain(entity, ref);

        assertThat(breed.getSpecie()).isEqualTo(ref);
        assertThat(breed.getName()).isEqualTo("Labrador");
    }
}
