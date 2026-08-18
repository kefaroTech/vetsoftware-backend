package com.vetsoftware.app.openaccount.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.openaccount.application.command.UpdateOpenAccountCommand;
import com.vetsoftware.app.openaccount.application.dto.OpenAccountDto;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
import com.vetsoftware.app.openaccount.application.port.out.OwnerQueryPort;
import com.vetsoftware.app.openaccount.domain.OpenAccount;
import com.vetsoftware.app.openaccount.domain.OpenAccountNotFoundException;
import com.vetsoftware.app.openaccount.domain.OpenAccountVersionConflictException;
import com.vetsoftware.app.openaccount.testsupport.OpenAccountMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateOpenAccountService")
class UpdateOpenAccountServiceTest {

    @Mock
    private OpenAccountRepository repository;
    @Mock
    private OwnerQueryPort ownerQueryPort;
    @InjectMocks
    private UpdateOpenAccountService service;

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("actualiza el owner resuelto por el puerto y persiste")
        void actualiza_el_owner_y_persiste() {
            OpenAccount cuenta = OpenAccountMother.abierta();
            when(repository.findByIdAndCompanyId(OpenAccountMother.OPEN_ACCOUNT_ID,
                    OpenAccountMother.COMPANY_ID)).thenReturn(Optional.of(cuenta));
            when(ownerQueryPort.findByIdAndCompanyId(OpenAccountMother.OTRO_OWNER.id(),
                    OpenAccountMother.COMPANY_ID))
                    .thenReturn(Optional.of(OpenAccountMother.OTRO_OWNER));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            OpenAccountDto dto = service.execute(OpenAccountMother.comandoActualizar());

            assertThat(dto.owner().id()).isEqualTo(OpenAccountMother.OTRO_OWNER.id());
            ArgumentCaptor<OpenAccount> captor = ArgumentCaptor.forClass(OpenAccount.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getOwner()).isEqualTo(OpenAccountMother.OTRO_OWNER);
        }

        @Test
        @DisplayName("un expectedVersion null omite el chequeo de version")
        void expected_version_null_omite_el_chequeo() {
            OpenAccount cuenta = OpenAccountMother.abierta();
            when(repository.findByIdAndCompanyId(OpenAccountMother.OPEN_ACCOUNT_ID,
                    OpenAccountMother.COMPANY_ID)).thenReturn(Optional.of(cuenta));
            when(ownerQueryPort.findByIdAndCompanyId(OpenAccountMother.OTRO_OWNER.id(),
                    OpenAccountMother.COMPANY_ID))
                    .thenReturn(Optional.of(OpenAccountMother.OTRO_OWNER));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            UpdateOpenAccountCommand command = new UpdateOpenAccountCommand(
                    OpenAccountMother.OPEN_ACCOUNT_ID, OpenAccountMother.OTRO_OWNER.id(),
                    OpenAccountMother.COMPANY_ID, null);

            OpenAccountDto dto = service.execute(command);

            assertThat(dto.owner().id()).isEqualTo(OpenAccountMother.OTRO_OWNER.id());
        }
    }

    @Nested
    @DisplayName("validaciones que no deben escribir")
    class Validaciones {

        @Test
        @DisplayName("cuenta inexistente lanza y no toca el owner ni escribe")
        void cuenta_inexistente_lanza_y_no_toca_nada_mas() {
            when(repository.findByIdAndCompanyId(OpenAccountMother.OPEN_ACCOUNT_ID,
                    OpenAccountMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(OpenAccountMother.comandoActualizar()))
                    .isInstanceOf(OpenAccountNotFoundException.class);

            verify(repository, never()).save(any());
            verifyNoInteractions(ownerQueryPort);
        }

        /**
         * El filtro por empresa vive ahora EN la consulta: la cuenta ajena no llega a
         * cargarse, asi que el desenlace es un 404 y no un mensaje que confirme su
         * existencia en otro tenant. Se comprueba ademas que el servicio no toca la
         * variante ancha.
         */
        @Test
        @DisplayName("una cuenta de otra empresa se rechaza y no toca nada mas")
        void cuenta_de_otra_empresa_se_rechaza() {
            when(repository.findByIdAndCompanyId(OpenAccountMother.OPEN_ACCOUNT_ID,
                    OpenAccountMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(OpenAccountMother.comandoActualizar()))
                    .isInstanceOf(OpenAccountNotFoundException.class);

            verify(repository, never()).save(any());
            verify(repository, never()).findById(OpenAccountMother.OPEN_ACCOUNT_ID);
            verifyNoInteractions(ownerQueryPort);
        }

        /**
         * El {@code ownerId} si lo elige el cliente: un propietario de otra empresa
         * tiene que rebotar sin que la cuenta quede reapuntada ni se escriba nada.
         */
        @Test
        @DisplayName("un owner de otra empresa se rechaza y no escribe")
        void owner_de_otra_empresa_se_rechaza() {
            OpenAccount cuenta = OpenAccountMother.abierta();
            when(repository.findByIdAndCompanyId(OpenAccountMother.OPEN_ACCOUNT_ID,
                    OpenAccountMother.COMPANY_ID)).thenReturn(Optional.of(cuenta));
            when(ownerQueryPort.findByIdAndCompanyId(OpenAccountMother.OTRO_OWNER.id(),
                    OpenAccountMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(OpenAccountMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Owner not found: " + OpenAccountMother.OTRO_OWNER.id());

            verify(repository, never()).save(any());
            assertThat(cuenta.getOwner()).isEqualTo(OpenAccountMother.OWNER);
        }

        @Test
        @DisplayName("un expectedVersion que no coincide lanza conflicto y no toca nada mas")
        void version_no_coincide_lanza_conflicto() {
            OpenAccount cuenta = OpenAccountMother.abierta();
            when(repository.findByIdAndCompanyId(OpenAccountMother.OPEN_ACCOUNT_ID,
                    OpenAccountMother.COMPANY_ID)).thenReturn(Optional.of(cuenta));
            UpdateOpenAccountCommand command = new UpdateOpenAccountCommand(
                    OpenAccountMother.OPEN_ACCOUNT_ID, OpenAccountMother.OTRO_OWNER.id(),
                    OpenAccountMother.COMPANY_ID, 99L);

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(OpenAccountVersionConflictException.class);

            verify(repository, never()).save(any());
            verifyNoInteractions(ownerQueryPort);
        }

        @Test
        @DisplayName("un owner inexistente se rechaza y no escribe")
        void owner_inexistente_se_rechaza() {
            OpenAccount cuenta = OpenAccountMother.abierta();
            when(repository.findByIdAndCompanyId(OpenAccountMother.OPEN_ACCOUNT_ID,
                    OpenAccountMother.COMPANY_ID)).thenReturn(Optional.of(cuenta));
            when(ownerQueryPort.findByIdAndCompanyId(OpenAccountMother.OTRO_OWNER.id(),
                    OpenAccountMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(OpenAccountMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Owner not found: " + OpenAccountMother.OTRO_OWNER.id());

            verify(repository, never()).save(any());
        }
    }
}
