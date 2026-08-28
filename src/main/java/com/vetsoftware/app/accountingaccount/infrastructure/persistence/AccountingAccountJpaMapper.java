package com.vetsoftware.app.accountingaccount.infrastructure.persistence;

import com.vetsoftware.app.accountingaccount.domain.AccountingAccount;
import org.springframework.stereotype.Component;

/**
 * El unico sitio que conoce a la vez el modelo de dominio y la entidad JPA.
 *
 * <p>
 * <strong>Copia la version en los dos sentidos, y de eso depende que una
 * correccion sea una edicion y no un insert.</strong> Si {@code toJpa} dejara
 * la version en {@code null} sobre una entidad que ya tiene id, Hibernate la
 * tomaria por transitoria y el {@code merge} escribiria una fila nueva — que
 * chocaria contra {@code uq_accounting_accounts_code}.
 *
 * <p>
 * <strong>Es tambien el unico sitio donde el nivel cambia de forma.</strong> El
 * dominio lo expone como {@code int} porque su rango (1, 2, 4, 6) cabe de sobra
 * y evita conversiones en cada llamada; la entidad lo declara {@code byte}
 * porque la columna es {@code TINYINT} y con {@code ddl-auto: validate} un
 * {@code int} ahi impide construir el {@code SessionFactory} — y con el,
 * arrancar cualquier contexto del repositorio. El {@code cast} es seguro porque
 * el constructor del dominio ya rechazo cualquier valor fuera de esa lista.
 */
@Component
public class AccountingAccountJpaMapper {

    public AccountingAccountJpaEntity toJpa(AccountingAccount account) {
        AccountingAccountJpaEntity entity = new AccountingAccountJpaEntity();
        entity.setId(account.getId());
        entity.setCode(account.getCode());
        entity.setName(account.getName());
        entity.setAccountClass(account.getAccountClass());
        entity.setParentCode(account.getParentCode());
        entity.setAccountLevel((byte) account.getAccountLevel());
        entity.setPostable(account.isPostable());
        entity.setRequiresThirdParty(account.isRequiresThirdParty());
        entity.setValidFrom(account.getValidFrom());
        entity.setValidTo(account.getValidTo());
        entity.setCreatedDate(account.getCreatedDate());
        entity.setEnabled(account.isEnabled());
        entity.setVersion(account.getVersion());
        return entity;
    }

    public AccountingAccount toDomain(AccountingAccountJpaEntity entity) {
        return new AccountingAccount(entity.getId(), entity.getCode(), entity.getName(),
                entity.getAccountClass(), entity.getParentCode(), entity.getAccountLevel(),
                entity.isPostable(), entity.isRequiresThirdParty(), entity.getValidFrom(),
                entity.getValidTo(), entity.getCreatedDate(), entity.isEnabled(),
                entity.getVersion());
    }
}
