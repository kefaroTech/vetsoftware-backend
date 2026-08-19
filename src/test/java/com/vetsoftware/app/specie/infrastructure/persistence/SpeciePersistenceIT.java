package com.vetsoftware.app.specie.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.specie.domain.Specie;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

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

    @PersistenceContext
    private EntityManager entityManager;

    private Specie nuevaEspecie(String nombre) {
        return repository.save(Specie.create(nombre));
    }

    /**
     * Vacia el contexto de persistencia para que la siguiente lectura venga del
     * motor y no de la cache de primer nivel. Sin esto, media rodaja de bloqueo
     * optimista se responderia sola sin tocar MySQL.
     */
    private void releerDesdeLaBase() {
        entityManager.flush();
        entityManager.clear();
    }

    /** Cuenta la fila por SQL nativo: el {@code @SQLRestriction} no la taparia. */
    private long filasDeshabilitadasEnLaBase(Long id) {
        return ((Number) entityManager
                .createNativeQuery(
                        "SELECT COUNT(*) FROM species WHERE id = :id AND enabled = false")
                .setParameter("id", id).getSingleResult()).longValue();
    }

    /**
     * Todas las filas que esta rodaja pudo crear, vistas por fuera de Hibernate.
     */
    private List<?> idsDeLasEspeciesDeLaPrueba() {
        return entityManager
                .createNativeQuery("SELECT id FROM species WHERE name IN ('Perro', 'Gato')")
                .getResultList();
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

    /**
     * BE-26 — bloqueo optimista contra el motor.
     *
     * <p>
     * {@code GlobalExceptionHandlerUnitTest.bloqueo_optimista} solo comprueba que
     * una {@code ObjectOptimisticLockingFailureException} fabricada a mano se mapea
     * a 409; nunca ve una entidad real. Aqui se ejercita el {@code @Version} de
     * {@code species} de punta a punta: que el conflicto se produce, que el
     * {@code @SQLDelete} con sus <b>dos</b> parametros ligados ({@code id} y
     * {@code version}) se ejecuta contra MySQL, y que guardar una fila ya existente
     * hace {@code UPDATE} y no un {@code INSERT} duplicado.
     */
    @Nested
    @DisplayName("bloqueo optimista (BE-26)")
    class BloqueoOptimista {

        @Test
        @DisplayName("una especie recien insertada nace con version 0")
        void una_especie_recien_insertada_nace_con_version_cero() {
            Specie guardada = nuevaEspecie("Perro");
            releerDesdeLaBase();

            assertThat(repository.findById(guardada.getId())).map(Specie::getVersion).contains(0L);
        }

        /**
         * El caso que faltaba: dos copias de la misma fila, la segunda escribe con la
         * version que ya quedo obsoleta.
         */
        @Test
        @DisplayName("guardar una copia obsoleta sobre una fila ya modificada lanza el conflicto optimista")
        void guardar_una_copia_obsoleta_lanza_el_conflicto_optimista() {
            Long id = nuevaEspecie("Perro").getId();
            releerDesdeLaBase();

            Specie copiaQueGana = repository.findById(id).orElseThrow();
            Specie copiaQueQuedaraObsoleta = repository.findById(id).orElseThrow();
            assertThat(copiaQueQuedaraObsoleta.getVersion()).isEqualTo(0L);

            copiaQueGana.update("Gato");
            repository.save(copiaQueGana);
            releerDesdeLaBase();

            copiaQueQuedaraObsoleta.update("Hamster");

            assertThatThrownBy(() -> {
                repository.save(copiaQueQuedaraObsoleta);
                entityManager.flush();
            }).isInstanceOf(ObjectOptimisticLockingFailureException.class)
                    .hasMessageContaining("SpecieJpaEntity");
        }

        /**
         * La trampa que motivo la campaña: con {@code @Version}, Hibernate liga
         * {@code id} y {@code version} al SQL del {@code @SQLDelete}. Un
         * {@code WHERE id = ?} con un solo {@code ?} compila igual y revienta aqui.
         */
        @Test
        @DisplayName("el borrado logico versionado se ejecuta, deja enabled = false y la fila deja de verse")
        void el_borrado_logico_versionado_deshabilita_la_fila_sin_reventar() {
            Long id = nuevaEspecie("Perro").getId();
            releerDesdeLaBase();

            repository.delete(id);
            releerDesdeLaBase();

            assertThat(filasDeshabilitadasEnLaBase(id)).isOne();
            assertThat(repository.findById(id)).isEmpty();
            assertThat(repository.findAll()).extracting(Specie::getId).doesNotContain(id);
        }

        /**
         * Si la version no viaja de vuelta por dominio y mapper, llega {@code null} al
         * {@code merge}, Hibernate concluye que la fila es nueva e inserta un duplicado
         * en lugar de actualizar. El {@code hasSize(1)} es quien lo delata.
         */
        @Test
        @DisplayName("guardar una especie ya existente actualiza la fila y sube la version, no inserta otra")
        void guardar_una_especie_existente_actualiza_y_no_inserta_una_segunda_fila() {
            Long id = nuevaEspecie("Perro").getId();
            releerDesdeLaBase();

            Specie cargada = repository.findById(id).orElseThrow();
            cargada.update("Gato");
            repository.save(cargada);
            releerDesdeLaBase();

            assertThat(idsDeLasEspeciesDeLaPrueba()).hasSize(1);
            Specie releida = repository.findById(id).orElseThrow();
            assertThat(releida.getName()).isEqualTo("Gato");
            assertThat(releida.getVersion()).isEqualTo(1L);
        }
    }
}
