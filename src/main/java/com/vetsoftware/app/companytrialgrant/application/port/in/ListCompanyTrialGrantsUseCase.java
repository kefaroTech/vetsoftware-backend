package com.vetsoftware.app.companytrialgrant.application.port.in;

import com.vetsoftware.app.companytrialgrant.application.dto.CompanyTrialGrantDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Qué ha probado ya esta empresa, y hasta cuándo.
 *
 * <p>
 * Es el listado acotado por empresa: recibe el {@code companyId} y lo revalida
 * contra el principal. El hermano sin empresa —el barrido de vencimientos— es
 * otro caso de uso y va cerrado a plataforma.
 */
public interface ListCompanyTrialGrantsUseCase {

    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('companyTrialGrant.read')"
            + " and @authz.isMyCompany(#companyId))")
    List<CompanyTrialGrantDto> listByCompanyId(Long companyId);
}
