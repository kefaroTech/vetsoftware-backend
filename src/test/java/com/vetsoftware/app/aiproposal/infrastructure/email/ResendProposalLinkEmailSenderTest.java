package com.vetsoftware.app.aiproposal.infrastructure.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.aiproposal.application.dto.ProposalLinkEmail;
import com.vetsoftware.app.aiproposal.application.port.out.ProposalEmailThrottlePort;
import com.vetsoftware.app.infrastructure.email.ResendEmailClient;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * &#9940; <b>Lo que este correo NO lleva es la prueba que importa.</b>
 *
 * <p>
 * La direccion de destino <b>no esta verificada</b>: el prospecto la escribio
 * en un formulario anonimo y nadie comprobo que sea suya. Es decir, cualquiera
 * puede hacer que este dominio —con SPF y DKIM en regla, o sea uno que pasa los
 * filtros— entregue un mensaje a un tercero. Si el cuerpo llevara prosa del
 * modelo, y el texto libre del atacante es lo que la produce, eso deja de ser
 * un correo transaccional y pasa a ser un rele de phishing firmado por
 * nosotros.
 *
 * <p>
 * Por eso la prueba central no afirma que el HTML "quede bonito" sino que
 * <b>ninguna palabra del cliente ni del modelo aparece dentro</b>. Es la clase
 * de invariante que se rompe sola el dia que alguien "personaliza un poco" la
 * plantilla, y para entonces nadie recuerda por que estaba asi.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResendProposalLinkEmailSender — el enlace, y nada mas que el enlace")
class ResendProposalLinkEmailSenderTest {

    private static final String TOKEN = "Zm9vYmFyYmF6cXV4MDEyMzQ1Njc4OWFiY2RlZmdoaQ";

    private static final ProposalLinkEmail ENLACE = new ProposalLinkEmail("laura@vetchapinero.co",
            TOKEN, LocalDateTime.of(2026, 9, 13, 10, 0));

    @Mock
    private ResendEmailClient email;

    @Mock
    private ProposalEmailThrottlePort throttle;

    private ResendProposalLinkEmailSender sender;

    @BeforeEach
    void montar() {
        sender = new ResendProposalLinkEmailSender(email, throttle,
                "https://app.vetrina.co/oferta");
    }

    private String cuerpoEnviado() {
        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(email).send(anyString(), any(), anyString(), html.capture(), any());
        return html.getValue();
    }

    @Nested
    @DisplayName("Lo que el cuerpo lleva")
    class LoQueLleva {

        @Test
        @DisplayName("lleva el enlace con el token en la cadena de consulta")
        void lleva_el_enlace_con_el_token() {
            when(throttle.tryAcquire(anyString())).thenReturn(true);
            when(email.isEnabled()).thenReturn(true);

            sender.send(ENLACE);

            assertThat(cuerpoEnviado()).contains("https://app.vetrina.co/oferta/?token=" + TOKEN);
        }

        @Test
        @DisplayName("lleva la fecha de caducidad, que es lo unico util ademas del enlace")
        void lleva_la_fecha_de_caducidad() {
            when(throttle.tryAcquire(anyString())).thenReturn(true);
            when(email.isEnabled()).thenReturn(true);

            sender.send(ENLACE);

            assertThat(cuerpoEnviado()).contains("13/09/2026");
        }

        @Test
        @DisplayName("va al prospecto y a nadie mas: no hay copia a ventas")
        void va_solo_al_prospecto() {
            when(throttle.tryAcquire(anyString())).thenReturn(true);
            when(email.isEnabled()).thenReturn(true);

            sender.send(ENLACE);

            ArgumentCaptor<String> destinatario = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> copia = ArgumentCaptor.forClass(String.class);
            verify(email).send(destinatario.capture(), copia.capture(), anyString(), anyString(),
                    any());
            assertThat(destinatario.getValue()).isEqualTo("laura@vetchapinero.co");
            assertThat(copia.getValue())
                    .as("un correo automatico a un buzon interno por cada peticion anonima es un"
                            + " canal que cualquiera puede llenar desde fuera")
                    .isNull();
        }
    }

    @Nested
    @DisplayName("Lo que el cuerpo NO lleva")
    class LoQueNoLleva {

        /**
         * &#9940; La lista es literal a proposito. Un {@code contains} generico sobre
         * "no hay HTML raro" no habria cazado el caso real, que es prosa perfectamente
         * bien formada escrita por quien manda el texto libre.
         */
        @Test
        @DisplayName("ni motivos, ni texto del cliente, ni nombres de modulos, ni resumen")
        void no_lleva_ni_una_palabra_del_modelo_ni_del_cliente() {
            when(throttle.tryAcquire(anyString())).thenReturn(true);
            when(email.isEnabled()).thenReturn(true);

            sender.send(ENLACE);
            String cuerpo = cuerpoEnviado();

            List<String> prohibidos = List.of("Le vendes a credito", "fundacion", "Chapinero",
                    "PACK_CLINIC", "CORE", "Historia clinica", "$");
            assertThat(prohibidos).allSatisfy(fragmento -> assertThat(cuerpo)
                    .as("el cuerpo no puede llevar texto del modelo ni del cliente: la direccion"
                            + " no esta verificada y esto seria un rele de phishing firmado por"
                            + " nosotros")
                    .doesNotContain(fragmento));
        }

        /**
         * La plantilla tiene exactamente dos marcadores. Que no quede ninguno sin
         * sustituir es lo que separa "el correo salio" de "al prospecto le llego
         * {@code {{{PROPOSAL_URL}}}} en pantalla".
         */
        @Test
        @DisplayName("no queda ningun marcador sin sustituir")
        void no_queda_ningun_marcador_sin_sustituir() {
            when(throttle.tryAcquire(anyString())).thenReturn(true);
            when(email.isEnabled()).thenReturn(true);

            sender.send(ENLACE);

            assertThat(cuerpoEnviado()).doesNotContain("{{{");
        }
    }

    @Nested
    @DisplayName("Cuando no se envia")
    class CuandoNoSeEnvia {

        /**
         * El limite por hora protege que este producto no sea un cañon de correo
         * apuntable: sin el, hostigar a una direccion es solo pedir propuestas.
         */
        @Test
        @DisplayName("sin cupo en la ventana no se manda nada")
        void sin_cupo_no_se_manda_nada() {
            when(throttle.tryAcquire(anyString())).thenReturn(false);

            sender.send(ENLACE);

            verify(email, never()).send(anyString(), any(), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("sin URL base no se manda un enlace que no lleva a ninguna parte")
        void sin_url_base_no_se_manda() {
            ResendProposalLinkEmailSender sinBase = new ResendProposalLinkEmailSender(email,
                    throttle, "");

            sinBase.send(ENLACE);

            verifyNoInteractions(throttle);
            verify(email, never()).send(anyString(), any(), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("un payload nulo no revienta: lo llama un afterCommit")
        void un_payload_nulo_no_revienta() {
            sender.send(null);

            verifyNoInteractions(throttle);
        }

        @Test
        @DisplayName("con el envio desactivado consume cupo y no llama a Resend")
        void con_el_envio_desactivado_no_llama_a_resend() {
            when(throttle.tryAcquire(anyString())).thenReturn(true);
            when(email.isEnabled()).thenReturn(false);

            sender.send(ENLACE);

            verify(email, never()).send(anyString(), any(), anyString(), anyString(), any());
        }

        /**
         * Lo invoca un {@code afterCommit} con la transaccion ya confirmada: una
         * excepcion ahi se propaga al llamante y convierte una propuesta bien guardada
         * en un 500 para el prospecto.
         */
        @Test
        @DisplayName("un fallo de Resend no se propaga")
        void un_fallo_de_resend_no_se_propaga() {
            when(throttle.tryAcquire(anyString())).thenReturn(true);
            when(email.isEnabled()).thenReturn(true);
            when(email.send(anyString(), any(), anyString(), anyString(), any()))
                    .thenThrow(new IllegalStateException("Resend no responde"));

            sender.send(ENLACE);
        }
    }

    @Nested
    @DisplayName("Invariantes del payload")
    class InvariantesDelPayload {

        @Test
        @DisplayName("el payload exige destinatario, token y caducidad")
        void el_payload_exige_sus_tres_campos() {
            org.assertj.core.api.Assertions
                    .assertThatThrownBy(
                            () -> new ProposalLinkEmail(null, TOKEN, LocalDateTime.now()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("contactEmail");
            org.assertj.core.api.Assertions
                    .assertThatThrownBy(
                            () -> new ProposalLinkEmail("laura@vet.co", " ", LocalDateTime.now()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("publicToken");
            org.assertj.core.api.Assertions
                    .assertThatThrownBy(() -> new ProposalLinkEmail("laura@vet.co", TOKEN, null))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("expiresAt");
        }
    }
}
