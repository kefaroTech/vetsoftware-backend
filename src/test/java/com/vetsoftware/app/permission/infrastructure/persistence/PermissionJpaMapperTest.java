package com.vetsoftware.app.permission.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.permission.domain.Permission;
import com.vetsoftware.app.permission.testsupport.PermissionMother;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link CompanyJpaEntity} y {@link SubModuleJpaEntity} pertenecen a otras
 * features y su constructor es {@code protected}: desde este paquete no se
 * pueden instanciar, asi que se mockean. No son entidades de dominio —no tienen
 * invariantes— sino filas, por lo que el doble no oculta ninguna regla de
 * negocio.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionJpaMapper — ida y vuelta dominio↔entidad")
class PermissionJpaMapperTest {

    private final PermissionJpaMapper mapper = new PermissionJpaMapper();

    @Mock
    private CompanyJpaEntity companyEntity;
    @Mock
    private SubModuleJpaEntity subModuleEntity;

    @Nested
    @DisplayName("toJpa")
    class ADominioPersistente {

        @Test
        @DisplayName("copia los campos y engancha las dos relaciones recibidas")
        void copia_los_campos_y_las_relaciones() {
            Permission permission = PermissionMother.permisoValido();

            PermissionJpaEntity entity = mapper.toJpa(permission, companyEntity, subModuleEntity);

            assertThat(entity.getId()).isEqualTo(PermissionMother.PERMISSION_ID);
            assertThat(entity.getName()).isEqualTo("Crear factura");
            assertThat(entity.getCode()).isEqualTo("billing.create");
            assertThat(entity.getCompany()).isSameAs(companyEntity);
            assertThat(entity.getSubModule()).isSameAs(subModuleEntity);
            assertThat(entity.getCreatedDate()).isEqualTo(PermissionMother.CREADO);
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("un permiso nuevo viaja sin id para que lo genere la base")
        void un_permiso_nuevo_viaja_sin_id() {
            Permission nuevo = Permission.create("Crear factura", "billing.create",
                    PermissionMother.CLINICA, PermissionMother.INVENTARIO);

            PermissionJpaEntity entity = mapper.toJpa(nuevo, companyEntity, subModuleEntity);

            assertThat(entity.getId()).isNull();
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("propaga el estado deshabilitado")
        void propaga_el_estado_deshabilitado() {
            PermissionJpaEntity entity = mapper.toJpa(PermissionMother.deshabilitado(),
                    companyEntity, subModuleEntity);

            assertThat(entity.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("toDomain")
    class ADominio {

        @Test
        @DisplayName("construye las referencias leyendo las entidades relacionadas")
        void construye_las_referencias_leyendo_las_relaciones() {
            when(companyEntity.getId()).thenReturn(PermissionMother.COMPANY_ID);
            when(companyEntity.getName()).thenReturn("Clinica Norte");
            when(companyEntity.getIdentifier()).thenReturn("NIT-900");
            when(subModuleEntity.getId()).thenReturn(PermissionMother.SUB_MODULE_ID);
            when(subModuleEntity.getName()).thenReturn("Inventario");
            when(subModuleEntity.getCode()).thenReturn("INV");
            PermissionJpaEntity entity = new PermissionJpaEntity();
            entity.setId(PermissionMother.PERMISSION_ID);
            entity.setName("Crear factura");
            entity.setCode("billing.create");
            entity.setCompany(companyEntity);
            entity.setSubModule(subModuleEntity);
            entity.setCreatedDate(PermissionMother.CREADO);
            entity.setEnabled(true);

            Permission domain = mapper.toDomain(entity);

            assertThat(domain.getId()).isEqualTo(PermissionMother.PERMISSION_ID);
            assertThat(domain.getCompany()).isEqualTo(PermissionMother.CLINICA);
            assertThat(domain.getSubModule()).isEqualTo(PermissionMother.INVENTARIO);
            assertThat(domain.getCreatedDate()).isEqualTo(PermissionMother.CREADO);
            assertThat(domain.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("la sobrecarga con refs no toca las relaciones de la entidad")
        void la_sobrecarga_con_refs_no_toca_las_relaciones() {
            PermissionJpaEntity entity = new PermissionJpaEntity();
            entity.setId(PermissionMother.PERMISSION_ID);
            entity.setName("Crear factura");
            entity.setCode("billing.create");
            entity.setCreatedDate(PermissionMother.CREADO);
            entity.setEnabled(false);

            Permission domain = mapper.toDomain(entity, PermissionMother.OTRA_CLINICA,
                    PermissionMother.FACTURACION);

            assertThat(domain.getCompany()).isEqualTo(PermissionMother.OTRA_CLINICA);
            assertThat(domain.getSubModule()).isEqualTo(PermissionMother.FACTURACION);
            assertThat(domain.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("dominio → entidad → dominio conserva todos los campos")
        void conserva_todos_los_campos() {
            Permission original = PermissionMother.permisoValido();

            PermissionJpaEntity entity = mapper.toJpa(original, companyEntity, subModuleEntity);
            Permission vuelta = mapper.toDomain(entity, original.getCompany(),
                    original.getSubModule());

            assertThat(vuelta.getId()).isEqualTo(original.getId());
            assertThat(vuelta.getName()).isEqualTo(original.getName());
            assertThat(vuelta.getCode()).isEqualTo(original.getCode());
            assertThat(vuelta.getCompany()).isEqualTo(original.getCompany());
            assertThat(vuelta.getSubModule()).isEqualTo(original.getSubModule());
            assertThat(vuelta.getCreatedDate()).isEqualTo(original.getCreatedDate());
            assertThat(vuelta.isEnabled()).isEqualTo(original.isEnabled());
        }
    }
}
