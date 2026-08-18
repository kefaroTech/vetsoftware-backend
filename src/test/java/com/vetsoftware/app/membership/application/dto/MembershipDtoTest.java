package com.vetsoftware.app.membership.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.membership.domain.Membership;
import com.vetsoftware.app.membership.domain.MembershipStatus;
import com.vetsoftware.app.membership.testsupport.MembershipMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El mapeo campo a campo se prueba entero a proposito: un intercambio entre dos
 * campos del mismo tipo compila, pasa cualquier test de "no es null", y solo se
 * ve en pantalla.
 */
@DisplayName("MembershipDto.from")
class MembershipDtoTest {

    @Test
    @DisplayName("copia cada campo del agregado en su posicion")
    void copia_cada_campo_del_agregado_en_su_posicion() {
        Membership membership = MembershipMother.activa();

        MembershipDto dto = MembershipDto.from(membership);

        assertThat(dto.id()).isEqualTo(MembershipMother.MEMBERSHIP_ID);
        assertThat(dto.name()).isEqualTo("Plan Oro");
        assertThat(dto.status()).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(dto.mandatory()).isFalse();
        assertThat(dto.createdDate()).isEqualTo(MembershipMother.CREADO);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("propaga la obligatoriedad")
    void propaga_la_obligatoriedad() {
        assertThat(MembershipDto.from(MembershipMother.obligatoria()).mandatory()).isTrue();
    }

    @Test
    @DisplayName("propaga la membresia deshabilitada")
    void propaga_la_membresia_deshabilitada() {
        assertThat(MembershipDto.from(MembershipMother.deshabilitada()).enabled()).isFalse();
    }
}
