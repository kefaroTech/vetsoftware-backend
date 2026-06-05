package com.vetsoftware.app.debtopenaccount.application.port.out;

public interface OpenAccountRefresher {
    void refresh(Long openAccountId);
}
