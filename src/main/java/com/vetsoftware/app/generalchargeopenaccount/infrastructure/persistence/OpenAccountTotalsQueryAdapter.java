package com.vetsoftware.app.generalchargeopenaccount.infrastructure.persistence;

import com.vetsoftware.app.generalchargeopenaccount.application.port.out.OpenAccountTotalsQueryPort;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountTotalsPort;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Delega en el cálculo de totales centralizado de la feature openaccount, que es la
 * única fuente de verdad para sumar cargos/abonos cruzando los slices hijos.
 */
@Component("generalChargeOpenAccountTotalsQueryAdapter")
public class OpenAccountTotalsQueryAdapter implements OpenAccountTotalsQueryPort {
    private final OpenAccountTotalsPort openAccountTotalsPort;

    public OpenAccountTotalsQueryAdapter(OpenAccountTotalsPort openAccountTotalsPort) {
        this.openAccountTotalsPort = openAccountTotalsPort;
    }

    @Override
    public BigDecimal totalCharges(Long openAccountId) {
        return openAccountTotalsPort.totalCharges(openAccountId);
    }

    @Override
    public BigDecimal totalPayments(Long openAccountId) {
        return openAccountTotalsPort.totalPayments(openAccountId);
    }
}
