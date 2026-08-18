package com.vetsoftware.app.systemuserpermission.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.systemuserpermission.domain.SystemUserPermission;
import com.vetsoftware.app.systemuserpermission.testsupport.SystemUserPermissionMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SystemUserPermissionDto.from")
class SystemUserPermissionDtoTest {

    @Test
    @DisplayName("copia cada campo del agregado en su posicion")
    void copia_cada_campo_del_agregado_en_su_posicion() {
        SystemUserPermission sup = SystemUserPermissionMother.asignacionActiva();

        SystemUserPermissionDto dto = SystemUserPermissionDto.from(sup);

        assertThat(dto.id()).isEqualTo(SystemUserPermissionMother.ID);
        assertThat(dto.createdDate()).isEqualTo(SystemUserPermissionMother.CREADO);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("aplana los companion VO en summaries sin perder campos")
    void aplana_los_companion_vo_en_summaries() {
        SystemUserPermissionDto dto = SystemUserPermissionDto
                .from(SystemUserPermissionMother.asignacionActiva());

        assertThat(dto.systemUser())
                .isEqualTo(new SystemUserSummaryDto(SystemUserPermissionMother.USUARIO.id(),
                        SystemUserPermissionMother.USUARIO.code()));
        assertThat(dto.systemPermission()).isEqualTo(new SystemPermissionSummaryDto(
                SystemUserPermissionMother.PERMISO.id(), SystemUserPermissionMother.PERMISO.name(),
                SystemUserPermissionMother.PERMISO.code()));
    }

    @Test
    @DisplayName("propaga la asignacion deshabilitada")
    void propaga_la_asignacion_deshabilitada() {
        assertThat(SystemUserPermissionDto
                .from(SystemUserPermissionMother.asignacionDeshabilitada()).enabled()).isFalse();
    }
}
