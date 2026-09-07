package com.vetsoftware.app.paymentrefund.infrastructure.orchestration;

import com.vetsoftware.app.paymentrefund.application.port.out.WithdrawalDeadlinePort;
import com.vetsoftware.app.publicholiday.application.command.ResolveBusinessDayDeadlineCommand;
import com.vetsoftware.app.publicholiday.application.dto.BusinessDayDeadlineDto;
import com.vetsoftware.app.publicholiday.application.port.in.ResolveBusinessDayDeadlineUseCase;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.stereotype.Component;

/**
 * {@code companyId} viaja {@code null}: quien registra una devolucion ya paso
 * el {@code @PreAuthorize("hasRole('SYSTEM')")} de
 * {@code RegisterPaymentRefundUseCase}, y esa misma autoridad basta para la
 * rama {@code hasRole('SYSTEM')} de {@code ResolveBusinessDayDeadlineUseCase} -
 * la del tenant no aplica aqui.
 */
@Component
public class PublicHolidayWithdrawalDeadlineAdapter implements WithdrawalDeadlinePort {

    private final ResolveBusinessDayDeadlineUseCase resolveUseCase;

    public PublicHolidayWithdrawalDeadlineAdapter(
            ResolveBusinessDayDeadlineUseCase resolveUseCase) {
        this.resolveUseCase = resolveUseCase;
    }

    /**
     * El plazo vence al cierre del dia habil, no a la misma hora del cobro: la ley
     * concede dias completos, y comparar solo la fecha evitaria rechazar un
     * retracto legitimo presentado mas tarde en la jornada de su ultimo dia.
     */
    @Override
    public LocalDateTime deadlineFrom(LocalDateTime receivedAt, int businessDays) {
        BusinessDayDeadlineDto deadline = resolveUseCase
                .resolve(new ResolveBusinessDayDeadlineCommand(receivedAt.toLocalDate(),
                        businessDays, null));
        return deadline.dueDate().atTime(LocalTime.MAX);
    }
}
