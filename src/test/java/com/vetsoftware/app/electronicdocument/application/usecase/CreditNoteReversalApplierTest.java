package com.vetsoftware.app.electronicdocument.application.usecase;

import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.CUFE;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.COMPANY_ID;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.facturaValidada;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.notaCreditoParcial;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.notaCreditoPendiente;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.notaCreditoTotal;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.notaDebito;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.posValidado;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.application.port.out.AccountReversalPort;
import com.vetsoftware.app.electronicdocument.application.port.out.CashPort;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.InventoryLedgerPort;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import com.vetsoftware.app.electronicdocument.domain.PaymentForm;
import com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreditNoteReversalApplier — subordina el reverso de cartera a la validacion DIAN")
class CreditNoteReversalApplierTest {

    @Mock
    private ElectronicDocumentRepository repository;
    @Mock
    private AccountReversalPort accountReversalPort;
    @Mock
    private InventoryLedgerPort inventoryLedger;
    @Mock
    private CashPort cashPort;

    private CreditNoteReversalApplier applier;

    @BeforeEach
    void montar() {
        applier = new CreditNoteReversalApplier(repository, accountReversalPort, inventoryLedger,
                cashPort);
    }

    /**
     * Documento equivalente POS VALIDADO sin ningun pago registrado: el "sin pagos"
     * es el otro lado del {@code if (!payments.isEmpty())} que decide si se
     * compensa la caja.
     */
    private static ElectronicDocument posValidadoSinPagos(Long id) {
        return new ElectronicDocument(id, COMPANY_ID, null, ElectronicDocumentType.DOC_EQUIV_POS,
                "POS", 55L, null, LocalDate.of(2026, 3, 10), "10:15:00-05:00", null, "CUDE-POS-X",
                "uuid-1", null, null, null, null, DianStatus.VALIDADO,
                LocalDateTime.of(2026, 3, 10, 10, 16), ElectronicDocumentMother.issuer(),
                ElectronicDocumentMother.customer(), ElectronicDocumentMother.bd("1000.00"),
                ElectronicDocumentMother.bd("1000.00"), ElectronicDocumentMother.bd("1190.00"),
                ElectronicDocumentMother.bd("1190.00"), PaymentForm.CONTADO,
                ElectronicDocumentMother.unaLineaGravada(), List.of(),
                LocalDateTime.of(2026, 3, 10, 10, 15), true, null, null, null, false,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null,
                ElectronicDocumentMother.EMPLOYEE_ID, ElectronicDocumentMother.BRANCH_ID);
    }

    @Nested
    @DisplayName("guardas — no aplica nada")
    class Guardas {

        @Test
        @DisplayName("una nota debito no reversa nada")
        void nota_debito_no_reversa() {
            applier.applyIfCreditNoteValidated(notaDebito(70L));

            verifyNoInteractions(repository, accountReversalPort, inventoryLedger, cashPort);
        }

        @Test
        @DisplayName("una nota credito PENDIENTE (aun no validada) no reversa nada")
        void nota_credito_pendiente_no_reversa() {
            applier.applyIfCreditNoteValidated(notaCreditoPendiente(71L));

            verifyNoInteractions(repository, accountReversalPort, inventoryLedger, cashPort);
        }

        @Test
        @DisplayName("sin factura original localizable por CUFE no hace nada")
        void sin_original_localizable_no_hace_nada() {
            when(repository.findByCufe(CUFE, COMPANY_ID)).thenReturn(Optional.empty());

            applier.applyIfCreditNoteValidated(notaCreditoTotal(72L));

            verifyNoInteractions(accountReversalPort, inventoryLedger, cashPort);
        }
    }

    @Nested
    @DisplayName("nota credito PARCIAL — no reversa la venta completa")
    class Parcial {

        @Test
        @DisplayName("una nota parcial no marca reversada la factura ni toca inventario/caja")
        void nota_parcial_no_reversa_la_factura() {
            ElectronicDocument original = facturaValidada(1L);
            when(repository.findByCufe(CUFE, COMPANY_ID)).thenReturn(Optional.of(original));

            applier.applyIfCreditNoteValidated(notaCreditoParcial(73L));

            assertThat(original.isReversed()).isFalse();
            verify(repository, never()).updateDianResult(any());
            verifyNoInteractions(inventoryLedger, cashPort);
        }
    }

    @Nested
    @DisplayName("nota credito TOTAL — reversa la factura y compensa la venta")
    class Total {

        @Test
        @DisplayName("marca la factura reversada y persiste el resultado")
        void marca_la_factura_reversada_y_persiste() {
            ElectronicDocument original = facturaValidada(1L);
            when(repository.findByCufe(CUFE, COMPANY_ID)).thenReturn(Optional.of(original));

            applier.applyIfCreditNoteValidated(notaCreditoTotal(74L));

            assertThat(original.isReversed()).isTrue();
            verify(repository).updateDianResult(original);
        }

        @Test
        @DisplayName("factura sin cuenta abierta (POS directo): repone inventario y compensa caja")
        void factura_pos_directa_repone_inventario_y_compensa_caja() {
            ElectronicDocument original = posValidado(2L);
            when(repository.findByCufe(CUFE, COMPANY_ID)).thenReturn(Optional.of(original));

            applier.applyIfCreditNoteValidated(notaCreditoTotal(75L));

            verify(inventoryLedger).reversePosSale(original.getId(), original.getCompanyId(), null);
            verify(cashPort).reverseSale(org.mockito.ArgumentMatchers.eq(original.getCompanyId()),
                    org.mockito.ArgumentMatchers.eq(original.getBranchId()),
                    org.mockito.ArgumentMatchers.eq(original.getId()), any(List.class),
                    org.mockito.ArgumentMatchers.isNull());
        }

        @Test
        @DisplayName("factura por cuenta abierta: no repone inventario (se repone al anular cada cargo)")
        void factura_por_cuenta_no_repone_inventario() {
            ElectronicDocument original = facturaValidada(1L);
            when(repository.findByCufe(CUFE, COMPANY_ID)).thenReturn(Optional.of(original));

            applier.applyIfCreditNoteValidated(notaCreditoTotal(76L));

            verifyNoInteractions(inventoryLedger, cashPort);
        }

        @Test
        @DisplayName("una factura ya reversada previamente es idempotente: no repite el efecto contable")
        void factura_ya_reversada_es_idempotente() {
            ElectronicDocument original = posValidado(2L);
            original.markReversed();
            when(repository.findByCufe(CUFE, COMPANY_ID)).thenReturn(Optional.of(original));

            applier.applyIfCreditNoteValidated(notaCreditoTotal(77L));

            verify(repository, never()).updateDianResult(any());
            verifyNoInteractions(inventoryLedger, cashPort);
        }

        @Test
        @DisplayName("factura POS directa sin pagos registrados no intenta compensar caja")
        void factura_pos_sin_pagos_no_compensa_caja() {
            ElectronicDocument original = posValidadoSinPagos(3L);
            when(repository.findByCufe(CUFE, COMPANY_ID)).thenReturn(Optional.of(original));

            applier.applyIfCreditNoteValidated(notaCreditoTotal(79L));

            verify(inventoryLedger).reversePosSale(original.getId(), original.getCompanyId(), null);
            verifyNoInteractions(cashPort);
        }

        @Test
        @DisplayName("la nota tiene cuenta abierta asociada: marca esa cuenta reversada")
        void marca_reversada_la_cuenta_de_la_nota() {
            ElectronicDocument original = facturaValidada(1L);
            when(repository.findByCufe(CUFE, COMPANY_ID)).thenReturn(Optional.of(original));
            ElectronicDocument nota = notaCreditoTotal(78L);

            applier.applyIfCreditNoteValidated(nota);

            verify(accountReversalPort).markReversed(nota.getOpenAccountId());
        }
    }
}
