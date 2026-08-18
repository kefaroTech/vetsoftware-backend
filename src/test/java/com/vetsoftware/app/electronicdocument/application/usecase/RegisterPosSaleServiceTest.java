package com.vetsoftware.app.electronicdocument.application.usecase;

import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.COMPANY_ID;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.EMPLOYEE_ID;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.facturaValidada;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.posPendiente;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.application.command.RegisterPosSaleCommand;
import com.vetsoftware.app.electronicdocument.application.command.RegisterPosSaleCommand.SaleLine;
import com.vetsoftware.app.electronicdocument.application.command.RegisterPosSaleCommand.SalePayment;
import com.vetsoftware.app.electronicdocument.application.command.SaleLineKind;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.SalesMetrics;
import com.vetsoftware.app.electronicdocument.application.port.out.SalesMetrics.Channel;
import com.vetsoftware.app.electronicdocument.application.port.out.SalesMetrics.Result;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import com.vetsoftware.app.electronicdocument.domain.PaymentMeans;
import com.vetsoftware.app.electronicdocument.domain.StockDiscountMismatchException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterPosSaleService — registra y emite una venta de mostrador")
class RegisterPosSaleServiceTest {

    @Mock
    private PosSaleRegistrar saleRegistrar;
    @Mock
    private ElectronicDocumentEmitter emitter;
    @Mock
    private DeliverElectronicDocumentService deliverService;
    @Mock
    private ElectronicDocumentRepository repository;
    @Mock
    private SalesMetrics salesMetrics;

    private RegisterPosSaleService service;

    @BeforeEach
    void montar() {
        service = new RegisterPosSaleService(saleRegistrar, emitter, deliverService, repository,
                salesMetrics);
    }

    private static RegisterPosSaleCommand comando(BigDecimal quantity, String clientRequestId) {
        return new RegisterPosSaleCommand(COMPANY_ID, ElectronicDocumentType.DOC_EQUIV_POS, true,
                null,
                List.of(new SaleLine(SaleLineKind.PRODUCT, 5L, "Alimento", quantity,
                        new BigDecimal("10000"))),
                List.of(new SalePayment(PaymentMeans.EFECTIVO, new BigDecimal("10000"))),
                clientRequestId, EMPLOYEE_ID, null);
    }

    @Nested
    @DisplayName("idempotencia por clientRequestId")
    class Idempotencia {

        @Test
        @DisplayName("con un client_request_id ya registrado devuelve el documento existente sin reprocesar")
        void con_client_request_id_repetido_devuelve_el_existente() {
            ElectronicDocument existente = facturaValidada(200L);
            when(repository.findByCompanyIdAndClientRequestId(COMPANY_ID, "req-abc"))
                    .thenReturn(Optional.of(existente));

            var dto = service.execute(comando(BigDecimal.ONE, "req-abc"));

            assertThat(dto.id()).isEqualTo(200L);
            verifyNoInteractions(saleRegistrar, emitter, deliverService, salesMetrics);
        }
    }

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("registra, emite, entrega y mide la venta completada")
        void registra_emite_entrega_y_mide() {
            ElectronicDocument pendiente = posPendiente();
            ElectronicDocument emitido = facturaValidada(201L);
            RegisterPosSaleCommand comando = comando(BigDecimal.ONE, null);
            when(saleRegistrar.registerPending(comando)).thenReturn(pendiente);
            when(emitter.emit(pendiente)).thenReturn(emitido);

            var dto = service.execute(comando);

            assertThat(dto.id()).isEqualTo(201L);
            verify(deliverService).deliverIfValidated(emitido);
            verify(salesMetrics).completed(Channel.POS, emitido.getDocumentType(),
                    emitido.getPayableAmount(), emitido.getLines().size());
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("cantidad de producto no entera se rechaza antes de tocar el registrador")
        void cantidad_no_entera_se_rechaza_antes_de_registrar() {
            RegisterPosSaleCommand comando = comando(new BigDecimal("1.5"), null);

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("debe ser entera");

            verifyNoInteractions(saleRegistrar, emitter, deliverService);
            verify(salesMetrics).failed(Channel.POS, ElectronicDocumentType.DOC_EQUIV_POS,
                    Result.REJECTED);
        }

        @Test
        @DisplayName("un mismatch de stock durante el registro se mide como ERROR y sigue subiendo")
        void mismatch_de_stock_se_mide_como_error() {
            RegisterPosSaleCommand comando = comando(BigDecimal.ONE, null);
            when(saleRegistrar.registerPending(comando))
                    .thenThrow(new StockDiscountMismatchException(1L, 5L, 1, 0));

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(StockDiscountMismatchException.class);

            verify(salesMetrics).failed(Channel.POS, ElectronicDocumentType.DOC_EQUIV_POS,
                    Result.ERROR);
            verifyNoInteractions(emitter, deliverService);
        }
    }
}
