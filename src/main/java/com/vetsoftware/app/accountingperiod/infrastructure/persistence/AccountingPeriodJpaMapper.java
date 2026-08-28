package com.vetsoftware.app.accountingperiod.infrastructure.persistence;

import com.vetsoftware.app.accountingperiod.domain.AccountingPeriod;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriodKey;
import org.springframework.stereotype.Component;

/**
 * El unico sitio que conoce a la vez el modelo de dominio y la entidad JPA.
 *
 * <p>
 * <strong>Un solo {@code toDomain} y sin sobrecarga de camino de
 * escritura</strong>: el dominio no guarda ningun companion VO —las dos firmas
 * son ids escalares— asi que no hay proxy que se pueda disparar al reconstruir
 * el periodo.
 *
 * <p>
 * <strong>Aqui se cruza la frontera del value object.</strong> La columna es un
 * {@code CHAR(7)} y el dominio guarda un {@link AccountingPeriodKey}; la vuelta
 * pasa por {@code AccountingPeriodKey.of}, que revalida el formato. No es
 * ceremonia: una fila escrita por SQL crudo o por una migracion de datos que se
 * saltara la constraint entraria al dominio como un mes imposible, y el fallo
 * apareceria mucho mas tarde, al comparar claves.
 *
 * <p>
 * <strong>La {@code version} viaja en los dos sentidos.</strong> Sin llevarla
 * al ida, cada {@code save} de un periodo ya persistido le pasaria a Hibernate
 * una version nula y la operacion se convertiria en un {@code INSERT}: el
 * bloqueo optimista dejaria de proteger nada justo en las dos operaciones que
 * mutan el estado.
 */
@Component
public class AccountingPeriodJpaMapper {

    public AccountingPeriodJpaEntity toJpa(AccountingPeriod period) {
        AccountingPeriodJpaEntity entity = new AccountingPeriodJpaEntity();
        entity.setId(period.getId());
        entity.setPeriodKey(period.getPeriodKey().value());
        entity.setStatus(period.getStatus());
        entity.setClosedAt(period.getClosedAt());
        entity.setClosedBySystemUserId(period.getClosedBySystemUserId());
        entity.setReopenedAt(period.getReopenedAt());
        entity.setReopenedBySystemUserId(period.getReopenedBySystemUserId());
        entity.setReopenedReason(period.getReopenedReason());
        entity.setCreatedDate(period.getCreatedDate());
        entity.setVersion(period.getVersion());
        return entity;
    }

    public AccountingPeriod toDomain(AccountingPeriodJpaEntity entity) {
        return new AccountingPeriod(entity.getId(), AccountingPeriodKey.of(entity.getPeriodKey()),
                entity.getStatus(), entity.getClosedAt(), entity.getClosedBySystemUserId(),
                entity.getReopenedAt(), entity.getReopenedBySystemUserId(),
                entity.getReopenedReason(), entity.getCreatedDate(), entity.getVersion());
    }
}
