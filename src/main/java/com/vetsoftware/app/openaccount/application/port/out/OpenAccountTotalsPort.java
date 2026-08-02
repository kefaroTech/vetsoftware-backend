package com.vetsoftware.app.openaccount.application.port.out;

import java.math.BigDecimal;

public interface OpenAccountTotalsPort {
    BigDecimal totalCharges(Long openAccountId);

    BigDecimal totalPayments(Long openAccountId);
}
