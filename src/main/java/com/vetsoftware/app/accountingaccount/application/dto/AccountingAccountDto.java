package com.vetsoftware.app.accountingaccount.application.dto;

import com.vetsoftware.app.accountingaccount.domain.AccountClass;
import com.vetsoftware.app.accountingaccount.domain.AccountingAccount;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <strong>Sin {@code version}</strong>: el numero de bloqueo optimista es una
 * barandilla del que escribe, no un dato de la cuenta. Publicarlo invitaria a
 * un cliente a devolverlo y a construir un protocolo de concurrencia que este
 * catalogo —que solo escribe plataforma— no necesita.
 */
public record AccountingAccountDto(Long id, String code, String name, AccountClass accountClass,
        String parentCode, int accountLevel, boolean postable, boolean requiresThirdParty,
        LocalDate validFrom, LocalDate validTo, LocalDateTime createdDate, boolean enabled) {

    public static AccountingAccountDto from(AccountingAccount account) {
        return new AccountingAccountDto(account.getId(), account.getCode(), account.getName(),
                account.getAccountClass(), account.getParentCode(), account.getAccountLevel(),
                account.isPostable(), account.isRequiresThirdParty(), account.getValidFrom(),
                account.getValidTo(), account.getCreatedDate(), account.isEnabled());
    }
}
