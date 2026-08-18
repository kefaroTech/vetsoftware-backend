package com.vetsoftware.app.inventory.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaEntity;
import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaBranchQueryPort (inventory)")
class JpaBranchQueryPortTest {

    private static final Long BRANCH_ID = 10L;
    private static final Long COMPANY_ID = 1L;

    @Mock
    private BranchJpaRepository branchJpaRepository;

    @InjectMocks
    private JpaBranchQueryPort port;

    private static BranchJpaEntity sede(boolean activa) {
        BranchJpaEntity entidad = mock(BranchJpaEntity.class);
        when(entidad.isActive()).thenReturn(activa);
        return entidad;
    }

    @Test
    @DisplayName("una sede activa de la empresa existe")
    void una_sede_activa_existe() {
        // El mock se arma ANTES del when(): construirlo dentro de thenReturn(...)
        // dejaria un when() anidado mientras el de branchJpaRepository sigue abierto.
        BranchJpaEntity activa = sede(true);
        when(branchJpaRepository.findByIdAndCompanyId(BRANCH_ID, COMPANY_ID))
                .thenReturn(Optional.of(activa));

        assertThat(port.existsActiveInCompany(BRANCH_ID, COMPANY_ID)).isTrue();
    }

    @Test
    @DisplayName("una sede inactiva no cuenta como existente")
    void una_sede_inactiva_no_existe() {
        BranchJpaEntity inactiva = sede(false);
        when(branchJpaRepository.findByIdAndCompanyId(BRANCH_ID, COMPANY_ID))
                .thenReturn(Optional.of(inactiva));

        assertThat(port.existsActiveInCompany(BRANCH_ID, COMPANY_ID)).isFalse();
    }

    @Test
    @DisplayName("una sede que no existe en la empresa devuelve falso")
    void una_sede_inexistente_devuelve_falso() {
        when(branchJpaRepository.findByIdAndCompanyId(BRANCH_ID, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThat(port.existsActiveInCompany(BRANCH_ID, COMPANY_ID)).isFalse();
    }

    @Test
    @DisplayName("un branchId null nunca existe, sin tocar el repositorio")
    void branchId_null_no_consulta_el_repositorio() {
        assertThat(port.existsActiveInCompany(null, COMPANY_ID)).isFalse();

        verifyNoInteractions(branchJpaRepository);
    }

    @Test
    @DisplayName("un companyId null nunca existe, sin tocar el repositorio")
    void companyId_null_no_consulta_el_repositorio() {
        assertThat(port.existsActiveInCompany(BRANCH_ID, null)).isFalse();

        verifyNoInteractions(branchJpaRepository);
    }
}
