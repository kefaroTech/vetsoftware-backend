package com.vetsoftware.app.cashregister.application.usecase;

import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.COMPANY_ID;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.EMPLEADO_ID;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.OTRA_COMPANY_ID;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.SESSION_ID;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.sesionAbierta;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.sesionCerrada;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.cashregister.application.command.RegisterCashMovementCommand;
import com.vetsoftware.app.cashregister.application.dto.CashMovementView;
import com.vetsoftware.app.cashregister.application.dto.CashSessionView;
import com.vetsoftware.app.cashregister.application.port.out.CashSessionRepository;
import com.vetsoftware.app.cashregister.domain.CashMovementType;
import com.vetsoftware.app.cashregister.domain.CashPaymentMethod;
import com.vetsoftware.app.cashregister.domain.CashReferenceType;
import com.vetsoftware.app.cashregister.domain.CashSession;
import com.vetsoftware.app.cashregister.domain.CashSessionClosedException;
import com.vetsoftware.app.cashregister.domain.CashSessionNotFoundException;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterCashMovementService — movimientos manuales de caja")
class RegisterCashMovementServiceTest {

    @Mock
    private CashSessionRepository repository;

    @InjectMocks
    private RegisterCashMovementService service;

    @Captor
    private ArgumentCaptor<CashSession> sesionCaptor;

    private static RegisterCashMovementCommand comando(CashMovementType tipo) {
        return new RegisterCashMovementCommand(COMPANY_ID, SESSION_ID, tipo, CashPaymentMethod.CASH,
                new BigDecimal("20000"), EMPLEADO_ID, "Pago del domicilio");
    }

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("agrega el movimiento a la sesion y la guarda")
        void agrega_el_movimiento_y_guarda() {
            when(repository.findByIdAndCompany(SESSION_ID, COMPANY_ID))
                    .thenReturn(Optional.of(sesionAbierta()));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.register(comando(CashMovementType.EXPENSE));

            verify(repository).save(sesionCaptor.capture());
            assertThat(sesionCaptor.getValue().getMovements()).hasSize(1);
            assertThat(sesionCaptor.getValue().getMovements().getFirst().getType())
                    .isEqualTo(CashMovementType.EXPENSE);
            assertThat(sesionCaptor.getValue().getMovements().getFirst().getAmount())
                    .isEqualByComparingTo("20000");
        }

        @Test
        @DisplayName("marca el movimiento como MANUAL y sin referencia a documento")
        void marca_el_movimiento_como_manual_y_sin_referencia() {
            when(repository.findByIdAndCompany(SESSION_ID, COMPANY_ID))
                    .thenReturn(Optional.of(sesionAbierta()));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.register(comando(CashMovementType.WITHDRAWAL));

            // Un movimiento tecleado no puede colgarse de un documento del POS: si
            // llevara referencia, la idempotencia de la orquestacion lo confundiria con
            // una venta ya registrada.
            verify(repository).save(sesionCaptor.capture());
            assertThat(sesionCaptor.getValue().getMovements().getFirst().getReferenceType())
                    .isEqualTo(CashReferenceType.MANUAL);
            assertThat(sesionCaptor.getValue().getMovements().getFirst().getReferenceId()).isNull();
            assertThat(sesionCaptor.getValue().getMovements().getFirst().getCreatedByEmployeeId())
                    .isEqualTo(EMPLEADO_ID);
        }

        @Test
        @DisplayName("devuelve la sesion con el movimiento y el total ya recalculado")
        void devuelve_la_sesion_con_el_total_recalculado() {
            when(repository.findByIdAndCompany(SESSION_ID, COMPANY_ID))
                    .thenReturn(Optional.of(sesionAbierta()));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CashSessionView vista = service.register(comando(CashMovementType.WITHDRAWAL));

            assertThat(vista.movements()).extracting(CashMovementView::note)
                    .containsExactly("Pago del domicilio");
            // Base 100.000 menos el retiro de 20.000.
            assertThat(vista.totals().getFirst().expectedAmount()).isEqualByComparingTo("80000");
        }
    }

    @Nested
    @DisplayName("solo se aceptan tipos manuales")
    class TiposPermitidos {

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = CashMovementType.class, names = {"SALE_IN", "OPEN_ACCOUNT_IN",
                "VOID_OUT"})
        @NullSource
        @DisplayName("un tipo de orquestacion se rechaza sin tocar el repositorio")
        void un_tipo_de_orquestacion_se_rechaza(CashMovementType tipo) {
            // Si el REST aceptara SALE_IN, cualquiera con permiso de caja podria inventar
            // una venta que no existe en el POS y descuadrar la conciliacion.
            assertThatThrownBy(() -> service.register(comando(tipo)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Solo se permiten movimientos manuales");

            verify(repository, never()).save(any());
            verify(repository, never()).findByIdAndCompany(any(), any());
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = CashMovementType.class, names = {"MANUAL_IN", "WITHDRAWAL", "EXPENSE"})
        @DisplayName("los tres tipos manuales si pasan")
        void los_tres_tipos_manuales_si_pasan(CashMovementType tipo) {
            when(repository.findByIdAndCompany(SESSION_ID, COMPANY_ID))
                    .thenReturn(Optional.of(sesionAbierta()));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.register(comando(tipo));

            verify(repository).save(any());
        }
    }

    @Nested
    @DisplayName("caminos de error: nada se escribe")
    class CaminosDeError {

        @Test
        @DisplayName("sesion inexistente o de otra empresa")
        void sesion_inexistente_o_de_otra_empresa() {
            when(repository.findByIdAndCompany(SESSION_ID, OTRA_COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.register(new RegisterCashMovementCommand(OTRA_COMPANY_ID,
                            SESSION_ID, CashMovementType.EXPENSE, CashPaymentMethod.CASH,
                            new BigDecimal("20000"), EMPLEADO_ID, null)))
                    .isInstanceOf(CashSessionNotFoundException.class)
                    .hasMessageContaining(String.valueOf(SESSION_ID));

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("una sesion ya cerrada no admite mas movimientos")
        void una_sesion_ya_cerrada_no_admite_mas_movimientos() {
            when(repository.findByIdAndCompany(SESSION_ID, COMPANY_ID))
                    .thenReturn(Optional.of(sesionCerrada()));

            // El guard vive en el agregado, no aqui: el caso de uso solo tiene que
            // dejarlo actuar antes de guardar.
            assertThatThrownBy(() -> service.register(comando(CashMovementType.EXPENSE)))
                    .isInstanceOf(CashSessionClosedException.class);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un monto no positivo lo rechaza el movimiento, no la sesion")
        void un_monto_no_positivo_lo_rechaza_el_movimiento() {
            when(repository.findByIdAndCompany(SESSION_ID, COMPANY_ID))
                    .thenReturn(Optional.of(sesionAbierta()));

            assertThatThrownBy(() -> service.register(new RegisterCashMovementCommand(COMPANY_ID,
                    SESSION_ID, CashMovementType.EXPENSE, CashPaymentMethod.CASH, BigDecimal.ZERO,
                    EMPLEADO_ID, null))).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("amount must be positive");

            verify(repository, never()).save(any());
        }
    }
}
