package com.vetsoftware.app.breed.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.breed.domain.Breed;
import com.vetsoftware.app.breed.domain.SpecieRef;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia del adaptador de razas contra MySQL real.
 *
 * <p>
 * Sin esta rodaja el {@code @EntityGraph(attributePaths = "specie")} de
 * {@code BreedJpaRepository} y el {@code UPDATE breeds SET enabled = true}
 * nativo de {@code reactivate} solo los ejercitaria produccion. La especie no
 * la siembra {@code SchemaSeed} (es un catalogo maestro sin fila raiz comun a
 * otras features), asi que este test la inserta por su cuenta.
 */
@Import({JpaBreedRepository.class, BreedJpaMapper.class})
@DisplayName("JpaBreedRepository — persistencia de razas contra MySQL real")
class BreedPersistenceIT extends AbstractDataJpaTest {

    private static final Long PERRO_ID = 800L;
    private static final Long GATO_ID = 801L;

    @Autowired
    private JpaBreedRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasEspecies() {
        sembrarEspecie(PERRO_ID, "Perro");
        sembrarEspecie(GATO_ID, "Gato");
        entityManager.flush();
    }

    private void sembrarEspecie(Long id, String nombre) {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO species (id, name, created_date)
                VALUES (:id, :nombre, '2026-01-01 00:00:00')
                """).setParameter("id", id).setParameter("nombre", nombre).executeUpdate();
    }

    private Breed nuevaRaza(String nombre, Long specieId, String specieName) {
        return repository.save(Breed.create(nombre, new SpecieRef(specieId, specieName)));
    }

    @Nested
    @DisplayName("ida y vuelta del agregado")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar y releer conserva nombre, especie y habilitacion")
        void guardar_y_releer_conserva_cada_campo() {
            Breed guardado = nuevaRaza("Labrador", PERRO_ID, "Perro");

            Breed leido = repository.findById(guardado.getId()).orElseThrow();

            assertThat(leido.getName()).isEqualTo("Labrador");
            assertThat(leido.getSpecie()).isEqualTo(new SpecieRef(PERRO_ID, "Perro"));
            assertThat(leido.isEnabled()).isTrue();
            assertThat(leido.getCreatedDate()).isNotNull();
        }

        @Test
        @DisplayName("findById de una raza inexistente devuelve vacio")
        void find_by_id_de_una_raza_inexistente_devuelve_vacio() {
            assertThat(repository.findById(999999L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll y findBySpecieId")
    class Listados {

        @Test
        @DisplayName("findAll trae las razas de todas las especies")
        void find_all_trae_las_razas_de_todas_las_especies() {
            nuevaRaza("Labrador", PERRO_ID, "Perro");
            nuevaRaza("Siames", GATO_ID, "Gato");

            assertThat(repository.findAll()).extracting(Breed::getName).contains("Labrador",
                    "Siames");
        }

        @Test
        @DisplayName("findBySpecieId acota por especie sin traer las de otra")
        void find_by_specie_id_acota_por_especie() {
            nuevaRaza("Labrador", PERRO_ID, "Perro");
            nuevaRaza("Siames", GATO_ID, "Gato");

            List<Breed> perros = repository.findBySpecieId(PERRO_ID);

            assertThat(perros).extracting(Breed::getName).containsExactly("Labrador");
        }
    }

    @Nested
    @DisplayName("delete y reactivate")
    class BajaYReactivacion {

        @Test
        @DisplayName("delete es una baja logica: no vuelve a aparecer en findById")
        void delete_es_una_baja_logica() {
            Breed guardado = nuevaRaza("Labrador", PERRO_ID, "Perro");

            repository.delete(guardado.getId());

            assertThat(repository.findById(guardado.getId())).isEmpty();
        }

        @Test
        @DisplayName("reactivate vuelve a habilitar una raza dada de baja")
        void reactivate_vuelve_a_habilitar_una_raza_dada_de_baja() {
            Breed guardado = nuevaRaza("Labrador", PERRO_ID, "Perro");
            repository.delete(guardado.getId());

            int filas = repository.reactivate(guardado.getId());

            assertThat(filas).isEqualTo(1);
            assertThat(repository.findById(guardado.getId())).map(Breed::isEnabled).contains(true);
        }

        @Test
        @DisplayName("reactivate sobre una raza inexistente no afecta filas")
        void reactivate_sobre_una_raza_inexistente_no_afecta_filas() {
            assertThat(repository.reactivate(999999L)).isZero();
        }
    }
}
