package com.vetsoftware.app.servicechargeopenaccount.infrastructure.persistence;

import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.COMPANY_ID;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.OPEN_ACCOUNT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.openaccount.domain.OpenAccountStatus;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaEntity;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaRepository;
import com.vetsoftware.app.servicechargeopenaccount.domain.OpenAccountRef;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaOpenAccountQueryPort (servicechargeopenaccount)")
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

            Optional<OpenAccountRef> found = port.findById(OPEN_ACCOUNT_ID);

            assertThat(found).contains(new OpenAccountRef(OPEN_ACCOUNT_ID, COMPANY_ID));
        }

        @Test
        @DisplayName("una cuenta inexistente no aparece")
        void una_cuenta_inexistente_no_aparece() {
            when(openAccountJpaRepository.findById(OPEN_ACCOUNT_ID)).thenReturn(Optional.empty());

            assertThat(port.findById(OPEN_ACCOUNT_ID)).isEmpty();
        }
    }

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
    @DisplayName("lockForUpdate")
    class LockForUpdate {

        @Test
        @DisplayName("delega en el FOR UPDATE ACOTADO por empresa, no en el ancho")
        void delega_en_el_for_update_acotado_por_empresa() {
            port.lockForUpdate(OPEN_ACCOUNT_ID, COMPANY_ID);

            // Con la variante ancha el PESSIMISTIC_WRITE caia sobre la fila de otro
            // tenant antes de cualquier comprobacion: lo soltaba el rollback del
            // rechazo posterior, pero se concedia. La acotada no devuelve fila para una
            // cuenta ajena y no bloquea nada.
            verify(openAccountJpaRepository).findByIdForUpdateAndCompanyId(OPEN_ACCOUNT_ID,
                    COMPANY_ID);
        }
    }

    @Nested
    @DisplayName("isOpen")
    class IsOpen {

        @Test
        @DisplayName("una cuenta OPEN es abierta")
        void una_cuenta_open_es_abierta() {
            when(openAccountJpaRepository.findById(OPEN_ACCOUNT_ID))
                    .thenReturn(Optional.of(openAccountEntity));
            when(openAccountEntity.getStatus()).thenReturn(OpenAccountStatus.OPEN);

            assertThat(port.isOpen(OPEN_ACCOUNT_ID)).isTrue();
        }

        @Test
        @DisplayName("una cuenta CLOSE no esta abierta")
        void una_cuenta_close_no_esta_abierta() {
            when(openAccountJpaRepository.findById(OPEN_ACCOUNT_ID))
                    .thenReturn(Optional.of(openAccountEntity));
            when(openAccountEntity.getStatus()).thenReturn(OpenAccountStatus.CLOSE);

            assertThat(port.isOpen(OPEN_ACCOUNT_ID)).isFalse();
        }

        @Test
        @DisplayName("una cuenta inexistente no esta abierta")
        void una_cuenta_inexistente_no_esta_abierta() {
            when(openAccountJpaRepository.findById(OPEN_ACCOUNT_ID)).thenReturn(Optional.empty());

            assertThat(port.isOpen(OPEN_ACCOUNT_ID)).isFalse();
        }
    }

    @Nested
    @DisplayName("outstandingAmount")
    class OutstandingAmount {

        @Test
        @DisplayName("devuelve el saldo pendiente de la cuenta")
        void devuelve_el_saldo_pendiente_de_la_cuenta() {
            when(openAccountJpaRepository.findById(OPEN_ACCOUNT_ID))
                    .thenReturn(Optional.of(openAccountEntity));
            when(openAccountEntity.getOutstandingAmount()).thenReturn(new BigDecimal("15000.00"));

            assertThat(port.outstandingAmount(OPEN_ACCOUNT_ID)).isEqualByComparingTo("15000.00");
        }

        @Test
        @DisplayName("una cuenta inexistente devuelve cero, no null")
        void una_cuenta_inexistente_devuelve_cero() {
            when(openAccountJpaRepository.findById(OPEN_ACCOUNT_ID)).thenReturn(Optional.empty());

            assertThat(port.outstandingAmount(OPEN_ACCOUNT_ID))
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
