package com.vetsoftware.app.electronicdocument.application.usecase;

import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.COMPANY_ID;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.OPEN_ACCOUNT_ID;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.facturaPendienteConId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.application.command.BuildElectronicDocumentCommand;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("BuildElectronicDocumentFromAccountService — construye sin emitir (endpoint provisional)")
class BuildElectronicDocumentFromAccountServiceTest {

    @Mock
    private DocumentBuilder documentBuilder;

    private BuildElectronicDocumentFromAccountService service;

    @BeforeEach
    void montar() {
        service = new BuildElectronicDocumentFromAccountService(documentBuilder);
    }

    @Test
    @DisplayName("delega la construccion en el DocumentBuilder y mapea el resultado a dto")
    void delega_en_document_builder() {
        ElectronicDocument documento = facturaPendienteConId(99L);
        when(documentBuilder.build(OPEN_ACCOUNT_ID, ElectronicDocumentType.FE_VENTA, COMPANY_ID,
                true)).thenReturn(documento);

        var dto = service.execute(new BuildElectronicDocumentCommand(OPEN_ACCOUNT_ID,
                ElectronicDocumentType.FE_VENTA, COMPANY_ID, true));

        assertThat(dto.id()).isEqualTo(99L);
        assertThat(dto.dianStatus()).isEqualTo(documento.getDianStatus());
    }
}
