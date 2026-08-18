package com.vetsoftware.app.openaccount.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
@DisplayName("DeleteOpenAccountService")
class DeleteOpenAccountServiceTest {

    @Mock
    private OpenAccountRepository repository;
    @InjectMocks
    private DeleteOpenAccountService service;

    @Nested
    @DisplayName("borrado")
    class Borrado {

        @Test
        @DisplayName("borra la cuenta de la empresa")
        void borra_la_cuenta_de_la_empresa() {
            OpenAccount cuenta = OpenAccountMother.abierta();
            when(repository.findByIdAndCompanyId(OpenAccountMother.OPEN_ACCOUNT_ID,
                    OpenAccountMother.COMPANY_ID)).thenReturn(Optional.of(cuenta));

            service.execute(OpenAccountMother.OPEN_ACCOUNT_ID, OpenAccountMother.COMPANY_ID);

            verify(repository).delete(OpenAccountMother.OPEN_ACCOUNT_ID);
        }
    }

    @Nested
    @DisplayName("validaciones que no deben borrar")
    class Validaciones {

        @Test
        @DisplayName("cuenta inexistente lanza y no borra")
        void cuenta_inexistente_lanza_y_no_borra() {
            when(repository.findByIdAndCompanyId(OpenAccountMother.OPEN_ACCOUNT_ID,
                    OpenAccountMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(OpenAccountMother.OPEN_ACCOUNT_ID,
                    OpenAccountMother.COMPANY_ID)).isInstanceOf(OpenAccountNotFoundException.class);

            verify(repository, never()).delete(OpenAccountMother.OPEN_ACCOUNT_ID);
        }

        /**
         * El filtro por empresa vive ahora EN la consulta: la cuenta ajena no llega a
         * cargarse, asi que el desenlace es un 404 y no un mensaje que confirme su
         * existencia en otro tenant. Se comprueba ademas que el servicio no toca la
         * variante ancha.
         */
        @Test
        @DisplayName("una cuenta de otra empresa se rechaza y no borra")
        void cuenta_de_otra_empresa_se_rechaza() {
            OpenAccount ajena = OpenAccountMother.deOtraEmpresa();
            when(repository.findByIdAndCompanyId(ajena.getId(), OpenAccountMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ajena.getId(), OpenAccountMother.COMPANY_ID))
                    .isInstanceOf(OpenAccountNotFoundException.class);

            verify(repository, never()).delete(ajena.getId());
            verify(repository, never()).findById(ajena.getId());
        }
    }
}
