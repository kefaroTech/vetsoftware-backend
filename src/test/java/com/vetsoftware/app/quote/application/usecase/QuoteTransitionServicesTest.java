package com.vetsoftware.app.quote.application.usecase;

import static com.vetsoftware.app.quote.testsupport.QuoteMother.AHORA;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.VIGENTE_HASTA;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.empresa;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.lineaModulo;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.persistida;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.quote.application.command.AcceptQuoteCommand;
import com.vetsoftware.app.quote.application.command.RejectQuoteCommand;
import com.vetsoftware.app.quote.application.command.SendQuoteCommand;
import com.vetsoftware.app.quote.application.dto.QuoteDto;
import com.vetsoftware.app.quote.application.port.out.QuoteRepository;
import com.vetsoftware.app.quote.domain.InvalidQuoteStatusTransitionException;
import com.vetsoftware.app.quote.domain.Quote;
import com.vetsoftware.app.quote.domain.QuoteExpiredException;
import com.vetsoftware.app.quote.domain.QuoteNotFoundException;
import com.vetsoftware.app.quote.domain.QuoteStatus;
import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Los tres servicios de transicion comparten el mismo esqueleto -carga acotada
 * o ancha segun haya empresa, transicion de dominio, guardado- y por eso
 * comparten clase de prueba: lo que hay que demostrar es el mismo par de cosas
 * en cada uno.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Servicios de transicion de la cotizacion")
class QuoteTransitionServicesTest {

    private static final Clock RELOJ = Clock.fixed(AHORA.atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault());
    private static final Long ID = 55L;
    private static final Long EMPRESA = 42L;

    @Mock
    private QuoteRepository repository;

    private void devuelveGuardado() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    @DisplayName("SendQuoteService")
    class Envio {

        @Test
        @DisplayName("con empresa carga por la variante acotada, nunca por la ancha")
        void con_empresa_carga_acotado() {
            when(repository.findByIdAndCompanyId(ID, EMPRESA))
                    .thenReturn(Optional.of(persistida(ID, QuoteStatus.DRAFT)));
            devuelveGuardado();

            QuoteDto dto = new SendQuoteService(repository, RELOJ)
                    .execute(new SendQuoteCommand(ID, EMPRESA));

            assertThat(dto.status()).isEqualTo("SENT");
            verify(repository, never()).findById(any());
        }

        @Test
        @DisplayName("sin empresa -oferta a prospecto, camino SYSTEM- carga por la ancha")
        void sin_empresa_carga_ancho() {
            when(repository.findById(ID))
                    .thenReturn(Optional.of(persistida(ID, QuoteStatus.DRAFT)));
            devuelveGuardado();

            new SendQuoteService(repository, RELOJ).execute(new SendQuoteCommand(ID, null));

            verify(repository, never()).findByIdAndCompanyId(any(), any());
        }

        @Test
        @DisplayName("un id de otra empresa no existe para este tenant: 404, y no escribe")
        void un_id_ajeno_no_existe() {
            when(repository.findByIdAndCompanyId(ID, EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> new SendQuoteService(repository, RELOJ)
                    .execute(new SendQuoteCommand(ID, EMPRESA)))
                    .isInstanceOf(QuoteNotFoundException.class)
                    .hasMessageContaining("Quote not found: 55");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("una oferta ya vencida no se envia y no se guarda")
        void una_vencida_no_se_envia() {
            when(repository.findByIdAndCompanyId(ID, EMPRESA)).thenReturn(Optional.of(persistida(ID,
                    QuoteStatus.DRAFT, AHORA.toLocalDate().minusDays(1), List.of(lineaModulo()))));

            assertThatThrownBy(() -> new SendQuoteService(repository, RELOJ)
                    .execute(new SendQuoteCommand(ID, EMPRESA)))
                    .isInstanceOf(QuoteExpiredException.class);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("AcceptQuoteService")
    class Aceptacion {

        @Test
        @DisplayName("sella cuando, quien y desde donde con el reloj inyectado")
        void sella_la_prueba_de_la_aceptacion() {
            when(repository.findByIdAndCompanyId(ID, EMPRESA))
                    .thenReturn(Optional.of(persistida(ID, QuoteStatus.SENT)));
            devuelveGuardado();

            new AcceptQuoteService(repository, RELOJ)
                    .execute(new AcceptQuoteCommand(ID, EMPRESA, "ana@ejemplo.com", "190.85.1.7"));

            ArgumentCaptor<Quote> guardada = ArgumentCaptor.forClass(Quote.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getStatus()).isEqualTo(QuoteStatus.ACCEPTED);
            assertThat(guardada.getValue().getAcceptedAt()).isEqualTo(AHORA);
            assertThat(guardada.getValue().getAcceptedByEmail()).isEqualTo("ana@ejemplo.com");
            assertThat(guardada.getValue().getAcceptedIp()).isEqualTo("190.85.1.7");
        }

        @Test
        @DisplayName("aceptar un borrador que nunca se envio no es una transicion valida")
        void un_borrador_no_se_acepta() {
            when(repository.findByIdAndCompanyId(ID, EMPRESA))
                    .thenReturn(Optional.of(persistida(ID, QuoteStatus.DRAFT)));

            assertThatThrownBy(() -> new AcceptQuoteService(repository, RELOJ)
                    .execute(new AcceptQuoteCommand(ID, EMPRESA, "ana@ejemplo.com", "1.1.1.1")))
                    .isInstanceOf(InvalidQuoteStatusTransitionException.class);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("sin empresa usa la carga ancha del camino SYSTEM")
        void sin_empresa_carga_ancho() {
            when(repository.findById(ID)).thenReturn(Optional.of(persistida(ID, QuoteStatus.SENT)));
            devuelveGuardado();

            new AcceptQuoteService(repository, RELOJ)
                    .execute(new AcceptQuoteCommand(ID, null, "ana@ejemplo.com", "1.1.1.1"));

            verify(repository, never()).findByIdAndCompanyId(any(), any());
        }
    }

    @Nested
    @DisplayName("RejectQuoteService")
    class Rechazo {

        @Test
        @DisplayName("SENT pasa a REJECTED conservando la oferta entera")
        void pasa_a_rechazada() {
            when(repository.findByIdAndCompanyId(ID, EMPRESA))
                    .thenReturn(Optional.of(persistida(ID, QuoteStatus.SENT)));
            devuelveGuardado();

            QuoteDto dto = new RejectQuoteService(repository)
                    .execute(new RejectQuoteCommand(ID, EMPRESA));

            assertThat(dto.status()).isEqualTo("REJECTED");
            assertThat(dto.lines()).isNotEmpty();
            assertThat(dto.totalAmount()).isEqualByComparingTo("119000.00");
        }

        @Test
        @DisplayName("sin empresa usa la carga ancha del camino SYSTEM")
        void sin_empresa_carga_ancho() {
            when(repository.findById(ID)).thenReturn(Optional.of(persistida(ID, QuoteStatus.SENT)));
            devuelveGuardado();

            new RejectQuoteService(repository).execute(new RejectQuoteCommand(ID, null));

            verify(repository, never()).findByIdAndCompanyId(any(), any());
        }

        @Test
        @DisplayName("una cotizacion inexistente da 404 y no escribe")
        void inexistente_da_404() {
            when(repository.findByIdAndCompanyId(ID, EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> new RejectQuoteService(repository)
                    .execute(new RejectQuoteCommand(ID, EMPRESA)))
                    .isInstanceOf(QuoteNotFoundException.class);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("FindQuoteService")
    class Consulta {

        @Test
        @DisplayName("con empresa lee acotado y proyecta los totales guardados")
        void con_empresa_lee_acotado() {
            when(repository.findByIdAndCompanyId(ID, EMPRESA))
                    .thenReturn(Optional.of(persistida(ID, QuoteStatus.SENT)));

            QuoteDto dto = new FindQuoteService(repository).findById(ID, EMPRESA);

            assertThat(dto.id()).isEqualTo(ID);
            assertThat(dto.company().id()).isEqualTo(empresa().id());
            assertThat(dto.totalAmount()).isEqualByComparingTo("119000.00");
            verify(repository, never()).findById(any());
        }

        @Test
        @DisplayName("sin empresa lee ancho: es el unico camino a una oferta de prospecto")
        void sin_empresa_lee_ancho() {
            when(repository.findById(ID)).thenReturn(Optional.of(persistida(ID, QuoteStatus.SENT)));

            new FindQuoteService(repository).findById(ID, null);

            verify(repository, never()).findByIdAndCompanyId(any(), any());
        }

        @Test
        @DisplayName("un id de otro tenant es un 404, no un 403 que confirme que existe")
        void un_id_ajeno_es_404() {
            when(repository.findByIdAndCompanyId(ID, EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> new FindQuoteService(repository).findById(ID, EMPRESA))
                    .isInstanceOf(QuoteNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Vigencia usada por los servicios")
    class Vigencia {

        @Test
        @DisplayName("el ultimo dia de vigencia todavia permite aceptar")
        void el_ultimo_dia_permite_aceptar() {
            Clock enElUltimoDia = Clock.fixed(
                    VIGENTE_HASTA.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                    ZoneId.systemDefault());
            when(repository.findByIdAndCompanyId(ID, EMPRESA))
                    .thenReturn(Optional.of(persistida(ID, QuoteStatus.SENT)));
            devuelveGuardado();

            QuoteDto dto = new AcceptQuoteService(repository, enElUltimoDia)
                    .execute(new AcceptQuoteCommand(ID, EMPRESA, "ana@ejemplo.com", "1.1.1.1"));

            assertThat(dto.status()).isEqualTo("ACCEPTED");
        }
    }
}
