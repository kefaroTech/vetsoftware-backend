package com.vetsoftware.app.surgerytype.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.surgerytype.domain.CompanyRef;
import com.vetsoftware.app.surgerytype.domain.SurgeryType;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia del tipo de cirugia contra MySQL real.
 *
 * <p>
 * <b>Por que un doble no sirve aqui.</b> El
 * {@code @SQLDelete}/{@code @SQLRestriction} que convierte el borrado en un
 * {@code enabled = false} invisible, la reactivacion por UPDATE nativo, y la
 * consulta {@code findAvailableById} que mezcla filas generales y de la empresa
 * con un {@code LEFT JOIN} las verifica el motor, no el mapper.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaSurgeryTypeRepository — tipos de cirugia contra MySQL real")
class SurgeryTypePersistenceIT extends AbstractDataJpaTest {

    private static final CompanyRef EMPRESA = new CompanyRef(SchemaSeed.COMPANY_ID,
            "Veterinaria de prueba", "900123456");
    private static final CompanyRef OTRA_EMPRESA = new CompanyRef(SchemaSeed.OTRA_COMPANY_ID,
            "Veterinaria ajena", "900654321");

    @Autowired
    private JpaSurgeryTypeRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
        entityManager.flush();
    }

    private void releerDesdeLaBase() {
        entityManager.flush();
        entityManager.clear();
    }

    private SurgeryType guardarPropio(String name) {
        return repository.save(SurgeryType.create(name, "desc", EMPRESA, false));
    }

    private SurgeryType guardarGeneral(String name) {
        return repository.save(SurgeryType.create(name, "desc", null, true));
    }

    @Nested
    @DisplayName("ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar y releer un tipo propio conserva cada campo, incluida la empresa")
        void guardar_y_releer_un_tipo_propio_conserva_cada_campo() {
            SurgeryType guardado = guardarPropio("Castracion");
            releerDesdeLaBase();

            SurgeryType leido = repository.findById(guardado.getId()).orElseThrow();

            assertThat(leido.getName()).isEqualTo("Castracion");
            assertThat(leido.getDescription()).isEqualTo("desc");
            assertThat(leido.getCompany()).isEqualTo(EMPRESA);
            assertThat(leido.isGeneral()).isFalse();
            assertThat(leido.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("guardar y releer un tipo general no trae empresa")
        void guardar_y_releer_un_tipo_general_no_trae_empresa() {
            SurgeryType guardado = guardarGeneral("Cirugia general");
            releerDesdeLaBase();

            SurgeryType leido = repository.findById(guardado.getId()).orElseThrow();

            assertThat(leido.getCompany()).isNull();
            assertThat(leido.isGeneral()).isTrue();
        }

        @Test
        @DisplayName("el save devuelve el id generado")
        void el_save_devuelve_el_id_generado() {
            SurgeryType guardado = guardarPropio("Castracion");

            assertThat(guardado.getId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("disponibilidad para la empresa")
    class DisponibilidadParaLaEmpresa {

        @Test
        @DisplayName("findByIdAndCompanyId encuentra un tipo general aunque sea de otra empresa")
        void find_by_id_and_company_id_encuentra_un_tipo_general() {
            SurgeryType general = guardarGeneral("Cirugia general");
            releerDesdeLaBase();

            assertThat(repository.findByIdAndCompanyId(general.getId(), SchemaSeed.OTRA_COMPANY_ID))
                    .map(SurgeryType::getId).contains(general.getId());
        }

        @Test
        @DisplayName("findByIdAndCompanyId no encuentra un tipo propio de otra empresa")
        void find_by_id_and_company_id_no_encuentra_un_tipo_ajeno() {
            SurgeryType propio = guardarPropio("Castracion");
            releerDesdeLaBase();

            assertThat(repository.findByIdAndCompanyId(propio.getId(), SchemaSeed.OTRA_COMPANY_ID))
                    .isEmpty();
        }

        @Test
        @DisplayName("findAllAvailableForCompany mezcla los generales con los propios, sin los ajenos")
        void find_all_available_mezcla_generales_y_propios() {
            SurgeryType propio = guardarPropio("Castracion");
            SurgeryType general = guardarGeneral("Cirugia general");
            SurgeryType ajeno = repository
                    .save(SurgeryType.create("Ajeno", "desc", OTRA_EMPRESA, false));
            releerDesdeLaBase();

            assertThat(repository.findAllAvailableForCompany(SchemaSeed.COMPANY_ID))
                    .extracting(SurgeryType::getId)
                    .containsExactlyInAnyOrder(propio.getId(), general.getId())
                    .doesNotContain(ajeno.getId());
        }
    }

    @Nested
    @DisplayName("listado global")
    class ListadoGlobal {

        @Test
        @DisplayName("findAll trae tipos de todas las empresas — es el listado SYSTEM")
        void find_all_trae_tipos_de_todas_las_empresas() {
            SurgeryType propio = guardarPropio("Castracion");
            SurgeryType ajeno = repository
                    .save(SurgeryType.create("Ajeno", "desc", OTRA_EMPRESA, false));
            releerDesdeLaBase();

            assertThat(repository.findAll()).extracting(SurgeryType::getId).contains(propio.getId(),
                    ajeno.getId());
        }
    }

    @Nested
    @DisplayName("borrado")
    class BorradoYReactivacion {

        @Test
        @DisplayName("el borrado es logico: la fila deja de verse por id")
        void el_borrado_es_logico() {
            SurgeryType borrado = guardarPropio("Castracion");
            releerDesdeLaBase();

            repository.delete(borrado.getId());
            releerDesdeLaBase();

            assertThat(repository.findById(borrado.getId())).isEmpty();
        }

        @Test
        @DisplayName("findOwnedByIdAndCompanyId ve la fila propia pero NO la general ni la ajena")
        void find_owned_solo_ve_la_fila_propia() {
            // Es el finder de los caminos de ESCRITURA. A diferencia de
            // findByIdAndCompanyId (disponibles), excluye las generales: si las incluyera,
            // el update les pondria el company_id del llamador.
            SurgeryType propio = guardarPropio("Castracion");
            SurgeryType general = guardarGeneral("Cirugia general");
            SurgeryType ajeno = repository
                    .save(SurgeryType.create("Ajeno", "desc", OTRA_EMPRESA, false));
            releerDesdeLaBase();

            assertThat(repository.findOwnedByIdAndCompanyId(propio.getId(), SchemaSeed.COMPANY_ID))
                    .isPresent();
            assertThat(repository.findOwnedByIdAndCompanyId(general.getId(), SchemaSeed.COMPANY_ID))
                    .isEmpty();
            assertThat(repository.findOwnedByIdAndCompanyId(ajeno.getId(), SchemaSeed.COMPANY_ID))
                    .isEmpty();
            // La general sigue siendo legible por el finder de disponibles.
            assertThat(repository.findByIdAndCompanyId(general.getId(), SchemaSeed.COMPANY_ID))
                    .isPresent();
        }
    }
}
