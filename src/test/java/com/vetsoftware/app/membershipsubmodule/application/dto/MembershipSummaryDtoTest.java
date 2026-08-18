package com.vetsoftware.app.membershipsubmodule.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.membershipsubmodule.domain.MembershipRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MembershipSummaryDto.from — mapeo campo por campo")
class MembershipSummaryDtoTest {

    @Test
    @DisplayName("copia cada campo del companion VO")
    void copia_cada_campo_del_companion_vo() {
        MembershipRef ref = new MembershipRef(900L, "Plan Premium");

        MembershipSummaryDto dto = MembershipSummaryDto.from(ref);

        assertThat(dto.id()).isEqualTo(900L);
        assertThat(dto.name()).isEqualTo("Plan Premium");
    }
}
