package com.vetsoftware.app.laboratorytest.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
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
@DisplayName("JpaBranchQueryPort — adaptador sobre BranchJpaRepository")
class JpaBranchQueryPortTest {

    private static final Long BRANCH_ID = 5L;
    private static final Long COMPANY_ID = 9L;

    @Mock
    private BranchJpaRepository branchJpaRepository;
    @Mock
    private BranchJpaEntity branchEntity;
    @InjectMocks
    private JpaBranchQueryPort port;

    @Nested
    @DisplayName("findActiveIdByIdAndCompanyId")
    class SedeSolicitada {

        @Test
        @DisplayName("devuelve el id de la sede solicitada cuando esta activa")
        void devuelve_el_id_de_la_sede_activa() {
            when(branchEntity.isActive()).thenReturn(true);
            when(branchEntity.getId()).thenReturn(BRANCH_ID);
            when(branchJpaRepository.findByIdAndCompanyId(BRANCH_ID, COMPANY_ID))
                    .thenReturn(Optional.of(branchEntity));

            Optional<Long> resultado = port.findActiveIdByIdAndCompanyId(BRANCH_ID, COMPANY_ID);

            assertThat(resultado).contains(BRANCH_ID);
        }

        @Test
        @DisplayName("una sede inactiva no se ofrece aunque exista en la empresa")
        void una_sede_inactiva_no_se_ofrece() {
            when(branchEntity.isActive()).thenReturn(false);
            when(branchJpaRepository.findByIdAndCompanyId(BRANCH_ID, COMPANY_ID))
                    .thenReturn(Optional.of(branchEntity));

            assertThat(port.findActiveIdByIdAndCompanyId(BRANCH_ID, COMPANY_ID)).isEmpty();
        }

        @Test
        @DisplayName("una sede de otra empresa no se entrega")
        void una_sede_de_otra_empresa_no_se_entrega() {
            when(branchJpaRepository.findByIdAndCompanyId(BRANCH_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThat(port.findActiveIdByIdAndCompanyId(BRANCH_ID, COMPANY_ID)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findDefaultActiveIdByCompanyId")
    class SedePorDefecto {

        @Test
        @DisplayName("prefiere la sede Principal activa")
        void prefiere_la_sede_principal_activa() {
            when(branchEntity.getId()).thenReturn(BRANCH_ID);
            when(branchJpaRepository.findFirstByCompany_IdAndCodeIgnoreCaseAndActiveTrue(COMPANY_ID,
                    "PRINCIPAL")).thenReturn(Optional.of(branchEntity));

            Optional<Long> resultado = port.findDefaultActiveIdByCompanyId(COMPANY_ID);

            assertThat(resultado).contains(BRANCH_ID);
        }

        @Test
        @DisplayName("sin Principal activa cae a la primera sede activa")
        void sin_principal_activa_cae_a_la_primera_activa() {
            Long otraSede = 8L;
            when(branchEntity.getId()).thenReturn(otraSede);
            when(branchJpaRepository.findFirstByCompany_IdAndCodeIgnoreCaseAndActiveTrue(COMPANY_ID,
                    "PRINCIPAL")).thenReturn(Optional.empty());
            when(branchJpaRepository.findFirstByCompany_IdAndActiveTrueOrderByIdAsc(COMPANY_ID))
                    .thenReturn(Optional.of(branchEntity));

            Optional<Long> resultado = port.findDefaultActiveIdByCompanyId(COMPANY_ID);

            assertThat(resultado).contains(otraSede);
        }

        @Test
        @DisplayName("una empresa sin ninguna sede activa devuelve vacio")
        void una_empresa_sin_ninguna_sede_activa_devuelve_vacio() {
            when(branchJpaRepository.findFirstByCompany_IdAndCodeIgnoreCaseAndActiveTrue(COMPANY_ID,
                    "PRINCIPAL")).thenReturn(Optional.empty());
            when(branchJpaRepository.findFirstByCompany_IdAndActiveTrueOrderByIdAsc(COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThat(port.findDefaultActiveIdByCompanyId(COMPANY_ID)).isEmpty();
        }
    }
}
