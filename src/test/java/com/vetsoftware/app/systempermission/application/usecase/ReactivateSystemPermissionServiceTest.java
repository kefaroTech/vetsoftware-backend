package com.vetsoftware.app.systempermission.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.systempermission.application.dto.SystemPermissionDto;
import com.vetsoftware.app.systempermission.application.port.out.SystemPermissionRepository;
import com.vetsoftware.app.systempermission.domain.SystemPermissionNotFoundException;
import com.vetsoftware.app.systempermission.testsupport.SystemPermissionMother;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivateSystemPermissionService")
class ReactivateSystemPermissionServiceTest {

    @Mock
    private SystemPermissionRepository repository;

    private ReactivateSystemPermissionService service;

    @BeforeEach
    void crearServicio() {
        service = new ReactivateSystemPermissionService(repository);
    }

    @Nested
    @DisplayName("reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("reactiva y devuelve el permiso releido")
        void reactiva_y_devuelve_el_permiso_releido() {
            when(repository.reactivate(1L)).thenReturn(1);
            when(repository.findById(1L))
                    .thenReturn(Optional.of(SystemPermissionMother.permisoValido()));

            SystemPermissionDto dto = service.execute(1L);

            assertThat(dto.id()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("no relee si la actualizacion no afecta ninguna fila")
        void no_relee_si_no_afecta_ninguna_fila() {
            when(repository.reactivate(1L)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(1L))
                    .isInstanceOf(SystemPermissionNotFoundException.class)
                    .hasMessageContaining("1");

            verify(repository).reactivate(1L);
            verifyNoMoreInteractions(repository);
        }
    }
}
