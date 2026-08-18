package com.vetsoftware.app.electronicdocument.infrastructure.cash;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.vetsoftware.app.cashregister.application.command.CashPaymentLine;
import com.vetsoftware.app.cashregister.application.command.RegisterCashInflowCommand;
import com.vetsoftware.app.cashregister.application.command.ReverseCashMovementsCommand;
import com.vetsoftware.app.cashregister.application.port.in.CashLedgerUseCase;
import com.vetsoftware.app.cashregister.domain.CashPaymentMethod;
import com.vetsoftware.app.cashregister.domain.CashReferenceType;
import com.vetsoftware.app.electronicdocument.application.port.out.CashPort.PaymentLine;
import com.vetsoftware.app.electronicdocument.domain.PaymentMeans;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CashRegisterAdapter — traduce el cobro POS a movimientos de caja")
class CashRegisterAdapterTest {

    @Mock
    private CashLedgerUseCase cashLedger;

    private CashRegisterAdapter adapter;

    @BeforeEach
    void montar() {
        adapter = new CashRegisterAdapter(cashLedger);
    }

    @Test
    @DisplayName("requireOpenSession delega en ensureEmployeeCashAvailable")
    void require_open_session_delega() {
        adapter.requireOpenSession(9L, 7L, 4L);

        verify(cashLedger).ensureEmployeeCashAvailable(9L, 7L, 4L);
    }

    @Nested
    @DisplayName("registerSale")
    class RegisterSale {

        @Test
        @DisplayName("registra el ingreso con referencia POS_DOCUMENT y los medios traducidos")
        void registra_el_ingreso_con_referencia_pos_document() {
            List<PaymentLine> pagos = List
                    .of(new PaymentLine(PaymentMeans.EFECTIVO, new BigDecimal("10000")));

            adapter.registerSale(9L, 7L, 100L, pagos, 4L);

            ArgumentCaptor<RegisterCashInflowCommand> captor = ArgumentCaptor
                    .forClass(RegisterCashInflowCommand.class);
            verify(cashLedger).registerInflow(captor.capture());
            assertThat(captor.getValue()).isEqualTo(new RegisterCashInflowCommand(9L, 7L, null,
                    CashReferenceType.POS_DOCUMENT, 100L,
                    List.of(new CashPaymentLine(CashPaymentMethod.CASH, new BigDecimal("10000"))),
                    4L));
        }
    }

    @Nested
    @DisplayName("reverseSale")
    class ReverseSale {

        @Test
        @DisplayName("compensa con referencia POS_DOCUMENT y los medios traducidos")
        void compensa_con_referencia_pos_document() {
            List<PaymentLine> pagos = List
                    .of(new PaymentLine(PaymentMeans.TRANSFERENCIA, new BigDecimal("5000")));

            adapter.reverseSale(9L, 7L, 100L, pagos, 4L);

            ArgumentCaptor<ReverseCashMovementsCommand> captor = ArgumentCaptor
                    .forClass(ReverseCashMovementsCommand.class);
            verify(cashLedger).reverse(captor.capture());
            assertThat(captor.getValue()).isEqualTo(
                    new ReverseCashMovementsCommand(9L, 7L, null, CashReferenceType.POS_DOCUMENT,
                            100L, List.of(new CashPaymentLine(CashPaymentMethod.TRANSFER,
                                    new BigDecimal("5000"))),
                            4L));
        }
    }

    @ParameterizedTest
    @EnumSource(PaymentMeans.class)
    @DisplayName("todo medio de pago DIAN tiene un medio de caja destino (el switch no deja ninguno sin mapear)")
    void todo_medio_de_pago_tiene_destino_en_caja(PaymentMeans means) {
        List<PaymentLine> pagos = List.of(new PaymentLine(means, BigDecimal.TEN));

        adapter.registerSale(9L, 7L, 100L, pagos, 4L);

        verify(cashLedger, org.mockito.Mockito.atLeastOnce()).registerInflow(any());
    }
}
