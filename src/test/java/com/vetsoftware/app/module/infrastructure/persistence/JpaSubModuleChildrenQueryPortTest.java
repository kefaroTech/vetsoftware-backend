package com.vetsoftware.app.module.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaSubModuleChildrenQueryPort")
class JpaSubModuleChildrenQueryPortTest {

    @Mock
    private SubModuleJpaRepository jpaRepository;

    private JpaSubModuleChildrenQueryPort port;

    @BeforeEach
    void crearAdaptador() {
        port = new JpaSubModuleChildrenQueryPort(jpaRepository);
    }

    @Nested
    @DisplayName("existsActiveByModuleId")
    class ExistsActiveByModuleId {

        @Test
        @DisplayName("delega en existsByModule_Id del repositorio Spring Data")
        void delega_en_exists_by_module_id() {
            when(jpaRepository.existsByModule_Id(1L)).thenReturn(true);

            assertThat(port.existsActiveByModuleId(1L)).isTrue();
        }

        @Test
        @DisplayName("sin sub-modulos activos devuelve false")
        void sin_hijos_activos_devuelve_false() {
            when(jpaRepository.existsByModule_Id(1L)).thenReturn(false);

            assertThat(port.existsActiveByModuleId(1L)).isFalse();
        }
    }
}
