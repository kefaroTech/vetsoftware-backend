package com.vetsoftware.app.accountmapping.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.accountmapping.application.command.CreateAccountMappingCommand;
import com.vetsoftware.app.accountmapping.application.dto.AccountMappingDto;
import com.vetsoftware.app.accountmapping.application.port.out.AccountMappingRepository;
import com.vetsoftware.app.accountmapping.application.port.out.AccountingAccountValidationPort;
import com.vetsoftware.app.accountmapping.application.port.out.CatalogItemValidationPort;
import com.vetsoftware.app.accountmapping.domain.AccountMapping;
import com.vetsoftware.app.accountmapping.domain.MappingKind;
import com.vetsoftware.app.accountmapping.testsupport.AccountMappingMother;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateAccountMappingService")
class CreateAccountMappingServiceTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-03-15T10:00:00Z"),
            ZoneOffset.UTC);

    @Mock
    private AccountMappingRepository repository;
    @Mock
    private AccountingAccountValidationPort accountingAccountValidationPort;
    @Mock
    private CatalogItemValidationPort catalogItemValidationPort;

    @Captor
    private ArgumentCaptor<AccountMapping> mappingCaptor;

    private CreateAccountMappingService service;

    @BeforeEach
    void setUp() {
        service = new CreateAccountMappingService(repository, accountingAccountValidationPort,
                catalogItemValidationPort, RELOJ);
    }

    private void debitoYCreditoPostables() {
        when(accountingAccountValidationPort.existsPostableByCode(AccountMappingMother.DEBIT_CODE))
                .thenReturn(true);
        when(accountingAccountValidationPort.existsPostableByCode(AccountMappingMother.CREDIT_CODE))
                .thenReturn(true);
    }

    @Nested
    @DisplayName("creacion sin articulo")
    class SinArticulo {

        @Test
        @DisplayName("persiste el mapeo con la fecha del reloj inyectado, sin tocar el articulo")
        void persiste_con_la_fecha_del_reloj() {
            debitoYCreditoPostables();
            when(repository.save(any())).thenReturn(AccountMappingMother.mapeoBancoAbierto());

            AccountMappingDto dto = service.execute(AccountMappingMother.comandoCrearBanco());

            verify(repository).save(mappingCaptor.capture());
            AccountMapping guardado = mappingCaptor.getValue();
            assertThat(guardado.getMappingKind()).isEqualTo(MappingKind.BANK);
            assertThat(guardado.getCreatedDate()).isEqualTo(LocalDateTime.now(RELOJ));
            assertThat(guardado.getId()).isNull();
            verifyNoInteractions(catalogItemValidationPort);
            assertThat(dto.id()).isEqualTo(AccountMappingMother.MAPPING_ID);
        }
    }

    @Nested
    @DisplayName("creacion con articulo")
    class ConArticulo {

        @Test
        @DisplayName("con el articulo existente, persiste el mapeo de ingreso completo")
        void con_articulo_existente_persiste() {
            debitoYCreditoPostables();
            when(accountingAccountValidationPort
                    .existsPostableByCode(AccountMappingMother.DEFERRED_CODE)).thenReturn(true);
            when(catalogItemValidationPort.existsById(AccountMappingMother.CATALOG_ITEM_ID))
                    .thenReturn(true);
            when(repository.save(any())).thenReturn(AccountMappingMother.mapeoIngresoAbierto());

            service.execute(AccountMappingMother.comandoCrearIngreso());

            verify(repository).save(mappingCaptor.capture());
            assertThat(mappingCaptor.getValue().getCatalogItemId())
                    .isEqualTo(AccountMappingMother.CATALOG_ITEM_ID);
        }

        @Test
        @DisplayName("con el articulo inexistente, lanza y no guarda")
        void con_articulo_inexistente_no_guarda() {
            debitoYCreditoPostables();
            when(accountingAccountValidationPort
                    .existsPostableByCode(AccountMappingMother.DEFERRED_CODE)).thenReturn(true);
            when(catalogItemValidationPort.existsById(AccountMappingMother.CATALOG_ITEM_ID))
                    .thenReturn(false);

            assertThatThrownBy(() -> service.execute(AccountMappingMother.comandoCrearIngreso()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Catalog item not found: " + AccountMappingMother.CATALOG_ITEM_ID);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("catalogItemId nulo no consulta el puerto de articulos en absoluto")
        void catalog_item_id_nulo_no_consulta_el_puerto() {
            debitoYCreditoPostables();
            when(repository.save(any())).thenReturn(AccountMappingMother.mapeoBancoAbierto());

            service.execute(AccountMappingMother.comandoCrearBanco());

            verify(catalogItemValidationPort, never()).existsById(any());
        }
    }

    @Nested
    @DisplayName("cuenta de debito que no admite el mapeo")
    class CuentaDebitoInvalida {

        @Test
        @DisplayName("existe pero no es postable: lanza sin consultar la cuenta de credito")
        void existe_pero_no_es_postable() {
            when(accountingAccountValidationPort
                    .existsPostableByCode(AccountMappingMother.DEBIT_CODE)).thenReturn(false);
            when(accountingAccountValidationPort.existsByCode(AccountMappingMother.DEBIT_CODE))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.execute(AccountMappingMother.comandoCrearBanco()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("debitAccountCode: accounting account "
                            + AccountMappingMother.DEBIT_CODE + " does not accept postings");

            verify(accountingAccountValidationPort, never())
                    .existsPostableByCode(AccountMappingMother.CREDIT_CODE);
            verifyNoInteractions(catalogItemValidationPort, repository);
        }

        @Test
        @DisplayName("no existe en absoluto: el mensaje distingue not-found de no-postable")
        void no_existe_en_absoluto() {
            when(accountingAccountValidationPort
                    .existsPostableByCode(AccountMappingMother.DEBIT_CODE)).thenReturn(false);
            when(accountingAccountValidationPort.existsByCode(AccountMappingMother.DEBIT_CODE))
                    .thenReturn(false);

            assertThatThrownBy(() -> service.execute(AccountMappingMother.comandoCrearBanco()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("debitAccountCode: accounting account not found: "
                            + AccountMappingMother.DEBIT_CODE);

            verifyNoInteractions(repository);
        }
    }

    @Nested
    @DisplayName("cuenta de credito que no admite el mapeo")
    class CuentaCreditoInvalida {

        @Test
        @DisplayName("debito valido, credito sin postable: lanza y no guarda")
        void debito_valido_credito_no_postable() {
            when(accountingAccountValidationPort
                    .existsPostableByCode(AccountMappingMother.DEBIT_CODE)).thenReturn(true);
            when(accountingAccountValidationPort
                    .existsPostableByCode(AccountMappingMother.CREDIT_CODE)).thenReturn(false);
            when(accountingAccountValidationPort.existsByCode(AccountMappingMother.CREDIT_CODE))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.execute(AccountMappingMother.comandoCrearBanco()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("creditAccountCode: accounting account "
                            + AccountMappingMother.CREDIT_CODE + " does not accept postings");

            verifyNoInteractions(catalogItemValidationPort, repository);
        }
    }

    @Nested
    @DisplayName("cuenta diferida que no admite el mapeo")
    class CuentaDiferidaInvalida {

        @Test
        @DisplayName("solo se comprueba cuando el comando trae deferredAccountCode")
        void solo_se_comprueba_cuando_hay_diferido() {
            debitoYCreditoPostables();
            when(repository.save(any())).thenReturn(AccountMappingMother.mapeoBancoAbierto());

            service.execute(AccountMappingMother.comandoCrearBanco());

            // Sin deferredAccountCode en el comando, requirePostable solo se llama para
            // debito y credito: exactamente dos invocaciones, ninguna de una tercera
            // cuenta que el comando ni siquiera trae.
            verify(accountingAccountValidationPort, times(2)).existsPostableByCode(any());
        }

        @Test
        @DisplayName("con deferredAccountCode presente pero no postable, lanza y no guarda")
        void con_diferido_no_postable_lanza() {
            debitoYCreditoPostables();
            when(accountingAccountValidationPort
                    .existsPostableByCode(AccountMappingMother.DEFERRED_CODE)).thenReturn(false);
            when(accountingAccountValidationPort.existsByCode(AccountMappingMother.DEFERRED_CODE))
                    .thenReturn(true);
            CreateAccountMappingCommand comando = new CreateAccountMappingCommand(
                    MappingKind.REVENUE, AccountMappingMother.MAPPING_KEY,
                    AccountMappingMother.CATALOG_ITEM_ID, "CONSULTA", "GRAVADO",
                    AccountMappingMother.DEBIT_CODE, AccountMappingMother.CREDIT_CODE,
                    AccountMappingMother.DEFERRED_CODE, LocalDate.of(2026, 1, 1), null);

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("deferredAccountCode: accounting account "
                            + AccountMappingMother.DEFERRED_CODE + " does not accept postings");

            verifyNoInteractions(catalogItemValidationPort, repository);
        }
    }
}
