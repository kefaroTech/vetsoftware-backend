package com.vetsoftware.app.generalchargeopenaccount.application.usecase;

import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.COMPANY_ID;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.OPEN_ACCOUNT_ID;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.OTRA_COMPANY_ID;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.cargo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.generalchargeopenaccount.application.dto.GeneralChargeOpenAccountDto;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.GeneralChargeOpenAccountRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListGeneralChargeOpenAccountsByOpenAccountService")
class ListGeneralChargeOpenAccountsByOpenAccountServiceTest {

    @Mock
    private GeneralChargeOpenAccountRepository repository;

    @InjectMocks
    private ListGeneralChargeOpenAccountsByOpenAccountService service;

    @Test
    @DisplayName("proyecta los cargos de la cuenta, en el orden del repositorio")
    void proyecta_los_cargos_de_la_cuenta() {
        when(repository.findByOpenAccountIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                .thenReturn(List.of(cargo(1L), cargo(2L), cargo(3L)));

        List<GeneralChargeOpenAccountDto> cargos = service.listByOpenAccount(OPEN_ACCOUNT_ID,
                COMPANY_ID);

        // El orden lo fija la query (por fecha): reordenarlo aqui cambiaria el detalle
        // de la cuenta que ve el cliente.
        assertThat(cargos).extracting(GeneralChargeOpenAccountDto::id).containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("una cuenta de otra empresa no devuelve nada")
    void una_cuenta_de_otra_empresa_no_devuelve_nada() {
        when(repository.findByOpenAccountIdAndCompanyId(OPEN_ACCOUNT_ID, OTRA_COMPANY_ID))
                .thenReturn(List.of());

        assertThat(service.listByOpenAccount(OPEN_ACCOUNT_ID, OTRA_COMPANY_ID)).isEmpty();
    }
}
