package com.vetsoftware.app.systemuserpermission.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.systempermission.infrastructure.persistence.SystemPermissionJpaEntity;
import com.vetsoftware.app.systempermission.infrastructure.persistence.SystemPermissionJpaRepository;
import com.vetsoftware.app.systemuserpermission.domain.SystemPermissionRef;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaSystemPermissionQueryPort")
class JpaSystemPermissionQueryPortTest {

    @Mock
    private SystemPermissionJpaRepository systemPermissionJpaRepository;
    @Mock
    private SystemPermissionJpaEntity entity;

    private JpaSystemPermissionQueryPort port;

    @BeforeEach
    void setUp() {
        port = new JpaSystemPermissionQueryPort(systemPermissionJpaRepository);
    }

    @Test
    @DisplayName("mapea la entidad encontrada a su ref")
    void mapea_la_entidad_encontrada_a_su_ref() {
        when(entity.getId()).thenReturn(8L);
        when(entity.getName()).thenReturn("Reportes");
        when(entity.getCode()).thenReturn("reports.manage");
        when(systemPermissionJpaRepository.findById(8L)).thenReturn(Optional.of(entity));

        Optional<SystemPermissionRef> ref = port.findById(8L);

        assertThat(ref).contains(new SystemPermissionRef(8L, "Reportes", "reports.manage"));
    }

    @Test
    @DisplayName("id inexistente devuelve vacio")
    void id_inexistente_devuelve_vacio() {
        when(systemPermissionJpaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThat(port.findById(999L)).isEmpty();
    }
}
