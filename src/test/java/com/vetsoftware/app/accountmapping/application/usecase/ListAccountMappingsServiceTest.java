package com.vetsoftware.app.accountmapping.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.accountmapping.application.dto.AccountMappingDto;
import com.vetsoftware.app.accountmapping.application.port.out.AccountMappingRepository;
import com.vetsoftware.app.accountmapping.domain.AccountMapping;
import com.vetsoftware.app.accountmapping.testsupport.AccountMappingMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListAccountMappingsService")
class ListAccountMappingsServiceTest {

    @Mock
    private AccountMappingRepository repository;

    @InjectMocks
    private ListAccountMappingsService service;

    @Test
    @DisplayName("traduce la pagina de dominio a una pagina de DTOs sin recalcular los totales")
    void traduce_la_pagina_de_dominio_a_dtos() {
        List<AccountMapping> contenido = List.of(AccountMappingMother.mapeoBancoAbierto());
        PageResult<AccountMapping> pagina = PageResult.of(contenido, 0, 20, 42L);
        when(repository.findAllEnabled(0, 20)).thenReturn(pagina);

        PageResult<AccountMappingDto> resultado = service.listAll(0, 20);

        verify(repository).findAllEnabled(0, 20);
        assertThat(resultado.content()).hasSize(1);
        assertThat(resultado.content().get(0).mappingKey())
                .isEqualTo(AccountMappingMother.MAPPING_KEY);
        // El total sale de la consulta, no de contar el contenido de esta pagina.
        assertThat(resultado.totalElements()).isEqualTo(42L);
    }
}
