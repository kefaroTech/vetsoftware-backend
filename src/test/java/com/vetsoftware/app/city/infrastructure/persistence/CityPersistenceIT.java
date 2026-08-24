package com.vetsoftware.app.city.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.city.domain.City;
import com.vetsoftware.app.city.domain.StateRef;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia de ciudades contra MySQL real.
 *
 * <p>
 * <b>Por que un doble no sirve aqui.</b> Las conductas que sostienen esta
 * feature las decide el motor, no el codigo Java:
 *
 * <ul>
 * <li><b>La unicidad del nombre es por departamento, no global.</b> El indice
 * unico es {@code (state_id, name)}: el mismo nombre de ciudad se repite entre
 * departamentos sin chocar. Un {@code Map} en memoria por id no distingue esa
 * frontera.</li>
 * <li><b>El soft delete lo hacen dos anotaciones de Hibernate.</b> El
 * {@code @SQLDelete} convierte el borrado en {@code UPDATE enabled = false} y
 * el {@code @SQLRestriction} esconde la fila de todas las consultas de entidad,
 * menos de la nativa de {@code reactivate}.</li>
 * <li><b>El {@code @EntityGraph} en {@code state}</b> es lo que evita el N+1 al
 * listar: solo se ve pasando por Hibernate.</li>
 * </ul>
 *
 * <p>
 * A diferencia de proveedores o categorias de producto, City es un catalogo
 * <b>global</b> sin {@code companyId}: no hay tenencia que aislar, asi que aqui
 * la frontera que importa es el departamento (state), no la empresa.
 *
 * <p>
 * <b>Por que no usa {@code SchemaSeed}.</b> City solo depende de {@code states}
 * (y esta, de {@code countries}): nada del resto de la cadena de
 * {@code SchemaSeed} (company, contrato, branch, product...) le hace falta. Los
 * ids de {@code SchemaSeed} son literales fijos y compartidos por todas las
 * features que lo usan; para un catalogo geografico esos mismos ids (900,
 * 901...) pueden coincidir con filas ya sembradas por otra cadena de FKs. Por
 * eso este test siembra su propio pais y sus propios departamentos dejando que
 * el motor asigne el id — cero coincidencias posibles.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaCityRepository — catalogo global, soft delete y aislamiento por departamento contra MySQL real")
class CityPersistenceIT extends AbstractDataJpaTest {

    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    @Autowired
    private JpaCityRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    private StateRef antioquia;
    private StateRef cundinamarca;

    @BeforeEach
    void sembrarPaisYDepartamentos() {
        Long countryId = insertar("INSERT INTO countries (name) VALUES ('Colombia (test)')");
        antioquia = new StateRef(insertar(
                "INSERT INTO states (name, country_id) VALUES ('Antioquia', " + countryId + ")"),
                "Antioquia");
        cundinamarca = new StateRef(insertar(
                "INSERT INTO states (name, country_id) VALUES ('Cundinamarca', " + countryId + ")"),
                "Cundinamarca");
        entityManager.flush();
    }

    /** Inserta y devuelve el id autogenerado, en la misma conexion/transaccion. */
    private Long insertar(String sql) {
        entityManager.createNativeQuery(sql).executeUpdate();
        return ((Number) entityManager.createNativeQuery("SELECT LAST_INSERT_ID()")
                .getSingleResult()).longValue();
    }

    private City nueva(String nombre, StateRef departamento) {
        return new City(null, nombre, departamento, "05001", CREADO, true);
    }

    private City guardarEn(StateRef departamento, String nombre) {
        return repository.save(nueva(nombre, departamento));
    }

    /** Soft delete + flush: el UPDATE tiene que llegar a la BD antes de releer. */
    private void deshabilitar(Long id) {
        repository.delete(id);
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("unicidad del nombre por departamento")
    class UnicidadDelNombre {

        @Test
        @DisplayName("el mismo nombre de ciudad se admite en dos departamentos distintos")
        void el_mismo_nombre_se_admite_en_dos_departamentos_distintos() {
            guardarEn(antioquia, "Medellin");

            City medellinCundinamarca = guardarEn(cundinamarca, "Medellin");

            assertThat(medellinCundinamarca.getId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("ida y vuelta del agregado")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar y releer conserva cada campo y el departamento hidratado")
        void guardar_y_releer_conserva_cada_campo() {
            City guardada = guardarEn(antioquia, "Envigado");

            City leida = repository.findById(guardada.getId()).orElseThrow();

            assertThat(leida.getName()).isEqualTo("Envigado");
            assertThat(leida.getDaneCode()).isEqualTo("05001");
            assertThat(leida.getCreatedDate()).isEqualTo(CREADO);
            assertThat(leida.isEnabled()).isTrue();
            // El StateRef no se guarda: se reconstruye desde el @ManyToOne al leer, y sus
            // invariantes rechazan un nombre vacio.
            assertThat(leida.getState()).isEqualTo(antioquia);
        }

        @Test
        @DisplayName("el daneCode nulo vuelve nulo, no en blanco")
        void el_dane_code_nulo_vuelve_nulo() {
            City guardada = repository
                    .save(new City(null, "Envigado", antioquia, null, CREADO, true));

            City leida = repository.findById(guardada.getId()).orElseThrow();

            assertThat(leida.getDaneCode()).isNull();
        }
    }

    @Nested
    @DisplayName("listado por departamento")
    class ListadoPorDepartamento {

        @Test
        @DisplayName("findByStateId no mezcla las ciudades de dos departamentos")
        void find_by_state_id_no_mezcla_los_departamentos() {
            City propia = guardarEn(antioquia, "Envigado");
            City ajena = guardarEn(cundinamarca, "Chia");

            assertThat(repository.findByStateId(antioquia.id())).extracting(City::getId)
                    .contains(propia.getId()).doesNotContain(ajena.getId());
        }

        @Test
        @DisplayName("un departamento recien creado sin ciudades propias devuelve una lista vacia")
        void un_departamento_sin_ciudades_devuelve_una_lista_vacia() {
            assertThat(repository.findByStateId(cundinamarca.id())).isEmpty();
        }
    }

    @Nested
    @DisplayName("listado global")
    class ListadoGlobal {

        @Test
        @DisplayName("findAll trae ciudades de todos los departamentos: es un catalogo, no un tenant")
        void find_all_trae_ciudades_de_todos_los_departamentos() {
            City deAntioquia = guardarEn(antioquia, "Envigado");
            City deOtroDepartamento = guardarEn(cundinamarca, "Chia");

            assertThat(repository.findAll()).extracting(City::getId).contains(deAntioquia.getId(),
                    deOtroDepartamento.getId());
        }
    }

    @Nested
    @DisplayName("soft delete y reactivacion")
    class SoftDelete {

        @Test
        @DisplayName("el listado deja de ver la ciudad deshabilitada")
        void el_listado_no_ve_la_deshabilitada() {
            City pausada = guardarEn(antioquia, "Envigado");

            deshabilitar(pausada.getId());

            assertThat(repository.findAll()).extracting(City::getId)
                    .doesNotContain(pausada.getId());
            assertThat(repository.findById(pausada.getId())).isEmpty();
        }

        @Test
        @DisplayName("reactivar devuelve la ciudad al listado")
        void reactivar_devuelve_la_ciudad_al_listado() {
            City pausada = guardarEn(antioquia, "Envigado");
            deshabilitar(pausada.getId());

            int filas = repository.reactivate(pausada.getId());

            assertThat(filas).isEqualTo(1);
            assertThat(repository.findById(pausada.getId())).map(City::getId)
                    .contains(pausada.getId());
        }

        @Test
        @DisplayName("reactivar un id inexistente no toca ninguna fila")
        void reactivar_un_id_inexistente_no_toca_ninguna_fila() {
            int filas = repository.reactivate(999_999_999L);

            assertThat(filas).isZero();
        }
    }
}
