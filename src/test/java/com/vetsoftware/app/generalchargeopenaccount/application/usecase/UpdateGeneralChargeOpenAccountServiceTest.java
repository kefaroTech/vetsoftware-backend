package com.vetsoftware.app.generalchargeopenaccount.application.usecase;

import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.CHARGE_ID;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.COMPANY_ID;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.CUENTA;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.CUENTA_AJENA;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.IVA_19;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.OPEN_ACCOUNT_ID;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.OTRA_CUENTA;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.OTRA_CUENTA_ID;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.cargo;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.comandoActualizar;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.comandoActualizarSinImpuesto;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.comandoTrasladar;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.generalchargeopenaccount.application.command.UpdateGeneralChargeOpenAccountCommand;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.GeneralChargeOpenAccountRepository;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.OpenAccountVersionGuard;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.TaxQueryPort;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccount;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccountNotFoundException;
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
@DisplayName("UpdateGeneralChargeOpenAccountService")
class UpdateGeneralChargeOpenAccountServiceTest {

    @Mock
    private GeneralChargeOpenAccountRepository repository;
    @Mock
    private OpenAccountQueryPort openAccountQueryPort;
    @Mock
    private TaxQueryPort taxQueryPort;
    @Mock
    private OpenAccountRefresher refresher;
    @Mock
    private OpenAccountVersionGuard versionGuard;

    @InjectMocks
    private UpdateGeneralChargeOpenAccountService service;

    @Captor
    private ArgumentCaptor<GeneralChargeOpenAccount> cargoCaptor;

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("aplica nombre, importe y cantidad nuevos sobre el cargo cargado")
        void aplica_los_valores_nuevos() {
            when(repository.findByIdAndCompanyId(CHARGE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cargo()));
            when(openAccountQueryPort.findById(OPEN_ACCOUNT_ID)).thenReturn(Optional.of(CUENTA));
            when(taxQueryPort.findById(IVA_19.id(), COMPANY_ID)).thenReturn(Optional.of(IVA_19));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comandoActualizar());

            verify(repository).save(cargoCaptor.capture());
            GeneralChargeOpenAccount guardado = cargoCaptor.getValue();
            assertThat(guardado.getName()).isEqualTo("Traslado nocturno");
            assertThat(guardado.getUnitAmount()).isEqualByComparingTo("1000");
            assertThat(guardado.getQuantity()).isEqualByComparingTo("3");
            assertThat(guardado.getOpenAccount()).isEqualTo(CUENTA);
        }

        @Test
        @DisplayName("recalcula el total y su desglose con los valores corregidos")
        void recalcula_el_total_y_su_desglose() {
            when(repository.findByIdAndCompanyId(CHARGE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cargo()));
            when(openAccountQueryPort.findById(OPEN_ACCOUNT_ID)).thenReturn(Optional.of(CUENTA));
            when(taxQueryPort.findById(IVA_19.id(), COMPANY_ID)).thenReturn(Optional.of(IVA_19));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comandoActualizar());

            // El cargo general es un importe libre: corregirlo TIENE que mover el total
            // (a diferencia del cargo de servicio, con el precio congelado del catalogo).
            verify(repository).save(cargoCaptor.capture());
            GeneralChargeOpenAccount guardado = cargoCaptor.getValue();
            assertThat(guardado.getTotalAmount()).isEqualByComparingTo("3000.00");
            assertThat(guardado.getBaseAmount()).isEqualByComparingTo("2521.01");
            assertThat(guardado.getTaxAmount()).isEqualByComparingTo("478.99");
        }

        @Test
        @DisplayName("quitar el impuesto no consulta el catalogo y deja el desglose limpio")
        void quitar_el_impuesto_no_consulta_el_catalogo() {
            when(repository.findByIdAndCompanyId(CHARGE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cargo()));
            when(openAccountQueryPort.findById(OPEN_ACCOUNT_ID)).thenReturn(Optional.of(CUENTA));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comandoActualizarSinImpuesto());

            verifyNoInteractions(taxQueryPort);
            verify(repository).save(cargoCaptor.capture());
            GeneralChargeOpenAccount guardado = cargoCaptor.getValue();
            assertThat(guardado.isHasTax()).isFalse();
            assertThat(guardado.getTaxName()).isNull();
            assertThat(guardado.getTaxAmount()).isEqualByComparingTo("0.00");
            assertThat(guardado.getBaseAmount()).isEqualByComparingTo("11900.00");
        }

        @Test
        @DisplayName("comprueba la version esperada y refresca solo la cuenta destino")
        void comprueba_la_version_y_refresca_solo_la_cuenta_destino() {
            when(repository.findByIdAndCompanyId(CHARGE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cargo()));
            when(openAccountQueryPort.findById(OPEN_ACCOUNT_ID)).thenReturn(Optional.of(CUENTA));
            when(taxQueryPort.findById(IVA_19.id(), COMPANY_ID)).thenReturn(Optional.of(IVA_19));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comandoActualizar());

            verify(versionGuard).assertVersion(COMPANY_ID, OPEN_ACCOUNT_ID, null);
            verify(refresher).refresh(COMPANY_ID, OPEN_ACCOUNT_ID);
            verify(refresher, never()).refresh(COMPANY_ID, OTRA_CUENTA_ID);
        }
    }

    @Nested
    @DisplayName("traslado entre cuentas")
    class Traslado {

        @Test
        @DisplayName("refresca las DOS cuentas: la vieja tambien cambia de total")
        void refresca_las_dos_cuentas() {
            // El cargo vive hoy en CUENTA (50) y el comando lo mueve a OTRA_CUENTA (51).
            when(repository.findByIdAndCompanyId(CHARGE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cargo()));
            when(openAccountQueryPort.findById(OTRA_CUENTA_ID))
                    .thenReturn(Optional.of(OTRA_CUENTA));
            when(taxQueryPort.findById(IVA_19.id(), COMPANY_ID)).thenReturn(Optional.of(IVA_19));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comandoTrasladar());

            // Sin el refresh de la cuenta de origen, esa cuenta sigue mostrando un total
            // que ya incluye un cargo que se llevaron a otro sitio.
            verify(refresher).refresh(COMPANY_ID, OTRA_CUENTA_ID);
            verify(refresher).refresh(COMPANY_ID, OPEN_ACCOUNT_ID);
        }
    }

    @Nested
    @DisplayName("caminos de error: nada se escribe")
    class CaminosDeError {

        @Test
        @DisplayName("cargo inexistente o de otra empresa")
        void cargo_inexistente_o_de_otra_empresa() {
            when(repository.findByIdAndCompanyId(CHARGE_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoActualizar()))
                    .isInstanceOf(GeneralChargeOpenAccountNotFoundException.class)
                    .hasMessageContaining(String.valueOf(CHARGE_ID));

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher, versionGuard, openAccountQueryPort, taxQueryPort);
        }

        @Test
        @DisplayName("cuenta destino inexistente")
        void cuenta_destino_inexistente() {
            when(repository.findByIdAndCompanyId(CHARGE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cargo()));
            when(openAccountQueryPort.findById(OPEN_ACCOUNT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("OpenAccount not found: " + OPEN_ACCOUNT_ID);

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher, taxQueryPort);
        }

        @Test
        @DisplayName("cuenta destino de otra empresa")
        void cuenta_destino_de_otra_empresa() {
            when(repository.findByIdAndCompanyId(CHARGE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cargo()));
            when(openAccountQueryPort.findById(OPEN_ACCOUNT_ID))
                    .thenReturn(Optional.of(CUENTA_AJENA));

            assertThatThrownBy(() -> service.execute(comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("open account does not belong to company");

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher, versionGuard, taxQueryPort);
        }

        @Test
        @DisplayName("impuesto inexistente en la empresa")
        void impuesto_inexistente() {
            when(repository.findByIdAndCompanyId(CHARGE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cargo()));
            when(openAccountQueryPort.findById(OPEN_ACCOUNT_ID)).thenReturn(Optional.of(CUENTA));
            when(taxQueryPort.findById(IVA_19.id(), COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Tax not found: " + IVA_19.id());

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher);
        }

        @Test
        @DisplayName("un comando invalido no deja el cargo a medias ni lo guarda")
        void un_comando_invalido_no_deja_el_cargo_a_medias() {
            GeneralChargeOpenAccount existente = cargo();
            when(repository.findByIdAndCompanyId(CHARGE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(existente));
            when(openAccountQueryPort.findById(OPEN_ACCOUNT_ID)).thenReturn(Optional.of(CUENTA));
            when(taxQueryPort.findById(IVA_19.id(), COMPANY_ID)).thenReturn(Optional.of(IVA_19));

            assertThatThrownBy(() -> service.execute(
                    new UpdateGeneralChargeOpenAccountCommand(CHARGE_ID, "  ", BigDecimal.TEN,
                            BigDecimal.ONE, IVA_19.id(), OPEN_ACCOUNT_ID, COMPANY_ID, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");

            assertThat(existente.getName()).isEqualTo("Traslado en ambulancia");
            verify(repository, never()).save(any());
            verifyNoInteractions(refresher);
        }
    }
}
