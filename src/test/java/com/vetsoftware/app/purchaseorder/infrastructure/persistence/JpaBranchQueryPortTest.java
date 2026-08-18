package com.vetsoftware.app.purchaseorder.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaEntity;
import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaRepository;
import com.vetsoftware.app.purchaseorder.domain.BranchRef;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaBranchQueryPort (purchaseorder)")
class JpaBranchQueryPortTest {

    private static final Long BRANCH_ID = 4L;
    private static final Long COMPANY_ID = 9L;

    @Mock
    private BranchJpaRepository branchJpaRepository;

    @InjectMocks
    private JpaBranchQueryPort port;

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("mapea la sede encontrada a su BranchRef")
        void mapea_la_sede_encontrada() {
            BranchJpaEntity entidad = mock(BranchJpaEntity.class);
            when(entidad.getId()).thenReturn(BRANCH_ID);
            when(entidad.getName()).thenReturn("Sede Norte");
            when(branchJpaRepository.findByIdAndCompanyId(BRANCH_ID, COMPANY_ID))
                    .thenReturn(Optional.of(entidad));

            Optional<BranchRef> ref = port.findById(BRANCH_ID, COMPANY_ID);

            assertThat(ref).contains(new BranchRef(BRANCH_ID, "Sede Norte"));
        }

        @Test
        @DisplayName("devuelve vacio si la sede no existe en la empresa")
        void devuelve_vacio_si_no_existe() {
            when(branchJpaRepository.findByIdAndCompanyId(BRANCH_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThat(port.findById(BRANCH_ID, COMPANY_ID)).isEmpty();
        }
    }
}
