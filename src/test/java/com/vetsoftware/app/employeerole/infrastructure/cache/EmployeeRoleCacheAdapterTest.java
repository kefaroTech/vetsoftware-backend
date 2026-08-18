package com.vetsoftware.app.employeerole.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * {@link TransactionSynchronizationManager} es una utilidad estatica de Spring
 * usable en JUnit puro: no hace falta {@code @SpringBootTest} ni una
 * transaccion real para registrar una sincronizacion e invocar su callback a
 * mano. Mismo patron que {@code EmployeeBranchCacheAdapterTest}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeRoleCacheAdapter")
class EmployeeRoleCacheAdapterTest {

    // Nombre del cache Redis: privado en produccion, se repite aqui a proposito
    // (no se expone solo para que el test pueda leerlo).
    private static final String CACHE_NAME = "employee-permissions";
    private static final Long EMPLOYEE_ID = 7L;

    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache cache;

    @InjectMocks
    private EmployeeRoleCacheAdapter adapter;

    @Nested
    @DisplayName("sin transaccion activa")
    class SinTransaccionActiva {

        @Test
        @DisplayName("evita de inmediato, sin esperar a ningun commit")
        void evita_de_inmediato() {
            when(cacheManager.getCache(CACHE_NAME)).thenReturn(cache);

            adapter.evictByEmployeeId(EMPLOYEE_ID);

            verify(cache).evict(EMPLOYEE_ID);
        }

        @Test
        @DisplayName("si el cache todavia no existe, no lanza ni intenta evictar")
        void no_lanza_si_el_cache_no_existe() {
            when(cacheManager.getCache(CACHE_NAME)).thenReturn(null);

            adapter.evictByEmployeeId(EMPLOYEE_ID);

            verifyNoInteractions(cache);
        }

        @Test
        @DisplayName("un employeeId null ni siquiera consulta el cache manager")
        void ignora_employee_id_null() {
            adapter.evictByEmployeeId(null);

            verifyNoInteractions(cacheManager, cache);
        }
    }

    @Nested
    @DisplayName("con transaccion activa")
    class ConTransaccionActiva {

        @BeforeEach
        void iniciarSincronizacion() {
            TransactionSynchronizationManager.initSynchronization();
        }

        @AfterEach
        void limpiarSincronizacion() {
            TransactionSynchronizationManager.clearSynchronization();
        }

        @Test
        @DisplayName("difiere la evicion hasta que el synchronization notifica el commit")
        void difiere_la_evicion_hasta_el_commit() {
            when(cacheManager.getCache(CACHE_NAME)).thenReturn(cache);

            adapter.evictByEmployeeId(EMPLOYEE_ID);

            // Registrada pero todavia no ejecutada: el rollback la dejaria sin efecto.
            verifyNoInteractions(cache);
            List<TransactionSynchronization> registradas = TransactionSynchronizationManager
                    .getSynchronizations();
            assertThat(registradas).hasSize(1);

            registradas.get(0).afterCommit();

            verify(cache).evict(EMPLOYEE_ID);
        }
    }
}
