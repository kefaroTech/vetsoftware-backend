package com.vetsoftware.app.electronicdocument.application.usecase;

import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.COMPANY_ID;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.OPEN_ACCOUNT_ID;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.facturaPendienteConId;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.facturaValidada;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.application.command.EmitElectronicDocumentCommand;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmitElectronicDocumentFromAccountService — emite end-to-end desde una cuenta cerrada")
class EmitElectronicDocumentFromAccountServiceTest {

    @Mock
    private DocumentBuilder documentBuilder;
    @Mock
    private ElectronicDocumentEmitter emitter;
    @Mock
    private DeliverElectronicDocumentService deliverService;
    @Mock
    private TransactionTemplate transactionTemplate;

    private EmitElectronicDocumentFromAccountService service;

    @BeforeEach
    void montar() {
        service = new EmitElectronicDocumentFromAccountService(documentBuilder, emitter,
                deliverService, transactionTemplate);
        when(transactionTemplate.execute(any())).thenAnswer(
                inv -> ((TransactionCallback<?>) inv.getArgument(0)).doInTransaction(null));
    }

    @Test
    @DisplayName("construye, emite y entrega la representacion si el documento quedo validado")
    void construye_emite_y_entrega() {
        ElectronicDocument pendiente = facturaPendienteConId(98L);
        ElectronicDocument validada = facturaValidada(98L);
        when(documentBuilder.build(OPEN_ACCOUNT_ID, ElectronicDocumentType.FE_VENTA, COMPANY_ID,
                false)).thenReturn(pendiente);
        when(emitter.emit(pendiente)).thenReturn(validada);

        var dto = service.execute(new EmitElectronicDocumentCommand(OPEN_ACCOUNT_ID,
                ElectronicDocumentType.FE_VENTA, COMPANY_ID, false));

        assertThat(dto.id()).isEqualTo(98L);
        verify(deliverService).deliverIfValidated(validada);
    }
}
