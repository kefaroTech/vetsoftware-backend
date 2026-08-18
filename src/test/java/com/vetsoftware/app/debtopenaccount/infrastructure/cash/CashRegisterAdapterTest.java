package com.vetsoftware.app.debtopenaccount.infrastructure.cash;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaEntity;
import com.vetsoftware.app.cashregister.application.command.CashPaymentLine;
import com.vetsoftware.app.cashregister.application.command.RegisterCashInflowCommand;
import com.vetsoftware.app.cashregister.application.command.ReverseCashMovementsCommand;
import com.vetsoftware.app.cashregister.application.port.in.CashLedgerUseCase;
import com.vetsoftware.app.cashregister.domain.CashPaymentMethod;
import com.vetsoftware.app.cashregister.domain.CashReferenceType;
import com.vetsoftware.app.debtopenaccount.domain.PaymentMethod;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaEntity;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
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

/**
 * Adaptador de orquestacion cuenta abierta -> caja. Resuelve la sede navegando
 * {@code OpenAccountJpaRepository} (por eso se mockea junto con el puerto de
 * caja) y traduce {@link PaymentMethod} al {@link CashPaymentMethod} de la
 * feature cashregister.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CashRegisterAdapter (debtopenaccount) — orquesta el abono contra la caja")
class CashRegisterAdapterTest {

    private static final Long COMPANY_ID = 9L;
    private static final Long OPEN_ACCOUNT_ID = 50L;
    private static final Long BRANCH_ID = 3L;
    private static final Long PAYMENT_ID = 100L;
    private static final Long EMPLOYEE_ID = 7L;

    @Mock
    private CashLedgerUseCase cashLedger;
    @Mock
    private OpenAccountJpaRepository openAccountJpaRepository;
    @Mock
    private OpenAccountJpaEntity openAccountEntity;
    @Mock
    private BranchJpaEntity branchEntity;

    private CashRegisterAdapter adapter;

    @BeforeEach
    void montar() {
        adapter = new CashRegisterAdapter(cashLedger, openAccountJpaRepository);
    }

    private void resuelveLaSede() {
        when(openAccountJpaRepository.findById(OPEN_ACCOUNT_ID))
                .thenReturn(Optional.of(openAccountEntity));
        when(openAccountEntity.getBranch()).thenReturn(branchEntity);
        when(branchEntity.getId()).thenReturn(BRANCH_ID);
    }

    @Nested
    @DisplayName("requireOpenSession")
    class RequireOpenSession {

        @Test
        @DisplayName("resuelve la sede de la cuenta y delega en ensureEmployeeCashAvailable")
        void resuelve_la_sede_y_delega() {
            resuelveLaSede();

            adapter.requireOpenSession(COMPANY_ID, OPEN_ACCOUNT_ID, EMPLOYEE_ID);

            verify(cashLedger).ensureEmployeeCashAvailable(COMPANY_ID, BRANCH_ID, EMPLOYEE_ID);
        }

        @Test
        @DisplayName("una cuenta inexistente no llega a preguntar por la caja")
        void una_cuenta_inexistente_no_llega_a_la_caja() {
            when(openAccountJpaRepository.findById(OPEN_ACCOUNT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> adapter.requireOpenSession(COMPANY_ID, OPEN_ACCOUNT_ID, EMPLOYEE_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("OpenAccount not found: " + OPEN_ACCOUNT_ID);

            verifyNoInteractions(cashLedger);
        }
    }

    @Nested
    @DisplayName("registerPayment")
    class RegisterPayment {

        @Test
        @DisplayName("registra el ingreso con referencia OPEN_ACCOUNT_PAYMENT y el importe del abono")
        void registra_el_ingreso_con_referencia_open_account_payment() {
            resuelveLaSede();

            adapter.registerPayment(COMPANY_ID, OPEN_ACCOUNT_ID, PAYMENT_ID, PaymentMethod.CASH,
                    new BigDecimal("30000"), EMPLOYEE_ID);

            ArgumentCaptor<RegisterCashInflowCommand> captor = ArgumentCaptor
                    .forClass(RegisterCashInflowCommand.class);
            verify(cashLedger).registerInflow(captor.capture());
            assertThat(captor.getValue()).isEqualTo(new RegisterCashInflowCommand(COMPANY_ID,
                    BRANCH_ID, null, CashReferenceType.OPEN_ACCOUNT_PAYMENT, PAYMENT_ID,
                    List.of(new CashPaymentLine(CashPaymentMethod.CASH, new BigDecimal("30000"))),
                    EMPLOYEE_ID));
        }

        @Test
        @DisplayName("una cuenta inexistente no llega a registrar el ingreso")
        void una_cuenta_inexistente_no_llega_a_registrar_el_ingreso() {
            when(openAccountJpaRepository.findById(OPEN_ACCOUNT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adapter.registerPayment(COMPANY_ID, OPEN_ACCOUNT_ID,
                    PAYMENT_ID, PaymentMethod.CASH, new BigDecimal("30000"), EMPLOYEE_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("OpenAccount not found: " + OPEN_ACCOUNT_ID);

            verifyNoInteractions(cashLedger);
        }
    }

    @Nested
    @DisplayName("reversePayment")
    class ReversePayment {

        @Test
        @DisplayName("compensa con referencia OPEN_ACCOUNT_PAYMENT y el actor que anula")
        void compensa_con_referencia_open_account_payment() {
            resuelveLaSede();

            adapter.reversePayment(COMPANY_ID, OPEN_ACCOUNT_ID, PAYMENT_ID, PaymentMethod.CARD,
                    new BigDecimal("30000"), EMPLOYEE_ID);

            ArgumentCaptor<ReverseCashMovementsCommand> captor = ArgumentCaptor
                    .forClass(ReverseCashMovementsCommand.class);
            verify(cashLedger).reverse(captor.capture());
            assertThat(captor.getValue()).isEqualTo(new ReverseCashMovementsCommand(COMPANY_ID,
                    BRANCH_ID, null, CashReferenceType.OPEN_ACCOUNT_PAYMENT, PAYMENT_ID,
                    List.of(new CashPaymentLine(CashPaymentMethod.CARD, new BigDecimal("30000"))),
                    EMPLOYEE_ID));
        }
    }

    @ParameterizedTest
    @EnumSource(PaymentMethod.class)
    @DisplayName("todo metodo de pago del abono tiene un metodo de caja destino (el switch no deja ninguno sin mapear)")
    void todo_metodo_de_pago_tiene_destino_en_caja(PaymentMethod metodo) {
        resuelveLaSede();

        adapter.registerPayment(COMPANY_ID, OPEN_ACCOUNT_ID, PAYMENT_ID, metodo, BigDecimal.TEN,
                EMPLOYEE_ID);

        verify(cashLedger, atLeastOnce()).registerInflow(any());
    }
}
