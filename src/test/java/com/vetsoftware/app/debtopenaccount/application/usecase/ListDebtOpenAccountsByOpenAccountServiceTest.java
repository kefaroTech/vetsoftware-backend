package com.vetsoftware.app.debtopenaccount.application.usecase;

import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.COMPANY_ID;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.OPEN_ACCOUNT_ID;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.OTRA_COMPANY_ID;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.abono;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.debtopenaccount.application.dto.DebtOpenAccountDto;
import com.vetsoftware.app.debtopenaccount.application.port.out.DebtOpenAccountRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListDebtOpenAccountsByOpenAccountService")
class ListDebtOpenAccountsByOpenAccountServiceTest {

    @Mock
    private DebtOpenAccountRepository repository;

    @InjectMocks
    private ListDebtOpenAccountsByOpenAccountService service;

    @Test
    @DisplayName("proyecta los abonos de la cuenta, en el orden del repositorio")
    void proyecta_los_abonos_de_la_cuenta() {
        when(repository.findByOpenAccountIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                .thenReturn(List.of(abono(1L), abono(2L), abono(3L)));

        List<DebtOpenAccountDto> abonos = service.listByOpenAccount(OPEN_ACCOUNT_ID, COMPANY_ID);

        // El orden lo fija la query (por fecha): reordenarlo aqui cambiaria el detalle
        // de pagos de la cuenta que ve el cliente.
        assertThat(abonos).extracting(DebtOpenAccountDto::id).containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("una cuenta de otra empresa no devuelve nada")
    void una_cuenta_de_otra_empresa_no_devuelve_nada() {
        when(repository.findByOpenAccountIdAndCompanyId(OPEN_ACCOUNT_ID, OTRA_COMPANY_ID))
                .thenReturn(List.of());

        assertThat(service.listByOpenAccount(OPEN_ACCOUNT_ID, OTRA_COMPANY_ID)).isEmpty();
    }
}
