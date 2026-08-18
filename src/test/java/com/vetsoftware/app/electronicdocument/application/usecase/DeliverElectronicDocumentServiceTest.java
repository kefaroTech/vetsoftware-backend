package com.vetsoftware.app.electronicdocument.application.usecase;

import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.facturaPendienteConId;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.facturaValidada;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.InvoiceFileStoragePort;
import com.vetsoftware.app.electronicdocument.application.port.out.InvoiceMailPort;
import com.vetsoftware.app.electronicdocument.application.port.out.InvoicePdfPort;
import com.vetsoftware.app.electronicdocument.application.port.out.QrGeneratorPort;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeliverElectronicDocumentService — genera QR + PDF, guarda en S3 y envia por correo")
class DeliverElectronicDocumentServiceTest {

    @Mock
    private ElectronicDocumentRepository repository;
    @Mock
    private QrGeneratorPort qrGenerator;
    @Mock
    private InvoicePdfPort invoicePdf;
    @Mock
    private InvoiceFileStoragePort fileStorage;
    @Mock
    private InvoiceMailPort mail;

    private DeliverElectronicDocumentService service;

    @BeforeEach
    void montar() {
        service = new DeliverElectronicDocumentService(repository, qrGenerator, invoicePdf,
                fileStorage, mail,
                "https://catalogo-vpfe.dian.gov.co/document/searchqr?documentkey=");
    }

    @Nested
    @DisplayName("guardas de idempotencia")
    class Guardas {

        @Test
        @DisplayName("un documento no VALIDADO no se entrega")
        void documento_no_validado_no_se_entrega() {
            ElectronicDocument pendiente = facturaPendienteConId(60L);

            service.deliverIfValidated(pendiente);

            verifyNoInteractions(qrGenerator, invoicePdf, fileStorage, mail, repository);
        }

        @Test
        @DisplayName("un documento VALIDADO con PDF ya adjunto no se reprocesa")
        void documento_ya_con_pdf_no_se_reprocesa() {
            ElectronicDocument validada = facturaValidada(61L);
            validada.attachRepresentation("invoices/9/61/SETP990.pdf");

            service.deliverIfValidated(validada);

            verifyNoInteractions(qrGenerator, invoicePdf, fileStorage, mail, repository);
        }
    }

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("genera el QR con el contenido oficial del proveedor y guarda el PDF en S3")
        void genera_qr_y_guarda_pdf_en_s3() {
            ElectronicDocument validada = facturaValidada(62L);
            when(qrGenerator.generatePngBase64(anyString())).thenReturn("QR_BASE64");
            when(invoicePdf.render(validada, "QR_BASE64")).thenReturn("pdf-bytes".getBytes());

            service.deliverIfValidated(validada);

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(fileStorage).store(keyCaptor.capture(), any(byte[].class),
                    org.mockito.ArgumentMatchers.eq("application/pdf"));
            assertThat(keyCaptor.getValue()).startsWith("invoices/9/62/");
            assertThat(validada.getPdfRepresentation()).isEqualTo(keyCaptor.getValue());
            verify(repository).updateDianResult(validada);
        }

        @Test
        @DisplayName("envia el correo con copia al emisor y el pdf como adjunto")
        void envia_correo_con_copia_al_emisor() {
            ElectronicDocument validada = facturaValidada(63L);
            when(qrGenerator.generatePngBase64(anyString())).thenReturn("QR_BASE64");
            when(invoicePdf.render(any(), any())).thenReturn("pdf-bytes".getBytes());

            service.deliverIfValidated(validada);

            verify(mail).send(org.mockito.ArgumentMatchers.eq(validada.getCustomer().email()),
                    org.mockito.ArgumentMatchers.eq(validada.getIssuer().email()), anyString(),
                    anyString(), anyString(), any(byte[].class));
        }

        @Test
        @DisplayName("un fallo de correo no interrumpe la entrega: el documento ya quedo validado")
        void fallo_de_correo_no_interrumpe_la_entrega() {
            ElectronicDocument validada = facturaValidada(64L);
            when(qrGenerator.generatePngBase64(anyString())).thenReturn("QR_BASE64");
            when(invoicePdf.render(any(), any())).thenReturn("pdf-bytes".getBytes());
            doThrow(new RuntimeException("SMTP caido")).when(mail).send(anyString(), anyString(),
                    anyString(), anyString(), anyString(), any(byte[].class));

            service.deliverIfValidated(validada);

            verify(repository).updateDianResult(validada);
            verify(fileStorage).store(anyString(), any(byte[].class), anyString());
        }

        @Test
        @DisplayName("si el documento ya trae el QR oficial del proveedor, no reconstruye la URL DIAN")
        void usa_el_qr_oficial_del_proveedor_si_ya_viene() {
            ElectronicDocument validada = facturaPendienteConId(65L);
            validada.markValidated("SETP", 990L, "CUFE-X", null, "uuid-x", "<xml/>",
                    "QR-OFICIAL-DEL-PROVEEDOR", "https://qr", null, LocalDateTime.now());
            when(qrGenerator.generatePngBase64(anyString())).thenReturn("QR_BASE64");
            when(invoicePdf.render(any(), any())).thenReturn("pdf-bytes".getBytes());

            service.deliverIfValidated(validada);

            verify(qrGenerator).generatePngBase64(eq("QR-OFICIAL-DEL-PROVEEDOR"));
        }

        @Test
        @DisplayName("sin sello (cufe/cude nulos) el QR cae a la URL base sin nada anexado")
        void sin_sello_el_qr_cae_a_la_url_base() {
            ElectronicDocument validada = facturaPendienteConId(67L);
            validada.markValidated(null, null, null, null, null, null, null, null, null,
                    LocalDateTime.now());
            when(qrGenerator.generatePngBase64(anyString())).thenReturn("QR_BASE64");
            when(invoicePdf.render(any(), any())).thenReturn("pdf-bytes".getBytes());

            service.deliverIfValidated(validada);

            verify(qrGenerator).generatePngBase64(
                    eq("https://catalogo-vpfe.dian.gov.co/document/searchqr?documentkey="));
        }

        @Test
        @DisplayName("sin numeracion fiscal asignada, el nombre del archivo cae al id del documento")
        void sin_numeracion_el_nombre_del_archivo_usa_el_id() {
            ElectronicDocument validada = facturaPendienteConId(66L);
            validada.markValidated(null, null, "CUFE-Y", null, "uuid-y", null, null, null, null,
                    LocalDateTime.now());
            when(qrGenerator.generatePngBase64(anyString())).thenReturn("QR_BASE64");
            when(invoicePdf.render(any(), any())).thenReturn("pdf-bytes".getBytes());

            service.deliverIfValidated(validada);

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(fileStorage).store(keyCaptor.capture(), any(byte[].class), anyString());
            assertThat(keyCaptor.getValue()).isEqualTo("invoices/9/66/66.pdf");
        }
    }
}
