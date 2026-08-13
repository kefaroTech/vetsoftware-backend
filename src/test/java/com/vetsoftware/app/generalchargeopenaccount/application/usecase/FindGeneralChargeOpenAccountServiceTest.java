package com.vetsoftware.app.generalchargeopenaccount.application.usecase;

import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.CHARGE_ID;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.COMPANY_ID;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.OTRA_COMPANY_ID;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.cargo;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.cargoAnulado;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.generalchargeopenaccount.application.dto.GeneralChargeOpenAccountDto;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.GeneralChargeOpenAccountRepository;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccountNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindGeneralChargeOpenAccountService")
class FindGeneralChargeOpenAccountServiceTest {

    @Mock
    private GeneralChargeOpenAccountRepository repository;

    @InjectMocks
    private FindGeneralChargeOpenAccountService service;

    @Test
    @DisplayName("devuelve el DTO del cargo de la empresa")
    void devuelve_el_dto_del_cargo_de_la_empresa() {
        when(repository.findByIdAndCompanyId(CHARGE_ID, COMPANY_ID))
                .thenReturn(Optional.of(cargo()));

        GeneralChargeOpenAccountDto dto = service.findById(CHARGE_ID, COMPANY_ID);

        assertThat(dto.id()).isEqualTo(CHARGE_ID);
        assertThat(dto.totalAmount()).isEqualByComparingTo("11900.00");
    }

    @Test
    @DisplayName("un cargo anulado se sigue pudiendo consultar: anular no es borrar")
    void un_cargo_anulado_se_sigue_pudiendo_consultar() {
        when(repository.findByIdAndCompanyId(CHARGE_ID, COMPANY_ID))
                .thenReturn(Optional.of(cargoAnulado()));

        assertThat(service.findById(CHARGE_ID, COMPANY_ID).voided()).isTrue();
    }

    @Test
    @DisplayName("un cargo de otra empresa se ve como inexistente, no como prohibido")
    void un_cargo_de_otra_empresa_se_ve_como_inexistente() {
        when(repository.findByIdAndCompanyId(CHARGE_ID, OTRA_COMPANY_ID))
                .thenReturn(Optional.empty());

        // 404 y no 403: responder "existe pero no es tuyo" ya filtra que existe.
        assertThatThrownBy(() -> service.findById(CHARGE_ID, OTRA_COMPANY_ID))
                .isInstanceOf(GeneralChargeOpenAccountNotFoundException.class)
                .hasMessageContaining(String.valueOf(CHARGE_ID));
    }
}
