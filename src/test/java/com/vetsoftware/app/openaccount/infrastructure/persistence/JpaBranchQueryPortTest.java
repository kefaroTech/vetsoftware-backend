package com.vetsoftware.app.openaccount.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaEntity;
import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaRepository;
import com.vetsoftware.app.openaccount.domain.BranchRef;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Adapter de resolución de sede para cuentas abiertas. Espeja al de citas:
 * filtro {@code
 * isActive()} en el camino explícito y cascada Principal-activa→primera-activa
 * en el camino por defecto. Verifica que una sede inactiva no se abra como
 * cuenta.
 */
@ExtendWith(MockitoExtension.class)
class JpaBranchQueryPortTest {

    @Mock
    private BranchJpaRepository branchJpaRepository;
    @InjectMocks
    private JpaBranchQueryPort port;

    private static final long COMPANY = 9L;

    private static BranchJpaEntity activeFound(long id, String name, String code) {
        BranchJpaEntity e = mock(BranchJpaEntity.class);
        when(e.isActive()).thenReturn(true);
        when(e.getId()).thenReturn(id);
        when(e.getName()).thenReturn(name);
        when(e.getCode()).thenReturn(code);
        return e;
    }

    private static BranchJpaEntity inactive() {
        BranchJpaEntity e = mock(BranchJpaEntity.class);
        when(e.isActive()).thenReturn(false);
        return e;
    }

    private static BranchJpaEntity mapped(long id, String name, String code) {
        BranchJpaEntity e = mock(BranchJpaEntity.class);
        when(e.getId()).thenReturn(id);
        when(e.getName()).thenReturn(name);
        when(e.getCode()).thenReturn(code);
        return e;
    }

    @Test
    void findActive_mapea_la_sede_cuando_esta_activa() {
        BranchJpaEntity activa = activeFound(11L, "Sede Norte", "NORTE");
        when(branchJpaRepository.findByIdAndCompanyId(11L, COMPANY))
                .thenReturn(Optional.of(activa));

        assertThat(port.findActiveByIdAndCompanyId(11L, COMPANY))
                .contains(new BranchRef(11L, "Sede Norte", "NORTE"));
    }

    @Test
    void findActive_descarta_la_sede_inactiva() {
        BranchJpaEntity inactiva = inactive();
        when(branchJpaRepository.findByIdAndCompanyId(11L, COMPANY))
                .thenReturn(Optional.of(inactiva));

        assertThat(port.findActiveByIdAndCompanyId(11L, COMPANY)).isEmpty();
    }

    @Test
    void findActive_vacio_si_no_existe() {
        when(branchJpaRepository.findByIdAndCompanyId(11L, COMPANY)).thenReturn(Optional.empty());

        assertThat(port.findActiveByIdAndCompanyId(11L, COMPANY)).isEmpty();
    }

    @Test
    void exists_delega_sin_filtrar_por_active() {
        when(branchJpaRepository.existsByIdAndCompany_Id(11L, COMPANY)).thenReturn(true);

        assertThat(port.existsByIdAndCompanyId(11L, COMPANY)).isTrue();
    }

    @Test
    void findDefault_usa_la_principal_activa_sin_consultar_el_fallback() {
        BranchJpaEntity principal = mapped(1L, "Principal", "PRINCIPAL");
        when(branchJpaRepository.findFirstByCompany_IdAndCodeIgnoreCaseAndActiveTrue(COMPANY,
                "PRINCIPAL")).thenReturn(Optional.of(principal));

        assertThat(port.findDefaultActiveByCompanyId(COMPANY))
                .contains(new BranchRef(1L, "Principal", "PRINCIPAL"));
        verify(branchJpaRepository, never()).findFirstByCompany_IdAndActiveTrueOrderByIdAsc(any());
    }

    @Test
    void findDefault_cae_a_la_primera_activa_si_no_hay_principal_activa() {
        when(branchJpaRepository.findFirstByCompany_IdAndCodeIgnoreCaseAndActiveTrue(COMPANY,
                "PRINCIPAL")).thenReturn(Optional.empty());
        BranchJpaEntity otra = mapped(5L, "Sede Sur", "SUR");
        when(branchJpaRepository.findFirstByCompany_IdAndActiveTrueOrderByIdAsc(COMPANY))
                .thenReturn(Optional.of(otra));

        assertThat(port.findDefaultActiveByCompanyId(COMPANY))
                .contains(new BranchRef(5L, "Sede Sur", "SUR"));
    }

    @Test
    void findDefault_vacio_si_no_hay_ninguna_sede_activa() {
        when(branchJpaRepository.findFirstByCompany_IdAndCodeIgnoreCaseAndActiveTrue(COMPANY,
                "PRINCIPAL")).thenReturn(Optional.empty());
        when(branchJpaRepository.findFirstByCompany_IdAndActiveTrueOrderByIdAsc(COMPANY))
                .thenReturn(Optional.empty());

        assertThat(port.findDefaultActiveByCompanyId(COMPANY)).isEmpty();
    }
}
