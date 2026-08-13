package com.vetsoftware.app.electronicdocument.application.usecase;

import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.COMPANY_ID;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.facturaPendienteConId;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.facturaValidada;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.application.command.ProcessProviderWebhookCommand;
import com.vetsoftware.app.electronicdocument.application.port.out.BillingEntitlementQueryPort;
import com.vetsoftware.app.electronicdocument.application.port.out.BillingMetrics;
import com.vetsoftware.app.electronicdocument.application.port.out.BillingMetrics.Origin;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.ParsedWebhook;
import com.vetsoftware.app.electronicdocument.application.port.out.ProviderConfigQueryPort;
import com.vetsoftware.app.electronicdocument.application.port.out.ProviderConfigSnapshot;
import com.vetsoftware.app.electronicdocument.application.port.out.ProviderWebhookParser;
import com.vetsoftware.app.electronicdocument.application.port.out.TransmissionLogPort;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.WebhookOutcome;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

/**
 * El webhook es una entrada publica: llega desde internet y su unica credencial
 * es la firma HMAC. Lo que se prueba aqui es que nada mueva el estado fiscal de
 * un documento antes de verificarla, y que reprocesar el mismo evento no cambie
 * nada, porque los proveedores reintentan.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessProviderWebhookService — el webhook asincrono del proveedor")
class ProcessProviderWebhookServiceTest {

    private static final Long DOCUMENT_ID = 55L;
    private static final String CUERPO = "{\"evento\":\"validado\"}";
    private static final String FIRMA = "sha256=abc";
    private static final String CLAVE = "KEY-1";
    private static final ProviderConfigSnapshot CONFIG = new ProviderConfigSnapshot("MATIAS",
            "https://matias.test", "id", "secret", "user", "pass", "token", "hook-secret", "ref");

    @Mock
    private ElectronicDocumentRepository repository;
    @Mock
    private ProviderConfigQueryPort configQueryPort;
    @Mock
    private TransmissionLogPort transmissionLog;
    @Mock
    private BillingEntitlementQueryPort billingEntitlement;
    @Mock
    private DocumentTransmitter documentTransmitter;
    @Mock
    private WebhookRejectionApplier rejectionApplier;
    @Mock
    private BillingMetrics billingMetrics;
    @Mock
    private ProviderWebhookParser matiasParser;

    private ProcessProviderWebhookService service;

    @BeforeEach
    void montarConElParserDeMatias() {
        when(matiasParser.providerName()).thenReturn("MATIAS");
        service = new ProcessProviderWebhookService(repository, configQueryPort, transmissionLog,
                billingEntitlement, documentTransmitter, rejectionApplier, billingMetrics,
                List.of(matiasParser));
    }

    private static ProcessProviderWebhookCommand webhook(String proveedor) {
        return new ProcessProviderWebhookCommand(proveedor, CUERPO, FIRMA);
    }

    private static ParsedWebhook parsed(WebhookOutcome outcome, String clave) {
        return new ParsedWebhook(outcome, clave, "SETP", 991L, "CUFE-1", null, "uuid-1", null, null,
                null, null, outcome == WebhookOutcome.REJECTED ? "Cliente sin RUT" : null);
    }

    /** Webhook bien firmado que apunta a un documento PENDIENTE de la empresa. */
    private ElectronicDocument llegaParaUnDocumentoPendiente(WebhookOutcome outcome) {
        ElectronicDocument documento = facturaPendienteConId(DOCUMENT_ID);
        when(matiasParser.parse(CUERPO)).thenReturn(parsed(outcome, CLAVE));
        when(transmissionLog.findDocumentIdByProviderKey(CLAVE))
                .thenReturn(Optional.of(DOCUMENT_ID));
        when(repository.findById(DOCUMENT_ID)).thenReturn(Optional.of(documento));
        when(billingEntitlement.isElectronicInvoicingEnabled(COMPANY_ID)).thenReturn(true);
        when(configQueryPort.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(CONFIG));
        when(matiasParser.verifySignature(CUERPO, FIRMA, "hook-secret")).thenReturn(true);
        return documento;
    }

    @Nested
    @DisplayName("enrutado y descarte temprano")
    class Enrutado {

        @Test
        @DisplayName("un proveedor desconocido se rechaza con su nombre")
        void un_proveedor_desconocido_se_rechaza_con_su_nombre() {
            assertThatThrownBy(() -> service.execute(webhook("OTRO")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Proveedor de webhook desconocido: OTRO");
        }

        @Test
        @DisplayName("el proveedor se reconoce sin importar mayusculas")
        void el_proveedor_se_reconoce_sin_importar_mayusculas() {
            when(matiasParser.parse(CUERPO)).thenReturn(parsed(WebhookOutcome.IGNORED, CLAVE));

            assertThatCode(() -> service.execute(webhook("matias"))).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("un evento que el parser marca como irrelevante no toca nada")
        void un_evento_irrelevante_no_toca_nada() {
            when(matiasParser.parse(CUERPO)).thenReturn(parsed(WebhookOutcome.IGNORED, CLAVE));

            service.execute(webhook("MATIAS"));

            verifyNoInteractions(transmissionLog, repository, documentTransmitter,
                    rejectionApplier);
        }

        @Test
        @DisplayName("un webhook sin clave de proveedor no se puede enrutar")
        void un_webhook_sin_clave_no_se_puede_enrutar() {
            when(matiasParser.parse(CUERPO)).thenReturn(parsed(WebhookOutcome.ACCEPTED, null));

            service.execute(webhook("MATIAS"));

            verifyNoInteractions(transmissionLog, repository);
        }

        @Test
        @DisplayName("una clave desconocida se ignora en vez de fallar")
        void una_clave_desconocida_se_ignora() {
            when(matiasParser.parse(CUERPO)).thenReturn(parsed(WebhookOutcome.ACCEPTED, CLAVE));
            when(transmissionLog.findDocumentIdByProviderKey(CLAVE)).thenReturn(Optional.empty());

            // El proveedor reintenta: devolver error lo haria reintentar para siempre por
            // un evento que nunca vamos a poder aplicar.
            service.execute(webhook("MATIAS"));

            verifyNoInteractions(repository, documentTransmitter, rejectionApplier);
        }

        @Test
        @DisplayName("un documento que ya no existe se ignora")
        void un_documento_que_ya_no_existe_se_ignora() {
            when(matiasParser.parse(CUERPO)).thenReturn(parsed(WebhookOutcome.ACCEPTED, CLAVE));
            when(transmissionLog.findDocumentIdByProviderKey(CLAVE))
                    .thenReturn(Optional.of(DOCUMENT_ID));
            when(repository.findById(DOCUMENT_ID)).thenReturn(Optional.empty());

            service.execute(webhook("MATIAS"));

            verifyNoInteractions(billingEntitlement, documentTransmitter, rejectionApplier);
        }
    }

    @Nested
    @DisplayName("nada se mueve antes de verificar la firma")
    class Firma {

        @Test
        @DisplayName("una firma invalida se rechaza y el documento no se toca")
        void una_firma_invalida_se_rechaza() {
            ElectronicDocument documento = facturaPendienteConId(DOCUMENT_ID);
            when(matiasParser.parse(CUERPO)).thenReturn(parsed(WebhookOutcome.ACCEPTED, CLAVE));
            when(transmissionLog.findDocumentIdByProviderKey(CLAVE))
                    .thenReturn(Optional.of(DOCUMENT_ID));
            when(repository.findById(DOCUMENT_ID)).thenReturn(Optional.of(documento));
            when(billingEntitlement.isElectronicInvoicingEnabled(COMPANY_ID)).thenReturn(true);
            when(configQueryPort.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(CONFIG));
            when(matiasParser.verifySignature(CUERPO, FIRMA, "hook-secret")).thenReturn(false);

            // Es la unica credencial de una entrada publica: sin ella, cualquiera podria
            // marcar facturas como validadas o rechazadas desde internet.
            assertThatThrownBy(() -> service.execute(webhook("MATIAS")))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Firma de webhook inválida");

            verifyNoInteractions(documentTransmitter, rejectionApplier);
        }

        @Test
        @DisplayName("la firma se verifica con el secret de la empresa del documento")
        void la_firma_se_verifica_con_el_secret_de_la_empresa() {
            llegaParaUnDocumentoPendiente(WebhookOutcome.ACCEPTED);

            service.execute(webhook("MATIAS"));

            // Cada empresa tiene su secret: verificar con otro dejaria pasar webhooks de
            // un tenant contra los documentos de otro.
            verify(matiasParser).verifySignature(CUERPO, FIRMA, "hook-secret");
        }

        @Test
        @DisplayName("sin proveedor configurado falla antes de aplicar nada")
        void sin_proveedor_configurado_falla() {
            ElectronicDocument documento = facturaPendienteConId(DOCUMENT_ID);
            when(matiasParser.parse(CUERPO)).thenReturn(parsed(WebhookOutcome.ACCEPTED, CLAVE));
            when(transmissionLog.findDocumentIdByProviderKey(CLAVE))
                    .thenReturn(Optional.of(DOCUMENT_ID));
            when(repository.findById(DOCUMENT_ID)).thenReturn(Optional.of(documento));
            when(billingEntitlement.isElectronicInvoicingEnabled(COMPANY_ID)).thenReturn(true);
            when(configQueryPort.findByCompanyId(COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(webhook("MATIAS")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no tiene proveedor DIAN configurado");
        }

        @Test
        @DisplayName("sin el submodulo de facturacion el webhook se ignora, ni se verifica")
        void sin_submodulo_el_webhook_se_ignora() {
            ElectronicDocument documento = facturaPendienteConId(DOCUMENT_ID);
            when(matiasParser.parse(CUERPO)).thenReturn(parsed(WebhookOutcome.ACCEPTED, CLAVE));
            when(transmissionLog.findDocumentIdByProviderKey(CLAVE))
                    .thenReturn(Optional.of(DOCUMENT_ID));
            when(repository.findById(DOCUMENT_ID)).thenReturn(Optional.of(documento));
            when(billingEntitlement.isElectronicInvoicingEnabled(COMPANY_ID)).thenReturn(false);

            // Una empresa sin BILLING no transmite, asi que no deberia recibir webhooks:
            // si llega uno, es ruido o un intento de manipulacion.
            service.execute(webhook("MATIAS"));

            verify(matiasParser, never()).verifySignature(any(), any(), any());
            verifyNoInteractions(configQueryPort, documentTransmitter, rejectionApplier);
        }
    }

    @Nested
    @DisplayName("idempotencia: el proveedor reintenta")
    class Idempotencia {

        @Test
        @DisplayName("un documento ya validado no se reprocesa")
        void un_documento_ya_validado_no_se_reprocesa() {
            ElectronicDocument validada = facturaValidada(DOCUMENT_ID);
            when(matiasParser.parse(CUERPO)).thenReturn(parsed(WebhookOutcome.ACCEPTED, CLAVE));
            when(transmissionLog.findDocumentIdByProviderKey(CLAVE))
                    .thenReturn(Optional.of(DOCUMENT_ID));
            when(repository.findById(DOCUMENT_ID)).thenReturn(Optional.of(validada));
            when(billingEntitlement.isElectronicInvoicingEnabled(COMPANY_ID)).thenReturn(true);
            when(configQueryPort.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(CONFIG));
            when(matiasParser.verifySignature(CUERPO, FIRMA, "hook-secret")).thenReturn(true);

            service.execute(webhook("MATIAS"));

            // Reprocesar volveria a reversar la cartera y a reenviar la representacion al
            // cliente por cada reintento del proveedor.
            verifyNoInteractions(documentTransmitter, rejectionApplier);
        }

        // No hay test del guard `outcome != REJECTED` que cierra el metodo: con los
        // tres valores del enum es inalcanzable, porque IGNORED ya se corto arriba y
        // ACCEPTED tiene su propia rama. Es un cinturon para un valor futuro, y
        // escribir un test que finja llegar ahi seria mentir sobre lo que se cubre.
    }

    @Nested
    @DisplayName("aceptado: nunca se valida sin sello fiscal")
    class Aceptado {

        @Test
        @DisplayName("no marca validado con lo que trae el webhook: le pide el sello al proveedor")
        void le_pide_el_sello_al_proveedor() {
            ElectronicDocument documento = llegaParaUnDocumentoPendiente(WebhookOutcome.ACCEPTED);

            service.execute(webhook("MATIAS"));

            // El webhook no trae CUFE ni XML firmado. Marcar VALIDADO con el sello vacio
            // dejaria una factura sin respaldo ante la DIAN; por eso se reconcilia, que
            // consulta el estado autoritativo y solo entonces valida.
            verify(documentTransmitter).reconcile(documento, Origin.WEBHOOK);
            verifyNoInteractions(rejectionApplier);
        }

        @Test
        @DisplayName("la rama de aceptado no mide aparte: la metrica la deja la reconciliacion")
        void la_rama_de_aceptado_no_mide_aparte() {
            llegaParaUnDocumentoPendiente(WebhookOutcome.ACCEPTED);

            service.execute(webhook("MATIAS"));

            // Medir aqui y dentro de reconcile contaria dos veces el mismo webhook.
            verifyNoInteractions(billingMetrics);
        }
    }

    @Nested
    @DisplayName("rechazado")
    class Rechazado {

        @Test
        @DisplayName("aplica el rechazo con el motivo y la clave que trajo el webhook")
        void aplica_el_rechazo_con_su_motivo() {
            ElectronicDocument documento = llegaParaUnDocumentoPendiente(WebhookOutcome.REJECTED);

            service.execute(webhook("MATIAS"));

            // El motivo es lo que ve el cajero para corregir y reemitir.
            verify(rejectionApplier).apply(documento, "MATIAS", CLAVE, "Cliente sin RUT");
            verifyNoInteractions(documentTransmitter);
        }

        @Test
        @DisplayName("mide el rechazo con el origen del webhook")
        void mide_el_rechazo_con_su_origen() {
            ElectronicDocument documento = llegaParaUnDocumentoPendiente(WebhookOutcome.REJECTED);

            service.execute(webhook("MATIAS"));

            verify(billingMetrics).finished(eq(DianStatus.RECHAZADO), eq(Origin.WEBHOOK),
                    eq(documento.getDocumentType()), any(Duration.class));
        }
    }
}
