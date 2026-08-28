package com.vetsoftware.app.customercredit.application.port.in;

import com.vetsoftware.app.customercredit.application.dto.CustomerCreditBalanceDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * <strong>Barrido de plataforma</strong>: saldos vivos de todas las clinicas,
 * incluidas las de clientes que ya se fueron —que es justo el caso que hace
 * falta encontrar, porque un saldo a favor de alguien que se dio de baja es un
 * pasivo que sigue vivo—.
 *
 * <p>
 * Su indice ({@code ix_ccb_applicable}) va sin la empresa delante por lo mismo
 * que el de caducidad, y el hermano acotado por empresa es
 * {@link FindCustomerCreditBalanceUseCase}.
 */
public interface ListAllCustomerCreditBalancesUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<CustomerCreditBalanceDto> listAll(int page, int pageSize);
}
