package com.vetsoftware.app.debtopenaccount.application.usecase;

import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.COMPANY_ID;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.CUENTA;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.MONTO;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.OPEN_ACCOUNT_ID;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.OTRA_CUENTA;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.OTRA_CUENTA_ID;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.PAYMENT_ID;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.abono;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.comandoActualizar;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.comandoTrasladar;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.debtopenaccount.application.command.UpdateDebtOpenAccountCommand;
import com.vetsoftware.app.debtopenaccount.application.port.out.DebtOpenAccountRepository;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountVersionGuard;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccount;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccountNotFoundException;
import com.vetsoftware.app.debtopenaccount.domain.PaymentMethod;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateDebtOpenAccountService")
class UpdateDebtOpenAccountServiceTest {

    @Mock
    private DebtOpenAccountRepository repository;
    @Mock
    private OpenAccountQueryPort openAccountQueryPort;
    @Mock
    private OpenAccountRefresher refresher;
    @Mock
    private OpenAccountVersionGuard versionGuard;

    @InjectMocks
    private UpdateDebtOpenAccountService service;

    @Captor
    private ArgumentCaptor<DebtOpenAccount> abonoCaptor;

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("aplica monto y medio de pago nuevos sobre el abono cargado")
        void aplica_los_valores_nuevos() {
            when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(abono()));
            when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(CUENTA));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comandoActualizar());

            verify(repository).save(abonoCaptor.capture());
            DebtOpenAccount guardado = abonoCaptor.getValue();
            assertThat(guardado.getAmount()).isEqualByComparingTo("45000");
            assertThat(guardado.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
            assertThat(guardado.getOpenAccount()).isEqualTo(CUENTA);
        }

        @Test
        @DisplayName("comprueba la version esperada y refresca solo la cuenta destino")
        void comprueba_la_version_y_refresca_solo_la_cuenta_destino() {
            when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(abono()));
            when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(CUENTA));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comandoActualizar());

            verify(versionGuard).assertVersion(COMPANY_ID, OPEN_ACCOUNT_ID, null);
            verify(refresher).refresh(COMPANY_ID, OPEN_ACCOUNT_ID);
            verify(refresher, never()).refresh(COMPANY_ID, OTRA_CUENTA_ID);
        }

        @Test
        @DisplayName("editar un abono NO vuelve a comprobar el saldo pendiente")
        void editar_un_abono_no_comprueba_el_saldo_pendiente() {
            when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(abono()));
            when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(CUENTA));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comandoActualizar());

            // Hueco conocido y deliberado en el codigo actual: a diferencia del alta,
            // la edicion no toma lock ni valida sobrepago, asi que subir el monto de un
            // abono si puede dejar el saldo negativo. Queda escrito para que el dia que
            // se cierre, el test falle y obligue a actualizarlo en vez de pasar callando.
            verify(openAccountQueryPort, never()).outstandingAmount(any());
            verify(openAccountQueryPort, never()).lockForUpdate(any(), any());
        }
    }

    @Nested
    @DisplayName("traslado entre cuentas")
    class Traslado {

        @Test
        @DisplayName("refresca las DOS cuentas: la vieja tambien cambia de saldo")
        void refresca_las_dos_cuentas() {
            // El abono vive hoy en CUENTA (50) y el comando lo mueve a OTRA_CUENTA (51).
            when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(abono()));
            when(openAccountQueryPort.findByIdAndCompanyId(OTRA_CUENTA_ID, COMPANY_ID))
                    .thenReturn(Optional.of(OTRA_CUENTA));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comandoTrasladar());

            // Sin el refresh de la cuenta de origen, esa cuenta sigue descontando un
            // abono que se llevaron a otro sitio: el cliente ve menos deuda de la que
            // tiene.
            verify(refresher).refresh(COMPANY_ID, OTRA_CUENTA_ID);
            verify(refresher).refresh(COMPANY_ID, OPEN_ACCOUNT_ID);
        }
    }

    @Nested
    @DisplayName("caminos de error: nada se escribe")
    class CaminosDeError {

        @Test
        @DisplayName("abono inexistente o de otra empresa")
        void abono_inexistente_o_de_otra_empresa() {
            when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoActualizar()))
                    .isInstanceOf(DebtOpenAccountNotFoundException.class)
                    .hasMessageContaining(String.valueOf(PAYMENT_ID));

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher, versionGuard, openAccountQueryPort);
        }

        @Test
        @DisplayName("cuenta destino inexistente")
        void cuenta_destino_inexistente() {
            when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(abono()));
            when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("OpenAccount not found: " + OPEN_ACCOUNT_ID);

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher, versionGuard);
        }

        @Test
        @DisplayName("cuenta destino de otra empresa: el abono no se traslada a otro tenant")
        void cuenta_destino_de_otra_empresa() {
            // La cuenta destino existe pero es de otra empresa: la consulta acotada no la
            // resuelve, asi que el traslado se rechaza. Antes la cuenta ajena SI se
            // cargaba y solo un if posterior impedia mover el abono a su cartera.
            when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(abono()));
            when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("OpenAccount not found: " + OPEN_ACCOUNT_ID);

            verify(openAccountQueryPort, never()).findById(any());
            verify(repository, never()).save(any());
            verifyNoInteractions(refresher, versionGuard);
        }

        @Test
        @DisplayName("un monto invalido no deja el abono a medias ni lo guarda")
        void un_monto_invalido_no_deja_el_abono_a_medias() {
            DebtOpenAccount existente = abono();
            when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(existente));
            when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(CUENTA));

            assertThatThrownBy(() -> service.execute(new UpdateDebtOpenAccountCommand(PAYMENT_ID,
                    BigDecimal.ZERO, "CASH", OPEN_ACCOUNT_ID, COMPANY_ID, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("amount must be positive");

            assertThat(existente.getAmount()).isEqualByComparingTo(MONTO);
            verify(repository, never()).save(any());
            verifyNoInteractions(refresher);
        }

        @Test
        @DisplayName("un medio de pago que no existe en el catalogo")
        void un_medio_de_pago_que_no_existe() {
            when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(abono()));
            when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(CUENTA));

            assertThatThrownBy(() -> service.execute(new UpdateDebtOpenAccountCommand(PAYMENT_ID,
                    MONTO, "CHEQUE", OPEN_ACCOUNT_ID, COMPANY_ID, null)))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("CHEQUE");

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher);
        }
    }
}
