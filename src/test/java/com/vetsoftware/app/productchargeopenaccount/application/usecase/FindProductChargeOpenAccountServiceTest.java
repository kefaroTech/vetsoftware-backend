package com.vetsoftware.app.productchargeopenaccount.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.productchargeopenaccount.application.dto.ProductChargeOpenAccountDto;
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
@DisplayName("FindProductChargeOpenAccountService")
class FindProductChargeOpenAccountServiceTest {

    @Mock
    private ProductChargeOpenAccountRepository repository;

    @InjectMocks
    private FindProductChargeOpenAccountService service;

    @Test
    @DisplayName("devuelve el DTO del cargo de la empresa consultada")
    void devuelve_el_dto_del_cargo() {
        when(repository.findByIdAndCompanyId(ProductChargeOpenAccountMother.CHARGE_ID,
                ProductChargeOpenAccountMother.COMPANY_ID))
                .thenReturn(Optional.of(ProductChargeOpenAccountMother.cargo()));

        ProductChargeOpenAccountDto dto = service.findById(ProductChargeOpenAccountMother.CHARGE_ID,
                ProductChargeOpenAccountMother.COMPANY_ID);

        assertThat(dto.id()).isEqualTo(ProductChargeOpenAccountMother.CHARGE_ID);
        assertThat(dto.totalAmount()).isEqualByComparingTo("11900.00");
    }

    @Test
    @DisplayName("un cargo de otra empresa se comporta como inexistente")
    void un_cargo_de_otra_empresa_se_comporta_como_inexistente() {
        // La lectura va siempre acotada por companyId: es la defensa contra IDOR.
        when(repository.findByIdAndCompanyId(ProductChargeOpenAccountMother.CHARGE_ID,
                ProductChargeOpenAccountMother.OTRA_COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(ProductChargeOpenAccountMother.CHARGE_ID,
                ProductChargeOpenAccountMother.OTRA_COMPANY_ID))
                .isInstanceOf(ProductChargeOpenAccountNotFoundException.class)
                .hasMessageContaining("ProductChargeOpenAccount not found: "
                        + ProductChargeOpenAccountMother.CHARGE_ID);
    }
}
