package com.vetsoftware.app.servicechargeopenaccount.application.port.out;

public interface OpenAccountRefresher {
    // companyId: scope multi-tenant; el caso de uso solo recalcula si la cuenta
    // pertenece a esa
    // empresa.
    void refresh(Long companyId, Long openAccountId);
}
