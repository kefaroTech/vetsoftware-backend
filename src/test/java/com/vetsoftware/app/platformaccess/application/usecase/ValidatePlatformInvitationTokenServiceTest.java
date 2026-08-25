package com.vetsoftware.app.platformaccess.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.platformaccess.application.dto.PlatformInvitationDto;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessAuditPort;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessInvitationRepository;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessRequestRepository;
import com.vetsoftware.app.platformaccess.domain.InvalidInvitationTokenException;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessInvitation;
import com.vetsoftware.app.platformaccess.testsupport.PlatformAccessMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * La pantalla previa a fijar la contraseña del superadministrador. Devuelve
 * <b>un solo dato</b> —el correo— y lo saca de la solicitud, nunca de la
 * petición: si el correo pudiera venir de fuera, quien poseyera una invitación
 * legítima elegiría la identidad de la cuenta que va a nacer.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ValidatePlatformInvitationTokenService — la pantalla previa al alta")
class ValidatePlatformInvitationTokenServiceTest {

    @Mock
    private PlatformAccessInvitationRepository invitationRepository;
    @Mock
    private PlatformAccessRequestRepository requestRepository;
    @Mock
    private PlatformAccessAuditPort audit;

    private ValidatePlatformInvitationTokenService crearServicio() {
        return new ValidatePlatformInvitationTokenService(invitationRepository, requestRepository,
                audit, PlatformAccessMother.RELOJ);
    }

    private void dadoQueElTokenResuelve(PlatformAccessInvitation invitacion) {
        when(invitationRepository.findByTokenHash(anyString())).thenReturn(Optional.of(invitacion));
    }

    @Nested
    @DisplayName("invitacion viva")
    class Viva {

        @Test
        @DisplayName("devuelve el correo de la SOLICITUD, no el del token ni el del cuerpo")
        void devuelve_el_correo_de_la_solicitud() {
            dadoQueElTokenResuelve(PlatformAccessMother.invitacionViva());
            when(requestRepository.findById(PlatformAccessMother.ID_SOLICITUD))
                    .thenReturn(Optional.of(PlatformAccessMother.solicitudDecidida(
                            com.vetsoftware.app.platformaccess.domain.PlatformAccessDecision.APPROVED)));

            PlatformInvitationDto dto = crearServicio().execute("token-invitacion");

            assertThat(dto.email()).isEqualTo(PlatformAccessMother.CORREO);
        }

        @Test
        @DisplayName("ata y desata el MDC con el id de la solicitud, no con el de la invitacion")
        void ata_y_desata_el_mdc_con_el_id_de_la_solicitud() {
            dadoQueElTokenResuelve(PlatformAccessMother.invitacionViva());
            when(requestRepository.findById(anyLong()))
                    .thenReturn(Optional.of(PlatformAccessMother.solicitudPendiente()));

            crearServicio().execute("token-invitacion");

            // El hilo del flujo es la solicitud: los quince eventos de la incidencia
            // se correlacionan por system.user.request.id de punta a punta.
            verify(audit).bindRequest(PlatformAccessMother.ID_SOLICITUD);
            verify(audit).unbindRequest();
        }
    }

    @Nested
    @DisplayName("invitacion muerta — un solo error para los cuatro motivos")
    class Muerta {

        @Test
        @DisplayName("un token que no existe sale como invitacion invalida")
        void un_token_inexistente_sale_como_invalido() {
            when(invitationRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> crearServicio().execute("token-invitacion"))
                    .isInstanceOf(InvalidInvitationTokenException.class)
                    .hasMessageContaining("does not exist");

            verify(requestRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("un token en blanco no consulta la base")
        void un_token_en_blanco_no_consulta_la_base() {
            assertThatThrownBy(() -> crearServicio().execute("   "))
                    .isInstanceOf(InvalidInvitationTokenException.class)
                    .hasMessageContaining("Invitation token is required");

            verify(invitationRepository, never()).findByTokenHash(anyString());
        }

        @Test
        @DisplayName("un token nulo se trata igual que uno en blanco")
        void un_token_nulo_se_trata_igual() {
            assertThatThrownBy(() -> crearServicio().execute(null))
                    .isInstanceOf(InvalidInvitationTokenException.class)
                    .hasMessageContaining("Invitation token is required");
        }

        @Test
        @DisplayName("una invitacion caducada no llega a leer la solicitud")
        void una_invitacion_caducada_no_lee_la_solicitud() {
            dadoQueElTokenResuelve(PlatformAccessMother.invitacionCaducada());

            assertThatThrownBy(() -> crearServicio().execute("token-invitacion"))
                    .isInstanceOf(InvalidInvitationTokenException.class)
                    .hasMessageContaining("no longer usable");

            verify(requestRepository, never()).findById(anyLong());
            verify(audit).unbindRequest();
        }

        @Test
        @DisplayName("una invitacion ya consumida no revela el correo de nadie")
        void una_invitacion_consumida_no_revela_el_correo() {
            dadoQueElTokenResuelve(PlatformAccessMother.invitacionConsumida());

            assertThatThrownBy(() -> crearServicio().execute("token-invitacion"))
                    .isInstanceOf(InvalidInvitationTokenException.class)
                    .hasMessageContaining("no longer usable");

            verify(requestRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("una invitacion viva cuya solicitud desaparecio sale como token muerto")
        void una_solicitud_desaparecida_sale_como_token_muerto() {
            dadoQueElTokenResuelve(PlatformAccessMother.invitacionViva());
            when(requestRepository.findById(anyLong())).thenReturn(Optional.empty());

            // No es un 500: hacia fuera es el mismo enlace muerto de siempre.
            assertThatThrownBy(() -> crearServicio().execute("token-invitacion"))
                    .isInstanceOf(InvalidInvitationTokenException.class)
                    .hasMessageContaining("no readable access request");

            verify(audit).unbindRequest();
        }
    }
}
