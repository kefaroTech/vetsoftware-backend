package com.vetsoftware.app.productchargeopenaccount.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.productchargeopenaccount.application.dto.ProductChargeOpenAccountDto;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.ProductChargeOpenAccountRepository;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccountNotFoundException;
import com.vetsoftware.app.productchargeopenaccount.testsupport.ProductChargeOpenAccountMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivateProductChargeOpenAccountService")
class ReactivateProductChargeOpenAccountServiceTest {

    @Mock
    private ProductChargeOpenAccountRepository repository;
    @Mock
    private OpenAccountRefresher refresher;

    @InjectMocks
    private ReactivateProductChargeOpenAccountService service;

    @Test
    @DisplayName("reactiva el cargo y recalcula el total de su cuenta")
    void reactiva_el_cargo_y_recalcula_su_cuenta() {
        when(repository.reactivate(ProductChargeOpenAccountMother.CHARGE_ID,
                ProductChargeOpenAccountMother.COMPANY_ID)).thenReturn(1);
        when(repository.findByIdAndCompanyId(ProductChargeOpenAccountMother.CHARGE_ID,
                ProductChargeOpenAccountMother.COMPANY_ID))
                .thenReturn(Optional.of(ProductChargeOpenAccountMother.cargo()));

        ProductChargeOpenAccountDto dto = service.execute(ProductChargeOpenAccountMother.CHARGE_ID,
                ProductChargeOpenAccountMother.COMPANY_ID);

        assertThat(dto.id()).isEqualTo(ProductChargeOpenAccountMother.CHARGE_ID);
        assertThat(dto.enabled()).isTrue();
        verify(refresher).refresh(ProductChargeOpenAccountMother.COMPANY_ID,
                ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID);
    }

    @Test
    @DisplayName("si el update no toca ninguna fila el cargo no existe para esa empresa")
    void si_el_update_no_toca_ninguna_fila_el_cargo_no_existe() {
        when(repository.reactivate(ProductChargeOpenAccountMother.CHARGE_ID,
                ProductChargeOpenAccountMother.OTRA_COMPANY_ID)).thenReturn(0);

        assertThatThrownBy(() -> service.execute(ProductChargeOpenAccountMother.CHARGE_ID,
                ProductChargeOpenAccountMother.OTRA_COMPANY_ID))
                .isInstanceOf(ProductChargeOpenAccountNotFoundException.class)
                .hasMessageContaining("ProductChargeOpenAccount not found: "
                        + ProductChargeOpenAccountMother.CHARGE_ID);

        verifyNoInteractions(refresher);
    }

    @Test
    @DisplayName("si la relectura vuelve vacia se aborta sin recalcular")
    void si_la_relectura_vuelve_vacia_se_aborta() {
        when(repository.reactivate(ProductChargeOpenAccountMother.CHARGE_ID,
                ProductChargeOpenAccountMother.COMPANY_ID)).thenReturn(1);
        when(repository.findByIdAndCompanyId(ProductChargeOpenAccountMother.CHARGE_ID,
                ProductChargeOpenAccountMother.COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(ProductChargeOpenAccountMother.CHARGE_ID,
                ProductChargeOpenAccountMother.COMPANY_ID))
                .isInstanceOf(ProductChargeOpenAccountNotFoundException.class);

        verify(refresher, never()).refresh(anyLong(), anyLong());
    }
}
