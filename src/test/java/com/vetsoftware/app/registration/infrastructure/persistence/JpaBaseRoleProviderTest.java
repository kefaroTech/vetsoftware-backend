package com.vetsoftware.app.registration.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.baserole.infrastructure.persistence.BaseRoleJpaEntity;
import com.vetsoftware.app.baserole.infrastructure.persistence.BaseRoleJpaRepository;
import com.vetsoftware.app.registration.application.port.out.BaseRoleProvider.BaseRoleData;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaBaseRoleProvider")
class JpaBaseRoleProviderTest {

    @Mock
    private BaseRoleJpaRepository jpaRepository;
    @InjectMocks
    private JpaBaseRoleProvider provider;

    // BaseRoleJpaEntity tiene constructor protegido (otra feature): se dobla en vez
    // de
    // construirse, igual que hace getReferenceById() en producción.
    private static BaseRoleJpaEntity fila(Long id, String name, String code, Boolean mandatory) {
        BaseRoleJpaEntity entity = mock(BaseRoleJpaEntity.class);
        when(entity.getId()).thenReturn(id);
        when(entity.getName()).thenReturn(name);
        when(entity.getCode()).thenReturn(code);
        when(entity.getMandatory()).thenReturn(mandatory);
        return entity;
    }

    @Test
    @DisplayName("mapea cada fila a BaseRoleData conservando el flag mandatory")
    void mapea_cada_fila_conservando_mandatory() {
        // Los dos mocks de fila() se resuelven en variables ANTES de stubear findAll():
        // construir la
        // lista dentro del propio thenReturn() intercalaría el when(...) interno de
        // fila() con este
        // when(...).thenReturn(...) todavía abierto, y Mockito lo reporta como stubbing
        // sin terminar.
        BaseRoleJpaEntity admin = fila(1L, "Administrador", "ADMIN", true);
        BaseRoleJpaEntity vet = fila(2L, "Veterinario", "VET", false);
        when(jpaRepository.findAll()).thenReturn(List.of(admin, vet));

        List<BaseRoleData> resultado = provider.findAll();

        assertThat(resultado).containsExactly(new BaseRoleData(1L, "Administrador", "ADMIN", true),
                new BaseRoleData(2L, "Veterinario", "VET", false));
    }

    @Test
    @DisplayName("mandatory nulo se traduce a false, nunca a NullPointerException")
    void mandatory_nulo_se_traduce_a_false() {
        BaseRoleJpaEntity recepcion = fila(3L, "Recepción", "RECEP", null);
        when(jpaRepository.findAll()).thenReturn(List.of(recepcion));

        List<BaseRoleData> resultado = provider.findAll();

        assertThat(resultado).containsExactly(new BaseRoleData(3L, "Recepción", "RECEP", false));
    }

    @Test
    @DisplayName("sin base roles habilitados devuelve una lista vacía")
    void sin_base_roles_devuelve_lista_vacia() {
        when(jpaRepository.findAll()).thenReturn(List.of());

        assertThat(provider.findAll()).isEmpty();
    }
}
