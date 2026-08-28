package com.vetsoftware.app.companycontactchannel.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.companycontactchannel.application.command.AuthorizeCompanyContactChannelCommand;
import com.vetsoftware.app.companycontactchannel.application.dto.CompanyContactChannelDto;
import com.vetsoftware.app.companycontactchannel.application.port.out.CompanyContactChannelRepository;
import com.vetsoftware.app.companycontactchannel.domain.CompanyContactChannel;
import com.vetsoftware.app.companycontactchannel.domain.ContactChannelType;
import com.vetsoftware.app.companycontactchannel.domain.ContactPurpose;
import com.vetsoftware.app.companycontactchannel.testsupport.CompanyContactChannelMother;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Dejar constancia de que la empresa autorizo un canal.
 *
 * <p>
 * <b>Lo que este test congela y una revision humana no ve</b> es de donde sale
 * {@code authorized_at}. Es la columna que decide, meses despues, si un aviso
 * ya enviado estaba permitido; con el reloj fijo se comprueba que sale del
 * servidor y no del cliente, que es la unica alternativa que hay que negar por
 * escrito.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthorizeCompanyContactChannelService — el permiso queda escrito")
class AuthorizeCompanyContactChannelServiceTest {

    private static final Long EMPRESA = CompanyContactChannelMother.COMPANY_ID;

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-03-05T14:30:00Z"),
            ZoneOffset.UTC);

    private static final LocalDateTime SELLO_ESPERADO = LocalDateTime.of(2026, 3, 5, 14, 30, 0);

    @Mock
    private CompanyContactChannelRepository repository;

    private AuthorizeCompanyContactChannelService service;

    @BeforeEach
    void servicio() {
        service = new AuthorizeCompanyContactChannelService(repository, RELOJ);
    }

    private static AuthorizeCompanyContactChannelCommand comando() {
        return new AuthorizeCompanyContactChannelCommand(EMPRESA, ContactChannelType.EMAIL,
                CompanyContactChannelMother.CORREO, ContactPurpose.BILLING,
                CompanyContactChannelMother.EVIDENCIA);
    }

    @Nested
    @DisplayName("Autorizacion")
    class Autorizacion {

        @Test
        @DisplayName("sella la fecha con el reloj inyectado, nunca con la del cliente")
        void sella_la_fecha_con_el_reloj_inyectado() {
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            CompanyContactChannelDto autorizado = service.execute(comando());

            ArgumentCaptor<CompanyContactChannel> guardado = ArgumentCaptor
                    .forClass(CompanyContactChannel.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getAuthorizedAt()).isEqualTo(SELLO_ESPERADO);
            assertThat(guardado.getValue().getCreatedDate()).isEqualTo(SELLO_ESPERADO);
            assertThat(autorizado.authorizedAt()).isEqualTo(SELLO_ESPERADO);
        }

        @Test
        @DisplayName("el canal nace NO primario: designar el principal es otro caso de uso")
        void el_canal_nace_no_primario() {
            // Si el alta pudiera marcarlo, un POST rutinario desviaria la facturacion de
            // la empresa a una direccion nueva y el rastro seria una peticion que parece
            // inofensiva. Ademas el permiso lo separa: crear no es actualizar.
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            CompanyContactChannelDto autorizado = service.execute(comando());

            ArgumentCaptor<CompanyContactChannel> guardado = ArgumentCaptor
                    .forClass(CompanyContactChannel.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().isPrimary()).isFalse();
            assertThat(autorizado.primary()).isFalse();
        }

        @Test
        @DisplayName("el canal nace vivo y con la evidencia que lo respalda")
        void el_canal_nace_vivo_y_con_evidencia() {
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            CompanyContactChannelDto autorizado = service.execute(comando());

            assertThat(autorizado.revokedAt()).isNull();
            assertThat(autorizado.revokedReason()).isNull();
            assertThat(autorizado.authorizationEvidence())
                    .isEqualTo(CompanyContactChannelMother.EVIDENCIA);
            assertThat(autorizado.address()).isEqualTo(CompanyContactChannelMother.CORREO);
        }

        @ParameterizedTest
        @EnumSource(ContactPurpose.class)
        @DisplayName("guarda el proposito que se autorizo, sin traducirlo ni ampliarlo")
        void guarda_el_proposito_que_se_autorizo(ContactPurpose proposito) {
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            CompanyContactChannelDto autorizado = service
                    .execute(new AuthorizeCompanyContactChannelCommand(EMPRESA,
                            ContactChannelType.SMS, CompanyContactChannelMother.MOVIL, proposito,
                            CompanyContactChannelMother.EVIDENCIA));

            assertThat(autorizado.purpose()).isEqualTo(proposito);
            assertThat(autorizado.channelType()).isEqualTo(ContactChannelType.SMS);
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("una direccion en blanco no llega al repositorio")
        void una_direccion_en_blanco_no_llega_al_repositorio() {
            assertThatThrownBy(() -> service.execute(
                    new AuthorizeCompanyContactChannelCommand(EMPRESA, ContactChannelType.EMAIL,
                            "  ", ContactPurpose.BILLING, CompanyContactChannelMother.EVIDENCIA)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("address is required");

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("una autorizacion sin evidencia no llega al repositorio")
        void una_autorizacion_sin_evidencia_no_llega_al_repositorio() {
            // Sin evidencia la fila aparenta estar respaldada y no lo esta, que es peor
            // que no tenerla: la empresa creeria poder demostrar un permiso que no puede.
            assertThatThrownBy(() -> service.execute(
                    new AuthorizeCompanyContactChannelCommand(EMPRESA, ContactChannelType.EMAIL,
                            CompanyContactChannelMother.CORREO, ContactPurpose.BILLING, "   ")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("authorizationEvidence is required");

            verifyNoInteractions(repository);
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("guarda el canal con la empresa del command, que es la del token")
        void guarda_el_canal_con_la_empresa_del_command() {
            // El controller la inyecta desde authz.currentCompanyId() y el puerto la
            // revalida. Aqui se congela el ultimo tramo: que el servicio no la sustituya
            // ni la deduzca de ninguna otra cosa.
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            service.execute(comando());

            ArgumentCaptor<CompanyContactChannel> guardado = ArgumentCaptor
                    .forClass(CompanyContactChannel.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getCompanyId()).isEqualTo(EMPRESA);
        }

        @Test
        @DisplayName("un command sin empresa no escribe nada")
        void un_command_sin_empresa_no_escribe_nada() {
            assertThatThrownBy(() -> service.execute(new AuthorizeCompanyContactChannelCommand(null,
                    ContactChannelType.EMAIL, CompanyContactChannelMother.CORREO,
                    ContactPurpose.BILLING, CompanyContactChannelMother.EVIDENCIA)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("companyId is required");

            verifyNoInteractions(repository);
        }
    }
}
