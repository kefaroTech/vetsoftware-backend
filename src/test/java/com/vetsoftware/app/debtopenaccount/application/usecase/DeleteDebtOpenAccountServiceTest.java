package com.vetsoftware.app.debtopenaccount.application.usecase;

import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.COMPANY_ID;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.OPEN_ACCOUNT_ID;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.OTRA_COMPANY_ID;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.PAYMENT_ID;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.abono;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.abonoDeOtraEmpresa;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.debtopenaccount.application.port.out.DebtOpenAccountRepository;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccountNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteDebtOpenAccountService")
class DeleteDebtOpenAccountServiceTest {

    @Mock
    private DebtOpenAccountRepository repository;
    @Mock
    private OpenAccountRefresher refresher;

    @InjectMocks
    private DeleteDebtOpenAccountService service;

    @Test
    @DisplayName("borra el abono y refresca el total de su cuenta")
    void borra_el_abono_y_refresca() {
        when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                .thenReturn(Optional.of(abono()));

        service.execute(PAYMENT_ID, COMPANY_ID);

        // El refresh va despues del borrado: al reves recalcularia el saldo con el
        // abono todavia contando, y la cuenta quedaria mostrando menos deuda.
        InOrder orden = Mockito.inOrder(repository, refresher);
        orden.verify(repository).delete(PAYMENT_ID, COMPANY_ID);
        orden.verify(refresher).refresh(COMPANY_ID, OPEN_ACCOUNT_ID);
    }

    @Test
    @DisplayName("abono inexistente o de otra empresa")
    void abono_inexistente_o_de_otra_empresa() {
        when(repository.findByIdAndCompanyId(PAYMENT_ID, OTRA_COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(PAYMENT_ID, OTRA_COMPANY_ID))
                .isInstanceOf(DebtOpenAccountNotFoundException.class)
                .hasMessageContaining(String.valueOf(PAYMENT_ID));

        verify(repository, never()).delete(any(), any());
        verifyNoInteractions(refresher);
    }

    @Test
    @DisplayName("abono cuya cuenta es de otra empresa: guard de tenancy, no se borra")
    void abono_cuya_cuenta_es_de_otra_empresa() {
        // Defensa en profundidad: la lectura ya venia scoped, pero si algun dia deja
        // de estarlo, este guard impide borrar el abono de otro tenant.
        when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                .thenReturn(Optional.of(abonoDeOtraEmpresa()));

        assertThatThrownBy(() -> service.execute(PAYMENT_ID, COMPANY_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("debt open account does not belong to company");

        verify(repository, never()).delete(any(), any());
        verifyNoInteractions(refresher);
    }
}
