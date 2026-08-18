package com.vetsoftware.app.rolepermission.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.permission.infrastructure.persistence.PermissionJpaEntity;
import com.vetsoftware.app.permission.infrastructure.persistence.PermissionJpaRepository;
import com.vetsoftware.app.rolepermission.domain.PermissionRef;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaPermissionQueryPort — resuelve el PermissionRef desde el repositorio Spring Data")
class JpaPermissionQueryPortTest {

    private static final Long COMPANY_ID = 9L;

    @Mock
    private PermissionJpaRepository permissionJpaRepository;

    @InjectMocks
    private JpaPermissionQueryPort port;

    private static PermissionJpaEntity entidad(Long id, String name, String code) {
        PermissionJpaEntity e = mock(PermissionJpaEntity.class);
        when(e.getId()).thenReturn(id);
        when(e.getName()).thenReturn(name);
        when(e.getCode()).thenReturn(code);
        return e;
    }

    @Nested
    @DisplayName("findById — sin filtro de empresa")
    class FindById {

        @Test
        @DisplayName("mapea la fila encontrada al PermissionRef")
        void mapea_la_fila_encontrada() {
            PermissionJpaEntity e = entidad(7L, "Ver animales", "ANIMAL_READ");
            when(permissionJpaRepository.findById(7L)).thenReturn(Optional.of(e));

            assertThat(port.findById(7L))
                    .contains(new PermissionRef(7L, "Ver animales", "ANIMAL_READ"));
        }

        @Test
        @DisplayName("vacio si el permiso no existe")
        void vacio_si_no_existe() {
            when(permissionJpaRepository.findById(7L)).thenReturn(Optional.empty());

            assertThat(port.findById(7L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByIdAndCompanyId — con filtro de empresa")
    class FindByIdAndCompanyId {

        @Test
        @DisplayName("mapea la fila encontrada al PermissionRef")
        void mapea_la_fila_encontrada() {
            PermissionJpaEntity e = entidad(7L, "Ver animales", "ANIMAL_READ");
            when(permissionJpaRepository.findByIdAndCompany_Id(7L, COMPANY_ID))
                    .thenReturn(Optional.of(e));

            assertThat(port.findByIdAndCompanyId(7L, COMPANY_ID))
                    .contains(new PermissionRef(7L, "Ver animales", "ANIMAL_READ"));
        }

        @Test
        @DisplayName("vacio si el permiso es de otra empresa")
        void vacio_si_es_de_otra_empresa() {
            when(permissionJpaRepository.findByIdAndCompany_Id(7L, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThat(port.findByIdAndCompanyId(7L, COMPANY_ID)).isEmpty();
        }
    }
}
