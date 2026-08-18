package com.vetsoftware.app.submodule.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.membershipsubmodule.infrastructure.persistence.MembershipSubModuleJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaMembershipSubModuleChildrenQueryPort (submodule) — membershipSubModule activos de un submodulo")
class JpaMembershipSubModuleChildrenQueryPortTest {

    @Mock
    private MembershipSubModuleJpaRepository jpaRepository;
    @InjectMocks
    private JpaMembershipSubModuleChildrenQueryPort port;

    @Nested
    @DisplayName("consulta")
    class Consulta {

        @Test
        @DisplayName("delega en existsBySubModule_Id y devuelve true si hay hijos activos")
        void delega_y_devuelve_true_si_hay_hijos_activos() {
            when(jpaRepository.existsBySubModule_Id(7L)).thenReturn(true);

            assertThat(port.existsActiveBySubModuleId(7L)).isTrue();
        }

        @Test
        @DisplayName("devuelve false si no hay membershipSubModule activos de ese submodulo")
        void devuelve_false_si_no_hay_hijos_activos() {
            when(jpaRepository.existsBySubModule_Id(7L)).thenReturn(false);

            assertThat(port.existsActiveBySubModuleId(7L)).isFalse();
        }
    }
}
