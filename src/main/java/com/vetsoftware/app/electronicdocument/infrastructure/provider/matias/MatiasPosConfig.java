package com.vetsoftware.app.electronicdocument.infrastructure.provider.matias;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * B6 — Datos operativos del documento equivalente POS y del fabricante del
 * software (Res. 000165/2023), parametrizables por despliegue vía propiedades
 * {@code vetsoftware.dian.pos.*} / {@code .software.*} en vez de literales en
 * el código. Los defaults conservan los valores previos para no romper entornos
 * existentes.
 */
@Component
public class MatiasPosConfig {
    private final String terminalNumber;
    private final String cashierType;
    private final String salesCode;
    private final String address;
    private final String defaultCashier;
    private final String softwareOwnerName;
    private final String softwareCompanyName;
    private final String softwareName;

    public MatiasPosConfig(
            @Value("${vetsoftware.dian.pos.terminal-number:CJ001}") String terminalNumber,
            @Value("${vetsoftware.dian.pos.cashier-type:Caja principal}") String cashierType,
            @Value("${vetsoftware.dian.pos.sales-code:POS01}") String salesCode,
            @Value("${vetsoftware.dian.pos.address:N/A}") String address,
            @Value("${vetsoftware.dian.pos.default-cashier:Cajero}") String defaultCashier,
            @Value("${vetsoftware.dian.software.owner-name:Lumbre}") String softwareOwnerName,
            @Value("${vetsoftware.dian.software.company-name:Lumbre}") String softwareCompanyName,
            @Value("${vetsoftware.dian.software.name:Lumbre}") String softwareName) {
        this.terminalNumber = terminalNumber;
        this.cashierType = cashierType;
        this.salesCode = salesCode;
        this.address = address;
        this.defaultCashier = defaultCashier;
        this.softwareOwnerName = softwareOwnerName;
        this.softwareCompanyName = softwareCompanyName;
        this.softwareName = softwareName;
    }

    public String terminalNumber() {
        return terminalNumber;
    }

    public String cashierType() {
        return cashierType;
    }

    public String salesCode() {
        return salesCode;
    }

    public String address() {
        return address;
    }

    public String defaultCashier() {
        return defaultCashier;
    }

    public String softwareOwnerName() {
        return softwareOwnerName;
    }

    public String softwareCompanyName() {
        return softwareCompanyName;
    }

    public String softwareName() {
        return softwareName;
    }
}
