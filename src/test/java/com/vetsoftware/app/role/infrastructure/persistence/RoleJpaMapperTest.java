package com.vetsoftware.app.role.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.role.domain.Role;
import com.vetsoftware.app.role.testsupport.RoleMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link CompanyJpaEntity} pertenece a otra feature y su constructor es
 * {@code protected}: desde este paquete no se puede instanciar, asi que se
 * mockea. No es una entidad de dominio —no tiene invariantes— sino una fila.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoleJpaMapper — ida y vuelta dominio↔entidad")
class RoleJpaMapperTest {

    private final RoleJpaMapper mapper = new RoleJpaMapper();

    @Mock
    private CompanyJpaEntity companyEntity;

    @Nested
    @DisplayName("toJpa")
    class ADominioPersistente {

        @Test
        @DisplayName("copia cada campo y engancha la empresa recibida")
        void copia_cada_campo_y_engancha_la_empresa() {
            Role role = RoleMother.veterinario();

            RoleJpaEntity entity = mapper.toJpa(role, companyEntity);

            assertThat(entity.getId()).isEqualTo(role.getId());
            assertThat(entity.getName()).isEqualTo(role.getName());
            assertThat(entity.getCode()).isEqualTo(role.getCode());
            assertThat(entity.getCompany()).isSameAs(companyEntity);
            assertThat(entity.getCreatedDate()).isEqualTo(role.getCreatedDate());
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("un rol nuevo viaja sin id para que lo genere la base")
        void un_rol_nuevo_viaja_sin_id() {
            Role nuevo = Role.create("Veterinario", "VET", RoleMother.CLINICA_NORTE);

            RoleJpaEntity entity = mapper.toJpa(nuevo, companyEntity);

            assertThat(entity.getId()).isNull();
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("propaga el estado deshabilitado")
        void propaga_el_estado_deshabilitado() {
            RoleJpaEntity entity = mapper.toJpa(RoleMother.deshabilitado(), companyEntity);

            assertThat(entity.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("toDomain")
    class ADominio {

        @Test
        @DisplayName("construye la empresa leyendo la relacion ya hidratada")
        void construye_la_empresa_leyendo_la_relacion() {
            when(companyEntity.getId()).thenReturn(RoleMother.COMPANY_ID);
            when(companyEntity.getName()).thenReturn("Clinica Norte");
            when(companyEntity.getIdentifier()).thenReturn("NIT-900");
            RoleJpaEntity entity = new RoleJpaEntity();
            entity.setId(RoleMother.ROLE_ID);
            entity.setName("Veterinario");
            entity.setCode("VET");
            entity.setCompany(companyEntity);
            entity.setCreatedDate(RoleMother.CREADO);
            entity.setEnabled(true);

            Role role = mapper.toDomain(entity);

            assertThat(role.getCompany()).isEqualTo(RoleMother.CLINICA_NORTE);
            assertThat(role.getName()).isEqualTo("Veterinario");
        }

        @Test
        @DisplayName("la sobrecarga con ref no toca la relacion de la entidad")
        void la_sobrecarga_con_ref_no_toca_la_relacion() {
            RoleJpaEntity entity = new RoleJpaEntity();
            entity.setId(RoleMother.ROLE_ID);
            entity.setName("Veterinario");
            entity.setCode("VET");
            entity.setCreatedDate(RoleMother.CREADO);
            entity.setEnabled(false);

            Role role = mapper.toDomain(entity, RoleMother.CLINICA_NORTE);

            assertThat(role.getCompany()).isEqualTo(RoleMother.CLINICA_NORTE);
            assertThat(role.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("dominio → entidad → dominio conserva todos los campos")
        void conserva_todos_los_campos() {
            Role original = RoleMother.veterinario();

            RoleJpaEntity entity = mapper.toJpa(original, companyEntity);
            Role vuelta = mapper.toDomain(entity, original.getCompany());

            assertThat(vuelta.getId()).isEqualTo(original.getId());
            assertThat(vuelta.getName()).isEqualTo(original.getName());
            assertThat(vuelta.getCode()).isEqualTo(original.getCode());
            assertThat(vuelta.getCompany()).isEqualTo(original.getCompany());
            assertThat(vuelta.getCreatedDate()).isEqualTo(original.getCreatedDate());
            assertThat(vuelta.isEnabled()).isEqualTo(original.isEnabled());
        }
    }
}
