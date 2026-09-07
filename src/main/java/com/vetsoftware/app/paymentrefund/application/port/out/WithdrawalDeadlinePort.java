package com.vetsoftware.app.paymentrefund.application.port.out;

import java.time.LocalDateTime;

/**
 * Resuelve cuando vence el plazo de retracto de un pago, contra el calendario
 * real de festivos colombianos.
 */
public interface WithdrawalDeadlinePort {

    /**
     * @return el ultimo instante del dia habil en que vence el plazo
     */
    LocalDateTime deadlineFrom(LocalDateTime receivedAt, int businessDays);
}
