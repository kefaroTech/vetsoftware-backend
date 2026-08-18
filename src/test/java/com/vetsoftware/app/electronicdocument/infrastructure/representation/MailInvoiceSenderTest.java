package com.vetsoftware.app.electronicdocument.infrastructure.representation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.vetsoftware.app.infrastructure.email.ResendEmailClient;
import java.util.List;
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

    @Nested
    @DisplayName("con adjunto")
    class ConAdjunto {

        @Test
        @DisplayName("envuelve el PDF en un unico Attachment con su nombre")
        void envuelve_el_pdf_en_un_attachment() {
            byte[] pdf = "pdf-bytes".getBytes();

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
            sender.send("cliente@correo.co", "emisor@correo.co", "Factura 2", "<p>Hola</p>",
                    "SETP991.pdf", null);

            verify(email).send("cliente@correo.co", "emisor@correo.co", "Factura 2", "<p>Hola</p>",
                    List.of());
        }
    }
}
