package com.vetsoftware.app.rolepermission.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employeerole.infrastructure.persistence.EmployeeRoleJpaRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * El callback {@code afterCommit} anonimo NO necesita contexto de Spring:
 * {@code TransactionSynchronizationManager} es un ThreadLocal estatico, igual
 * que en {@code AfterCommitMetricRecorderTest}. Se arma la sincronizacion a
 * mano y se dispara el callback registrado para verificar que evict corre
 * despues del commit, no antes.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RolePermissionCacheAdapter — invalida la cache de permisos por rol")
class RolePermissionCacheAdapterTest {

    private static final Long ROLE_ID = 3L;

    @Mock
    private EmployeeRoleJpaRepository employeeRoleJpaRepository;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache cache;

    private RolePermissionCacheAdapter adapter;

    @org.junit.jupiter.api.BeforeEach
    void construirAdapter() {
        adapter = new RolePermissionCacheAdapter(employeeRoleJpaRepository, cacheManager);
    }

    @AfterEach
    void limpiarEstadoTransaccional() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    private static void iniciarTransaccion() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
    }

    @Nested
    @DisplayName("guardas de entrada")
    class Guardas {

        @Test
        @DisplayName("roleId nulo no toca ni el repositorio ni la cache")
        void role_id_nulo_no_toca_nada() {
            adapter.evictByRoleId(null);

            verifyNoInteractions(employeeRoleJpaRepository, cacheManager);
        }

        @Test
        @DisplayName("sin empleados asignados al rol no toca la cache")
        void sin_empleados_no_toca_la_cache() {
            when(employeeRoleJpaRepository.findEmployeeIdsByRoleId(ROLE_ID)).thenReturn(List.of());

            adapter.evictByRoleId(ROLE_ID);

            verifyNoInteractions(cacheManager);
        }
    }

    @Nested
    @DisplayName("sin transaccion activa — evict inmediato")
    class SinTransaccion {

        @Test
        @DisplayName("evita cada empleado del rol en el acto")
        void evict_inmediato_de_cada_empleado() {
            when(employeeRoleJpaRepository.findEmployeeIdsByRoleId(ROLE_ID))
                    .thenReturn(List.of(10L, 11L));
            when(cacheManager.getCache("employee-permissions")).thenReturn(cache);

            adapter.evictByRoleId(ROLE_ID);

            verify(cache).evict(10L);
            verify(cache).evict(11L);
        }

        @Test
        @DisplayName("si la cache con ese nombre no existe no revienta y no evita nada")
        void cache_inexistente_no_revienta() {
            when(employeeRoleJpaRepository.findEmployeeIdsByRoleId(ROLE_ID))
                    .thenReturn(List.of(10L));
            when(cacheManager.getCache("employee-permissions")).thenReturn(null);

            adapter.evictByRoleId(ROLE_ID);

            verify(cache, never()).evict(anyLong());
        }
    }

    @Nested
    @DisplayName("con transaccion activa — evict diferido a afterCommit")
    class ConTransaccion {

        @Test
        @DisplayName("no evita nada antes del commit; solo al disparar afterCommit")
        void no_evita_antes_del_commit() {
            when(employeeRoleJpaRepository.findEmployeeIdsByRoleId(ROLE_ID))
                    .thenReturn(List.of(10L, 11L));
            when(cacheManager.getCache("employee-permissions")).thenReturn(cache);
            iniciarTransaccion();

            adapter.evictByRoleId(ROLE_ID);

            verify(cache, never()).evict(anyLong());
            assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);

            verify(cache).evict(10L);
            verify(cache).evict(11L);
        }

        @Test
        @DisplayName("un rollback nunca dispara el evict")
        void rollback_no_dispara_el_evict() {
            when(employeeRoleJpaRepository.findEmployeeIdsByRoleId(ROLE_ID))
                    .thenReturn(List.of(10L));
            iniciarTransaccion();

            adapter.evictByRoleId(ROLE_ID);
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(synchronization -> synchronization
                            .afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

            verify(cacheManager, never()).getCache(org.mockito.ArgumentMatchers.anyString());
        }

        @Test
        @DisplayName("dos roles distintos registran cada uno su propia sincronizacion")
        void dos_roles_registran_dos_sincronizaciones() {
            when(employeeRoleJpaRepository.findEmployeeIdsByRoleId(ROLE_ID))
                    .thenReturn(List.of(10L));
            when(employeeRoleJpaRepository.findEmployeeIdsByRoleId(4L)).thenReturn(List.of(20L));
            iniciarTransaccion();

            adapter.evictByRoleId(ROLE_ID);
            adapter.evictByRoleId(4L);

            assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(2);
            verify(employeeRoleJpaRepository, times(1)).findEmployeeIdsByRoleId(ROLE_ID);
            verify(employeeRoleJpaRepository, times(1)).findEmployeeIdsByRoleId(4L);
        }
    }
}
