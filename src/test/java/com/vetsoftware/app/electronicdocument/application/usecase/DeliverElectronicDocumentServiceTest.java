package com.vetsoftware.app.electronicdocument.application.usecase;

import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.facturaPendienteConId;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.facturaValidada;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.InvoiceFileStoragePort;
import com.vetsoftware.app.electronicdocument.application.port.out.DocumentDeliveryMetrics;
import com.vetsoftware.app.electronicdocument.application.port.out.InvoiceMailPort;
import com.vetsoftware.app.electronicdocument.application.port.out.InvoiceMailPort.DeliveryOutcome;
import com.vetsoftware.app.electronicdocument.application.port.out.InvoicePdfPort;
import com.vetsoftware.app.electronicdocument.application.port.out.QrGeneratorPort;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
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
    @Mock
    private DocumentDeliveryMetrics deliveryMetrics;

    private DeliverElectronicDocumentService service;

    @BeforeEach
    void montar() {
        service = new DeliverElectronicDocumentService(repository, qrGenerator, invoicePdf,
                fileStorage, mail, deliveryMetrics,
                "https://catalogo-vpfe.dian.gov.co/document/searchqr?documentkey=");
    }

    /**
     * El puerto de correo devuelve un futuro, nunca lanza y nunca devuelve
     * {@code null}: sin este stub el mock devolveria {@code null} y el caso de uso
     * fallaria por un motivo que no es el que el test mide.
     */
    private void stubEnvio(DeliveryOutcome outcome) {
        when(mail.send(anyString(), anyString(), anyString(), anyString(), anyString(),
                any(byte[].class))).thenReturn(CompletableFuture.completedFuture(outcome));
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
            stubEnvio(DeliveryOutcome.ACCEPTED);

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
            stubEnvio(DeliveryOutcome.ACCEPTED);

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
            stubEnvio(DeliveryOutcome.FAILED);

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
            stubEnvio(DeliveryOutcome.ACCEPTED);

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
            stubEnvio(DeliveryOutcome.ACCEPTED);

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
            stubEnvio(DeliveryOutcome.ACCEPTED);

            service.deliverIfValidated(validada);

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(fileStorage).store(keyCaptor.capture(), any(byte[].class), anyString());
            assertThat(keyCaptor.getValue()).isEqualTo("invoices/9/66/66.pdf");
        }
    }

    /**
     * Issue #203. Entre el {@code store} en S3 y el {@code updateDianResult} hay
     * una ventana en la que el PDF existe y ninguna fila lo apunta: si la escritura
     * falla, el objeto queda huerfano en el bucket, invisible para la retencion, el
     * borrado y la reexpedicion, que parten todos de la fila.
     *
     * <p>
     * El {@code catch} que compensa no altera el camino feliz, asi que sin estos
     * casos se puede borrar entero y la suite sigue verde. Eso es exactamente lo
     * que estos tres impiden.
     */
    @Nested
    @DisplayName("compensacion del PDF ya subido a S3")
    class Compensacion {

        private static final String CLAVE = "invoices/9/70/SETP990.pdf";

        private ElectronicDocument stubHastaLaSubida(Long id) {
            ElectronicDocument validada = facturaValidada(id);
            when(qrGenerator.generatePngBase64(anyString())).thenReturn("QR_BASE64");
            when(invoicePdf.render(any(), any())).thenReturn("pdf-bytes".getBytes());
            return validada;
        }

        @Test
        @DisplayName("un fallo al grabar la referencia borra del bucket el PDF recien subido")
        void un_fallo_al_grabar_la_referencia_borra_el_pdf() {
            ElectronicDocument validada = stubHastaLaSubida(70L);
            when(repository.updateDianResult(validada)).thenThrow(
                    new IllegalStateException("OptimisticLock contra el job de contingencia"));

            assertThatThrownBy(() -> service.deliverIfValidated(validada))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("OptimisticLock");

            ArgumentCaptor<String> subida = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> borrada = ArgumentCaptor.forClass(String.class);
            verify(fileStorage).store(subida.capture(), any(byte[].class), anyString());
            verify(fileStorage).delete(borrada.capture());
            // Lo que importa no es que se borre algo, sino que se borre EXACTAMENTE el
            // objeto que se acaba de subir: con otra clave el huerfano sigue ahi y ademas
            // se destruye el PDF de otra factura.
            assertThat(borrada.getValue()).isEqualTo(subida.getValue()).isEqualTo(CLAVE);
            // El documento no llego a entregarse: ni correo ni contador de entregado.
            verifyNoInteractions(mail, deliveryMetrics);
        }

        @Test
        @DisplayName("se propaga la excepcion original, no la del borrado, con la secundaria como suppressed")
        void se_propaga_la_original_con_la_del_borrado_como_suppressed() {
            ElectronicDocument validada = stubHastaLaSubida(70L);
            RuntimeException original = new IllegalStateException(
                    "no se pudo grabar la referencia del PDF");
            when(repository.updateDianResult(validada)).thenThrow(original);
            doThrow(new IllegalStateException("S3 rechazo el DELETE")).when(fileStorage)
                    .delete(anyString());

            // La original es la que explica por que fallo la entrega; taparla con el
            // fallo del borrado dejaria al operador diagnosticando el sintoma equivocado.
            assertThatThrownBy(() -> service.deliverIfValidated(validada)).isSameAs(original)
                    .hasMessageContaining("no se pudo grabar la referencia");

            assertThat(original.getSuppressed()).hasSize(1);
            assertThat(original.getSuppressed()[0]).hasMessageContaining("S3 rechazo el DELETE");
        }

        @Test
        @DisplayName("con la referencia ya grabada no se borra nada, ni siquiera si el correo falla")
        void con_la_referencia_grabada_no_se_borra_nada() {
            ElectronicDocument validada = stubHastaLaSubida(70L);
            stubEnvio(DeliveryOutcome.FAILED);

            service.deliverIfValidated(validada);

            // El correo es posterior a la referencia: el PDF ya esta inventariado y
            // borrarlo dejaria la fila apuntando a un objeto inexistente.
            verify(fileStorage, never()).delete(anyString());
            verify(deliveryMetrics).deliveryFailed();
        }
    }

    /**
     * Issue #242. El contador de entregas fallidas era una serie plana en cero
     * <b>por construccion</b>: su {@code catch} estaba detras de un adaptador
     * {@code @Async} que por contrato no lanza, asi que no veia ninguno de los
     * fallos reales del proveedor. La prueba que lo cubria mockeaba el puerto con
     * {@code doThrow} y pasaba en verde sobre codigo muerto.
     *
     * <p>
     * Estos casos son los que impiden que eso vuelva: todos entran por el
     * <b>desenlace</b> del futuro, que es la unica via por la que llega un fallo
     * real. Ninguno usa {@code doThrow}, porque el adaptador de produccion no lanza
     * — salvo el ultimo, que cubre el unico caso en el que si lo hace.
     */
    @Nested
    @DisplayName("el contador refleja el desenlace real del envio, no el encolado")
    class ContadorDeEntrega {

        private ElectronicDocument stubHastaElCorreo(Long id) {
            ElectronicDocument validada = facturaValidada(id);
            when(qrGenerator.generatePngBase64(anyString())).thenReturn("QR_BASE64");
            when(invoicePdf.render(any(), any())).thenReturn("pdf-bytes".getBytes());
            return validada;
        }

        @Test
        @DisplayName("un rechazo del proveedor incrementa el contador de fallidas")
        void un_rechazo_del_proveedor_cuenta_como_fallo() {
            ElectronicDocument validada = stubHastaElCorreo(80L);
            stubEnvio(DeliveryOutcome.FAILED);

            service.deliverIfValidated(validada);

            // Antes del arreglo esto era inalcanzable: el adaptador devolvia sin lanzar
            // y el caso de uso contaba una entrega exitosa que nunca ocurrio.
            verify(deliveryMetrics).deliveryFailed();
            verify(deliveryMetrics, never()).delivered();
        }

        @Test
        @DisplayName("solo un envio aceptado por el proveedor cuenta como entregado")
        void solo_un_envio_aceptado_cuenta_como_entregado() {
            ElectronicDocument validada = stubHastaElCorreo(81L);
            stubEnvio(DeliveryOutcome.ACCEPTED);

            service.deliverIfValidated(validada);

            verify(deliveryMetrics).delivered();
            verify(deliveryMetrics, never()).deliveryFailed();
        }

        /**
         * Sin este caso, la forma barata de "arreglar" el issue seria contar como fallo
         * todo lo que no se envie — y dev, que corre con el correo apagado, publicaria
         * una tasa de error del 100 % permanente.
         */
        @Test
        @DisplayName("con el correo deshabilitado no se cuenta ni entrega ni fallo")
        void con_el_correo_deshabilitado_no_se_cuenta_nada() {
            ElectronicDocument validada = stubHastaElCorreo(82L);
            stubEnvio(DeliveryOutcome.SKIPPED);

            service.deliverIfValidated(validada);

            verifyNoInteractions(deliveryMetrics);
        }

        /**
         * El contrato del puerto dice que el futuro nunca se completa excepcionalmente.
         * Si algun dia se rompiera, el fallo no puede evaporarse en un futuro que nadie
         * observa.
         */
        @Test
        @DisplayName("un futuro completado excepcionalmente tambien cuenta como fallo")
        void un_futuro_fallido_cuenta_como_fallo() {
            ElectronicDocument validada = stubHastaElCorreo(83L);
            when(mail.send(anyString(), anyString(), anyString(), anyString(), anyString(),
                    any(byte[].class)))
                    .thenReturn(CompletableFuture
                            .failedFuture(new IllegalStateException("el pool murio a mitad")));

            service.deliverIfValidated(validada);

            verify(deliveryMetrics).deliveryFailed();
        }

        /**
         * El unico fallo que el {@code catch} sincrono si veia y sigue viendo: que la
         * tarea no se pueda ni encolar. Se conserva a proposito — es la razon por la
         * que ese {@code catch} no se borro entero.
         */
        @Test
        @DisplayName("si el envio no se puede ni encolar, se cuenta como fallo y la entrega no se rompe")
        void si_el_envio_no_se_puede_encolar_cuenta_como_fallo() {
            ElectronicDocument validada = stubHastaElCorreo(84L);
            doThrow(new RejectedExecutionException("emailTaskExecutor saturado")).when(mail).send(
                    anyString(), anyString(), anyString(), anyString(), anyString(),
                    any(byte[].class));

            service.deliverIfValidated(validada);

            verify(deliveryMetrics).deliveryFailed();
            // El PDF ya quedo inventariado: el fallo del correo no puede deshacerlo.
            verify(repository).updateDianResult(validada);
        }
    }
}
