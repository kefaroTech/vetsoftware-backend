package com.vetsoftware.app.productchargeopenaccount.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.productchargeopenaccount.application.dto.ProductChargeOpenAccountDto;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.ProductChargeOpenAccountRepository;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccount;
import com.vetsoftware.app.productchargeopenaccount.testsupport.ProductChargeOpenAccountMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListProductChargeOpenAccountsService")
class ListProductChargeOpenAccountsServiceTest {

    @Mock
    private ProductChargeOpenAccountRepository repository;

    @InjectMocks
    private ListProductChargeOpenAccountsService service;

    @Test
    @DisplayName("mapea la pagina del repositorio a DTOs conservando los metadatos")
    void mapea_la_pagina_conservando_los_metadatos() {
        PageResult<ProductChargeOpenAccount> pagina = new PageResult<>(
                List.of(ProductChargeOpenAccountMother.cargo(),
                        ProductChargeOpenAccountMother.cargo(101L)),
                1, 20, 22L, 2);
        when(repository.findAllByCompanyId(ProductChargeOpenAccountMother.COMPANY_ID, 1, 20))
                .thenReturn(pagina);

        PageResult<ProductChargeOpenAccountDto> resultado = service
                .listAll(ProductChargeOpenAccountMother.COMPANY_ID, 1, 20);

        assertThat(resultado.content()).extracting(ProductChargeOpenAccountDto::id)
                .containsExactly(ProductChargeOpenAccountMother.CHARGE_ID, 101L);
        assertThat(resultado.page()).isEqualTo(1);
        assertThat(resultado.pageSize()).isEqualTo(20);
        assertThat(resultado.totalElements()).isEqualTo(22L);
        assertThat(resultado.totalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("una empresa sin cargos devuelve una pagina vacia, no null")
    void una_empresa_sin_cargos_devuelve_pagina_vacia() {
        when(repository.findAllByCompanyId(ProductChargeOpenAccountMother.OTRA_COMPANY_ID, 0, 20))
                .thenReturn(new PageResult<>(List.of(), 0, 20, 0L, 0));

        PageResult<ProductChargeOpenAccountDto> resultado = service
                .listAll(ProductChargeOpenAccountMother.OTRA_COMPANY_ID, 0, 20);

        assertThat(resultado.content()).isEmpty();
        assertThat(resultado.totalElements()).isZero();
    }
}
