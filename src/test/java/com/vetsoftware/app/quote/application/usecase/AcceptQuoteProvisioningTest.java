package com.vetsoftware.app.quote.application.usecase;

import static com.vetsoftware.app.quote.testsupport.QuoteMother.AHORA;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.CLIENT_REQUEST_ID;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.NUMERO;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.PRICE_LIST_ID;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.VIGENTE_HASTA;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.empresa;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.lineaModulo;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.persistida;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.quote.application.command.AcceptQuoteCommand;
import com.vetsoftware.app.quote.application.port.out.QuoteRepository;
import com.vetsoftware.app.quote.application.port.out.SubscriptionProvisioningPort;
import com.vetsoftware.app.quote.domain.BillingCycle;
import com.vetsoftware.app.quote.domain.Quote;
import com.vetsoftware.app.quote.domain.QuoteStatus;
import java.time.Clock;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * DC-2: <b>aceptar una cotizacion firma el contrato, en el mismo commit.</b>
 *
 * <p>
 * Escrito contra el javadoc de {@code SubscriptionProvisioningPort} —«se invoca
 * DENTRO de la transaccion de la aceptacion» y «no puede tragarse
 * excepciones»—, no contra el cuerpo del servicio.
 *
 * <p>
 * <b>Que se pone rojo si se revierte la logica.</b> Antes de DC-2 aceptar solo
 * movia el estado; si alguien deshace el cambio, el primer test falla porque el
 * puerto no se invoca. Y si alguien envuelve la llamada en un {@code try/catch}
 * «para que un problema del contrato no tumbe la aceptacion», falla el tercero
 * — que es justamente el que impide reintroducir la ventana de «cotizacion
 * firmada sin contrato detras».
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AcceptQuoteService - la aceptacion firma el contrato")
class AcceptQuoteProvisioningTest {

    private static final Long ID = 55L;
    private static final Long EMPRESA = 42L;

    private static final Clock RELOJ = Clock.fixed(
            AHORA.atZone(java.time.ZoneId.of("America/Bogota")).toInstant(),
            java.time.ZoneId.of("America/Bogota"));

    @Mock
    private QuoteRepository repository;

    @Mock
    private SubscriptionProvisioningPort provisioning;

    private AcceptQuoteService service() {
        return new AcceptQuoteService(repository, provisioning, RELOJ);
    }

    private static AcceptQuoteCommand comando() {
        return new AcceptQuoteCommand(ID, EMPRESA, "ana@ejemplo.com", "190.85.1.7");
    }

    private void devuelveGuardado() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("provisiona el contrato de la empresa de la cotizacion, despues de sellarla")
    void provisiona_el_contrato_despues_de_sellar() {
        when(repository.findByIdAndCompanyId(ID, EMPRESA))
                .thenReturn(Optional.of(persistida(ID, QuoteStatus.SENT)));
        devuelveGuardado();

        service().execute(comando());

        // El orden importa: el contrato se firma sobre una cotizacion que YA consta
        // aceptada, porque el snapshot que lee la otra rodaja exige status ACCEPTED.
        // Provisionar antes de guardar la encontraria todavia en SENT.
        InOrder orden = inOrder(repository, provisioning);
        orden.verify(repository).save(any());
        orden.verify(provisioning).provisionFromAcceptedQuote(ID, empresa().id());
    }

    @Test
    @DisplayName("una oferta a un prospecto se acepta y NO provisiona: no hay empresa todavia")
    void una_oferta_a_prospecto_no_provisiona() {
        Quote deProspecto = Quote.create(NUMERO, null, "Veterinaria del Sur", "ana@ejemplo.com",
                "12345678", "3001112233", PRICE_LIST_ID, BillingCycle.MONTHLY, VIGENTE_HASTA, 15,
                CLIENT_REQUEST_ID, java.util.List.of(lineaModulo()), AHORA);
        deProspecto.send(AHORA.toLocalDate());
        when(repository.findById(ID)).thenReturn(Optional.of(deProspecto));
        devuelveGuardado();

        service().execute(new AcceptQuoteCommand(ID, null, "ana@ejemplo.com", "190.85.1.7"));

        // chk_quotes_party admite una oferta sin empresa. Aceptarla es la prueba de
        // que el prospecto dijo que si; el contrato llegara cuando la empresa exista.
        verify(repository).save(any());
        verifyNoInteractions(provisioning);
    }

    @Test
    @DisplayName("si el contrato no se puede firmar, la aceptacion se cae con el")
    void si_el_contrato_falla_la_aceptacion_se_cae() {
        when(repository.findByIdAndCompanyId(ID, EMPRESA))
                .thenReturn(Optional.of(persistida(ID, QuoteStatus.SENT)));
        devuelveGuardado();
        doThrowOnProvision();

        // Capturar aqui dejaria una cotizacion ACCEPTED sin contrato detras: el
        // cliente habria firmado y el sistema no le habria dado nada, y esa ventana
        // solo se cierra a mano. Propagar es lo que hace que la transaccion revierta
        // tambien la aceptacion.
        assertThatThrownBy(() -> service().execute(comando()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no hay catalogo comercial sembrado");
    }

    private void doThrowOnProvision() {
        org.mockito.Mockito.doThrow(new IllegalStateException("no hay catalogo comercial sembrado"))
                .when(provisioning).provisionFromAcceptedQuote(any(), any());
    }
}
