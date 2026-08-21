package com.vetsoftware.app.branch.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.branch.domain.Branch;
import com.vetsoftware.app.branch.domain.CityRef;
import com.vetsoftware.app.branch.domain.CompanyRef;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia de sucursales contra MySQL real.
 *
 * <p>
 * Lo que no ve un mapper ni un service con dobles: que {@code codeExists} y
 * {@code codeExistsForOther} de verdad distinguen por empresa (el índice único
 * es {@code (company_id, code)}, no {@code code} a secas), y que
 * {@code existsOtherActiveByCompanyId} lee el estado real de otras filas.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaBranchRepository — sucursales contra MySQL real")
class BranchPersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_COMPANY = SchemaSeed.OTRA_COMPANY_ID;
    private static final Long CITY = SchemaSeed.CITY_ID;

    @Autowired
    private JpaBranchRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
    }

    private Branch nuevaSede(String code) {
        return Branch.create("Sede " + code, code, "Cra 1", "3001112233",
                new CityRef(CITY, "Medellin"),
                new CompanyRef(COMPANY, "Veterinaria de prueba", "900123456"));
    }

    @Nested
    @DisplayName("ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar asigna id, hidrata ciudad y empresa, y releer conserva los campos")
        void guardar_asigna_id_e_hidrata_las_asociaciones() {
            Branch guardada = repository.save(nuevaSede("SUR"));

            assertThat(guardada.getId()).isNotNull();
            assertThat(guardada.getCity().name()).isEqualTo("Medellin");
            assertThat(guardada.getCompany().identifier()).isEqualTo("900123456");

            Branch leida = repository.findByIdAndCompanyId(guardada.getId(), COMPANY).orElseThrow();
            assertThat(leida.getCode()).isEqualTo("SUR");
            assertThat(leida.isActive()).isTrue();
        }

        @Test
        @DisplayName("una sede de otra empresa no aparece al buscar por id y empresa")
        void una_sede_de_otra_empresa_no_aparece() {
            Branch guardada = repository.save(nuevaSede("SUR"));

            Optional<Branch> encontrada = repository.findByIdAndCompanyId(guardada.getId(),
                    OTRA_COMPANY);

            assertThat(encontrada).isEmpty();
        }
    }

    @Nested
    @DisplayName("listado por empresa")
    class Listado {

        @Test
        @DisplayName("findAllByCompanyId trae solo las sedes de esa empresa, incluidas las sembradas")
        void trae_solo_las_sedes_de_la_empresa() {
            repository.save(nuevaSede("SUR"));

            List<Branch> sedes = repository.findAllByCompanyId(COMPANY);

            assertThat(sedes).extracting(Branch::getCode).contains("SUR", "CENTRO", "NORTE");
            assertThat(sedes).allSatisfy(b -> assertThat(b.getCompany().id()).isEqualTo(COMPANY));
        }
    }

    @Nested
    @DisplayName("unicidad e invariantes de negocio")
    class Unicidad {

        @Test
        @DisplayName("codeExists distingue por empresa: el mismo código en otra empresa no cuenta")
        void code_exists_distingue_por_empresa() {
            repository.save(nuevaSede("SUR"));

            assertThat(repository.codeExists(COMPANY, "SUR")).isTrue();
            assertThat(repository.codeExists(OTRA_COMPANY, "SUR")).isFalse();
        }

        @Test
        @DisplayName("codeExistsForOther ignora la propia fila al editar")
        void code_exists_for_other_ignora_la_propia_fila() {
            Branch guardada = repository.save(nuevaSede("SUR"));

            assertThat(repository.codeExistsForOther(COMPANY, "SUR", guardada.getId())).isFalse();
            assertThat(repository.codeExistsForOther(COMPANY, "SUR", guardada.getId() + 1))
                    .isTrue();
        }

        @Test
        @DisplayName("existsOtherActiveByCompanyId ve las sedes activas sembradas como \"otras\"")
        void exists_other_active_ve_las_sedes_sembradas() {
            Branch guardada = repository.save(nuevaSede("SUR"));

            // CENTRO y NORTE ya vienen activas del seed: son "otra sede activa" para SUR.
            assertThat(repository.existsOtherActiveByCompanyId(COMPANY, guardada.getId())).isTrue();
        }
    }
}
