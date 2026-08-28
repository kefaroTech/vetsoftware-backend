package com.vetsoftware.app.supplierwithholding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.supplierwithholding.application.command.RegisterSupplierWithholdingPaymentCommand;
import com.vetsoftware.app.supplierwithholding.application.port.out.SupplierWithholdingRepository;
import com.vetsoftware.app.supplierwithholding.domain.SupplierWithholding;
import com.vetsoftware.app.supplierwithholding.domain.SupplierWithholdingNotFoundException;
import com.vetsoftware.app.supplierwithholding.testsupport.SupplierWithholdingMother;
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
@DisplayName("RegisterSupplierWithholdingPaymentService")
class RegisterSupplierWithholdingPaymentServiceTest {

    private static final Long ID = 210L;

    @Mock
    private SupplierWithholdingRepository repository;

    @InjectMocks
    private RegisterSupplierWithholdingPaymentService service;

    @Captor
    private ArgumentCaptor<SupplierWithholding> captor;

    @Nested
    @DisplayName("anotacion del acuse de pago")
    class Anotacion {

        @Test
        @DisplayName("anota el acuse sobre la retencion encontrada y la persiste")
        void anota_el_acuse_sobre_la_retencion_encontrada() {
            SupplierWithholding practicada = SupplierWithholdingMother.conId(ID,
                    SupplierWithholdingMother.renta());
            when(repository.findById(ID)).thenReturn(Optional.of(practicada));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(new RegisterSupplierWithholdingPaymentCommand(ID, "PAGO-2026-001"));

            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getPaymentReceiptRef()).isEqualTo("PAGO-2026-001");
        }

        @Test
        @DisplayName("retencion inexistente no anota acuse de pago")
        void retencion_inexistente_no_anota_acuse() {
            when(repository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service
                    .execute(new RegisterSupplierWithholdingPaymentCommand(ID, "PAGO-2026-001")))
                    .isInstanceOf(SupplierWithholdingNotFoundException.class)
                    .hasMessageContaining("Supplier withholding not found: " + ID);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un acuse de pago en blanco no se acepta")
        void un_acuse_de_pago_en_blanco_no_se_acepta() {
            SupplierWithholding practicada = SupplierWithholdingMother.conId(ID,
                    SupplierWithholdingMother.renta());
            when(repository.findById(ID)).thenReturn(Optional.of(practicada));

            assertThatThrownBy(
                    () -> service.execute(new RegisterSupplierWithholdingPaymentCommand(ID, "   ")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("paymentReceiptRef is required");

            verify(repository, never()).save(any());
        }
    }
}
