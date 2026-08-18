package com.vetsoftware.app.debtopenaccount.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.debtopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.openaccount.domain.OpenAccountStatus;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaEntity;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaRepository;
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
@DisplayName("JpaOpenAccountQueryPort (debtopenaccount) — resuelve la cuenta desde el repositorio Spring Data")
class JpaOpenAccountQueryPortTest {

    private static final Long OPEN_ACCOUNT_ID = 50L;
    private static final Long COMPANY_ID = 9L;

    @Mock
    private OpenAccountJpaRepository openAccountJpaRepository;

    @InjectMocks
    private JpaOpenAccountQueryPort port;

    /**
     * Cada metodo del puerto lee un unico campo de la fila (id+empresa, estado, o
     * saldo): un solo mock "completo" con los tres stubeados dejaria, en cada test,
     * dos stubs sin usar y STRICT_STUBS los marca como
     * {@code UnnecessaryStubbingException}. Un factory por escenario evita
     * sobre-stubear.
     */
    private static OpenAccountJpaEntity entidadConEmpresa() {
        OpenAccountJpaEntity entity = mock(OpenAccountJpaEntity.class);
        CompanyJpaEntity company = mock(CompanyJpaEntity.class);
        when(company.getId()).thenReturn(COMPANY_ID);
        when(entity.getId()).thenReturn(OPEN_ACCOUNT_ID);
        when(entity.getCompany()).thenReturn(company);
        return entity;
    }

    private static OpenAccountJpaEntity entidadConEstado(OpenAccountStatus status) {
        OpenAccountJpaEntity entity = mock(OpenAccountJpaEntity.class);
        when(entity.getStatus()).thenReturn(status);
        return entity;
    }

    private static OpenAccountJpaEntity entidadConSaldo(String saldo) {
        OpenAccountJpaEntity entity = mock(OpenAccountJpaEntity.class);
        when(entity.getOutstandingAmount()).thenReturn(new BigDecimal(saldo));
        return entity;
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("mapea la cuenta encontrada al OpenAccountRef con su empresa")
        void mapea_la_cuenta_encontrada() {
            // La entidad se construye ANTES del when: el factory abre y cierra sus
            // propios when()/thenReturn(), y anidarlo dentro del thenReturn de este
            // when deja la stubbing de arriba a medias (UnfinishedStubbingException).
            OpenAccountJpaEntity entity = entidadConEmpresa();
            when(openAccountJpaRepository.findById(OPEN_ACCOUNT_ID))
                    .thenReturn(Optional.of(entity));

            assertThat(port.findById(OPEN_ACCOUNT_ID))
                    .contains(new OpenAccountRef(OPEN_ACCOUNT_ID, COMPANY_ID));
        }

        @Test
        @DisplayName("vacio si la cuenta no existe")
        void vacio_si_la_cuenta_no_existe() {
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
            OpenAccountJpaEntity entity = entidadConEmpresa();
            when(openAccountJpaRepository.findByIdAndCompany_Id(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(entity));

            assertThat(port.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .contains(new OpenAccountRef(OPEN_ACCOUNT_ID, COMPANY_ID));
        }

        @Test
        @DisplayName("la cuenta de otra empresa no aparece: el filtro lo hace la consulta")
        void la_cuenta_de_otra_empresa_no_aparece() {
            // El adaptador no compara la empresa en Java: delega en el derivado
            // findByIdAndCompany_Id. Ese salto es lo que impide que el servicio llegue a
            // ver la cuenta del otro tenant y le cuelgue un abono.
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
        @DisplayName("true cuando el estado es OPEN")
        void true_cuando_el_estado_es_open() {
            OpenAccountJpaEntity entity = entidadConEstado(OpenAccountStatus.OPEN);
            when(openAccountJpaRepository.findById(OPEN_ACCOUNT_ID))
                    .thenReturn(Optional.of(entity));

            assertThat(port.isOpen(OPEN_ACCOUNT_ID)).isTrue();
        }

        @Test
        @DisplayName("false cuando el estado no es OPEN")
        void false_cuando_el_estado_no_es_open() {
            OpenAccountJpaEntity entity = entidadConEstado(OpenAccountStatus.CLOSE);
            when(openAccountJpaRepository.findById(OPEN_ACCOUNT_ID))
                    .thenReturn(Optional.of(entity));

            assertThat(port.isOpen(OPEN_ACCOUNT_ID)).isFalse();
        }

        @Test
        @DisplayName("false cuando la cuenta no existe")
        void false_cuando_la_cuenta_no_existe() {
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
            OpenAccountJpaEntity entity = entidadConSaldo("12345.67");
            when(openAccountJpaRepository.findById(OPEN_ACCOUNT_ID))
                    .thenReturn(Optional.of(entity));

            assertThat(port.outstandingAmount(OPEN_ACCOUNT_ID)).isEqualByComparingTo("12345.67");
        }

        @Test
        @DisplayName("cero cuando la cuenta no existe")
        void cero_cuando_la_cuenta_no_existe() {
            when(openAccountJpaRepository.findById(OPEN_ACCOUNT_ID)).thenReturn(Optional.empty());

            assertThat(port.outstandingAmount(OPEN_ACCOUNT_ID))
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
