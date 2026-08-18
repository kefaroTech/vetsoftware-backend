package com.vetsoftware.app.electronicdocument.application.usecase;

import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.COMPANY_ID;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.facturaPendienteConId;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.posPendiente;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.application.port.out.BillingEntitlementQueryPort;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ElectronicDocumentEmitter — punto unico de emision segun el derecho BILLING")
class ElectronicDocumentEmitterTest {

    @Mock
    private BillingEntitlementQueryPort billingEntitlement;
    @Mock
    private NumberAssigner numberAssigner;
    @Mock
    private DocumentTransmitter documentTransmitter;

    private ElectronicDocumentEmitter emitter;

    @BeforeEach
    void montar() {
        emitter = new ElectronicDocumentEmitter(billingEntitlement, numberAssigner,
                documentTransmitter);
    }

    @Test
    @DisplayName("sin el submodulo BILLING el documento queda igual, sin numerar ni transmitir")
    void sin_billing_no_numera_ni_transmite() {
        ElectronicDocument documento = facturaPendienteConId(90L);
        when(billingEntitlement.isElectronicInvoicingEnabled(COMPANY_ID)).thenReturn(false);

        assertThat(emitter.emit(documento)).isSameAs(documento);

        verifyNoInteractions(numberAssigner, documentTransmitter);
    }

    @Test
    @DisplayName("un documento de numeracion local se numera con consecutivo antes de transmitir")
    void documento_local_se_numera_con_consecutivo() {
        ElectronicDocument documento = facturaPendienteConId(91L);
        ElectronicDocument transmitido = facturaPendienteConId(91L);
        when(billingEntitlement.isElectronicInvoicingEnabled(COMPANY_ID)).thenReturn(true);
        when(documentTransmitter.transmit(documento)).thenReturn(transmitido);

        assertThat(emitter.emit(documento)).isSameAs(transmitido);

        verify(numberAssigner).assign(documento);
        verify(numberAssigner, never()).assignResolutionOnly(any());
    }

    @Test
    @DisplayName("un documento POS (consecutivo del proveedor) solo recibe resolucion+prefijo")
    void documento_pos_solo_recibe_resolucion_y_prefijo() {
        ElectronicDocument pos = posPendiente();
        when(billingEntitlement.isElectronicInvoicingEnabled(COMPANY_ID)).thenReturn(true);
        when(documentTransmitter.transmit(pos)).thenReturn(pos);

        emitter.emit(pos);

        verify(numberAssigner).assignResolutionOnly(pos);
        verify(numberAssigner, never()).assign(any());
    }
}
