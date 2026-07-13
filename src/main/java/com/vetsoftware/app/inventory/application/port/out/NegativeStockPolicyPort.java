package com.vetsoftware.app.inventory.application.port.out;

public interface NegativeStockPolicyPort {
    /** ¿La empresa permite vender/consumir sin existencias (stock negativo)? Default: false (bloquear). */
    boolean allowsNegative(Long companyId);
}
