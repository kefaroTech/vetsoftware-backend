package com.vetsoftware.app.accountmapping.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.accountmapping.application.dto.AccountMappingDto;
import com.vetsoftware.app.accountmapping.application.port.out.AccountMappingRepository;
import com.vetsoftware.app.accountmapping.domain.AccountMappingNotFoundException;
import com.vetsoftware.app.accountmapping.testsupport.AccountMappingMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindAccountMappingService")
class FindAccountMappingServiceTest {

    @Mock
    private AccountMappingRepository repository;

    @InjectMocks
    private FindAccountMappingService service;

    @Test
    @DisplayName("devuelve el DTO cuando el mapeo existe")
    void devuelve_el_dto_cuando_existe() {
        when(repository.findById(AccountMappingMother.MAPPING_ID))
                .thenReturn(Optional.of(AccountMappingMother.mapeoBancoAbierto()));

        AccountMappingDto dto = service.findById(AccountMappingMother.MAPPING_ID);

        assertThat(dto.mappingKey()).isEqualTo(AccountMappingMother.MAPPING_KEY);
    }

    @Test
    @DisplayName("lanza cuando el mapeo no existe")
    void lanza_cuando_no_existe() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L))
                .isInstanceOf(AccountMappingNotFoundException.class)
                .hasMessageContaining("Account mapping not found: 999");
    }
}
