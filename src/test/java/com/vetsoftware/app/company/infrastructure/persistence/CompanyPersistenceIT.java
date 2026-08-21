package com.vetsoftware.app.company.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.company.domain.CityRef;
import com.vetsoftware.app.company.domain.Company;
import com.vetsoftware.app.company.domain.MembershipRef;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia de empresas contra MySQL real.
 *
 * <p>
 * Company es el tenant raiz: depende de {@code cities} y {@code memberships}
 * por FK, pero no del resto de la cadena de {@code SchemaSeed} (branch,
 * product, categoria...), asi que este test siembra solo su propia ciudad y su
 * propia membresia dejando que el motor asigne el id — igual criterio que
 * {@code CityPersistenceIT}.
 *
 * <p>
 * Lo que un doble no puede probar: el soft delete via {@code @SQLDelete} +
 * {@code @SQLRestriction}, el {@code @EntityGraph} de {@code city} y
 * {@code membership} que evita el N+1 al listar, y que la unicidad del NIT
 * ({@code identifier}) la exige un indice UNIQUE real, no el codigo Java.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaCompanyRepository — tenant raiz, soft delete y unicidad del NIT contra MySQL real")
class CompanyPersistenceIT extends AbstractDataJpaTest {

    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    @Autowired
    private JpaCompanyRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    private CityRef bogota;
    private MembershipRef premium;
    private final AtomicInteger nitConsecutivo = new AtomicInteger(1);

    @BeforeEach
    void sembrarCiudadYMembresia() {
        Long countryId = insertar("INSERT INTO countries (name) VALUES ('Colombia (test)')");
        Long stateId = insertar(
                "INSERT INTO states (name, country_id) VALUES ('Cundinamarca (test)', " + countryId
                        + ")");
        Long cityId = insertar(
                "INSERT INTO cities (name, state_id) VALUES ('Bogota (test)', " + stateId + ")");
        Long membershipId = insertar("INSERT INTO memberships (name, status, mandatory) "
                + "VALUES ('Premium (test)', 'ACTIVE', false)");
        bogota = new CityRef(cityId, "Bogota (test)");
        premium = new MembershipRef(membershipId, "Premium (test)", "ACTIVE");
        entityManager.flush();
    }

    /** Inserta y devuelve el id autogenerado, en la misma conexion/transaccion. */
    private Long insertar(String sql) {
        entityManager.createNativeQuery(sql).executeUpdate();
        return ((Number) entityManager.createNativeQuery("SELECT LAST_INSERT_ID()")
                .getSingleResult()).longValue();
    }

    private Company nueva(String nombre) {
        return new Company(null, nombre, "NIT-" + nitConsecutivo.getAndIncrement(),
                "Calle 123 #45-67", "3001234567", bogota, premium, CREADO, null, true);
    }

    private Company guardar(String nombre) {
        return repository.save(nueva(nombre));
    }

    /** Soft delete + flush: el UPDATE tiene que llegar a la BD antes de releer. */
    private void deshabilitar(Long id) {
        repository.delete(id);
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("ida y vuelta del agregado")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar y releer conserva cada campo y las referencias hidratadas")
        void guardar_y_releer_conserva_cada_campo() {
            Company guardada = guardar("Clinica Norte");

            Company leida = repository.findById(guardada.getId()).orElseThrow();

            assertThat(leida.getName()).isEqualTo("Clinica Norte");
            assertThat(leida.getAddress()).isEqualTo("Calle 123 #45-67");
            assertThat(leida.getContactNumber()).isEqualTo("3001234567");
            assertThat(leida.getCreatedDate()).isEqualTo(CREADO);
            assertThat(leida.isEnabled()).isTrue();
            // Los companion VO no se guardan como columnas propias: se reconstruyen desde
            // el @ManyToOne al leer, y sus invariantes rechazan un nombre en blanco.
            assertThat(leida.getCity()).isEqualTo(bogota);
            assertThat(leida.getMembership()).isEqualTo(premium);
        }

        @Test
        @DisplayName("direccion y telefono nulos se conservan nulos, no en blanco")
        void direccion_y_telefono_nulos_se_conservan_nulos() {
            Company guardada = repository.save(
                    new Company(null, "Clinica Norte", "NIT-" + nitConsecutivo.getAndIncrement(),
                            null, null, bogota, premium, CREADO, null, true));

            Company leida = repository.findById(guardada.getId()).orElseThrow();

            assertThat(leida.getAddress()).isNull();
            assertThat(leida.getContactNumber()).isNull();
        }
    }

    @Nested
    @DisplayName("unicidad del NIT")
    class UnicidadDelNit {

        @Test
        @DisplayName("dos empresas con el mismo NIT violan el indice unico")
        void dos_empresas_con_el_mismo_nit_violan_el_indice_unico() {
            String nitCompartido = "NIT-DUPLICADO";
            repository.save(new Company(null, "Clinica Norte", nitCompartido, null, null, bogota,
                    premium, CREADO, null, true));

            assertThatThrownBy(() -> repository.save(new Company(null, "Clinica Sur", nitCompartido,
                    null, null, bogota, premium, CREADO, null, true)))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("listado")
    class Listado {

        @Test
        @DisplayName("findAllVisibleTo(null) trae todas las empresas habilitadas")
        void find_all_visible_to_null_trae_todas_las_habilitadas() {
            Company primera = guardar("Clinica Norte");
            Company segunda = guardar("Clinica Sur");

            assertThat(repository.findAllVisibleTo(null)).extracting(Company::getId)
                    .contains(primera.getId(), segunda.getId());
        }

        /**
         * El alcance es la barrera, y aqui se comprueba contra la base: con una empresa
         * informada el listado trae esa fila y ninguna otra. Mientras el puerto ofrecio
         * un {@code findAll()} pelado, {@code GET /companies} entregaba el registro de
         * todos los tenants a cualquier empleado con {@code company.read}.
         */
        @Test
        @DisplayName("findAllVisibleTo con empresa trae solo esa, nunca la de otro tenant")
        void find_all_visible_to_acota_a_una_sola_empresa() {
            Company propia = guardar("Clinica Norte");
            Company ajena = guardar("Clinica Sur");

            assertThat(repository.findAllVisibleTo(propia.getId())).extracting(Company::getId)
                    .containsExactly(propia.getId());
            assertThat(repository.findAllVisibleTo(ajena.getId())).extracting(Company::getId)
                    .doesNotContain(propia.getId());
        }
    }

    @Nested
    @DisplayName("soft delete y reactivacion")
    class SoftDelete {

        @Test
        @DisplayName("el listado y la busqueda dejan de ver la empresa deshabilitada")
        void el_listado_no_ve_la_deshabilitada() {
            Company pausada = guardar("Clinica Norte");

            deshabilitar(pausada.getId());

            assertThat(repository.findAllVisibleTo(null)).extracting(Company::getId)
                    .doesNotContain(pausada.getId());
            assertThat(repository.findById(pausada.getId())).isEmpty();
        }

        @Test
        @DisplayName("reactivar devuelve la empresa al listado")
        void reactivar_devuelve_la_empresa_al_listado() {
            Company pausada = guardar("Clinica Norte");
            deshabilitar(pausada.getId());

            int filas = repository.reactivate(pausada.getId());

            assertThat(filas).isEqualTo(1);
            assertThat(repository.findById(pausada.getId())).map(Company::getId)
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
