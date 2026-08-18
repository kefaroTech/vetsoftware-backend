package com.vetsoftware.app.systemuserpermission.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.systempermission.infrastructure.persistence.SystemPermissionJpaEntity;
import com.vetsoftware.app.systemuser.infrastructure.persistence.SystemUserJpaEntity;
import com.vetsoftware.app.systemuserpermission.domain.SystemUserPermission;
import com.vetsoftware.app.systemuserpermission.testsupport.SystemUserPermissionMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El mapper es el unico punto que conoce dominio y entidad JPA a la vez, asi
 * que un campo cruzado aqui no lo detecta ninguna otra capa: compila, persiste
 * y solo se ve en pantalla.
 *
 * <p>
 * Las entidades JPA de las otras features (systemuser, systempermission) se
 * mockean porque su constructor sin argumentos es {@code protected} y no son
 * instanciables desde este paquete. No tienen logica: son portadores de datos,
 * y mockearlas no oculta comportamiento.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SystemUserPermissionJpaMapper")
class SystemUserPermissionJpaMapperTest {

    private final SystemUserPermissionJpaMapper mapper = new SystemUserPermissionJpaMapper();

    @Mock
    private SystemUserJpaEntity systemUserEntity;
    @Mock
    private SystemPermissionJpaEntity systemPermissionEntity;

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar y engancha cada asociacion")
        void copia_cada_campo_escalar_y_engancha_cada_asociacion() {
            SystemUserPermission sup = SystemUserPermissionMother.asignacionActiva();

            SystemUserPermissionJpaEntity entity = mapper.toJpa(sup, systemUserEntity,
                    systemPermissionEntity);

            assertThat(entity.getId()).isEqualTo(SystemUserPermissionMother.ID);
            assertThat(entity.getSystemUser()).isSameAs(systemUserEntity);
            assertThat(entity.getSystemPermission()).isSameAs(systemPermissionEntity);
            assertThat(entity.getCreatedDate()).isEqualTo(SystemUserPermissionMother.CREADO);
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("propaga la asignacion deshabilitada")
        void propaga_la_asignacion_deshabilitada() {
            SystemUserPermissionJpaEntity entity = mapper.toJpa(
                    SystemUserPermissionMother.asignacionDeshabilitada(), systemUserEntity,
                    systemPermissionEntity);

            assertThat(entity.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("toDomain con refs precargados — camino de escritura")
    class ToDomainConRefs {

        @Test
        @DisplayName("reconstruye el agregado sin tocar las asociaciones JPA")
        void reconstruye_el_agregado_sin_tocar_las_asociaciones() {
            SystemUserPermissionJpaEntity entity = new SystemUserPermissionJpaEntity();
            entity.setId(SystemUserPermissionMother.ID);
            entity.setCreatedDate(SystemUserPermissionMother.CREADO);
            entity.setEnabled(true);

            SystemUserPermission sup = mapper.toDomain(entity, SystemUserPermissionMother.USUARIO,
                    SystemUserPermissionMother.PERMISO);

            assertThat(sup.getId()).isEqualTo(SystemUserPermissionMother.ID);
            assertThat(sup.getSystemUser()).isEqualTo(SystemUserPermissionMother.USUARIO);
            assertThat(sup.getSystemPermission()).isEqualTo(SystemUserPermissionMother.PERMISO);
            assertThat(sup.getCreatedDate()).isEqualTo(SystemUserPermissionMother.CREADO);
            assertThat(sup.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("la ida y vuelta dominio -> entidad -> dominio no pierde nada")
        void la_ida_y_vuelta_no_pierde_nada() {
            SystemUserPermission original = SystemUserPermissionMother.asignacionActiva();

            SystemUserPermissionJpaEntity entity = mapper.toJpa(original, systemUserEntity,
                    systemPermissionEntity);
            SystemUserPermission vuelta = mapper.toDomain(entity, original.getSystemUser(),
                    original.getSystemPermission());

            assertThat(vuelta.getId()).isEqualTo(original.getId());
            assertThat(vuelta.getSystemUser()).isEqualTo(original.getSystemUser());
            assertThat(vuelta.getSystemPermission()).isEqualTo(original.getSystemPermission());
            assertThat(vuelta.getCreatedDate()).isEqualTo(original.getCreatedDate());
            assertThat(vuelta.isEnabled()).isEqualTo(original.isEnabled());
        }
    }

    @Nested
    @DisplayName("toDomain desde las asociaciones — camino de lectura")
    class ToDomainDesdeAsociaciones {

        @Test
        @DisplayName("construye cada companion VO desde su propia asociacion")
        void construye_cada_companion_vo_desde_su_asociacion() {
            when(systemUserEntity.getId()).thenReturn(SystemUserPermissionMother.USUARIO.id());
            when(systemUserEntity.getCode()).thenReturn(SystemUserPermissionMother.USUARIO.code());
            when(systemPermissionEntity.getId())
                    .thenReturn(SystemUserPermissionMother.PERMISO.id());
            when(systemPermissionEntity.getName())
                    .thenReturn(SystemUserPermissionMother.PERMISO.name());
            when(systemPermissionEntity.getCode())
                    .thenReturn(SystemUserPermissionMother.PERMISO.code());

            SystemUserPermissionJpaEntity entity = new SystemUserPermissionJpaEntity();
            entity.setId(SystemUserPermissionMother.ID);
            entity.setCreatedDate(SystemUserPermissionMother.CREADO);
            entity.setEnabled(true);
            entity.setSystemUser(systemUserEntity);
            entity.setSystemPermission(systemPermissionEntity);

            SystemUserPermission sup = mapper.toDomain(entity);

            assertThat(sup.getSystemUser()).isEqualTo(SystemUserPermissionMother.USUARIO);
            assertThat(sup.getSystemPermission()).isEqualTo(SystemUserPermissionMother.PERMISO);
        }
    }
}
