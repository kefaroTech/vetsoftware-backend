package com.vetsoftware.app.numberingresolution.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaBranchQueryPort (numberingresolution)")
class JpaBranchQueryPortTest {

    private static final Long BRANCH_ID = 3L;
    private static final Long COMPANY_ID = 9L;

    @Mock
    private BranchJpaRepository branchJpaRepository;

    @InjectMocks
    private JpaBranchQueryPort port;

    @Nested
    @DisplayName("existsByIdAndCompanyId")
    class ExistsByIdAndCompanyId {

        @Test
        @DisplayName("delega en el repositorio de sedes filtrando por empresa")
        void delega_en_el_repositorio_filtrando_por_empresa() {
            when(branchJpaRepository.existsByIdAndCompany_Id(BRANCH_ID, COMPANY_ID))
                    .thenReturn(true);

            assertThat(port.existsByIdAndCompanyId(BRANCH_ID, COMPANY_ID)).isTrue();
        }

        @Test
        @DisplayName("una sede de otra empresa no existe en este alcance")
        void una_sede_de_otra_empresa_no_existe() {
            when(branchJpaRepository.existsByIdAndCompany_Id(BRANCH_ID, COMPANY_ID))
                    .thenReturn(false);

            assertThat(port.existsByIdAndCompanyId(BRANCH_ID, COMPANY_ID)).isFalse();
        }
    }
}
