package com.vetsoftware.app.customercredit.application.usecase;

import com.vetsoftware.app.customercredit.application.port.out.CustomerCreditBalanceRepository;
import com.vetsoftware.app.customercredit.application.port.out.CustomerCreditEntryRepository;
import com.vetsoftware.app.customercredit.domain.CreditLot;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Lo que los tres casos de uso de escritura hacen igual con la fila resumen.
 *
 * <p>
 * Es una clase de estaticos y no un {@code @Service} a proposito: no tiene
 * estado, no es un puerto y no es un caso de uso. Existe para que
 * {@code nextExpiryOn} se recalcule <strong>de la misma forma</strong> en el
 * abono, en el consumo y en la caducidad; que cada uno lo dedujera por su
 * cuenta es como una proyeccion empieza a mentir.
 */
final class CustomerCreditBalances {

    private CustomerCreditBalances() {
    }

    /**
     * Reescribe la caducidad mas proxima de la empresa a partir del libro.
     *
     * <p>
     * Los lotes llegan <strong>ya ordenados por el que antes caduca, con los sin
     * fecha al final</strong>, asi que el primero que tenga fecha es la respuesta y
     * no hace falta recorrerlos todos. Si ninguno caduca, la columna queda vacia:
     * es un dato ausente, no un cero ni una fecha lejana inventada.
     */
    static void refreshNextExpiry(CustomerCreditEntryRepository entries,
            CustomerCreditBalanceRepository balances, Long companyId, LocalDateTime now) {
        List<CreditLot> openLots = entries.findOpenLotsByCompanyId(companyId);
        LocalDate next = openLots.stream().map(CreditLot::expiresOn)
                .filter(java.util.Objects::nonNull).findFirst().orElse(null);
        balances.refreshNextExpiry(companyId, next, now);
    }
}
