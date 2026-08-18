package com.vetsoftware.app.productchargeopenaccount.infrastructure.persistence;

import static com.vetsoftware.app.productchargeopenaccount.testsupport.ProductChargeOpenAccountMother.COMPANY_ID;
import static com.vetsoftware.app.productchargeopenaccount.testsupport.ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaEntity;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaRepository;
import com.vetsoftware.app.productchargeopenaccount.domain.OpenAccountRef;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Rodaja unitaria del adaptador que resuelve la cuenta abierta desde esta
 * feature. Lo que fija es de que consulta cuelga cada variante: la acotada
 * tiene que ir por {@code findByIdAndCompany_Id} y no por un filtro en Java,
 * porque es lo unico que impide que un cargo de esta empresa acabe en la cuenta
 * abierta de otra (BE-COV).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaOpenAccountQueryPort (productchargeopenaccount)")
class JpaOpenAccountQueryPortTest {

    @Mock
    private OpenAccountJpaRepository openAccountJpaRepository;
    @Mock
    private OpenAccountJpaEntity openAccountEntity;
    @Mock
    private CompanyJpaEntity companyEntity;

    @InjectMocks
    private JpaOpenAccountQueryPort port;

    @Nested
    @DisplayName("findByIdAndCompanyId")
    class FindByIdAndCompanyId {

        @Test
        @DisplayName("mapea la cuenta cuando pertenece a la empresa pedida")
        void mapea_la_cuenta_de_la_empresa_pedida() {
            when(openAccountJpaRepository.findByIdAndCompany_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(openAccountEntity));
            when(openAccountEntity.getId()).thenReturn(OPEN_ACCOUNT_ID);
            when(openAccountEntity.getCompany()).thenReturn(companyEntity);
            when(companyEntity.getId()).thenReturn(COMPANY_ID);

            assertThat(port.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .contains(new OpenAccountRef(OPEN_ACCOUNT_ID, COMPANY_ID));
        }

        @Test
        @DisplayName("la cuenta de otra empresa no aparece: el filtro lo hace la consulta")
        void la_cuenta_de_otra_empresa_no_aparece() {
            // El adaptador no compara la empresa en Java: delega en el derivado
            // findByIdAndCompany_Id. Ese salto es lo que impide que el servicio llegue a
            // ver la cuenta del otro tenant y le cuelgue un cargo.
            Long otraEmpresa = COMPANY_ID + 1;
            when(openAccountJpaRepository.findByIdAndCompany_Id(OPEN_ACCOUNT_ID, otraEmpresa))
                    .thenReturn(Optional.empty());

            assertThat(port.findByIdAndCompanyId(OPEN_ACCOUNT_ID, otraEmpresa)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("mapea la cuenta encontrada con la empresa de su company")
        void mapea_la_cuenta_encontrada_con_la_empresa_de_su_company() {
            when(openAccountJpaRepository.findById(OPEN_ACCOUNT_ID))
                    .thenReturn(Optional.of(openAccountEntity));
            when(openAccountEntity.getId()).thenReturn(OPEN_ACCOUNT_ID);
            when(openAccountEntity.getCompany()).thenReturn(companyEntity);
            when(companyEntity.getId()).thenReturn(COMPANY_ID);

            assertThat(port.findById(OPEN_ACCOUNT_ID))
                    .contains(new OpenAccountRef(OPEN_ACCOUNT_ID, COMPANY_ID));
        }

        @Test
        @DisplayName("una cuenta inexistente no aparece")
        void una_cuenta_inexistente_no_aparece() {
            when(openAccountJpaRepository.findById(OPEN_ACCOUNT_ID)).thenReturn(Optional.empty());

            assertThat(port.findById(OPEN_ACCOUNT_ID)).isEmpty();
        }
    }

    @Nested
    @DisplayName("lockForUpdate")
    class LockForUpdate {

        @Test
        @DisplayName("delega en el FOR UPDATE ACOTADO por empresa, no en el ancho")
        void delega_en_el_for_update_acotado_por_empresa() {
            port.lockForUpdate(OPEN_ACCOUNT_ID, COMPANY_ID);

            // Con la variante ancha el PESSIMISTIC_WRITE caia sobre la fila de otro
            // tenant antes de cualquier comprobacion: lo soltaba el rollback del rechazo
            // posterior, pero se concedia. La acotada no devuelve fila para una cuenta
            // ajena y no bloquea nada.
            verify(openAccountJpaRepository).findByIdForUpdateAndCompanyId(OPEN_ACCOUNT_ID,
                    COMPANY_ID);
        }
    }
}
