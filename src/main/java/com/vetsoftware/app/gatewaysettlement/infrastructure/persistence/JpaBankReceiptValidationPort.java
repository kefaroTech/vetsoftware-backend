package com.vetsoftware.app.gatewaysettlement.infrastructure.persistence;

import com.vetsoftware.app.bankreceipt.infrastructure.persistence.BankReceiptJpaRepository;
import com.vetsoftware.app.gatewaysettlement.application.port.out.BankReceiptValidationPort;
import org.springframework.stereotype.Component;

/**
 * El unico archivo de la rodaja que conoce la persistencia del extracto
 * bancario — el cruce que el vertical slicing permite de forma acotada:
 * {@code infrastructure/persistence} de una feature puede importar el
 * {@code XxxJpaRepository} de otra, y nada mas.
 *
 * <p>
 * <strong>{@code existsById} y no {@code findById}</strong>: la consulta que
 * Spring Data genera para el primero es un {@code SELECT COUNT(*)} sobre la
 * clave primaria, sin traer una sola columna del extracto. Esta rodaja no
 * necesita el importe ni la fecha de la entrada, y traerlos para descartarlos
 * ataria esta feature a la forma de la otra.
 */
@Component
public class JpaBankReceiptValidationPort implements BankReceiptValidationPort {

    private final BankReceiptJpaRepository bankReceiptJpaRepository;

    public JpaBankReceiptValidationPort(BankReceiptJpaRepository bankReceiptJpaRepository) {
        this.bankReceiptJpaRepository = bankReceiptJpaRepository;
    }

    @Override
    public boolean exists(Long bankReceiptId) {
        return bankReceiptId != null && bankReceiptJpaRepository.existsById(bankReceiptId);
    }
}
