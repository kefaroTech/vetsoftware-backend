package com.vetsoftware.app.cashregister.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaEntity;
import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaBranchQueryPort (cashregister) — sede activa de la empresa")
class JpaBranchQueryPortTest {

    private static final Long BRANCH_ID = 4L;
    private static final Long COMPANY_ID = 9L;

    @Mock
    private BranchJpaRepository branchJpaRepository;

    @InjectMocks
    private JpaBranchQueryPort port;

    @Nested
    @DisplayName("existsActiveInCompany")
    class ExistsActiveInCompany {

        @Test
        @DisplayName("una sede activa de la empresa existe")
        void una_sede_activa_de_la_empresa_existe() {
            BranchJpaEntity entidad = mock(BranchJpaEntity.class);
            when(entidad.isActive()).thenReturn(true);
            when(branchJpaRepository.findByIdAndCompanyId(BRANCH_ID, COMPANY_ID))
                    .thenReturn(Optional.of(entidad));

            assertThat(port.existsActiveInCompany(BRANCH_ID, COMPANY_ID)).isTrue();
        }

        @Test
        @DisplayName("una sede inactiva no cuenta como existente")
        void una_sede_inactiva_no_cuenta() {
            BranchJpaEntity entidad = mock(BranchJpaEntity.class);
            when(entidad.isActive()).thenReturn(false);
            when(branchJpaRepository.findByIdAndCompanyId(BRANCH_ID, COMPANY_ID))
                    .thenReturn(Optional.of(entidad));

            assertThat(port.existsActiveInCompany(BRANCH_ID, COMPANY_ID)).isFalse();
        }

        @Test
        @DisplayName("una sede que no pertenece a la empresa no existe")
        void una_sede_que_no_pertenece_a_la_empresa_no_existe() {
            when(branchJpaRepository.findByIdAndCompanyId(BRANCH_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThat(port.existsActiveInCompany(BRANCH_ID, COMPANY_ID)).isFalse();
        }

        @Test
        @DisplayName("sin branchId o companyId no consulta el repositorio")
        void sin_branch_id_o_company_id_no_consulta_el_repositorio() {
            assertThat(port.existsActiveInCompany(null, COMPANY_ID)).isFalse();
            assertThat(port.existsActiveInCompany(BRANCH_ID, null)).isFalse();

            verifyNoInteractions(branchJpaRepository);
        }
    }
}
