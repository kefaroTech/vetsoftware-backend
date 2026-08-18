package com.vetsoftware.app.membershipsubmodule.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.membershipsubmodule.domain.MembershipSubModule;
import com.vetsoftware.app.membershipsubmodule.testsupport.MembershipSubModuleMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MembershipSubModuleDto.from — mapeo campo por campo")
class MembershipSubModuleDtoTest {

    @Test
    @DisplayName("copia cada campo de la relacion activa")
    void copia_cada_campo_de_la_relacion_activa() {
        MembershipSubModule relacion = MembershipSubModuleMother.activa();

        MembershipSubModuleDto dto = MembershipSubModuleDto.from(relacion);

        assertThat(dto.id()).isEqualTo(relacion.getId());
        assertThat(dto.membership()).isEqualTo(MembershipSummaryDto.from(relacion.getMembership()));
        assertThat(dto.subModule()).isEqualTo(SubModuleSummaryDto.from(relacion.getSubModule()));
        assertThat(dto.createdDate()).isEqualTo(relacion.getCreatedDate());
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("una relacion deshabilitada mapea enabled en falso")
    void una_relacion_deshabilitada_mapea_enabled_en_falso() {
        MembershipSubModule relacion = MembershipSubModuleMother.deshabilitada();

        MembershipSubModuleDto dto = MembershipSubModuleDto.from(relacion);

        assertThat(dto.enabled()).isFalse();
    }
}
