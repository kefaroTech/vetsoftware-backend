package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaEntity;
import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Resolución de la sede emisora de una venta POS. Una venta NO se emite desde una sede fuera de operación:
 * toda rama exige ACTIVA. Cascada: branchId del request (activo y de la empresa); si no viene, la "Principal"
 * activa; si no, la primera sede activa; si no hay ninguna activa, vacío (el builder lanza). Nunca cae a una
 * sede de otra empresa ni a una inactiva.
 */
@ExtendWith(MockitoExtension.class)
class JpaBranchResolverTest {

    @Mock private BranchJpaRepository branchJpaRepository;
    @InjectMocks private JpaBranchResolver resolver;

    private static final long COMPANY = 9L;

    /** Entidad activa (para el camino de branchId explícito, que filtra por isActive() y luego lee el id). */
    private static BranchJpaEntity activeEntity(long id) {
        BranchJpaEntity e = mock(BranchJpaEntity.class);
        when(e.isActive()).thenReturn(true);
        when(e.getId()).thenReturn(id);
        return e;
    }

    /** Entidad inactiva: el filtro isActive() la descarta antes de leer el id. */
    private static BranchJpaEntity inactiveEntity() {
        BranchJpaEntity e = mock(BranchJpaEntity.class);
        when(e.isActive()).thenReturn(false);
        return e;
    }

    /** Entidad de los caminos por defecto (la query ya filtra active), solo se lee el id. */
    private static BranchJpaEntity idEntity(long id) {
        BranchJpaEntity e = mock(BranchJpaEntity.class);
        when(e.getId()).thenReturn(id);
        return e;
    }

    @Test
    void resuelve_la_sede_solicitada_activa_y_de_la_empresa() {
        BranchJpaEntity activa = activeEntity(11L);
        when(branchJpaRepository.findByIdAndCompanyId(11L, COMPANY)).thenReturn(Optional.of(activa));

        assertThat(resolver.resolve(COMPANY, 11L)).contains(11L);
        verify(branchJpaRepository, never()).findFirstByCompany_IdAndCodeIgnoreCaseAndActiveTrue(any(), any());
    }

    @Test
    void rechaza_la_sede_solicitada_INACTIVA_devolviendo_vacio() {
        BranchJpaEntity inactiva = inactiveEntity();
        when(branchJpaRepository.findByIdAndCompanyId(11L, COMPANY)).thenReturn(Optional.of(inactiva));

        assertThat(resolver.resolve(COMPANY, 11L))
            .as("no se emite POS desde una sede desactivada").isEmpty();
    }

    @Test
    void devuelve_vacio_si_la_sede_solicitada_no_pertenece_a_la_empresa() {
        when(branchJpaRepository.findByIdAndCompanyId(11L, COMPANY)).thenReturn(Optional.empty());

        assertThat(resolver.resolve(COMPANY, 11L)).isEmpty();
    }

    @Test
    void sin_branchId_cae_a_la_principal_activa() {
        BranchJpaEntity principal = idEntity(1L);
        when(branchJpaRepository.findFirstByCompany_IdAndCodeIgnoreCaseAndActiveTrue(COMPANY, "PRINCIPAL"))
            .thenReturn(Optional.of(principal));

        assertThat(resolver.resolve(COMPANY, null)).contains(1L);
        verify(branchJpaRepository, never()).findFirstByCompany_IdAndActiveTrueOrderByIdAsc(any());
    }

    @Test
    void sin_principal_activa_cae_a_la_primera_sede_activa() {
        when(branchJpaRepository.findFirstByCompany_IdAndCodeIgnoreCaseAndActiveTrue(COMPANY, "PRINCIPAL"))
            .thenReturn(Optional.empty());
        BranchJpaEntity primeraActiva = idEntity(5L);
        when(branchJpaRepository.findFirstByCompany_IdAndActiveTrueOrderByIdAsc(COMPANY))
            .thenReturn(Optional.of(primeraActiva));

        assertThat(resolver.resolve(COMPANY, null)).contains(5L);
    }

    @Test
    void devuelve_vacio_si_la_empresa_no_tiene_ninguna_sede_activa() {
        when(branchJpaRepository.findFirstByCompany_IdAndCodeIgnoreCaseAndActiveTrue(COMPANY, "PRINCIPAL"))
            .thenReturn(Optional.empty());
        when(branchJpaRepository.findFirstByCompany_IdAndActiveTrueOrderByIdAsc(COMPANY))
            .thenReturn(Optional.empty());

        assertThat(resolver.resolve(COMPANY, null)).isEmpty();
    }
}
