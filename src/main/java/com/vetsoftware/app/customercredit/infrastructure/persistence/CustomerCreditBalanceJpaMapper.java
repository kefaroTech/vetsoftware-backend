package com.vetsoftware.app.customercredit.infrastructure.persistence;

import com.vetsoftware.app.customercredit.domain.CustomerCreditBalance;
import org.springframework.stereotype.Component;

/**
 * Solo tiene camino de lectura, y esa asimetria es intencionada: la fila
 * resumen no se escribe mapeando un agregado de dominio, se escribe con las
 * instrucciones condicionales de {@link CustomerCreditBalanceJpaRepository}. Un
 * {@code toJpa} aqui seria la puerta por la que volveria el ciclo
 * leer-modificar-guardar que esta tabla existe para impedir.
 */
@Component
public class CustomerCreditBalanceJpaMapper {

    public CustomerCreditBalance toDomain(CustomerCreditBalanceJpaEntity entity) {
        return new CustomerCreditBalance(entity.getId(), entity.getCompanyId(),
                entity.getBalanceAmount(), entity.getNextExpiryOn(), entity.getRecalculatedAt(),
                entity.getVersion());
    }
}
