package com.vetsoftware.app.state.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.state.domain.CountryRef;
import com.vetsoftware.app.state.domain.State;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Rodaja de persistencia del catalogo de departamentos contra MySQL real.
 *
 * <p>
 * <b>Por que un doble no sirve aqui.</b> Lo que sostiene esta feature lo decide
 * el motor, no el codigo Java: la unicidad del nombre por pais depende de un
 * indice compuesto ({@code uq_states_country_name} sobre
 * {@code country_id + name}) que un {@code Map} en memoria no reproduce, y el
 * soft delete lo hacen {@code @SQLDelete}/{@code @SQLRestriction}, invisibles
 * fuera de Hibernate. Un stub del repositorio definiria el contrato en el
 * propio test — justo donde vivio BE-01.
 */
@Import({JpaStateRepository.class, StateJpaMapper.class})
@DisplayName("JpaStateRepository — ida y vuelta, filtro por pais y baja logica contra MySQL real")
class StatePersistenceIT extends AbstractDataJpaTest {

    private static final Long OTRO_PAIS_ID = 970L;

    private static final CountryRef COLOMBIA = new CountryRef(SchemaSeed.COUNTRY_ID,
            "Pais de prueba");
    private static final CountryRef OTRO_PAIS = new CountryRef(OTRO_PAIS_ID, "Otro Pais");

    @Autowired
    private JpaStateRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO countries (id, name, created_date, enabled)
                VALUES (:id, 'Otro Pais', NOW(), true)
                """).setParameter("id", OTRO_PAIS_ID).executeUpdate();
        entityManager.flush();
    }

    private State guardar(CountryRef pais, String nombre, String dane) {
        return repository.save(State.create(nombre, pais, dane));
    }

    private void releerDesdeLaBase() {
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("ida y vuelta del agregado")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar y releer conserva cada campo y el pais hidratado")
        void guardar_y_releer_conserva_cada_campo() {
            State guardado = guardar(COLOMBIA, "Cundinamarca", "25");
            releerDesdeLaBase();

            State leido = repository.findById(guardado.getId()).orElseThrow();

            assertThat(leido.getName()).isEqualTo("Cundinamarca");
            assertThat(leido.getDaneCode()).isEqualTo("25");
            assertThat(leido.isEnabled()).isTrue();
            // El CountryRef no se guarda: se reconstruye desde el @ManyToOne al leer.
            assertThat(leido.getCountry()).isEqualTo(COLOMBIA);
        }

        @Test
        @DisplayName("un codigo dane nulo vuelve nulo, no en blanco")
        void codigo_dane_nulo_vuelve_nulo() {
            State guardado = guardar(COLOMBIA, "Cundinamarca", null);
            releerDesdeLaBase();

            assertThat(repository.findById(guardado.getId()).orElseThrow().getDaneCode()).isNull();
        }
    }

    @Nested
    @DisplayName("filtro por pais")
    class FiltroPorPais {

        @Test
        @DisplayName("findByCountryId no mezcla los departamentos de otro pais")
        void find_by_country_id_no_mezcla_los_de_otro_pais() {
            State propio = guardar(COLOMBIA, "Cundinamarca", "25");
            guardar(OTRO_PAIS, "Region Ajena", null);
            releerDesdeLaBase();

            assertThat(repository.findByCountryId(SchemaSeed.COUNTRY_ID)).extracting(State::getId)
                    .containsExactly(propio.getId());
        }

        @Test
        @DisplayName("findAll trae los departamentos de todos los paises")
        void find_all_trae_los_de_todos_los_paises() {
            State propio = guardar(COLOMBIA, "Cundinamarca", "25");
            State ajeno = guardar(OTRO_PAIS, "Region Ajena", null);
            releerDesdeLaBase();

            assertThat(repository.findAll()).extracting(State::getId).contains(propio.getId(),
                    ajeno.getId());
        }
    }

    @Nested
    @DisplayName("unicidad del nombre por pais")
    class UnicidadDelNombre {

        @Test
        @DisplayName("dos departamentos con el mismo nombre en el mismo pais violan el indice unico")
        void el_mismo_nombre_en_el_mismo_pais_viola_el_indice() {
            guardar(COLOMBIA, "Cundinamarca", "25");

            assertThatThrownBy(() -> repository.save(State.create("Cundinamarca", COLOMBIA, "25")))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("el mismo nombre queda libre en otro pais")
        void el_mismo_nombre_queda_libre_en_otro_pais() {
            guardar(COLOMBIA, "Cundinamarca", "25");

            State enOtroPais = guardar(OTRO_PAIS, "Cundinamarca", null);

            assertThat(enOtroPais.getId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("baja logica y reactivacion")
    class BajaLogicaYReactivacion {

        @Test
        @DisplayName("delete es logico: la fila deja de verse por findById y en el listado")
        void delete_es_logico_y_deja_de_verse() {
            State guardado = guardar(COLOMBIA, "Cundinamarca", "25");
            releerDesdeLaBase();

            repository.delete(guardado.getId());
            releerDesdeLaBase();

            assertThat(repository.findById(guardado.getId())).isEmpty();
            assertThat(repository.findByCountryId(SchemaSeed.COUNTRY_ID)).extracting(State::getId)
                    .doesNotContain(guardado.getId());
        }

        @Test
        @DisplayName("reactivate devuelve la fila al listado activo")
        void reactivate_devuelve_la_fila_al_listado() {
            State guardado = guardar(COLOMBIA, "Cundinamarca", "25");
            releerDesdeLaBase();
            repository.delete(guardado.getId());
            releerDesdeLaBase();

            int filas = repository.reactivate(guardado.getId());
            releerDesdeLaBase();

            assertThat(filas).isEqualTo(1);
            assertThat(repository.findById(guardado.getId())).isPresent();
        }
    }
}
