package com.vetsoftware.app.specie.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.specie.domain.Specie;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia del adaptador de especies contra MySQL real.
 *
 * <p>
 * Sin esta rodaja el {@code UNIQUE} de {@code species.name} y el
 * {@code UPDATE species SET enabled = true} nativo de
 * {@code SpecieJpaRepository.reactivate} solo los ejercitaria produccion.
 * {@code Specie} es la raiz del catalogo (no tiene FK a otra feature), asi que
 * a diferencia de {@code BreedPersistenceIT} no hace falta sembrar nada antes.
 */
@Import({JpaSpecieRepository.class, SpecieJpaMapper.class})
@DisplayName("JpaSpecieRepository — persistencia de especies contra MySQL real")
class SpeciePersistenceIT extends AbstractDataJpaTest {

    @Autowired
    private JpaSpecieRepository repository;

    private Specie nuevaEspecie(String nombre) {
        return repository.save(Specie.create(nombre));
    }

    @Nested
    @DisplayName("ida y vuelta del agregado")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar y releer conserva nombre y habilitacion")
        void guardar_y_releer_conserva_cada_campo() {
            Specie guardada = nuevaEspecie("Perro");

            Specie leida = repository.findById(guardada.getId()).orElseThrow();

            assertThat(leida.getName()).isEqualTo("Perro");
            assertThat(leida.isEnabled()).isTrue();
            assertThat(leida.getCreatedDate()).isNotNull();
        }

        @Test
        @DisplayName("findById de una especie inexistente devuelve vacio")
        void find_by_id_de_una_especie_inexistente_devuelve_vacio() {
            assertThat(repository.findById(999999L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll")
    class Listados {

        @Test
        @DisplayName("findAll trae todas las especies guardadas")
        void find_all_trae_todas_las_especies_guardadas() {
            nuevaEspecie("Perro");
            nuevaEspecie("Gato");

            assertThat(repository.findAll()).extracting(Specie::getName).contains("Perro", "Gato");
        }
    }

    @Nested
    @DisplayName("delete y reactivate")
    class BajaYReactivacion {

        @Test
        @DisplayName("delete es una baja logica: no vuelve a aparecer en findById")
        void delete_es_una_baja_logica() {
            Specie guardada = nuevaEspecie("Perro");

            repository.delete(guardada.getId());

            assertThat(repository.findById(guardada.getId())).isEmpty();
        }

        @Test
        @DisplayName("reactivate vuelve a habilitar una especie dada de baja")
        void reactivate_vuelve_a_habilitar_una_especie_dada_de_baja() {
            Specie guardada = nuevaEspecie("Perro");
            repository.delete(guardada.getId());

            int filas = repository.reactivate(guardada.getId());

            assertThat(filas).isEqualTo(1);
            assertThat(repository.findById(guardada.getId())).map(Specie::isEnabled).contains(true);
        }

        @Test
        @DisplayName("reactivate sobre una especie inexistente no afecta filas")
        void reactivate_sobre_una_especie_inexistente_no_afecta_filas() {
            assertThat(repository.reactivate(999999L)).isZero();
        }
    }
}
