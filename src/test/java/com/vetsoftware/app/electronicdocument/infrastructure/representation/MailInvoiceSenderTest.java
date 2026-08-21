package com.vetsoftware.app.electronicdocument.infrastructure.representation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.application.port.out.InvoiceMailPort.DeliveryOutcome;
import com.vetsoftware.app.infrastructure.email.EmailDispatchOutcome;
import com.vetsoftware.app.infrastructure.email.ResendEmailClient;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("MailInvoiceSender — envia la representacion grafica por Resend")
class MailInvoiceSenderTest {

    @Mock
    private ResendEmailClient email;

    private MailInvoiceSender sender;

    @BeforeEach
    void montar() {
        sender = new MailInvoiceSender(email);
    }

    private void stubCliente(EmailDispatchOutcome outcome) {
        when(email.send(anyString(), any(), anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(outcome));
    }

    @Nested
    @DisplayName("con adjunto")
    class ConAdjunto {

        @Test
        @DisplayName("envuelve el PDF en un unico Attachment con su nombre")
        void envuelve_el_pdf_en_un_attachment() {
            byte[] pdf = "pdf-bytes".getBytes();
            stubCliente(EmailDispatchOutcome.ACCEPTED);

            sender.send("cliente@correo.co", "emisor@correo.co", "Factura 1", "<p>Hola</p>",
                    "SETP990.pdf", pdf);

            ArgumentCaptor<List<ResendEmailClient.Attachment>> captor = ArgumentCaptor
                    .forClass(List.class);
            verify(email).send(eq("cliente@correo.co"), eq("emisor@correo.co"), eq("Factura 1"),
                    eq("<p>Hola</p>"), captor.capture());
            assertThat(captor.getValue()).hasSize(1);
            assertThat(captor.getValue().get(0).filename()).isEqualTo("SETP990.pdf");
            assertThat(captor.getValue().get(0).content()).isEqualTo(pdf);
        }
    }

    @Nested
    @DisplayName("sin adjunto")
    class SinAdjunto {

        @Test
        @DisplayName("un adjunto null se traduce en una lista vacia, no en un attachment con null")
        void adjunto_null_se_traduce_en_lista_vacia() {
            stubCliente(EmailDispatchOutcome.ACCEPTED);

            sender.send("cliente@correo.co", "emisor@correo.co", "Factura 2", "<p>Hola</p>",
                    "SETP991.pdf", null);

            verify(email).send("cliente@correo.co", "emisor@correo.co", "Factura 2", "<p>Hola</p>",
                    List.of());
        }
    }

    /**
     * Issue #242. Esta traduccion es la costura por la que el desenlace real del
     * envio llega al contador de negocio. Si se aplanara a un booleano, o si
     * {@code SKIPPED} se mapeara a {@code FAILED}, el contador volveria a mentir —
     * en la direccion contraria a antes, pero mentir igual.
     */
    @Nested
    @DisplayName("traduccion del desenlace del cliente de correo")
    class Desenlace {

        @Test
        @DisplayName("aceptado por Resend se traduce en ACCEPTED")
        void aceptado_se_traduce_en_accepted() {
            stubCliente(EmailDispatchOutcome.ACCEPTED);

            assertThat(sender.send("cliente@correo.co", "emisor@correo.co", "Factura 3",
                    "<p>Hola</p>", "SETP992.pdf", null))
                    .isCompletedWithValue(DeliveryOutcome.ACCEPTED);
        }

        @Test
        @DisplayName("un fallo del proveedor se traduce en FAILED y NO en una excepcion")
        void fallo_se_traduce_en_failed() {
            stubCliente(EmailDispatchOutcome.FAILED);

            assertThat(sender.send("cliente@correo.co", "emisor@correo.co", "Factura 4",
                    "<p>Hola</p>", "SETP993.pdf", null))
                    .isCompletedWithValue(DeliveryOutcome.FAILED).isNotCompletedExceptionally();
        }

        @Test
        @DisplayName("el correo deshabilitado se traduce en SKIPPED, que no es un fallo")
        void deshabilitado_se_traduce_en_skipped() {
            stubCliente(EmailDispatchOutcome.SKIPPED);

            assertThat(sender.send("cliente@correo.co", "emisor@correo.co", "Factura 5",
                    "<p>Hola</p>", "SETP994.pdf", null))
                    .isCompletedWithValue(DeliveryOutcome.SKIPPED);
        }
    }
}
