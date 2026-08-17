package com.vetsoftware.app.servicechargeopenaccount.application.usecase;

import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.COMPANY_ID;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.OTRA_COMPANY_ID;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.cargo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.servicechargeopenaccount.application.dto.ServiceChargeOpenAccountDto;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.ServiceChargeOpenAccountRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListServiceChargeOpenAccountsService — listado paginado")
class ListServiceChargeOpenAccountsServiceTest {

    @Mock
    private ServiceChargeOpenAccountRepository repository;

    @InjectMocks
    private ListServiceChargeOpenAccountsService service;

    @Test
    @DisplayName("proyecta la pagina del repositorio conservando sus metadatos")
    void proyecta_la_pagina_conservando_metadatos() {
        when(repository.findAllByCompanyId(COMPANY_ID, 1, 20))
                .thenReturn(new PageResult<>(List.of(cargo(1L), cargo(2L)), 1, 20, 42L, 3));

        PageResult<ServiceChargeOpenAccountDto> pagina = service.listAll(COMPANY_ID, 1, 20);

        assertThat(pagina.content()).extracting(ServiceChargeOpenAccountDto::id).containsExactly(1L,
                2L);
        assertThat(pagina.page()).isEqualTo(1);
        assertThat(pagina.pageSize()).isEqualTo(20);
        assertThat(pagina.totalElements()).isEqualTo(42L);
        assertThat(pagina.totalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("traslada la paginacion pedida tal cual al repositorio")
    void traslada_la_paginacion_pedida_tal_cual() {
        when(repository.findAllByCompanyId(COMPANY_ID, 7, 5))
                .thenReturn(new PageResult<>(List.of(), 7, 5, 0L, 0));

        // Si el service reinterpreta page/pageSize, el front pagina sobre otra cosa
        // distinta de la que cree.
        assertThat(service.listAll(COMPANY_ID, 7, 5).page()).isEqualTo(7);
    }

    @Test
    @DisplayName("una empresa sin cargos devuelve una pagina vacia, no null")
    void una_empresa_sin_cargos_devuelve_pagina_vacia() {
        when(repository.findAllByCompanyId(OTRA_COMPANY_ID, 0, 20))
                .thenReturn(new PageResult<>(List.of(), 0, 20, 0L, 0));

        assertThat(service.listAll(OTRA_COMPANY_ID, 0, 20).content()).isEmpty();
    }
}
