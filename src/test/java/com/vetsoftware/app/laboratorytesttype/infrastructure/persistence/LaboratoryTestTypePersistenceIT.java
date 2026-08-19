package com.vetsoftware.app.laboratorytesttype.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.laboratorytesttype.domain.CompanyRef;
import com.vetsoftware.app.laboratorytesttype.domain.LaboratoryTestType;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.SchemaSeed;
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
 * Rodaja de persistencia de tipos de examen de laboratorio contra MySQL real.
 *
 * <p>
 * <b>Por que un doble no sirve aqui.</b>
 *
 * <ul>
 * <li><b>El {@code @SQLDelete} + {@code @SQLRestriction}</b> convierten el
 * borrado en {@code UPDATE enabled = false} y esconden la fila del resto de
 * consultas de entidad; un repositorio en memoria no reproduce ese
 * comportamiento.</li>
 * <li><b>El {@code @EntityGraph} en {@code company}</b> es lo que evita el N+1
 * al listar y al buscar, y solo se ve pasando por Hibernate.</li>
 * <li><b>{@code findAllByGeneralTrueOrCompany_Id}</b> mezcla dos condiciones en
 * el WHERE (tipos generales + tipos propios de la empresa): es SQL real, no
 * logica Java.</li>
 * </ul>
 */
@Import({JpaLaboratoryTestTypeRepository.class, LaboratoryTestTypeJpaMapper.class})
@DisplayName("JpaLaboratoryTestTypeRepository — disponibilidad general/propia y soft delete contra MySQL real")
class LaboratoryTestTypePersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY_ID = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_COMPANY_ID = SchemaSeed.OTRA_COMPANY_ID;

    private static final CompanyRef EMPRESA = new CompanyRef(COMPANY_ID, "Veterinaria de prueba",
            "900123456");
    private static final CompanyRef OTRA_EMPRESA = new CompanyRef(OTRA_COMPANY_ID,
            "Veterinaria ajena", "900654321");

    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    @Autowired
    private JpaLaboratoryTestTypeRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
    }

    private LaboratoryTestType propio(String nombre, CompanyRef empresa) {
        return new LaboratoryTestType(null, nombre, "Tipo de prueba", empresa, false, CREADO, null,
                true);
    }

    private LaboratoryTestType general(String nombre) {
        return new LaboratoryTestType(null, nombre, "Tipo general de prueba", null, true, CREADO,
                null, true);
    }

    private LaboratoryTestType guardarPropio(String nombre) {
        return repository.save(propio(nombre, EMPRESA));
    }

    private LaboratoryTestType guardarGeneral(String nombre) {
        return repository.save(general(nombre));
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
        @DisplayName("guardar y releer un tipo propio conserva cada campo y la empresa hidratada")
        void guardar_y_releer_un_tipo_propio_conserva_cada_campo() {
            LaboratoryTestType guardado = guardarPropio("Hemograma");

            LaboratoryTestType leido = repository.findById(guardado.getId()).orElseThrow();

            assertThat(leido.getName()).isEqualTo("Hemograma");
            assertThat(leido.getDescription()).isEqualTo("Tipo de prueba");
            assertThat(leido.isGeneral()).isFalse();
            assertThat(leido.isEnabled()).isTrue();
            // El CompanyRef no se guarda: se reconstruye desde el @ManyToOne al leer, y
            // sus invariantes rechazan un nombre o un identificador vacios.
            assertThat(leido.getCompany())
                    .isEqualTo(new CompanyRef(COMPANY_ID, "Veterinaria de prueba", "900123456"));
        }

        @Test
        @DisplayName("guardar y releer un tipo general conserva company nula")
        void guardar_y_releer_un_tipo_general_conserva_company_nula() {
            LaboratoryTestType guardado = guardarGeneral("Perfil renal");

            LaboratoryTestType leido = repository.findById(guardado.getId()).orElseThrow();

            assertThat(leido.isGeneral()).isTrue();
            assertThat(leido.getCompany()).isNull();
        }
    }

    @Nested
    @DisplayName("disponibilidad por empresa")
    class Disponibilidad {

        @Test
        @DisplayName("findByIdAndCompanyId encuentra un tipo general aunque la empresa sea otra")
        void find_by_id_and_company_id_encuentra_un_tipo_general() {
            LaboratoryTestType general = guardarGeneral("Perfil renal");

            assertThat(repository.findByIdAndCompanyId(general.getId(), OTRA_COMPANY_ID))
                    .map(LaboratoryTestType::getId).contains(general.getId());
        }

        @Test
        @DisplayName("findByIdAndCompanyId no devuelve el tipo propio de otra empresa")
        void find_by_id_and_company_id_no_devuelve_el_de_otra_empresa() {
            LaboratoryTestType ajeno = repository.save(propio("Coprologico", OTRA_EMPRESA));

            assertThat(repository.findByIdAndCompanyId(ajeno.getId(), COMPANY_ID)).isEmpty();
        }

        @Test
        @DisplayName("findAllAvailableForCompany mezcla los generales con los propios, sin los ajenos")
        void find_all_available_mezcla_generales_y_propios() {
            LaboratoryTestType general = guardarGeneral("Perfil renal");
            LaboratoryTestType propio = guardarPropio("Hemograma");
            LaboratoryTestType ajeno = repository.save(propio("Coprologico", OTRA_EMPRESA));

            assertThat(repository.findAllAvailableForCompany(COMPANY_ID))
                    .extracting(LaboratoryTestType::getId).contains(general.getId(), propio.getId())
                    .doesNotContain(ajeno.getId());
        }
    }

    @Nested
    @DisplayName("soft delete y reactivacion")
    class SoftDelete {

        @Test
        @DisplayName("el listado disponible deja de ver el tipo deshabilitado")
        void el_listado_disponible_no_ve_el_deshabilitado() {
            LaboratoryTestType pausado = guardarPropio("Hemograma");

            deshabilitar(pausado.getId());

            assertThat(repository.findAllAvailableForCompany(COMPANY_ID))
                    .extracting(LaboratoryTestType::getId).doesNotContain(pausado.getId());
        }

        @Test
        @DisplayName("reactivar devuelve el tipo al listado disponible")
        void reactivar_devuelve_el_tipo_al_listado_disponible() {
            LaboratoryTestType pausado = guardarPropio("Hemograma");
            deshabilitar(pausado.getId());

            int filas = repository.reactivate(pausado.getId(), COMPANY_ID);

            assertThat(filas).isEqualTo(1);
            assertThat(repository.findAllAvailableForCompany(COMPANY_ID))
                    .extracting(LaboratoryTestType::getId).contains(pausado.getId());
        }

        @Test
        @DisplayName("reactivar un id inexistente no afecta ninguna fila")
        void reactivar_un_id_inexistente_no_afecta_ninguna_fila() {
            assertThat(repository.reactivate(999999L, COMPANY_ID)).isZero();
        }

        @Test
        @DisplayName("reactivar con el companyId de OTRA empresa afecta 0 filas y lo deja oculto")
        void reactivar_con_la_empresa_ajena_no_afecta_ninguna_fila() {
            // El WHERE es la unica barrera de la reactivacion: no hay lectura previa que
            // valide la propiedad.
            LaboratoryTestType pausado = guardarPropio("Hemograma");
            deshabilitar(pausado.getId());

            assertThat(repository.reactivate(pausado.getId(), OTRA_COMPANY_ID)).isZero();
            assertThat(repository.findAllAvailableForCompany(COMPANY_ID))
                    .extracting(LaboratoryTestType::getId).doesNotContain(pausado.getId());
        }

        @Test
        @DisplayName("una fila general no la reactiva ninguna empresa: company_id es NULL")
        void una_fila_general_no_la_reactiva_ninguna_empresa() {
            // Consecuencia deliberada de acotar la escritura: reactivar una fila general
            // la devolveria a TODOS los tenants, asi que ninguna empresa puede hacerlo.
            LaboratoryTestType general = guardarGeneral("Perfil renal");
            deshabilitar(general.getId());

            assertThat(repository.reactivate(general.getId(), COMPANY_ID)).isZero();
            assertThat(repository.findAllAvailableForCompany(COMPANY_ID))
                    .extracting(LaboratoryTestType::getId).doesNotContain(general.getId());
        }

        @Test
        @DisplayName("findOwnedByIdAndCompanyId ve la fila propia pero NO la general ni la ajena")
        void find_owned_solo_ve_la_fila_propia() {
            // Es el finder de los caminos de ESCRITURA. A diferencia de
            // findByIdAndCompanyId (disponibles), excluye las generales: si las incluyera,
            // el update les pondria el company_id del llamador.
            LaboratoryTestType propio = guardarPropio("Hemograma");
            LaboratoryTestType general = guardarGeneral("Perfil renal");
            LaboratoryTestType ajeno = repository.save(propio("Coprologico", OTRA_EMPRESA));

            assertThat(repository.findOwnedByIdAndCompanyId(propio.getId(), COMPANY_ID))
                    .isPresent();
            assertThat(repository.findOwnedByIdAndCompanyId(general.getId(), COMPANY_ID)).isEmpty();
            assertThat(repository.findOwnedByIdAndCompanyId(ajeno.getId(), COMPANY_ID)).isEmpty();
            // La general sigue siendo legible por el finder de disponibles.
            assertThat(repository.findByIdAndCompanyId(general.getId(), COMPANY_ID)).isPresent();
        }
    }

    @Nested
    @DisplayName("listado global")
    class ListadoGlobal {

        @Test
        @DisplayName("findAll incluye tipos propios y generales de todas las empresas")
        void find_all_incluye_propios_y_generales_de_todas_las_empresas() {
            LaboratoryTestType general = guardarGeneral("Perfil renal");
            LaboratoryTestType ajeno = repository.save(propio("Coprologico", OTRA_EMPRESA));

            assertThat(repository.findAll()).extracting(LaboratoryTestType::getId)
                    .contains(general.getId(), ajeno.getId());
        }
    }
}
