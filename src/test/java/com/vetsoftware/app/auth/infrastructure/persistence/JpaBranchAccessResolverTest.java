package com.vetsoftware.app.auth.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employeebranch.infrastructure.persistence.EmployeeBranchJpaRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@code findBranchIdsByEmployeeId} es una proyección directa a
 * {@code List<Long>}: no hay entidad JPA que doblar.
 */
@ExtendWith(MockitoExtension.class)
class JpaBranchAccessResolverTest {

    @Mock
    private EmployeeBranchJpaRepository employeeBranchJpaRepository;
    @InjectMocks
    private JpaBranchAccessResolver resolver;

    @Nested
    @DisplayName("resolveFor")
    class Resolver {

        @Test
        @DisplayName("devuelve el conjunto de sedes asignadas al empleado")
        void devuelve_el_conjunto_de_sedes() {
            when(employeeBranchJpaRepository.findBranchIdsByEmployeeId(7L))
                    .thenReturn(List.of(10L, 20L));

            assertThat(resolver.resolveFor(7L)).containsExactlyInAnyOrder(10L, 20L);
        }

        @Test
        @DisplayName("un empleado sin sedes asignadas devuelve un conjunto vacío")
        void sin_sedes_devuelve_vacio() {
            when(employeeBranchJpaRepository.findBranchIdsByEmployeeId(7L)).thenReturn(List.of());

            assertThat(resolver.resolveFor(7L)).isEqualTo(Set.of());
        }
    }

    @Test
    @DisplayName("evict no lanza: su efecto es la anotación @CacheEvict")
    void evict_no_lanza() {
        resolver.evict(7L);
    }
}
