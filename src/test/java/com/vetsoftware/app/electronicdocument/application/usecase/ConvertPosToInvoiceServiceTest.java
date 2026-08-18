package com.vetsoftware.app.electronicdocument.application.usecase;

import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.COMPANY_ID;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.OPEN_ACCOUNT_ID;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.documento;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.facturaValidada;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.application.command.ConvertPosToInvoiceCommand;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConvertPosToInvoiceService — convierte un documento equivalente POS en factura")
class ConvertPosToInvoiceServiceTest {

    private static final Long POS_ID = 110L;

    @Mock
    private ElectronicDocumentRepository repository;
    @Mock
    private DocumentBuilder documentBuilder;
    @Mock
    private ElectronicDocumentEmitter emitter;
    @Mock
    private DeliverElectronicDocumentService deliverService;
    @Mock
    private TransactionTemplate transactionTemplate;

    private ConvertPosToInvoiceService service;

    @BeforeEach
    void montar() {
        service = new ConvertPosToInvoiceService(repository, documentBuilder, emitter,
                deliverService, transactionTemplate);
        when(transactionTemplate.execute(any())).thenAnswer(
                inv -> ((TransactionCallback<?>) inv.getArgument(0)).doInTransaction(null));
    }

    private static ConvertPosToInvoiceCommand comando() {
        return new ConvertPosToInvoiceCommand(POS_ID, COMPANY_ID);
    }

    /**
     * POS validado y ligado a una cuenta cerrada — el unico caso convertible a
     * factura.
     */
    private static ElectronicDocument posConCuenta(Long id) {
        return documento(id, COMPANY_ID, ElectronicDocumentType.DOC_EQUIV_POS, DianStatus.VALIDADO,
                "POS", 55L, null, "CUDE-POS-1", false, OPEN_ACCOUNT_ID);
    }

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("construye la factura desde la cuenta del POS, emite y entrega")
        void construye_emite_y_entrega() {
            ElectronicDocument pos = posConCuenta(POS_ID);
            ElectronicDocument factura = documento(300L, COMPANY_ID,
                    ElectronicDocumentType.FE_VENTA, DianStatus.VALIDADO, "SETP", 991L, "CUFE-2",
                    null, false, OPEN_ACCOUNT_ID);
            when(repository.findByIdAndCompanyId(POS_ID, COMPANY_ID)).thenReturn(Optional.of(pos));
            when(repository.existsByOpenAccountIdAndDocumentType(pos.getOpenAccountId(),
                    ElectronicDocumentType.FE_VENTA)).thenReturn(false);
            when(documentBuilder.build(pos.getOpenAccountId(), ElectronicDocumentType.FE_VENTA,
                    COMPANY_ID, false)).thenReturn(factura);
            when(emitter.emit(factura)).thenReturn(factura);

            var dto = service.execute(comando());

            assertThat(dto.id()).isEqualTo(300L);
            verify(deliverService).deliverIfValidated(factura);
        }
    }

    @Nested
    @DisplayName("validaciones que abortan sin construir la factura")
    class Validaciones {

        @Test
        @DisplayName("documento POS inexistente lanza ElectronicDocumentNotFoundException")
        void pos_inexistente() {
            when(repository.findByIdAndCompanyId(POS_ID, COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando()))
                    .hasMessageContaining(String.valueOf(POS_ID));
            verifyNoInteractions(documentBuilder, emitter, deliverService);
        }

        /**
         * El filtro por empresa vive ahora EN la consulta: el POS ajeno no llega a
         * cargarse. El {@code never()} sobre la variante ancha es lo que impide que
         * alguien reintroduzca el {@code findById} + {@code if} posterior.
         */
        @Test
        @DisplayName("documento POS de otra empresa se reporta como no encontrado")
        void pos_de_otra_empresa_no_encontrado() {
            when(repository.findByIdAndCompanyId(POS_ID, COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando()))
                    .hasMessageContaining(String.valueOf(POS_ID));
            verify(repository, never()).findById(POS_ID);
            verifyNoInteractions(documentBuilder);
        }

        @Test
        @DisplayName("solo un documento equivalente POS puede convertirse a factura")
        void solo_pos_puede_convertirse() {
            ElectronicDocument factura = facturaValidada(POS_ID);
            when(repository.findByIdAndCompanyId(POS_ID, COMPANY_ID))
                    .thenReturn(Optional.of(factura));

            assertThatThrownBy(() -> service.execute(comando()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("documento equivalente POS puede convertirse");
            verifyNoInteractions(documentBuilder);
        }

        @Test
        @DisplayName("un POS sin cuenta asociada no puede reconstruir la factura")
        void pos_sin_cuenta_no_puede_reconstruir() {
            ElectronicDocument posSinCuenta = documento(POS_ID, COMPANY_ID,
                    ElectronicDocumentType.DOC_EQUIV_POS, DianStatus.VALIDADO, "POS", 1L, null,
                    "CUDE-Y", false, null);
            when(repository.findByIdAndCompanyId(POS_ID, COMPANY_ID))
                    .thenReturn(Optional.of(posSinCuenta));

            assertThatThrownBy(() -> service.execute(comando()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no referencia una cuenta");
            verifyNoInteractions(documentBuilder);
        }

        @Test
        @DisplayName("una cuenta ya convertida a FE_VENTA no se convierte dos veces")
        void cuenta_ya_convertida_no_se_repite() {
            ElectronicDocument pos = posConCuenta(POS_ID);
            when(repository.findByIdAndCompanyId(POS_ID, COMPANY_ID)).thenReturn(Optional.of(pos));
            when(repository.existsByOpenAccountIdAndDocumentType(pos.getOpenAccountId(),
                    ElectronicDocumentType.FE_VENTA)).thenReturn(true);

            assertThatThrownBy(() -> service.execute(comando()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ya fue convertido");
            verifyNoInteractions(documentBuilder);
        }
    }
}
