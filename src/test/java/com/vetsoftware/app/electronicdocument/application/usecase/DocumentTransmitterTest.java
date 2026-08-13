package com.vetsoftware.app.electronicdocument.application.usecase;

import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.COMPANY_ID;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.facturaPendienteConId;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.facturaValidada;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.application.port.out.BillingEntitlementQueryPort;
import com.vetsoftware.app.electronicdocument.application.port.out.BillingMetrics;
import com.vetsoftware.app.electronicdocument.application.port.out.BillingMetrics.Origin;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicInvoiceProviderPort;
import com.vetsoftware.app.electronicdocument.application.port.out.ProviderConfigQueryPort;
import com.vetsoftware.app.electronicdocument.application.port.out.ProviderConfigSnapshot;
import com.vetsoftware.app.electronicdocument.application.port.out.ProviderResult;
import com.vetsoftware.app.electronicdocument.application.port.out.TransmissionLogPort;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
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

/**
 * Nucleo de transmision a la DIAN. Lo que se prueba son las guardas que evitan
 * hablar con el proveedor cuando no toca, la eleccion del adaptador por nombre
 * y el reparto de responsabilidades: el HTTP fuera de transaccion y el
 * desenlace en manos del persistidor.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentTransmitter — transmision y reconciliacion con la DIAN")
class DocumentTransmitterTest {

    private static final Long DOCUMENT_ID = 55L;
    private static final ProviderConfigSnapshot CONFIG = new ProviderConfigSnapshot("MATIAS",
            "https://matias.test", "id", "secret", "user", "pass", "token", "hook", "ref");

    @Mock
    private ProviderConfigQueryPort configQueryPort;
    @Mock
    private TransmissionLogPort transmissionLog;
    @Mock
    private DeliverElectronicDocumentService deliverService;
    @Mock
    private BillingEntitlementQueryPort billingEntitlement;
    @Mock
    private TransmissionResultPersister resultPersister;
    @Mock
    private BillingMetrics billingMetrics;
    @Mock
    private ElectronicInvoiceProviderPort matias;

    private DocumentTransmitter transmitter;

    @BeforeEach
    void montarConElProveedorMatias() {
        when(matias.providerName()).thenReturn("MATIAS");
        transmitter = new DocumentTransmitter(configQueryPort, transmissionLog, deliverService,
                billingEntitlement, resultPersister, billingMetrics, List.of(matias));
    }

    private static ProviderResult resultado(DianStatus estado) {
        return new ProviderResult(estado, "SETP", 991L, "CUFE-1", null, "uuid-1", "<xml/>", "qr",
                "https://qr", null, "KEY-1", null, null, 200, "{}");
    }

    private void conFacturacionElectronicaActiva() {
        when(billingEntitlement.isElectronicInvoicingEnabled(COMPANY_ID)).thenReturn(true);
        when(configQueryPort.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(CONFIG));
    }

    @Nested
    @DisplayName("transmit — camino feliz")
    class Transmision {

        @Test
        @DisplayName("elige el adaptador por el nombre de proveedor de la configuracion")
        void elige_el_adaptador_por_el_nombre_del_proveedor() {
            ElectronicDocument documento = facturaPendienteConId(DOCUMENT_ID);
            ElectronicDocument validada = facturaValidada(DOCUMENT_ID);
            conFacturacionElectronicaActiva();
            when(matias.transmit(documento, CONFIG)).thenReturn(resultado(DianStatus.VALIDADO));
            when(resultPersister.persist(documento, resultado(DianStatus.VALIDADO), "MATIAS"))
                    .thenReturn(validada);

            assertThat(transmitter.transmit(documento)).isSameAs(validada);
        }

        @Test
        @DisplayName("el desenlace lo guarda el persistidor, no el transmisor")
        void el_desenlace_lo_guarda_el_persistidor() {
            ElectronicDocument documento = facturaPendienteConId(DOCUMENT_ID);
            conFacturacionElectronicaActiva();
            when(matias.transmit(any(), any())).thenReturn(resultado(DianStatus.VALIDADO));
            when(resultPersister.persist(any(), any(), any()))
                    .thenReturn(facturaValidada(DOCUMENT_ID));

            transmitter.transmit(documento);

            // El HTTP tarda hasta 75 segundos y corre sin transaccion; el desenlace va en
            // una transaccion propia y corta. Guardarlo aqui retendria la conexion del
            // pool durante todo el HTTP.
            verify(resultPersister).persist(eq(documento), any(), eq("MATIAS"));
        }

        @Test
        @DisplayName("mide el resultado con el origen que le pasan")
        void mide_el_resultado_con_su_origen() {
            ElectronicDocument documento = facturaPendienteConId(DOCUMENT_ID);
            ElectronicDocument validada = facturaValidada(DOCUMENT_ID);
            conFacturacionElectronicaActiva();
            when(matias.transmit(any(), any())).thenReturn(resultado(DianStatus.VALIDADO));
            when(resultPersister.persist(any(), any(), any())).thenReturn(validada);

            transmitter.transmit(documento, Origin.RETRY);

            verify(billingMetrics).finished(eq(DianStatus.VALIDADO), eq(Origin.RETRY),
                    eq(validada.getDocumentType()), any(Duration.class));
        }
    }

    @Nested
    @DisplayName("transmit — guardas que evitan hablar con el proveedor")
    class Guardas {

        @Test
        @DisplayName("sin el submodulo de facturacion no se contacta al proveedor")
        void sin_submodulo_de_facturacion_no_se_contacta_al_proveedor() {
            ElectronicDocument documento = facturaPendienteConId(DOCUMENT_ID);
            when(billingEntitlement.isElectronicInvoicingEnabled(COMPANY_ID)).thenReturn(false);

            assertThat(transmitter.transmit(documento)).isSameAs(documento);

            // El documento se queda PENDIENTE, no se degrada: los datos quedan guardados y
            // se puede emitir cuando la empresa contrate el modulo.
            assertThat(documento.getDianStatus()).isEqualTo(DianStatus.PENDIENTE);
            verify(matias, never()).transmit(any(), any());
            verifyNoInteractions(resultPersister, configQueryPort);
        }

        @Test
        @DisplayName("sin proveedor configurado falla en vez de emitir a ciegas")
        void sin_proveedor_configurado_falla() {
            ElectronicDocument documento = facturaPendienteConId(DOCUMENT_ID);
            when(billingEntitlement.isElectronicInvoicingEnabled(COMPANY_ID)).thenReturn(true);
            when(configQueryPort.findByCompanyId(COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transmitter.transmit(documento))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no tiene un proveedor DIAN configurado");

            verifyNoInteractions(resultPersister);
        }

        @Test
        @DisplayName("un proveedor sin adaptador se reporta con su nombre")
        void un_proveedor_sin_adaptador_se_reporta_con_su_nombre() {
            ElectronicDocument documento = facturaPendienteConId(DOCUMENT_ID);
            when(billingEntitlement.isElectronicInvoicingEnabled(COMPANY_ID)).thenReturn(true);
            when(configQueryPort.findByCompanyId(COMPANY_ID))
                    .thenReturn(Optional.of(new ProviderConfigSnapshot("OTRO", null, null, null,
                            null, null, null, null, null)));

            // Configurar un proveedor que nadie implementa es un error de configuracion:
            // el mensaje tiene que decir cual para poder arreglarlo.
            assertThatThrownBy(() -> transmitter.transmit(documento))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No hay adaptador para el proveedor: OTRO");
        }

        @Test
        @DisplayName("si algo revienta se mide como fallo y la excepcion sigue subiendo")
        void si_algo_revienta_se_mide_como_fallo() {
            ElectronicDocument documento = facturaPendienteConId(DOCUMENT_ID);
            conFacturacionElectronicaActiva();
            when(matias.transmit(any(), any())).thenThrow(new IllegalStateException("timeout"));

            assertThatThrownBy(() -> transmitter.transmit(documento))
                    .isInstanceOf(IllegalStateException.class).hasMessage("timeout");

            // Tragarse la excepcion dejaria el documento en un limbo silencioso.
            verify(billingMetrics).failed(eq(Origin.INITIAL), eq(documento.getDocumentType()),
                    any(Duration.class));
            verify(billingMetrics, never()).finished(any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("reconcile — rescatar los PENDIENTE cuando se pierde el webhook")
    class Reconciliacion {

        @Test
        @DisplayName("un documento que ya no esta pendiente no se reconcilia")
        void un_documento_que_ya_no_esta_pendiente_no_se_reconcilia() {
            ElectronicDocument validada = facturaValidada(DOCUMENT_ID);

            assertThat(transmitter.reconcile(validada)).isSameAs(validada);

            verify(matias, never()).fetchStatus(any(), any());
            verifyNoInteractions(billingEntitlement, configQueryPort, resultPersister);
        }

        @Test
        @DisplayName("sin el submodulo de facturacion no se consulta al proveedor")
        void sin_submodulo_no_se_consulta_al_proveedor() {
            ElectronicDocument documento = facturaPendienteConId(DOCUMENT_ID);
            when(billingEntitlement.isElectronicInvoicingEnabled(COMPANY_ID)).thenReturn(false);

            assertThat(transmitter.reconcile(documento)).isSameAs(documento);

            verify(matias, never()).fetchStatus(any(), any());
            verifyNoInteractions(resultPersister);
        }

        @Test
        @DisplayName("un documento que nunca se transmitio no tiene nada que reconciliar")
        void un_documento_que_nunca_se_transmitio_no_se_reconcilia() {
            ElectronicDocument documento = facturaPendienteConId(DOCUMENT_ID);
            conFacturacionElectronicaActiva();
            when(transmissionLog.findLatestProviderKey(DOCUMENT_ID)).thenReturn(Optional.empty());

            // Sin clave del proveedor no hay a que preguntarle por el.
            assertThat(transmitter.reconcile(documento)).isSameAs(documento);
            verify(matias, never()).fetchStatus(any(), any());
        }

        @Test
        @DisplayName("un proveedor sincrono no soporta consulta y se deja igual")
        void un_proveedor_sincrono_se_deja_igual() {
            ElectronicDocument documento = facturaPendienteConId(DOCUMENT_ID);
            conFacturacionElectronicaActiva();
            when(transmissionLog.findLatestProviderKey(DOCUMENT_ID))
                    .thenReturn(Optional.of("KEY-1"));
            when(matias.fetchStatus("KEY-1", CONFIG)).thenReturn(Optional.empty());

            assertThat(transmitter.reconcile(documento)).isSameAs(documento);
            verifyNoInteractions(resultPersister);
        }

        @Test
        @DisplayName("si el proveedor dice que sigue en cola, se mide pero no se persiste")
        void si_sigue_en_cola_se_mide_pero_no_se_persiste() {
            ElectronicDocument documento = facturaPendienteConId(DOCUMENT_ID);
            conFacturacionElectronicaActiva();
            when(transmissionLog.findLatestProviderKey(DOCUMENT_ID))
                    .thenReturn(Optional.of("KEY-1"));
            when(matias.fetchStatus("KEY-1", CONFIG))
                    .thenReturn(Optional.of(resultado(DianStatus.PENDIENTE)));

            assertThat(transmitter.reconcile(documento)).isSameAs(documento);

            verify(billingMetrics).finished(eq(DianStatus.PENDIENTE), eq(Origin.RECONCILIATION),
                    any(), any(Duration.class));
            verifyNoInteractions(resultPersister, deliverService);
        }

        @Test
        @DisplayName("con un estado terminal persiste el desenlace y entrega la representacion")
        void con_un_estado_terminal_persiste_y_entrega() {
            ElectronicDocument documento = facturaPendienteConId(DOCUMENT_ID);
            ElectronicDocument validada = facturaValidada(DOCUMENT_ID);
            conFacturacionElectronicaActiva();
            when(transmissionLog.findLatestProviderKey(DOCUMENT_ID))
                    .thenReturn(Optional.of("KEY-1"));
            when(matias.fetchStatus("KEY-1", CONFIG))
                    .thenReturn(Optional.of(resultado(DianStatus.VALIDADO)));
            when(resultPersister.persistReconciled(any(), any(), eq("MATIAS"), eq("KEY-1")))
                    .thenReturn(validada);

            assertThat(transmitter.reconcile(documento)).isSameAs(validada);

            // El webhook perdido tiene que acabar igual que si hubiera llegado: incluida
            // la entrega de la representacion grafica al cliente.
            verify(deliverService).deliverIfValidated(validada);
        }

        @Test
        @DisplayName("si algo revienta se mide como fallo con el origen de reconciliacion")
        void si_algo_revienta_se_mide_como_fallo() {
            ElectronicDocument documento = facturaPendienteConId(DOCUMENT_ID);
            conFacturacionElectronicaActiva();
            when(transmissionLog.findLatestProviderKey(DOCUMENT_ID))
                    .thenReturn(Optional.of("KEY-1"));
            when(matias.fetchStatus(any(), any())).thenThrow(new IllegalStateException("timeout"));

            assertThatThrownBy(() -> transmitter.reconcile(documento))
                    .isInstanceOf(IllegalStateException.class);

            verify(billingMetrics).failed(eq(Origin.RECONCILIATION), any(), any(Duration.class));
        }
    }
}
