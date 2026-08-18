package com.vetsoftware.app.employeebranch.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaEntity;
import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaBranchQueryPort (employeebranch)")
class JpaBranchQueryPortTest {

    private static final Long COMPANY_ID = 900L;

    @Mock
    private BranchJpaRepository branchJpaRepository;

    @InjectMocks
    private JpaBranchQueryPort port;

    private static BranchJpaEntity sede(long id) {
        BranchJpaEntity entidad = mock(BranchJpaEntity.class);
        when(entidad.getId()).thenReturn(id);
        return entidad;
    }

    @Test
    @DisplayName("mapea las sedes de la empresa a solo sus ids, en el orden del repositorio")
    void mapea_las_sedes_de_la_empresa_a_sus_ids() {
        // Los mocks se arman ANTES del when(): construirlos dentro de thenReturn(...)
        // dejaria un when() anidado mientras el de branchJpaRepository sigue abierto.
        List<BranchJpaEntity> sedes = List.of(sede(910L), sede(911L));
        when(branchJpaRepository.findAllByCompanyId(COMPANY_ID)).thenReturn(sedes);

        assertThat(port.findBranchIdsByCompanyId(COMPANY_ID)).containsExactly(910L, 911L);
    }

    @Test
    @DisplayName("una empresa sin sedes devuelve una lista vacia")
    void una_empresa_sin_sedes_devuelve_lista_vacia() {
        when(branchJpaRepository.findAllByCompanyId(COMPANY_ID)).thenReturn(List.of());

        assertThat(port.findBranchIdsByCompanyId(COMPANY_ID)).isEmpty();
    }
}
