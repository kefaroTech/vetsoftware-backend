package com.vetsoftware.app.rolepermission.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaEntity;
import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaRepository;
import com.vetsoftware.app.rolepermission.domain.RoleRef;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaRoleQueryPort (rolepermission) — resuelve el RoleRef desde el repositorio Spring Data")
class JpaRoleQueryPortTest {

    private static final Long COMPANY_ID = 9L;

    @Mock
    private RoleJpaRepository roleJpaRepository;

    @InjectMocks
    private JpaRoleQueryPort port;

    private static RoleJpaEntity entidad(Long id, String name, String code) {
        RoleJpaEntity e = mock(RoleJpaEntity.class);
        when(e.getId()).thenReturn(id);
        when(e.getName()).thenReturn(name);
        when(e.getCode()).thenReturn(code);
        return e;
    }

    @Nested
    @DisplayName("findById — sin filtro de empresa")
    class FindById {

        @Test
        @DisplayName("mapea la fila encontrada al RoleRef")
        void mapea_la_fila_encontrada() {
            RoleJpaEntity e = entidad(3L, "Veterinario", "VET");
            when(roleJpaRepository.findById(3L)).thenReturn(Optional.of(e));

            assertThat(port.findById(3L)).contains(new RoleRef(3L, "Veterinario", "VET"));
        }

        @Test
        @DisplayName("vacio si el rol no existe")
        void vacio_si_no_existe() {
            when(roleJpaRepository.findById(3L)).thenReturn(Optional.empty());

            assertThat(port.findById(3L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByIdAndCompanyId — con filtro de empresa")
    class FindByIdAndCompanyId {

        @Test
        @DisplayName("mapea la fila encontrada al RoleRef")
        void mapea_la_fila_encontrada() {
            RoleJpaEntity e = entidad(3L, "Veterinario", "VET");
            when(roleJpaRepository.findByIdAndCompany_Id(3L, COMPANY_ID))
                    .thenReturn(Optional.of(e));

            assertThat(port.findByIdAndCompanyId(3L, COMPANY_ID))
                    .contains(new RoleRef(3L, "Veterinario", "VET"));
        }

        @Test
        @DisplayName("vacio si el rol es de otra empresa")
        void vacio_si_es_de_otra_empresa() {
            when(roleJpaRepository.findByIdAndCompany_Id(3L, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThat(port.findByIdAndCompanyId(3L, COMPANY_ID)).isEmpty();
        }
    }
}
