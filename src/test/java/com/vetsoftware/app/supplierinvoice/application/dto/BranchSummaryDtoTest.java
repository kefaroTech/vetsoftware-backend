package com.vetsoftware.app.supplierinvoice.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.supplierinvoice.domain.BranchRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BranchSummaryDto")
class BranchSummaryDtoTest {

    @Test
    @DisplayName("from mapea id y nombre de la sede")
    void from_mapea_id_y_nombre() {
        BranchSummaryDto dto = BranchSummaryDto.from(new BranchRef(3L, "Sede Centro"));

        assertThat(dto.id()).isEqualTo(3L);
        assertThat(dto.name()).isEqualTo("Sede Centro");
    }
}
