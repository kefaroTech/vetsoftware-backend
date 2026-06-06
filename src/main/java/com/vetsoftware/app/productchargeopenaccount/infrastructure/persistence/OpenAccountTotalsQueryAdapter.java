package com.vetsoftware.app.productchargeopenaccount.infrastructure.persistence;

import com.vetsoftware.app.openaccount.application.port.out.OpenAccountTotalsPort;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.OpenAccountTotalsQueryPort;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Delega en el cálculo de totales centralizado de la feature openaccount, que es la
 * única fuente de verdad para sumar cargos/abonos cruzando los slices hijos.
 */
@Component("productChargeOpenAccountTotalsQueryAdapter")
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
