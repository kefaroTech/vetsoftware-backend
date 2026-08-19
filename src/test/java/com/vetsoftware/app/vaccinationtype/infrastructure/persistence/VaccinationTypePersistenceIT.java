package com.vetsoftware.app.vaccinationtype.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.SchemaSeed;
import com.vetsoftware.app.vaccinationtype.domain.CompanyRef;
import com.vetsoftware.app.vaccinationtype.domain.VaccinationType;
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
 * Rodaja de persistencia de tipos de vacuna contra MySQL real.
 *
 * <p>
 * <b>Por que un doble no sirve aqui.</b> Lo que sostiene esta feature lo decide
 * el motor, no el codigo Java:
 *
 * <ul>
 * <li><b>El catalogo mezcla filas generales y por empresa.</b> El
 * {@code @Query} JPQL con {@code LEFT JOIN} de {@code findAvailableById} y el
 * derivado {@code findAllByGeneralTrueOrCompany_Id} son los que deciden que un
 * tipo general se vea desde cualquier empresa: eso solo lo verifica Hibernate
 * contra datos reales.</li>
 * <li><b>El soft delete lo hacen dos anotaciones de Hibernate.</b> El
 * {@code @SQLDelete} convierte el borrado en {@code UPDATE enabled = false} y
 * el {@code @SQLRestriction} esconde la fila de todas las consultas de entidad,
 * menos de la nativa de {@code reactivate}.</li>
 * <li><b>El {@code @EntityGraph} en {@code company}</b> es lo que evita el N+1
 * al listar.</li>
 * </ul>
 */
@Import({JpaVaccinationTypeRepository.class, VaccinationTypeJpaMapper.class})
@DisplayName("JpaVaccinationTypeRepository — catalogo general/por empresa y soft delete contra MySQL real")
class VaccinationTypePersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY_ID = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_COMPANY_ID = SchemaSeed.OTRA_COMPANY_ID;

    private static final CompanyRef EMPRESA = new CompanyRef(COMPANY_ID, "Veterinaria de prueba",
            "900123456");
    private static final CompanyRef OTRA_EMPRESA = new CompanyRef(OTRA_COMPANY_ID,
            "Veterinaria ajena", "900654321");

    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    @Autowired
    private JpaVaccinationTypeRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
    }

    private VaccinationType propia(String nombre, CompanyRef empresa) {
        return new VaccinationType(null, nombre, "Vacuna de prueba", empresa, false, CREADO, null,
                true);
    }

    private VaccinationType general(String nombre) {
        return new VaccinationType(null, nombre, "Vacuna de prueba", null, true, CREADO, null,
                true);
    }

    private VaccinationType guardar(String nombre) {
        return repository.save(propia(nombre, EMPRESA));
    }

    /**
     * Vacia el contexto de persistencia para que la siguiente lectura venga de la
     * base.
     */
    private void releerDesdeLaBase() {
        entityManager.flush();
        entityManager.clear();
    }

    /** Soft delete + flush: el UPDATE tiene que llegar a la BD antes de releer. */
    private void deshabilitar(Long id) {
        repository.delete(id);
        releerDesdeLaBase();
    }

    @Nested
    @DisplayName("ida y vuelta del agregado")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar y releer conserva cada campo y la empresa hidratada")
        void guardar_y_releer_conserva_cada_campo() {
            VaccinationType guardado = guardar("Rabia");
            releerDesdeLaBase();

            VaccinationType leido = repository.findById(guardado.getId()).orElseThrow();

            assertThat(leido.getName()).isEqualTo("Rabia");
            assertThat(leido.getDescription()).isEqualTo("Vacuna de prueba");
            assertThat(leido.isGeneral()).isFalse();
            assertThat(leido.getCreatedDate()).isEqualTo(CREADO);
            assertThat(leido.isEnabled()).isTrue();
            assertThat(leido.getCompany()).isEqualTo(EMPRESA);
        }

        @Test
        @DisplayName("un tipo general se guarda y relee sin compania")
        void un_tipo_general_se_guarda_y_relee_sin_compania() {
            VaccinationType guardado = repository.save(general("Vacuna universal"));
            releerDesdeLaBase();

            VaccinationType leido = repository.findById(guardado.getId()).orElseThrow();

            assertThat(leido.getCompany()).isNull();
            assertThat(leido.isGeneral()).isTrue();
        }
    }

    @Nested
    @DisplayName("disponibilidad por empresa")
    class Disponibilidad {

        @Test
        @DisplayName("findByIdAndCompanyId encuentra un tipo propio de la empresa")
        void find_by_id_and_company_id_encuentra_un_tipo_propio() {
            VaccinationType propio = guardar("Rabia");
            releerDesdeLaBase();

            assertThat(repository.findByIdAndCompanyId(propio.getId(), COMPANY_ID))
                    .map(VaccinationType::getId).contains(propio.getId());
        }

        @Test
        @DisplayName("findByIdAndCompanyId no devuelve un tipo propio de otra empresa")
        void find_by_id_and_company_id_no_devuelve_el_de_otra_empresa() {
            VaccinationType ajeno = repository.save(propia("Rabia ajena", OTRA_EMPRESA));
            releerDesdeLaBase();

            assertThat(repository.findByIdAndCompanyId(ajeno.getId(), COMPANY_ID)).isEmpty();
        }

        @Test
        @DisplayName("findByIdAndCompanyId si devuelve un tipo general aunque sea de otra empresa")
        void find_by_id_and_company_id_devuelve_el_general() {
            VaccinationType comun = repository.save(general("Vacuna universal"));
            releerDesdeLaBase();

            assertThat(repository.findByIdAndCompanyId(comun.getId(), COMPANY_ID))
                    .map(VaccinationType::getId).contains(comun.getId());
        }

        @Test
        @DisplayName("findAllAvailableForCompany trae los generales y los propios, no los ajenos")
        void find_all_available_for_company_trae_generales_y_propios() {
            VaccinationType propio = guardar("Rabia");
            VaccinationType comun = repository.save(general("Vacuna universal"));
            repository.save(propia("Rabia ajena", OTRA_EMPRESA));
            releerDesdeLaBase();

            assertThat(repository.findAllAvailableForCompany(COMPANY_ID))
                    .extracting(VaccinationType::getId)
                    .containsExactlyInAnyOrder(propio.getId(), comun.getId());
        }
    }

    @Nested
    @DisplayName("listado global")
    class ListadoGlobal {

        @Test
        @DisplayName("findAll trae los tipos de todas las empresas")
        void find_all_trae_los_tipos_de_todas_las_empresas() {
            VaccinationType propio = guardar("Rabia");
            VaccinationType ajeno = repository.save(propia("Rabia ajena", OTRA_EMPRESA));
            releerDesdeLaBase();

            assertThat(repository.findAll()).extracting(VaccinationType::getId)
                    .contains(propio.getId(), ajeno.getId());
        }
    }

    @Nested
    @DisplayName("soft delete y reactivacion")
    class SoftDeleteYReactivacion {

        @Test
        @DisplayName("el borrado es logico: el tipo deja de verse por id")
        void el_borrado_es_logico() {
            VaccinationType pausado = guardar("Rabia");

            deshabilitar(pausado.getId());

            assertThat(repository.findById(pausado.getId())).isEmpty();
        }

        @Test
        @DisplayName("reactivar devuelve el tipo al listado por id")
        void reactivar_devuelve_el_tipo_al_listado() {
            VaccinationType pausado = guardar("Rabia");
            deshabilitar(pausado.getId());

            int filas = repository.reactivate(pausado.getId(), COMPANY_ID);
            releerDesdeLaBase();

            assertThat(filas).isEqualTo(1);
            assertThat(repository.findById(pausado.getId())).map(VaccinationType::getId)
                    .contains(pausado.getId());
        }

        @Test
        @DisplayName("reactivar un id inexistente no afecta ninguna fila")
        void reactivar_un_id_inexistente_no_afecta_ninguna_fila() {
            int filas = repository.reactivate(999999L, COMPANY_ID);

            assertThat(filas).isZero();
        }

        @Test
        @DisplayName("reactivar con el companyId de OTRA empresa afecta 0 filas y lo deja oculto")
        void reactivar_con_la_empresa_ajena_no_afecta_ninguna_fila() {
            // El WHERE es la unica barrera de la reactivacion: no hay lectura previa que
            // valide la propiedad.
            VaccinationType pausado = guardar("Rabia");
            deshabilitar(pausado.getId());

            int filas = repository.reactivate(pausado.getId(), OTRA_COMPANY_ID);
            releerDesdeLaBase();

            assertThat(filas).isZero();
            assertThat(repository.findById(pausado.getId())).isEmpty();
        }

        @Test
        @DisplayName("findOwnedByIdAndCompanyId ve la fila propia pero NO la ajena")
        void find_owned_solo_ve_la_fila_propia() {
            // Es el finder de los caminos de ESCRITURA. A diferencia de
            // findByIdAndCompanyId (disponibles), excluye lo que la empresa solo puede
            // consultar: si no, el update le pondria su company_id a la fila ajena.
            VaccinationType propio = guardar("Rabia");
            VaccinationType ajeno = repository.save(propia("Rabia ajena", OTRA_EMPRESA));
            releerDesdeLaBase();

            assertThat(repository.findOwnedByIdAndCompanyId(propio.getId(), COMPANY_ID))
                    .isPresent();
            assertThat(repository.findOwnedByIdAndCompanyId(ajeno.getId(), COMPANY_ID)).isEmpty();
        }
    }
}
