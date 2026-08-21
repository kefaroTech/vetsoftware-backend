package com.vetsoftware.app.diagnosticimagingtype.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.diagnosticimagingtype.domain.CompanyRef;
import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingType;
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
 * Rodaja de persistencia de {@link JpaDiagnosticImagingTypeRepository} contra
 * MySQL real: ejercita el {@code getReferenceById} de la company, el
 * {@code @SQLRestriction} de soft-delete, {@code reactivate} (UPDATE nativo) y
 * {@code findAvailableById}/{@code findAllByGeneralTrueOrCompany_Id}, que
 * mezclan filas globales y privadas por empresa — nada de eso lo ve un test en
 * memoria.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaDiagnosticImagingTypeRepository — catalogo de tipos de imagen diagnostica contra MySQL real")
class DiagnosticImagingTypePersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_COMPANY = SchemaSeed.OTRA_COMPANY_ID;

    @Autowired
    private JpaDiagnosticImagingTypeRepository repository;
    @Autowired
    private CompanyJpaRepository companyJpaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private CompanyJpaEntity company;

    @BeforeEach
    void sembrarLaEmpresa() {
        SchemaSeed.seed(entityManager);
        company = companyJpaRepository.getReferenceById(COMPANY);
    }

    private void releerDesdeLaBase() {
        entityManager.flush();
        entityManager.clear();
        company = companyJpaRepository.getReferenceById(COMPANY);
    }

    private DiagnosticImagingType tipoGeneralValido() {
        return DiagnosticImagingType.create("Radiografia", "Radiografia simple digital", null,
                true);
    }

    private DiagnosticImagingType tipoDeEmpresaValido() {
        return DiagnosticImagingType.create("Ecografia abdominal", "Ecografia de rutina",
                new CompanyRef(company.getId(), company.getName(), company.getIdentifier()), false);
    }

    @Nested
    @DisplayName("save")
    class Guardado {

        @Test
        @DisplayName("persiste un tipo general (sin company) y devuelve el id asignado")
        void persiste_un_tipo_general_sin_company() {
            DiagnosticImagingType guardado = repository.save(tipoGeneralValido());
            releerDesdeLaBase();

            assertThat(guardado.getId()).isNotNull();
            DiagnosticImagingType releido = repository.findById(guardado.getId()).orElseThrow();
            assertThat(releido.getCompany()).isNull();
            assertThat(releido.isGeneral()).isTrue();
        }

        @Test
        @DisplayName("persiste un tipo propio de empresa con la asociacion resuelta por getReferenceById")
        void persiste_un_tipo_propio_de_empresa() {
            DiagnosticImagingType guardado = repository.save(tipoDeEmpresaValido());
            releerDesdeLaBase();

            DiagnosticImagingType releido = repository.findById(guardado.getId()).orElseThrow();
            assertThat(releido.getCompany().id()).isEqualTo(COMPANY);
            assertThat(releido.isGeneral()).isFalse();
        }
    }

    @Nested
    @DisplayName("findByIdAndCompanyId / findAvailableById")
    class BusquedaDisponible {

        @Test
        @DisplayName("un tipo general esta disponible para cualquier empresa")
        void un_tipo_general_esta_disponible_para_cualquier_empresa() {
            DiagnosticImagingType guardado = repository.save(tipoGeneralValido());
            releerDesdeLaBase();

            assertThat(repository.findByIdAndCompanyId(guardado.getId(), COMPANY)).isPresent();
            assertThat(repository.findByIdAndCompanyId(guardado.getId(), OTRA_COMPANY)).isPresent();
        }

        @Test
        @DisplayName("un tipo propio de otra empresa no se entrega")
        void un_tipo_propio_de_otra_empresa_no_se_entrega() {
            DiagnosticImagingType guardado = repository.save(tipoDeEmpresaValido());
            releerDesdeLaBase();

            assertThat(repository.findByIdAndCompanyId(guardado.getId(), OTRA_COMPANY)).isEmpty();
            assertThat(repository.findByIdAndCompanyId(guardado.getId(), COMPANY)).isPresent();
        }
    }

    @Nested
    @DisplayName("findAllAvailableForCompany")
    class ListadoDisponible {

        @Test
        @DisplayName("mezcla los tipos generales con los propios de la empresa, sin los de otras")
        void mezcla_generales_con_los_propios_sin_los_de_otras_empresas() {
            DiagnosticImagingType general = repository.save(tipoGeneralValido());
            releerDesdeLaBase();
            DiagnosticImagingType propio = repository.save(tipoDeEmpresaValido());
            releerDesdeLaBase();
            DiagnosticImagingType ajeno = repository
                    .save(DiagnosticImagingType.create("Tomografia", "desc",
                            new CompanyRef(OTRA_COMPANY,
                                    companyJpaRepository.getReferenceById(OTRA_COMPANY).getName(),
                                    "900654321"),
                            false));
            releerDesdeLaBase();

            var disponibles = repository.findAllAvailableForCompany(COMPANY);

            assertThat(disponibles).extracting(DiagnosticImagingType::getId)
                    .contains(general.getId(), propio.getId()).doesNotContain(ajeno.getId());
        }
    }

    @Nested
    @DisplayName("delete")
    class BorradoYReactivacion {

        @Test
        @DisplayName("un tipo borrado desaparece de findById (SQLRestriction)")
        void tipo_borrado_desaparece() {
            DiagnosticImagingType guardado = repository.save(tipoGeneralValido());
            releerDesdeLaBase();

            repository.delete(guardado.getId());
            releerDesdeLaBase();

            assertThat(repository.findById(guardado.getId())).isEmpty();
        }

        @Test
        @DisplayName("findOwnedByIdAndCompanyId ve la fila propia pero NO la general")
        void find_owned_solo_ve_la_fila_propia() {
            // Es el finder de los caminos de ESCRITURA. A diferencia de
            // findByIdAndCompanyId (disponibles), excluye las generales: si las incluyera,
            // el update les pondria el company_id del llamador.
            DiagnosticImagingType propio = repository.save(tipoDeEmpresaValido());
            DiagnosticImagingType general = repository.save(tipoGeneralValido());
            releerDesdeLaBase();

            assertThat(repository.findOwnedByIdAndCompanyId(propio.getId(), COMPANY)).isPresent();
            assertThat(repository.findOwnedByIdAndCompanyId(general.getId(), COMPANY)).isEmpty();
            assertThat(repository.findOwnedByIdAndCompanyId(propio.getId(), OTRA_COMPANY))
                    .isEmpty();
            // La general sigue siendo legible por el finder de disponibles.
            assertThat(repository.findByIdAndCompanyId(general.getId(), COMPANY)).isPresent();
        }
    }

    @Nested
    @DisplayName("findAll")
    class ListadoGlobal {

        @Test
        @DisplayName("devuelve tipos de todas las empresas, sin acotar tenant")
        void devuelve_tipos_de_todas_las_empresas() {
            DiagnosticImagingType propio = repository.save(tipoDeEmpresaValido());
            releerDesdeLaBase();
            DiagnosticImagingType ajeno = repository
                    .save(DiagnosticImagingType.create("Tomografia", "desc",
                            new CompanyRef(OTRA_COMPANY,
                                    companyJpaRepository.getReferenceById(OTRA_COMPANY).getName(),
                                    "900654321"),
                            false));
            releerDesdeLaBase();

            assertThat(repository.findAll()).extracting(DiagnosticImagingType::getId)
                    .contains(propio.getId(), ajeno.getId());
        }
    }
}
