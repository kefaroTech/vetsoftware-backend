package com.vetsoftware.app.quote.infrastructure.orchestration;

import static com.vetsoftware.app.quote.testsupport.QuoteMother.CLIENT_REQUEST_ID;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.empresa;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.modulo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.auth.application.dto.SystemContext;
import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.quote.application.command.CreateQuoteCommand;
import com.vetsoftware.app.quote.application.command.QuoteLineCommand;
import com.vetsoftware.app.quote.application.command.SendQuoteCommand;
import com.vetsoftware.app.quote.application.dto.QuoteDto;
import com.vetsoftware.app.quote.application.port.in.CreateQuoteUseCase;
import com.vetsoftware.app.quote.application.port.in.SendQuoteUseCase;
import com.vetsoftware.app.quote.domain.QuoteStatus;
import com.vetsoftware.app.quote.testsupport.QuoteMother;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * La escalada a plataforma, acotada a dos llamadas — y lo que la hace segura.
 *
 * <p>
 * <b>Lo que este adaptador NO puede permitirse.</b> Su propio javadoc lo
 * advierte: si algun dia {@code CreateQuoteService} o {@code SendQuoteService}
 * resolvieran la empresa desde el principal en vez de recibirla en el command,
 * esta elevacion les daria <em>el de plataforma</em> y la cotizacion nacería
 * sin empresa —o peor, contra otra—. Lo unico que hoy lo impide es que la
 * empresa viaja <b>por comando</b>, y eso es lo que fija
 * {@link LaEmpresaViajaPorComando} afirmando las dos mitades a la vez: que
 * dentro de la escalada el principal es el de plataforma <em>y</em> que el
 * command sigue llevando la empresa del tenant.
 *
 * <p>
 * <b>{@link SystemAuthRunner} no se mockea.</b> No es un puerto: es la propia
 * escalada, o sea justamente lo que hay que comprobar. Un doble devolveria el
 * valor sin intercambiar el principal y dejaria sin red la unica linea que
 * importa.
 *
 * <p>
 * <b>Por que la oferta tiene que quedar {@code SENT}.</b> {@code Quote.accept}
 * exige {@code SENT} —ver {@code QuoteTest.un_borrador_no_se_acepta}—, asi que
 * un flujo que dejara la cotizacion en {@code DRAFT} moriria en el ultimo clic
 * del cliente. Y la idempotencia obliga a la mitad complementaria: una que ya
 * se emitio o que el cliente ya acepto <b>no se reenvia</b>, porque eso
 * lanzaria {@code InvalidQuoteStatusTransitionException} y convertiria un
 * reintento inofensivo en un 409 en mitad de una compra.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlatformQuoteIssuerAdapter — la escalada acotada")
class PlatformQuoteIssuerAdapterTest {

    private static final Long ID_EMITIDA = 1L;

    @Mock
    private CreateQuoteUseCase createUseCase;
    @Mock
    private SendQuoteUseCase sendUseCase;
    @Captor
    private ArgumentCaptor<SendQuoteCommand> enviado;

    /** El de verdad: es la escalada, no un colaborador que se pueda doblar. */
    private final SystemAuthRunner systemAuthRunner = new SystemAuthRunner();

    private PlatformQuoteIssuerAdapter adaptador() {
        return new PlatformQuoteIssuerAdapter(createUseCase, sendUseCase, systemAuthRunner);
    }

    @AfterEach
    void limpiarElContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    private static CreateQuoteCommand comando() {
        return new CreateQuoteCommand(CLIENT_REQUEST_ID, empresa().id(), null, null, null, null,
                QuoteMother.PRICE_LIST_ID, "MONTHLY", LocalDate.of(2026, 9, 6), 0,
                List.of(new QuoteLineCommand(modulo().id(), 1, BigDecimal.ZERO)));
    }

    private static QuoteDto oferta(QuoteStatus status) {
        return QuoteDto.from(QuoteMother.persistida(ID_EMITIDA, status));
    }

    /** El principal de un empleado del tenant, para ver que se restaura. */
    private static Authentication principalDelTenant() {
        return new UsernamePasswordAuthenticationToken("empleado@clinica.test", null,
                List.of(new SimpleGrantedAuthority("quote.request")));
    }

    @Nested
    @DisplayName("La empresa viaja por comando, no por el principal")
    class LaEmpresaViajaPorComando {

        /**
         * Las dos mitades en el mismo caso, porque por separado no prueban nada: dentro
         * de la escalada el principal <b>es</b> el de plataforma —de ahi que la
         * elevacion sea real y no decorativa— y aun asi el command sigue llevando la
         * empresa del tenant. El dia que {@code CreateQuoteService} lea el principal
         * para saber de quien es la cotizacion, esa combinacion deja de ser inofensiva.
         */
        @Test
        @DisplayName("dentro de la escalada el principal es el de plataforma, y la empresa del"
                + " tenant sigue en el command")
        void el_principal_es_el_de_plataforma_y_la_empresa_va_en_el_command() {
            SecurityContextHolder.getContext().setAuthentication(principalDelTenant());
            List<Object> principalVisto = new ArrayList<>();
            List<Object> autoridadesVistas = new ArrayList<>();
            List<Long> empresaVista = new ArrayList<>();
            when(createUseCase.execute(any())).thenAnswer(invocation -> {
                Authentication dentro = SecurityContextHolder.getContext().getAuthentication();
                principalVisto.add(dentro.getPrincipal());
                autoridadesVistas.addAll(dentro.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority).toList());
                empresaVista.add(invocation.getArgument(0, CreateQuoteCommand.class).companyId());
                return oferta(QuoteStatus.DRAFT);
            });
            when(sendUseCase.execute(any())).thenReturn(oferta(QuoteStatus.SENT));

            adaptador().issue(comando());

            assertThat(principalVisto).containsExactly(SystemContext.INSTANCE);
            assertThat(autoridadesVistas).containsExactly("ROLE_SYSTEM");
            assertThat(empresaVista).containsExactly(empresa().id());
        }

        @Test
        @DisplayName("el envio tambien lleva la empresa del tenant, no la de plataforma")
        void el_envio_tambien_lleva_la_empresa_del_tenant() {
            when(createUseCase.execute(any())).thenReturn(oferta(QuoteStatus.DRAFT));
            when(sendUseCase.execute(any())).thenReturn(oferta(QuoteStatus.SENT));

            adaptador().issue(comando());

            verify(sendUseCase).execute(enviado.capture());
            assertThat(enviado.getValue())
                    .isEqualTo(new SendQuoteCommand(ID_EMITIDA, empresa().id()));
        }

        @Test
        @DisplayName("al salir devuelve el principal que habia, no deja al hilo como plataforma")
        void al_salir_devuelve_el_principal_que_habia() {
            Authentication antes = principalDelTenant();
            SecurityContextHolder.getContext().setAuthentication(antes);
            when(createUseCase.execute(any())).thenReturn(oferta(QuoteStatus.SENT));

            adaptador().issue(comando());

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(antes);
        }

        /**
         * Si la elevacion no se deshiciera al fallar, el hilo volveria al pool de
         * Tomcat con {@code ROLE_SYSTEM} puesto y la siguiente peticion que lo
         * reutilizara heredaria los privilegios de plataforma.
         */
        @Test
        @DisplayName("tambien lo devuelve cuando la creacion revienta")
        void tambien_lo_devuelve_cuando_la_creacion_revienta() {
            Authentication antes = principalDelTenant();
            SecurityContextHolder.getContext().setAuthentication(antes);
            when(createUseCase.execute(any())).thenThrow(new IllegalStateException("boom"));

            assertThatThrownBy(() -> adaptador().issue(comando()))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("boom");

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(antes);
        }
    }

    @Nested
    @DisplayName("Que sale de la escalada")
    class LoQueQuedaEmitido {

        @Test
        @DisplayName("un borrador se emite y la oferta queda SENT, que es lo que Quote.accept exige")
        void un_borrador_se_emite_y_queda_sent() {
            when(createUseCase.execute(any())).thenReturn(oferta(QuoteStatus.DRAFT));
            when(sendUseCase.execute(any())).thenReturn(oferta(QuoteStatus.SENT));

            assertThat(adaptador().issue(comando()).status()).isEqualTo("SENT");
        }

        @Test
        @DisplayName("una oferta que ya se emitio no se reenvia: el reintento no se convierte en"
                + " un 409")
        void una_oferta_ya_emitida_no_se_reenvia() {
            QuoteDto yaEmitida = oferta(QuoteStatus.SENT);
            when(createUseCase.execute(any())).thenReturn(yaEmitida);

            assertThat(adaptador().issue(comando())).isSameAs(yaEmitida);

            verifyNoInteractions(sendUseCase);
        }

        @Test
        @DisplayName("una oferta que el cliente ya acepto tampoco se reenvia")
        void una_oferta_ya_aceptada_tampoco_se_reenvia() {
            QuoteDto yaAceptada = oferta(QuoteStatus.ACCEPTED);
            when(createUseCase.execute(any())).thenReturn(yaAceptada);

            assertThat(adaptador().issue(comando())).isSameAs(yaAceptada);

            verifyNoInteractions(sendUseCase);
        }
    }
}
