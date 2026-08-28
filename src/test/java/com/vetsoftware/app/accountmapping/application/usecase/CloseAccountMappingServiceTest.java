package com.vetsoftware.app.accountmapping.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.accountmapping.application.dto.AccountMappingDto;
import com.vetsoftware.app.accountmapping.application.port.out.AccountMappingRepository;
import com.vetsoftware.app.accountmapping.domain.AccountMapping;
import com.vetsoftware.app.accountmapping.domain.AccountMappingAlreadyClosedException;
import com.vetsoftware.app.accountmapping.domain.AccountMappingNotFoundException;
import com.vetsoftware.app.accountmapping.testsupport.AccountMappingMother;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CloseAccountMappingService")
class CloseAccountMappingServiceTest {

    @Mock
    private AccountMappingRepository repository;

    @Captor
    private ArgumentCaptor<AccountMapping> mappingCaptor;

    @InjectMocks
    private CloseAccountMappingService service;

    @Nested
    @DisplayName("mapeo abierto")
    class MapeoAbierto {

        @Test
        @DisplayName("guarda el mapeo con la fecha de fin del comando, conservando la version")
        void guarda_con_la_fecha_de_fin_del_comando() {
            AccountMapping abierto = AccountMappingMother.mapeoBancoAbierto();
            when(repository.findById(AccountMappingMother.MAPPING_ID))
                    .thenReturn(Optional.of(abierto));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            AccountMappingDto dto = service.execute(AccountMappingMother
                    .comandoCerrar(AccountMappingMother.MAPPING_ID, LocalDate.of(2026, 6, 1)));

            verify(repository).save(mappingCaptor.capture());
            assertThat(mappingCaptor.getValue().getValidTo()).isEqualTo(LocalDate.of(2026, 6, 1));
            // La version viaja intacta: es la barandilla del ciclo leer-modificar-guardar
            // con bloqueo optimista.
            assertThat(mappingCaptor.getValue().getVersion()).isEqualTo(abierto.getVersion());
            assertThat(dto.validTo()).isEqualTo(LocalDate.of(2026, 6, 1));
        }
    }

    @Nested
    @DisplayName("mapeos que no admiten el cierre")
    class NoAdmitenCierre {

        @Test
        @DisplayName("mapeo inexistente: lanza y no guarda")
        void mapeo_inexistente() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service
                    .execute(AccountMappingMother.comandoCerrar(999L, LocalDate.of(2026, 6, 1))))
                    .isInstanceOf(AccountMappingNotFoundException.class)
                    .hasMessageContaining("Account mapping not found: 999");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("mapeo ya cerrado: el dominio lo rechaza y el service no llega a guardar")
        void mapeo_ya_cerrado() {
            AccountMapping cerrado = AccountMappingMother
                    .mapeoBancoCerrado(LocalDate.of(2026, 3, 1));
            when(repository.findById(AccountMappingMother.MAPPING_ID))
                    .thenReturn(Optional.of(cerrado));

            assertThatThrownBy(() -> service.execute(AccountMappingMother
                    .comandoCerrar(AccountMappingMother.MAPPING_ID, LocalDate.of(2026, 6, 1))))
                    .isInstanceOf(AccountMappingAlreadyClosedException.class)
                    .hasMessageContaining("already closed since 2026-03-01");

            verify(repository, never()).save(any());
        }
    }
}
