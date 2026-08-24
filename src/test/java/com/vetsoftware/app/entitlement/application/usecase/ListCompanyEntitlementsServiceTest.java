package com.vetsoftware.app.entitlement.application.usecase;

import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.AHORA;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.COMPANY_ID;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.facturacion;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.permisoExistente;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.entitlement.application.dto.CompanyEntitlementDto;
import com.vetsoftware.app.entitlement.application.port.out.CompanyEntitlementRepository;
import com.vetsoftware.app.entitlement.domain.AccessLevel;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListCompanyEntitlementsService — el listado de auditoria")
class ListCompanyEntitlementsServiceTest {

    @Mock
    private CompanyEntitlementRepository repository;
    @InjectMocks
    private ListCompanyEntitlementsService service;

    @Test
    @DisplayName("conserva los metadatos de la consulta y no los recalcula sobre la pagina")
    void conserva_los_metadatos_de_la_consulta() {
        when(repository.findPageByCompanyId(COMPANY_ID, 2, 20)).thenReturn(PageResult
                .of(List.of(permisoExistente(facturacion(), AccessLevel.NONE)), 2, 20, 41L));

        PageResult<CompanyEntitlementDto> pagina = service.listByCompanyId(COMPANY_ID, 2, 20);

        assertThat(pagina.totalElements()).isEqualTo(41L);
        assertThat(pagina.totalPages()).isEqualTo(3);
        assertThat(pagina.content()).singleElement().satisfies(dto -> {
            assertThat(dto.accessLevel()).isEqualTo("NONE");
            assertThat(dto.subModule().code()).isEqualTo("BILLING");
            assertThat(dto.recalculatedAt()).isEqualTo(AHORA.minusDays(1));
        });
    }

    @Test
    @DisplayName("una empresa sin permisos devuelve una pagina vacia, no un error")
    void una_empresa_sin_permisos_devuelve_pagina_vacia() {
        when(repository.findPageByCompanyId(COMPANY_ID, 0, 20)).thenReturn(PageResult.empty(0, 20));

        assertThat(service.listByCompanyId(COMPANY_ID, 0, 20).content()).isEmpty();
    }
}
