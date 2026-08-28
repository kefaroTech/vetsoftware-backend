package com.vetsoftware.app.bankreceipt.infrastructure.persistence;

import com.vetsoftware.app.bankreceipt.domain.BankReceipt;
import org.springframework.stereotype.Component;

/**
 * El unico sitio que conoce a la vez el modelo de dominio y la entidad JPA.
 *
 * <p>
 * <strong>Un solo {@code toDomain} y sin sobrecarga de camino de
 * escritura</strong>: el dominio no guarda ningun companion VO —la tabla no
 * referencia a nadie— asi que no hay proxy que se pueda disparar al reconstruir
 * la entrada.
 *
 * <p>
 * <strong>La {@code version} viaja en los dos sentidos.</strong> Sin llevarla
 * al ida, cada {@code save} de una entrada ya persistida le pasaria a Hibernate
 * una version nula y la operacion se convertiria en un {@code INSERT}: el
 * bloqueo optimista dejaria de proteger nada justo en la operacion que muta el
 * estado.
 */
@Component
public class BankReceiptJpaMapper {

    public BankReceiptJpaEntity toJpa(BankReceipt receipt) {
        BankReceiptJpaEntity entity = new BankReceiptJpaEntity();
        entity.setId(receipt.getId());
        entity.setBankAccountRef(receipt.getBankAccountRef());
        entity.setBankReference(receipt.getBankReference());
        entity.setReceivedOn(receipt.getReceivedOn());
        entity.setAmount(receipt.getAmount());
        entity.setDescription(receipt.getDescription());
        entity.setStatus(receipt.getStatus());
        entity.setIdentifiedAt(receipt.getIdentifiedAt());
        entity.setCreatedDate(receipt.getCreatedDate());
        entity.setVersion(receipt.getVersion());
        return entity;
    }

    public BankReceipt toDomain(BankReceiptJpaEntity entity) {
        return new BankReceipt(entity.getId(), entity.getBankAccountRef(),
                entity.getBankReference(), entity.getReceivedOn(), entity.getAmount(),
                entity.getDescription(), entity.getStatus(), entity.getIdentifiedAt(),
                entity.getCreatedDate(), entity.getVersion());
    }
}
