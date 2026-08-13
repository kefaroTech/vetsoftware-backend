package com.vetsoftware.app.debtopenaccount.application.usecase;

import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.COMPANY_ID;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.OTRA_COMPANY_ID;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.PAYMENT_ID;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.abono;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.abonoAnulado;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.debtopenaccount.application.dto.DebtOpenAccountDto;
import com.vetsoftware.app.debtopenaccount.application.port.out.DebtOpenAccountRepository;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccountNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindDebtOpenAccountService")
class FindDebtOpenAccountServiceTest {

    @Mock
    private DebtOpenAccountRepository repository;

    @InjectMocks
    private FindDebtOpenAccountService service;

    @Test
    @DisplayName("devuelve el DTO del abono de la empresa")
    void devuelve_el_dto_del_abono_de_la_empresa() {
        when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                .thenReturn(Optional.of(abono()));

        DebtOpenAccountDto dto = service.findById(PAYMENT_ID, COMPANY_ID);

        assertThat(dto.id()).isEqualTo(PAYMENT_ID);
        assertThat(dto.amount()).isEqualByComparingTo("30000");
    }

    @Test
    @DisplayName("un abono anulado se sigue pudiendo consultar: anular no es borrar")
    void un_abono_anulado_se_sigue_pudiendo_consultar() {
        when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                .thenReturn(Optional.of(abonoAnulado()));

        assertThat(service.findById(PAYMENT_ID, COMPANY_ID).voided()).isTrue();
    }

    @Test
    @DisplayName("un abono de otra empresa se ve como inexistente, no como prohibido")
    void un_abono_de_otra_empresa_se_ve_como_inexistente() {
        when(repository.findByIdAndCompanyId(PAYMENT_ID, OTRA_COMPANY_ID))
                .thenReturn(Optional.empty());

        // 404 y no 403: responder "existe pero no es tuyo" ya filtra que existe.
        assertThatThrownBy(() -> service.findById(PAYMENT_ID, OTRA_COMPANY_ID))
                .isInstanceOf(DebtOpenAccountNotFoundException.class)
                .hasMessageContaining(String.valueOf(PAYMENT_ID));
    }
}
