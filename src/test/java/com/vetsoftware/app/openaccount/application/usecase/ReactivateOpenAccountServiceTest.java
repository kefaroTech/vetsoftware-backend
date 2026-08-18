package com.vetsoftware.app.openaccount.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.openaccount.application.dto.OpenAccountDto;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
import com.vetsoftware.app.openaccount.domain.OpenAccount;
import com.vetsoftware.app.openaccount.domain.OpenAccountNotFoundException;
import com.vetsoftware.app.openaccount.testsupport.OpenAccountMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivateOpenAccountService")
class ReactivateOpenAccountServiceTest {

    @Mock
    private OpenAccountRepository repository;
    @InjectMocks
    private ReactivateOpenAccountService service;

    @Nested
    @DisplayName("reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("reactiva la cuenta de la empresa y la devuelve")
        void reactiva_la_cuenta_y_la_devuelve() {
            OpenAccount cuenta = OpenAccountMother.abierta();
            when(repository.reactivate(OpenAccountMother.OPEN_ACCOUNT_ID,
                    OpenAccountMother.COMPANY_ID)).thenReturn(1);
            when(repository.findByIdAndCompanyId(OpenAccountMother.OPEN_ACCOUNT_ID,
                    OpenAccountMother.COMPANY_ID)).thenReturn(Optional.of(cuenta));

            OpenAccountDto dto = service.execute(OpenAccountMother.OPEN_ACCOUNT_ID,
                    OpenAccountMother.COMPANY_ID);

            assertThat(dto.id()).isEqualTo(OpenAccountMother.OPEN_ACCOUNT_ID);
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("si el UPDATE no afecta filas lanza NotFound sin consultar de nuevo")
        void sin_filas_afectadas_lanza_not_found_sin_consultar_de_nuevo() {
            when(repository.reactivate(OpenAccountMother.OPEN_ACCOUNT_ID,
                    OpenAccountMother.COMPANY_ID)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(OpenAccountMother.OPEN_ACCOUNT_ID,
                    OpenAccountMother.COMPANY_ID)).isInstanceOf(OpenAccountNotFoundException.class);

            verify(repository, never()).findByIdAndCompanyId(any(), any());
        }

        @Test
        @DisplayName("si tras reactivar no se vuelve a encontrar la fila lanza NotFound")
        void tras_reactivar_no_se_encuentra_lanza_not_found() {
            when(repository.reactivate(OpenAccountMother.OPEN_ACCOUNT_ID,
                    OpenAccountMother.COMPANY_ID)).thenReturn(1);
            when(repository.findByIdAndCompanyId(OpenAccountMother.OPEN_ACCOUNT_ID,
                    OpenAccountMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(OpenAccountMother.OPEN_ACCOUNT_ID,
                    OpenAccountMother.COMPANY_ID)).isInstanceOf(OpenAccountNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenancy {

        @Test
        @DisplayName("la cuenta de OTRA empresa es 404 y el UPDATE no llega a escribir")
        void cuenta_de_otra_empresa_es_not_found_y_no_escribe() {
            // Antes se reactivaba primero y se comparaba la empresa despues: la unica
            // barrera era el rollback. Ahora la empresa viaja DENTRO del UPDATE, asi que
            // cero filas afectadas es la respuesta y no queda escritura que deshacer.
            OpenAccount ajena = OpenAccountMother.deOtraEmpresa();
            when(repository.reactivate(ajena.getId(), OpenAccountMother.COMPANY_ID)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(ajena.getId(), OpenAccountMother.COMPANY_ID))
                    .isInstanceOf(OpenAccountNotFoundException.class);

            verify(repository, never()).findByIdAndCompanyId(any(), any());
            verify(repository, never()).findById(any());
            verify(repository, never()).save(any());
        }
    }
}
