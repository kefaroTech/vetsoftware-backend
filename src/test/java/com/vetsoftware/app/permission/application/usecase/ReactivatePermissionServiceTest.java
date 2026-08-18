package com.vetsoftware.app.permission.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.permission.application.dto.PermissionDto;
import com.vetsoftware.app.permission.application.port.out.PermissionRepository;
import com.vetsoftware.app.permission.domain.PermissionNotFoundException;
import com.vetsoftware.app.permission.testsupport.PermissionMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivatePermissionService")
class ReactivatePermissionServiceTest {

    private static final Long EMPRESA = PermissionMother.COMPANY_ID;
    private static final Long ID = PermissionMother.PERMISSION_ID;

    @Mock
    private PermissionRepository repository;

    @InjectMocks
    private ReactivatePermissionService service;

    @Nested
    @DisplayName("reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("reactiva y devuelve el permiso releido de esa empresa")
        void reactiva_y_devuelve_el_permiso_releido() {
            when(repository.reactivate(ID, EMPRESA)).thenReturn(1);
            when(repository.findByIdAndCompanyId(ID, EMPRESA))
                    .thenReturn(Optional.of(PermissionMother.permisoValido()));

            PermissionDto dto = service.execute(ID, EMPRESA);

            assertThat(dto.id()).isEqualTo(ID);
            assertThat(dto.enabled()).isTrue();
        }

        @Test
        @DisplayName("sin empresa seleccionada (SYSTEM puro) reactiva sin acotar")
        void sin_empresa_reactiva_sin_acotar() {
            when(repository.reactivate(ID)).thenReturn(1);
            when(repository.findById(ID)).thenReturn(Optional.of(PermissionMother.permisoValido()));

            assertThat(service.execute(ID, null).id()).isEqualTo(ID);

            verify(repository, never()).reactivate(anyLong(), anyLong());
        }
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenancy {

        @Test
        @DisplayName("un permiso de otra empresa no se reactiva ni se relee")
        void un_permiso_de_otra_empresa_no_se_reactiva() {
            when(repository.reactivate(ID, EMPRESA)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(ID, EMPRESA))
                    .isInstanceOf(PermissionNotFoundException.class)
                    .hasMessageContaining(String.valueOf(ID));

            verify(repository, never()).reactivate(anyLong());
            verify(repository, never()).findById(any());
            verify(repository, never()).findByIdAndCompanyId(any(), any());
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("ningun permiso afectado: no vuelve a leer")
        void ningun_permiso_afectado() {
            when(repository.reactivate(ID, EMPRESA)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(ID, EMPRESA))
                    .isInstanceOf(PermissionNotFoundException.class)
                    .hasMessageContaining(String.valueOf(ID));

            verify(repository, never()).findByIdAndCompanyId(ID, EMPRESA);
        }
    }
}
