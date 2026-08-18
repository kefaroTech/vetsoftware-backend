package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaEntity;
import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.BranchInfoQueryPort.BranchInfo;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaBranchInfoQueryPort — datos de la sede emisora para el POS")
class JpaBranchInfoQueryPortTest {

    @Mock
    private BranchJpaRepository branchJpaRepository;

    private JpaBranchInfoQueryPort port;

    @BeforeEach
    void montar() {
        port = new JpaBranchInfoQueryPort(branchJpaRepository);
    }

    @Test
    @DisplayName("un branchId null no consulta el repositorio y devuelve vacio")
    void branch_id_null_no_consulta_el_repositorio() {
        assertThat(port.findById(null)).isEmpty();

        verifyNoInteractions(branchJpaRepository);
    }

    @Test
    @DisplayName("una sede existente se traduce a BranchInfo con nombre, codigo y direccion")
    void sede_existente_se_traduce_a_branch_info() throws Exception {
        BranchJpaEntity entity = com.vetsoftware.app.electronicdocument.testsupport.ReflectionEntities
                .newInstance(BranchJpaEntity.class);
        entity.setName("Sede Norte");
        entity.setCode("N01");
        entity.setAddress("Cra 1 # 2-3");
        when(branchJpaRepository.findById(7L)).thenReturn(Optional.of(entity));

        Optional<BranchInfo> info = port.findById(7L);

        assertThat(info).contains(new BranchInfo("Sede Norte", "N01", "Cra 1 # 2-3"));
    }

    @Test
    @DisplayName("una sede inexistente devuelve vacio")
    void sede_inexistente_devuelve_vacio() {
        when(branchJpaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(port.findById(99L)).isEmpty();
    }
}
