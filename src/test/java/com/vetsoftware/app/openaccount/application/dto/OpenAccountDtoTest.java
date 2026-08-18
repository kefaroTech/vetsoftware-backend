package com.vetsoftware.app.openaccount.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.openaccount.domain.OpenAccount;
import com.vetsoftware.app.openaccount.domain.OpenAccountStatus;
import com.vetsoftware.app.openaccount.testsupport.OpenAccountMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OpenAccountDto.from")
class OpenAccountDtoTest {

    @Test
    @DisplayName("mapea campo por campo una cuenta abierta, sin cierre")
    void mapea_campo_por_campo_una_cuenta_abierta() {
        OpenAccount cuenta = OpenAccountMother.abierta();

        OpenAccountDto dto = OpenAccountDto.from(cuenta);

        assertThat(dto.id()).isEqualTo(cuenta.getId());
        assertThat(dto.owner().id()).isEqualTo(cuenta.getOwner().id());
        assertThat(dto.owner().name()).isEqualTo(cuenta.getOwner().name());
        assertThat(dto.owner().document()).isEqualTo(cuenta.getOwner().document());
        assertThat(dto.totalAmount()).isEqualByComparingTo(cuenta.getTotalAmount());
        assertThat(dto.paidAmount()).isEqualByComparingTo(cuenta.getPaidAmount());
        assertThat(dto.outstandingAmount()).isEqualByComparingTo(cuenta.getOutstandingAmount());
        assertThat(dto.company().id()).isEqualTo(cuenta.getCompany().id());
        assertThat(dto.company().identifier()).isEqualTo(cuenta.getCompany().identifier());
        assertThat(dto.branch().id()).isEqualTo(cuenta.getBranch().id());
        assertThat(dto.branch().code()).isEqualTo(cuenta.getBranch().code());
        assertThat(dto.status()).isEqualTo(OpenAccountStatus.OPEN);
        assertThat(dto.createdBy().id()).isEqualTo(cuenta.getCreatedBy().id());
        assertThat(dto.createdDate()).isEqualTo(cuenta.getCreatedDate());
        assertThat(dto.enabled()).isTrue();
        // Rama clave: sin cierre, closedBy debe salir null y no reventar con un NPE al
        // intentar mapear un EmployeeRef inexistente.
        assertThat(dto.closedBy()).isNull();
        assertThat(dto.closedAt()).isNull();
        assertThat(dto.closeReason()).isNull();
        assertThat(dto.reversed()).isFalse();
        assertThat(dto.reversedAt()).isNull();
        assertThat(dto.version()).isEqualTo(cuenta.getVersion());
    }

    @Test
    @DisplayName("mapea el closedBy cuando la cuenta ya fue cerrada")
    void mapea_el_closed_by_cuando_la_cuenta_fue_cerrada() {
        OpenAccount cerrada = OpenAccountMother.cerrada();

        OpenAccountDto dto = OpenAccountDto.from(cerrada);

        assertThat(dto.status()).isEqualTo(OpenAccountStatus.CLOSE);
        assertThat(dto.closedBy()).isNotNull();
        assertThat(dto.closedBy().id()).isEqualTo(cerrada.getClosedBy().id());
        assertThat(dto.closedBy().name()).isEqualTo(cerrada.getClosedBy().name());
        assertThat(dto.closedAt()).isEqualTo(cerrada.getClosedAt());
    }
}
