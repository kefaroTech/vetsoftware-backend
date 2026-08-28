package com.vetsoftware.app.companylimitevent.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.companylimitevent.application.command.RecordLimitEventCommand;
import com.vetsoftware.app.companylimitevent.application.port.out.CompanyLimitEventRepository;
import com.vetsoftware.app.companylimitevent.domain.CompanyLimitEvent;
import com.vetsoftware.app.companylimitevent.domain.EventActor;
import com.vetsoftware.app.companylimitevent.domain.LimitEventType;
import com.vetsoftware.app.companylimitevent.domain.LimitSource;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
/**
 * <b>Lo que R-LIMIT-18 exige no se prueba aquí, y antes se fingía que sí.</b>
 * Este archivo tenía un caso que leía la anotación {@code @Transactional} por
 * reflexión y comprobaba que decía {@code REQUIRES_NEW}. Eso confirma lo que
 * está escrito en el fuente, no lo que ocurre: la propagación la aplica el
 * proxy de Spring, así que pasaba en verde con el proxy ausente, con el
 * servicio invocado por autollamada desde su propia clase, o con la gestión de
 * transacciones apagada — y en los tres casos la fila que existe para demostrar
 * el límite se va con la transacción que revierte.
 *
 * <p>
 * La regla vive ahora en
 * {@code companylimitevent.infrastructure.persistence.RecordLimitEventRollbackIT},
 * que provoca el rechazo, deshace la transacción externa y <em>después</em> lee
 * la fila contra MySQL real. Lo que queda aquí es lo que un test unitario sí
 * puede afirmar: qué se escribe, y que no se consulta nada mientras la
 * transacción externa sigue viva.
 */
@DisplayName("RecordLimitEventService — qué escribe el hecho de cupo")
class RecordLimitEventServiceTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-03-14T15:30:00Z"),
            ZoneOffset.UTC);

    @Mock
    private CompanyLimitEventRepository repository;

    @Test
    @DisplayName("escribe el hecho con los tres números del momento y el empleado que lo intentó")
    void escribe_el_hecho_con_los_tres_numeros_del_momento() {
        when(repository.append(any())).thenAnswer(invocation -> invocation.getArgument(0));
        RecordLimitEventService service = new RecordLimitEventService(repository, RELOJ);

        service.execute(new RecordLimitEventCommand(42L, 1L, LimitEventType.LIMIT_BLOCKED, 100, 100,
                1, LimitSource.CATALOG_DEFAULT, null, EventActor.employee(9L), null, null));

        ArgumentCaptor<CompanyLimitEvent> escrito = ArgumentCaptor
                .forClass(CompanyLimitEvent.class);
        verify(repository).append(escrito.capture());
        assertThat(escrito.getValue()).satisfies(hecho -> {
            assertThat(hecho.getEventType()).isEqualTo(LimitEventType.LIMIT_BLOCKED);
            assertThat(hecho.getLimitQuantity()).isEqualTo(100);
            assertThat(hecho.getUsedQuantity()).isEqualTo(100);
            assertThat(hecho.getRequestedDelta()).isEqualTo(1);
            assertThat(hecho.getActor().employeeId()).isEqualTo(9L);
            assertThat(hecho.getOccurredAt())
                    .isEqualTo(java.time.LocalDateTime.of(2026, 3, 14, 15, 30));
        });
    }

    @Test
    @DisplayName("todos los números llegan resueltos en el command: el hecho no consulta nada"
            + " mientras la transacción externa sigue viva")
    void el_hecho_no_consulta_nada_mas_que_su_repositorio() {
        when(repository.append(any())).thenAnswer(invocation -> invocation.getArgument(0));
        RecordLimitEventService service = new RecordLimitEventService(repository, RELOJ);

        service.execute(new RecordLimitEventCommand(42L, 1L, LimitEventType.THRESHOLD_WARNED, 100,
                80, 1, LimitSource.SUBSCRIPTION, null, EventActor.automatedProcess(), null, null));

        verify(repository).append(any());
        org.mockito.Mockito.verifyNoMoreInteractions(repository);
    }
}
