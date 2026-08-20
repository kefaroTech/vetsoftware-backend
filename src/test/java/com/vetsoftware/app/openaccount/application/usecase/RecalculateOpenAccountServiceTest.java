package com.vetsoftware.app.openaccount.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountTotalsPort;
import com.vetsoftware.app.openaccount.domain.OpenAccount;
import com.vetsoftware.app.openaccount.domain.OpenAccountNotFoundException;
import com.vetsoftware.app.openaccount.testsupport.OpenAccountMother;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecalculateOpenAccountService")
class RecalculateOpenAccountServiceTest {

    @Mock
    private OpenAccountRepository repository;
    @Mock
    private OpenAccountTotalsPort totalsPort;
    @InjectMocks
    private RecalculateOpenAccountService service;

    @Nested
    @DisplayName("recalculo")
    class Recalculo {

        @Test
        @DisplayName("recalcula el total, el pagado y el saldo, y persiste bajo lock")
        void recalcula_los_totales_y_persiste() {
            OpenAccount cuenta = OpenAccountMother.abierta();
            when(repository.findByIdForUpdateAndCompanyId(OpenAccountMother.OPEN_ACCOUNT_ID,
                    OpenAccountMother.COMPANY_ID)).thenReturn(Optional.of(cuenta));
            when(totalsPort.totalCharges(OpenAccountMother.OPEN_ACCOUNT_ID))
                    .thenReturn(new BigDecimal("1000.00"));
            when(totalsPort.totalPayments(OpenAccountMother.OPEN_ACCOUNT_ID))
                    .thenReturn(new BigDecimal("400.00"));

            service.recalculate(OpenAccountMother.COMPANY_ID, OpenAccountMother.OPEN_ACCOUNT_ID);

            ArgumentCaptor<OpenAccount> captor = ArgumentCaptor.forClass(OpenAccount.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo("1000.00");
            assertThat(captor.getValue().getPaidAmount()).isEqualByComparingTo("400.00");
            assertThat(captor.getValue().getOutstandingAmount()).isEqualByComparingTo("600.00");
        }
    }

    @Nested
    @DisplayName("validaciones que no deben escribir")
    class Validaciones {

        @Test
        @DisplayName("cuenta inexistente o de otra empresa lanza y no consulta totales ni escribe")
        void cuenta_inexistente_lanza_y_no_toca_totales() {
            when(repository.findByIdForUpdateAndCompanyId(OpenAccountMother.OPEN_ACCOUNT_ID,
                    OpenAccountMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.recalculate(OpenAccountMother.COMPANY_ID,
                    OpenAccountMother.OPEN_ACCOUNT_ID))
                    .isInstanceOf(OpenAccountNotFoundException.class);

            verifyNoInteractions(totalsPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("unos totales con mas cobrado que facturado no se persisten")
        void unos_totales_con_mas_cobrado_que_facturado_no_se_persisten() {
            // El recalculo es la ruta por la que un saldo negativo llegaria a la fila
            // aunque el caso de uso que lo dispara no tenga guard de sobrepago
            // —reactivar un abono deshabilitado, por ejemplo—. La entidad lo rechaza y
            // aqui se comprueba lo que importa: que no se escribe la cuenta corrupta.
            OpenAccount cuenta = OpenAccountMother.abierta();
            when(repository.findByIdForUpdateAndCompanyId(OpenAccountMother.OPEN_ACCOUNT_ID,
                    OpenAccountMother.COMPANY_ID)).thenReturn(Optional.of(cuenta));
            when(totalsPort.totalCharges(OpenAccountMother.OPEN_ACCOUNT_ID))
                    .thenReturn(new BigDecimal("1000.00"));
            when(totalsPort.totalPayments(OpenAccountMother.OPEN_ACCOUNT_ID))
                    .thenReturn(new BigDecimal("1500.00"));

            assertThatThrownBy(() -> service.recalculate(OpenAccountMother.COMPANY_ID,
                    OpenAccountMother.OPEN_ACCOUNT_ID)).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no puede superar el total facturado");

            verify(repository, never()).save(any());
            assertThat(cuenta.getOutstandingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
