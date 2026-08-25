package com.vetsoftware.app.platformaccess.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.platformaccess.domain.PlatformAccessRequest;
import com.vetsoftware.app.platformaccess.testsupport.PlatformAccessMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Los dos DTO de salida del flujo. Lo que se fija aquí no es que copien campos
 * sino <b>cuáles NO copian</b>: la pantalla del aprobador recibe cuatro datos y
 * ni uno más, y la de la invitación recibe uno. Los hashes del token y del
 * código viven en la misma entidad y cualquiera de los dos filtrado a la
 * respuesta HTTP publicaría el verificador de un código de seis dígitos, es
 * decir, el código.
 */
@DisplayName("DTOs de salida — lo que sale y, sobre todo, lo que no")
class PlatformAccessDtosTest {

    @Test
    @DisplayName("from copia los cuatro campos de la pantalla del aprobador")
    void from_copia_los_cuatro_campos() {
        PlatformAccessRequest solicitud = PlatformAccessMother.solicitudPendiente();

        PlatformAccessRequestDto dto = PlatformAccessRequestDto.from(solicitud);

        assertThat(dto.fullName()).isEqualTo(PlatformAccessMother.NOMBRE);
        assertThat(dto.email()).isEqualTo(PlatformAccessMother.CORREO);
        assertThat(dto.reason()).isEqualTo(PlatformAccessMother.MOTIVO);
        assertThat(dto.requestedAt()).isEqualTo(solicitud.getCreatedDate());
    }

    @Test
    @DisplayName("requestedAt es createdDate crudo: el front formatea, el backend no manda texto")
    void requested_at_es_el_instante_crudo() {
        PlatformAccessRequestDto dto = PlatformAccessRequestDto
                .from(PlatformAccessMother.solicitudPendiente());

        assertThat(dto.requestedAt()).isEqualTo(PlatformAccessMother.AHORA.minusHours(1));
    }

    @Test
    @DisplayName("el record tiene exactamente cuatro componentes: ningun hash puede colarse")
    void el_record_tiene_exactamente_cuatro_componentes() {
        // Si alguien anade un quinto campo, este test cae y obliga a justificarlo.
        assertThat(PlatformAccessRequestDto.class.getRecordComponents()).hasSize(4)
                .extracting(java.lang.reflect.RecordComponent::getName)
                .containsExactly("fullName", "email", "reason", "requestedAt");
    }

    @Test
    @DisplayName("la invitacion solo expone el correo: ni el nombre ni el motivo")
    void la_invitacion_solo_expone_el_correo() {
        assertThat(PlatformInvitationDto.class.getRecordComponents()).hasSize(1)
                .extracting(java.lang.reflect.RecordComponent::getName).containsExactly("email");
        assertThat(new PlatformInvitationDto(PlatformAccessMother.CORREO).email())
                .isEqualTo(PlatformAccessMother.CORREO);
    }
}
