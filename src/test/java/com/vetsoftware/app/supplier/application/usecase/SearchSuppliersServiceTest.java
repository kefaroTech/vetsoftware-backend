package com.vetsoftware.app.supplier.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.supplier.application.command.SearchSuppliersCommand;
import com.vetsoftware.app.supplier.application.dto.SupplierDto;
import com.vetsoftware.app.supplier.application.port.out.SupplierRepository;
import com.vetsoftware.app.supplier.domain.CompanyRef;
import com.vetsoftware.app.supplier.domain.Supplier;
import com.vetsoftware.app.supplier.testsupport.SupplierMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchSuppliersService")
class SearchSuppliersServiceTest {

    private static final Long COMPANY_ID = 10L;
    private static final CompanyRef CLINICA = SupplierMother.empresa(COMPANY_ID, "Clinica Norte",
            "900123456");

    @Mock
    private SupplierRepository repository;

    @InjectMocks
    private SearchSuppliersService service;

    @Test
    @DisplayName("traslada el command al repositorio y mapea cada fila de la pagina a su dto")
    void traslada_el_command_y_mapea_cada_fila_a_su_dto() {
        SearchSuppliersCommand command = new SearchSuppliersCommand(COMPANY_ID, "sur", null, 0, 20);
        Supplier sur = SupplierMother.completo("Distribuidora Sur", CLINICA);
        when(repository.search(command)).thenReturn(new PageResult<>(List.of(sur), 0, 20, 1L, 1));

        PageResult<SupplierDto> pagina = service.execute(command);

        assertThat(pagina.content()).extracting(SupplierDto::name)
                .containsExactly("Distribuidora Sur");
        assertThat(pagina.totalElements()).isEqualTo(1L);
        assertThat(pagina.page()).isZero();
        assertThat(pagina.pageSize()).isEqualTo(20);
        assertThat(pagina.totalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("una busqueda sin coincidencias devuelve una pagina vacia")
    void una_busqueda_sin_coincidencias_devuelve_pagina_vacia() {
        SearchSuppliersCommand command = new SearchSuppliersCommand(COMPANY_ID, "inexistente", null,
                0, 20);
        when(repository.search(command)).thenReturn(new PageResult<>(List.of(), 0, 20, 0L, 0));

        PageResult<SupplierDto> pagina = service.execute(command);

        assertThat(pagina.content()).isEmpty();
        assertThat(pagina.totalElements()).isZero();
    }
}
