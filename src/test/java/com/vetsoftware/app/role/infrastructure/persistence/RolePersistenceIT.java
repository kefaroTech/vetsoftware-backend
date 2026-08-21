package com.vetsoftware.app.role.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.role.domain.CompanyRef;
import com.vetsoftware.app.role.domain.Role;
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
 * Rodaja de persistencia de roles contra MySQL real.
 *
 * <p>
 * Lo que no ve un mapper ni un service con dobles: que {@code reactivate} de
 * verdad acota por {@code company_id} en el UPDATE —no solo por id— y que
 * {@code findAllByCompanyId} no arrastra filas de otra empresa.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaRoleRepository — roles contra MySQL real")
class RolePersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_COMPANY = SchemaSeed.OTRA_COMPANY_ID;

    @Autowired
    private JpaRoleRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
    }

    private Role nuevoVeterinario(String code) {
        return Role.create("Veterinario", code,
                new CompanyRef(COMPANY, "Veterinaria de prueba", "900123456"));
    }

    @Nested
    @DisplayName("ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar asigna id, hidrata la empresa, y releer conserva los campos")
        void guardar_asigna_id_e_hidrata_la_empresa() {
            Role guardado = repository.save(nuevoVeterinario("VET-A"));

            assertThat(guardado.getId()).isNotNull();
            assertThat(guardado.getCompany().identifier()).isEqualTo("900123456");

            Role leido = repository.findByIdAndCompanyId(guardado.getId(), COMPANY).orElseThrow();
            assertThat(leido.getCode()).isEqualTo("VET-A");
            assertThat(leido.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("un rol de otra empresa no aparece al buscar por id y empresa")
        void un_rol_de_otra_empresa_no_aparece() {
            Role guardado = repository.save(nuevoVeterinario("VET-A"));

            Optional<Role> encontrado = repository.findByIdAndCompanyId(guardado.getId(),
                    OTRA_COMPANY);

            assertThat(encontrado).isEmpty();
        }
    }

    @Nested
    @DisplayName("listado")
    class Listado {

        @Test
        @DisplayName("findAllByCompanyId trae solo los roles de esa empresa")
        void trae_solo_los_roles_de_la_empresa() {
            Role guardado = repository.save(nuevoVeterinario("VET-A"));
            repository.save(Role.create("Administrador", "ADMIN-OTRA",
                    new CompanyRef(OTRA_COMPANY, "Veterinaria ajena", "900654321")));

            List<Role> roles = repository.findAllByCompanyId(COMPANY);

            assertThat(roles).extracting(Role::getId).contains(guardado.getId());
            assertThat(roles).allSatisfy(r -> assertThat(r.getCompany().id()).isEqualTo(COMPANY));
        }

        @Test
        @DisplayName("findAll incluye los roles de todas las empresas")
        void find_all_incluye_todas_las_empresas() {
            Role guardado = repository.save(nuevoVeterinario("VET-A"));

            List<Role> todos = repository.findAll();

            assertThat(todos).extracting(Role::getId).contains(guardado.getId());
        }
    }

    @Nested
    @DisplayName("borrado")
    class Borrado {

        @Test
        @DisplayName("delete es un soft-delete: no vuelve a aparecer por findById")
        void delete_es_un_soft_delete() {
            Role guardado = repository.save(nuevoVeterinario("VET-A"));

            repository.delete(guardado.getId());

            assertThat(repository.findById(guardado.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("reactivate vuelve a habilitar un rol borrado de la misma empresa")
        void reactivate_vuelve_a_habilitar_un_rol_de_la_misma_empresa() {
            Role guardado = repository.save(nuevoVeterinario("VET-A"));
            repository.delete(guardado.getId());

            int filas = repository.reactivate(guardado.getId(), COMPANY);

            assertThat(filas).isEqualTo(1);
            assertThat(repository.findByIdAndCompanyId(guardado.getId(), COMPANY)).isPresent();
        }

        @Test
        @DisplayName("reactivate no afecta un rol borrado de otra empresa")
        void reactivate_no_afecta_un_rol_de_otra_empresa() {
            Role guardado = repository.save(nuevoVeterinario("VET-A"));
            repository.delete(guardado.getId());

            int filas = repository.reactivate(guardado.getId(), OTRA_COMPANY);

            assertThat(filas).isZero();
        }
    }
}
