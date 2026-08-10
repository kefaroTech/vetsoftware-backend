package com.vetsoftware.app.productchargeopenaccount.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.productchargeopenaccount.application.dto.ProductChargeOpenAccountDto;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.ProductChargeOpenAccountRepository;
import com.vetsoftware.app.productchargeopenaccount.testsupport.ProductChargeOpenAccountMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListProductChargeOpenAccountsByOpenAccountService")
class ListProductChargeOpenAccountsByOpenAccountServiceTest {

    @Mock
    private ProductChargeOpenAccountRepository repository;

    @InjectMocks
    private ListProductChargeOpenAccountsByOpenAccountService service;

    @Test
    @DisplayName("devuelve los cargos de la cuenta convertidos a DTO")
    void devuelve_los_cargos_de_la_cuenta() {
        when(repository.findByOpenAccountIdAndCompanyId(
                ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID,
                ProductChargeOpenAccountMother.COMPANY_ID))
                .thenReturn(List.of(ProductChargeOpenAccountMother.cargo(),
                        ProductChargeOpenAccountMother.cargoAnulado()));

        List<ProductChargeOpenAccountDto> resultado = service.listByOpenAccount(
                ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID,
                ProductChargeOpenAccountMother.COMPANY_ID);

        assertThat(resultado).hasSize(2);
        // Los anulados siguen listandose: la fila queda visible con su traza.
        assertThat(resultado).extracting(ProductChargeOpenAccountDto::voided).containsExactly(false,
                true);
    }

    @Test
    @DisplayName("una cuenta de otra empresa no devuelve cargos")
    void una_cuenta_de_otra_empresa_no_devuelve_cargos() {
        when(repository.findByOpenAccountIdAndCompanyId(
                ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID,
                ProductChargeOpenAccountMother.OTRA_COMPANY_ID)).thenReturn(List.of());

        assertThat(service.listByOpenAccount(ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID,
                ProductChargeOpenAccountMother.OTRA_COMPANY_ID)).isEmpty();
    }
}
