package com.vetsoftware.app.company.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.company.domain.MembershipRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MembershipSummaryDto.from")
class MembershipSummaryDtoTest {

    @Test
    @DisplayName("traslada id, nombre y estado sin cruzar los dos campos de texto")
    void traslada_id_nombre_y_estado() {
        MembershipSummaryDto dto = MembershipSummaryDto
                .from(new MembershipRef(21L, "Premium", "ACTIVE"));

        assertThat(dto.id()).isEqualTo(21L);
        assertThat(dto.name()).isEqualTo("Premium");
        assertThat(dto.status()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("el DTO resultante equivale al construido a mano con los mismos valores")
    void equivale_al_construido_a_mano() {
        assertThat(MembershipSummaryDto.from(new MembershipRef(22L, "Basica", "TRIAL")))
                .isEqualTo(new MembershipSummaryDto(22L, "Basica", "TRIAL"));
    }
}
