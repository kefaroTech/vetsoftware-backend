package com.vetsoftware.app.companycontactchannel.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.companycontactchannel.application.command.RevokeCompanyContactChannelCommand;
import com.vetsoftware.app.companycontactchannel.application.dto.CompanyContactChannelDto;
import com.vetsoftware.app.companycontactchannel.application.port.out.CompanyContactChannelRepository;
import com.vetsoftware.app.companycontactchannel.domain.CompanyContactChannel;
import com.vetsoftware.app.companycontactchannel.domain.CompanyContactChannelAlreadyRevokedException;
import com.vetsoftware.app.companycontactchannel.domain.CompanyContactChannelNotFoundException;
import com.vetsoftware.app.companycontactchannel.testsupport.CompanyContactChannelMother;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Cerrar un canal sin borrarlo.
 *
 * <p>
 * <b>Lo que este test congela</b> son las dos cosas que revocar NO hace: no
 * borra la fila y no baja el marcador de primario. Las dos son intuiciones
 * razonables al leer el codigo, y las dos destruyen informacion que despues no
 * se puede reconstruir.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RevokeCompanyContactChannelService — cerrar sin borrar")
class RevokeCompanyContactChannelServiceTest {

    private static final Long ID = 8500L;
    private static final Long EMPRESA = CompanyContactChannelMother.COMPANY_ID;
    private static final String MOTIVO = "El cliente pidio no recibir mas cobros por correo";

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-06-18T19:05:45Z"),
            ZoneOffset.UTC);

    private static final LocalDateTime CIERRE_ESPERADO = LocalDateTime.of(2026, 6, 18, 19, 5, 45);

    @Mock
    private CompanyContactChannelRepository repository;

    private RevokeCompanyContactChannelService service;

    @BeforeEach
    void servicio() {
        service = new RevokeCompanyContactChannelService(repository, RELOJ);
    }

    private static RevokeCompanyContactChannelCommand comando() {
        return new RevokeCompanyContactChannelCommand(ID, EMPRESA, MOTIVO);
    }

    @Nested
    @DisplayName("Revocacion")
    class Revocacion {

        @Test
        @DisplayName("escribe la fecha del reloj y el motivo del command")
        void escribe_la_fecha_y_el_motivo() {
            when(repository.findByIdAndCompanyId(ID, EMPRESA))
                    .thenReturn(Optional.of(CompanyContactChannelMother.vivo(ID)));
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            CompanyContactChannelDto revocado = service.execute(comando());

            ArgumentCaptor<CompanyContactChannel> guardado = ArgumentCaptor
                    .forClass(CompanyContactChannel.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getRevokedAt()).isEqualTo(CIERRE_ESPERADO);
            assertThat(guardado.getValue().getRevokedReason()).isEqualTo(MOTIVO);
            assertThat(revocado.revokedAt()).isEqualTo(CIERRE_ESPERADO);
            assertThat(revocado.revokedReason()).isEqualTo(MOTIVO);
        }

        @Test
        @DisplayName("la fila se queda: revocar NO es un borrado logico")
        void la_fila_se_queda() {
            // No hay `enabled`, no hay `delete` en el puerto y la direccion sigue ahi.
            // Si alguien convirtiera esto en una baja, la empresa perderia la prueba de
            // que el aviso de marzo iba a una direccion autorizada en marzo.
            when(repository.findByIdAndCompanyId(ID, EMPRESA))
                    .thenReturn(Optional.of(CompanyContactChannelMother.vivo(ID)));
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            CompanyContactChannelDto revocado = service.execute(comando());

            assertThat(revocado.id()).isEqualTo(ID);
            assertThat(revocado.address()).isEqualTo(CompanyContactChannelMother.CORREO);
            assertThat(revocado.authorizedAt())
                    .isEqualTo(CompanyContactChannelMother.AUTORIZADO_EL);
            assertThat(revocado.authorizationEvidence())
                    .isEqualTo(CompanyContactChannelMother.EVIDENCIA);
        }

        @Test
        @DisplayName("conserva is_primary: el hueco lo libera la columna generada, no el servicio")
        void conserva_is_primary() {
            when(repository.findByIdAndCompanyId(ID, EMPRESA))
                    .thenReturn(Optional.of(CompanyContactChannelMother.primario(ID)));
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            CompanyContactChannelDto revocado = service.execute(comando());

            assertThat(revocado.primary()).isTrue();
            assertThat(revocado.revokedAt()).isEqualTo(CIERRE_ESPERADO);
        }
    }

    @Nested
    @DisplayName("Rechazos")
    class Rechazos {

        @Test
        @DisplayName("un canal inexistente sale 404 y no escribe")
        void un_canal_inexistente_no_escribe() {
            when(repository.findByIdAndCompanyId(ID, EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando()))
                    .isInstanceOf(CompanyContactChannelNotFoundException.class)
                    .hasMessageContaining("8500");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("revocar dos veces sale 409 y no reescribe la fecha original")
        void revocar_dos_veces_no_reescribe_la_fecha() {
            // Es la mitad de arriba de una defensa de dos capas: esta comprobacion cubre
            // al que llega tarde y lo sabe la lectura, y @Version cubre el empate exacto
            // de dos revocaciones simultaneas.
            when(repository.findByIdAndCompanyId(ID, EMPRESA))
                    .thenReturn(Optional.of(CompanyContactChannelMother.revocado(ID)));

            assertThatThrownBy(() -> service.execute(comando()))
                    .isInstanceOf(CompanyContactChannelAlreadyRevokedException.class)
                    .hasMessageContaining("8500");

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("carga el canal acotado por empresa, nunca por id a secas")
        void carga_el_canal_acotado_por_empresa() {
            // El puerto de salida no ofrece variante ancha, asi que este servicio no
            // puede equivocarse. El caso queda escrito para que se ponga rojo el dia que
            // alguien anada un findById(id) creyendo que simplifica
            // (CARGA_POR_ID_ACOTADA_POR_EMPRESA).
            when(repository.findByIdAndCompanyId(ID, EMPRESA))
                    .thenReturn(Optional.of(CompanyContactChannelMother.vivo(ID)));
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            service.execute(comando());

            verify(repository).findByIdAndCompanyId(ID, EMPRESA);
        }

        @Test
        @DisplayName("el canal de otra empresa sale no encontrado, no prohibido, y no escribe")
        void el_canal_de_otra_empresa_sale_no_encontrado() {
            // Un 403 confirmaria que la fila existe, y con ids consecutivos eso es un
            // censo de por donde se le escribe a la competencia.
            when(repository.findByIdAndCompanyId(ID, CompanyContactChannelMother.OTRA_COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(new RevokeCompanyContactChannelCommand(ID,
                    CompanyContactChannelMother.OTRA_COMPANY_ID, MOTIVO)))
                    .isInstanceOf(CompanyContactChannelNotFoundException.class);

            verify(repository, never()).save(any());
        }
    }
}
