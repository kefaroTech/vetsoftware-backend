package com.vetsoftware.app.animalcolor.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.animalcolor.domain.AnimalColor;
import com.vetsoftware.app.animalcolor.domain.SpecieRef;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
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
 * Rodaja de persistencia del adaptador de colores contra MySQL real.
 *
 * <p>
 * Sin esta rodaja el {@code @EntityGraph(attributePaths = "specie")} de
 * {@code AnimalColorJpaRepository} y el {@code UPDATE animal_colors SET
 * enabled = true} nativo de {@code reactivate} solo los ejercitaria produccion.
 * La especie no la siembra {@code SchemaSeed} (es un catalogo maestro sin fila
 * raiz comun a otras features), asi que este test la inserta por su cuenta.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaAnimalColorRepository — persistencia de colores contra MySQL real")
class AnimalColorPersistenceIT extends AbstractDataJpaTest {

    private static final Long PERRO_ID = 800L;
    private static final Long GATO_ID = 801L;

    @Autowired
    private JpaAnimalColorRepository repository;

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

    private AnimalColor nuevoColor(String nombre, Long specieId, String specieName) {
        return repository.save(AnimalColor.create(nombre, new SpecieRef(specieId, specieName)));
    }

    @Nested
    @DisplayName("ida y vuelta del agregado")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar y releer conserva nombre, especie y habilitacion")
        void guardar_y_releer_conserva_cada_campo() {
            AnimalColor guardado = nuevoColor("Negro", PERRO_ID, "Perro");

            AnimalColor leido = repository.findById(guardado.getId()).orElseThrow();

            assertThat(leido.getName()).isEqualTo("Negro");
            assertThat(leido.getSpecie()).isEqualTo(new SpecieRef(PERRO_ID, "Perro"));
            assertThat(leido.isEnabled()).isTrue();
            assertThat(leido.getCreatedDate()).isNotNull();
        }

        @Test
        @DisplayName("findById de un color inexistente devuelve vacio")
        void find_by_id_de_un_color_inexistente_devuelve_vacio() {
            assertThat(repository.findById(999999L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll y findBySpecieId")
    class Listados {

        @Test
        @DisplayName("findAll trae los colores de todas las especies")
        void find_all_trae_los_colores_de_todas_las_especies() {
            nuevoColor("Negro", PERRO_ID, "Perro");
            nuevoColor("Blanco", GATO_ID, "Gato");

            assertThat(repository.findAll()).extracting(AnimalColor::getName).contains("Negro",
                    "Blanco");
        }

        @Test
        @DisplayName("findBySpecieId acota por especie sin traer los de otra")
        void find_by_specie_id_acota_por_especie() {
            nuevoColor("Negro", PERRO_ID, "Perro");
            nuevoColor("Blanco", GATO_ID, "Gato");

            List<AnimalColor> perros = repository.findBySpecieId(PERRO_ID);

            assertThat(perros).extracting(AnimalColor::getName).containsExactly("Negro");
        }
    }

    @Nested
    @DisplayName("delete y reactivate")
    class BajaYReactivacion {

        @Test
        @DisplayName("delete es una baja logica: no vuelve a aparecer en findById")
        void delete_es_una_baja_logica() {
            AnimalColor guardado = nuevoColor("Negro", PERRO_ID, "Perro");

            repository.delete(guardado.getId());

            assertThat(repository.findById(guardado.getId())).isEmpty();
        }

        @Test
        @DisplayName("reactivate vuelve a habilitar un color dado de baja")
        void reactivate_vuelve_a_habilitar_un_color_dado_de_baja() {
            AnimalColor guardado = nuevoColor("Negro", PERRO_ID, "Perro");
            repository.delete(guardado.getId());

            int filas = repository.reactivate(guardado.getId());

            assertThat(filas).isEqualTo(1);
            assertThat(repository.findById(guardado.getId())).map(AnimalColor::isEnabled)
                    .contains(true);
        }

        @Test
        @DisplayName("reactivate sobre un color inexistente no afecta filas")
        void reactivate_sobre_un_color_inexistente_no_afecta_filas() {
            assertThat(repository.reactivate(999999L)).isZero();
        }
    }
}
