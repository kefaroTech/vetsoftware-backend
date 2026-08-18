package com.vetsoftware.app.permission.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.permission.domain.SubModuleRef;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaEntity;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaSubModuleQueryPort (permission)")
class JpaSubModuleQueryPortTest {

    @Mock
    private SubModuleJpaRepository subModuleJpaRepository;
    @Mock
    private SubModuleJpaEntity subModuleEntity;

    @InjectMocks
    private JpaSubModuleQueryPort port;

    @Test
    @DisplayName("submodulo existente se mapea a SubModuleRef")
    void submodulo_existente_se_mapea() {
        when(subModuleEntity.getId()).thenReturn(5L);
        when(subModuleEntity.getName()).thenReturn("Inventario");
        when(subModuleEntity.getCode()).thenReturn("INV");
        when(subModuleJpaRepository.findById(5L)).thenReturn(Optional.of(subModuleEntity));

        Optional<SubModuleRef> ref = port.findById(5L);

        assertThat(ref).contains(new SubModuleRef(5L, "Inventario", "INV"));
    }

    @Test
    @DisplayName("submodulo inexistente devuelve vacio")
    void submodulo_inexistente_devuelve_vacio() {
        when(subModuleJpaRepository.findById(5L)).thenReturn(Optional.empty());

        assertThat(port.findById(5L)).isEmpty();
    }
}
