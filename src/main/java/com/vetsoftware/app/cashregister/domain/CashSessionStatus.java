package com.vetsoftware.app.cashregister.domain;

/** Estado de una sesión de caja. Una sola OPEN por (empresa, sede, terminal); al cerrar pasa a CLOSED (definitivo). */
public enum CashSessionStatus {
    OPEN,
    CLOSED
}
