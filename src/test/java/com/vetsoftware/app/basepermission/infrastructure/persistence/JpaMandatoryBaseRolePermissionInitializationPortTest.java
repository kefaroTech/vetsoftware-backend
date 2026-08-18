package com.vetsoftware.app.basepermission.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.baserole.infrastructure.persistence.BaseRoleJpaEntity;
import com.vetsoftware.app.baserole.infrastructure.persistence.BaseRoleJpaRepository;
import com.vetsoftware.app.baserolepermission.infrastructure.persistence.BaseRolePermissionJpaEntity;
import com.vetsoftware.app.baserolepermission.infrastructure.persistence.BaseRolePermissionJpaRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Adaptador no estandar: no implementa un {@code Xxx<Algo>Repository} de la
 * propia feature sino que orquesta los repos Spring Data de {@code baserole} y
 * {@code baserolepermission} para sembrar el vinculo obligatorio de un permiso
 * base nuevo con cada rol mandatorio. Se prueba con JUnit + Mockito sobre los
 * tres repos Spring Data subyacentes, igual que un {@code JpaXxxQueryPort}.
 *
 * <p>
 * {@code BaseRoleJpaEntity} y {@code BasePermissionJpaEntity} se mockean porque
 * su constructor sin argumentos es {@code protected}. No tienen logica: son
 * portadores de datos.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaMandatoryBaseRolePermissionInitializationPort — vinculo con los roles mandatorios")
class JpaMandatoryBaseRolePermissionInitializationPortTest {

    @Mock
    private BaseRoleJpaRepository baseRoleJpaRepository;
    @Mock
    private BasePermissionJpaRepository basePermissionJpaRepository;
    @Mock
    private BaseRolePermissionJpaRepository baseRolePermissionJpaRepository;
    @InjectMocks
    private JpaMandatoryBaseRolePermissionInitializationPort port;

    @Mock
    private BaseRoleJpaEntity rolMandatorioA;
    @Mock
    private BaseRoleJpaEntity rolMandatorioB;
    @Mock
    private BasePermissionJpaEntity basePermissionEntity;

    @Nested
    @DisplayName("inicializacion")
    class Inicializacion {

        @Test
        @DisplayName("crea un vinculo por cada rol mandatorio que aun no lo tiene")
        void crea_un_vinculo_por_cada_rol_mandatorio_que_aun_no_lo_tiene() {
            when(rolMandatorioA.getId()).thenReturn(10L);
            when(rolMandatorioB.getId()).thenReturn(20L);
            when(baseRoleJpaRepository.findByMandatoryTrue())
                    .thenReturn(List.of(rolMandatorioA, rolMandatorioB));
            when(basePermissionJpaRepository.getReferenceById(5L)).thenReturn(basePermissionEntity);
            when(baseRolePermissionJpaRepository.existsByBaseRoleIdAndBasePermissionId(10L, 5L))
                    .thenReturn(false);
            when(baseRolePermissionJpaRepository.existsByBaseRoleIdAndBasePermissionId(20L, 5L))
                    .thenReturn(false);

            port.initializeForMandatoryBaseRoles(5L);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<BaseRolePermissionJpaEntity>> captor = ArgumentCaptor
                    .forClass(List.class);
            verify(baseRolePermissionJpaRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(2);
            assertThat(captor.getValue()).extracting(BaseRolePermissionJpaEntity::getBaseRole)
                    .containsExactly(rolMandatorioA, rolMandatorioB);
            assertThat(captor.getValue())
                    .allSatisfy(entity -> assertThat(entity.getBasePermission())
                            .isSameAs(basePermissionEntity));
        }

        @Test
        @DisplayName("solo crea vinculos para los roles mandatorios que aun no lo tienen")
        void solo_crea_vinculos_para_los_roles_que_faltan() {
            when(rolMandatorioA.getId()).thenReturn(10L);
            when(rolMandatorioB.getId()).thenReturn(20L);
            when(baseRoleJpaRepository.findByMandatoryTrue())
                    .thenReturn(List.of(rolMandatorioA, rolMandatorioB));
            when(basePermissionJpaRepository.getReferenceById(5L)).thenReturn(basePermissionEntity);
            when(baseRolePermissionJpaRepository.existsByBaseRoleIdAndBasePermissionId(10L, 5L))
                    .thenReturn(true);
            when(baseRolePermissionJpaRepository.existsByBaseRoleIdAndBasePermissionId(20L, 5L))
                    .thenReturn(false);

            port.initializeForMandatoryBaseRoles(5L);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<BaseRolePermissionJpaEntity>> captor = ArgumentCaptor
                    .forClass(List.class);
            verify(baseRolePermissionJpaRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(1);
            assertThat(captor.getValue().get(0).getBaseRole()).isSameAs(rolMandatorioB);
        }
    }
}
