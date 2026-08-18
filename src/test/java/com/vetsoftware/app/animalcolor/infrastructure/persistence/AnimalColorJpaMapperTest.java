package com.vetsoftware.app.animalcolor.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animalcolor.domain.AnimalColor;
import com.vetsoftware.app.animalcolor.domain.SpecieRef;
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
@DisplayName("AnimalColorJpaMapper — ida y vuelta dominio <-> entidad JPA")
class AnimalColorJpaMapperTest {

    private final AnimalColorJpaMapper mapper = new AnimalColorJpaMapper();

    @Mock
    private SpecieJpaEntity specieEntity;

    @Test
    @DisplayName("toJpa copia cada campo del dominio, incluida la especie dada")
    void to_jpa_copia_cada_campo_del_dominio() {
        LocalDateTime creado = LocalDateTime.of(2026, 1, 15, 10, 30);
        AnimalColor color = new AnimalColor(2L, "Negro", new SpecieRef(1L, "Perro"), creado, true);

        AnimalColorJpaEntity entity = mapper.toJpa(color, specieEntity);

        assertThat(entity.getId()).isEqualTo(2L);
        assertThat(entity.getName()).isEqualTo("Negro");
        assertThat(entity.getSpecie()).isSameAs(specieEntity);
        assertThat(entity.getCreatedDate()).isEqualTo(creado);
        assertThat(entity.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("toJpa conserva enabled=false de un color deshabilitado")
    void to_jpa_conserva_enabled_false() {
        AnimalColor color = new AnimalColor(2L, "Negro", new SpecieRef(1L, "Perro"),
                LocalDateTime.of(2026, 1, 15, 10, 30), false);

        AnimalColorJpaEntity entity = mapper.toJpa(color, specieEntity);

        assertThat(entity.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("toDomain(entity) reconstruye la especie desde la relacion JPA")
    void to_domain_reconstruye_la_especie_desde_la_relacion() {
        when(specieEntity.getId()).thenReturn(1L);
        when(specieEntity.getName()).thenReturn("Perro");
        LocalDateTime creado = LocalDateTime.of(2026, 1, 15, 10, 30);
        AnimalColorJpaEntity entity = new AnimalColorJpaEntity();
        entity.setId(2L);
        entity.setName("Negro");
        entity.setSpecie(specieEntity);
        entity.setCreatedDate(creado);
        entity.setEnabled(false);

        AnimalColor color = mapper.toDomain(entity);

        assertThat(color.getId()).isEqualTo(2L);
        assertThat(color.getName()).isEqualTo("Negro");
        assertThat(color.getSpecie()).isEqualTo(new SpecieRef(1L, "Perro"));
        assertThat(color.getCreatedDate()).isEqualTo(creado);
        assertThat(color.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("toDomain(entity, ref) usa la referencia dada sin tocar la relacion JPA")
    void to_domain_con_ref_usa_la_referencia_dada() {
        LocalDateTime creado = LocalDateTime.of(2026, 1, 15, 10, 30);
        AnimalColorJpaEntity entity = new AnimalColorJpaEntity();
        entity.setId(2L);
        entity.setName("Negro");
        entity.setCreatedDate(creado);
        entity.setEnabled(true);
        SpecieRef ref = new SpecieRef(9L, "Gato");

        AnimalColor color = mapper.toDomain(entity, ref);

        assertThat(color.getSpecie()).isEqualTo(ref);
        assertThat(color.getName()).isEqualTo("Negro");
    }
}
