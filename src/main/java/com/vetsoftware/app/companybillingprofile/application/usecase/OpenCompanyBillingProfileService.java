package com.vetsoftware.app.companybillingprofile.application.usecase;

import com.vetsoftware.app.companybillingprofile.application.command.OpenCompanyBillingProfileCommand;
import com.vetsoftware.app.companybillingprofile.application.dto.CompanyBillingProfileDto;
import com.vetsoftware.app.companybillingprofile.application.port.in.OpenCompanyBillingProfileUseCase;
import com.vetsoftware.app.companybillingprofile.application.port.out.CityQueryPort;
import com.vetsoftware.app.companybillingprofile.application.port.out.CompanyBillingProfileRepository;
import com.vetsoftware.app.companybillingprofile.domain.CityRef;
import com.vetsoftware.app.companybillingprofile.domain.CompanyBillingProfile;
import com.vetsoftware.app.companybillingprofile.domain.CompanyBillingProfileAlreadyOpenException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abre la primera ficha de facturacion de una empresa.
 *
 * <p>
 * <strong>La comprobacion previa no sustituye a
 * {@code uq_company_billing_profiles_current}: la traduce.</strong> Entre la
 * lectura y el {@code INSERT} cabe otra transaccion, asi que lo unico que
 * garantiza que no haya dos fichas vigentes es la columna generada y su indice
 * unico. Lo que se gana aqui es que el caso comun —el boton pulsado dos veces,
 * la empresa que ya tiene ficha y busca «crear» en vez de «cambiar»— conteste
 * un 409 que explica que existe la sucesion, en vez de un 500 con un
 * {@code Duplicate entry} sobre una columna que no aparece en ningun sitio del
 * codigo.
 *
 * <p>
 * <strong>El municipio se resuelve aqui y no en el repositorio.</strong> Es lo
 * que pide el CLAUDE.md para las FK cross-feature: el adaptador usa
 * {@code getReferenceById} sin validar, y quien traduce «ese id no existe» en
 * un mensaje con el id delante es este servicio.
 */
@Observed(name = "company.billing.profile.open")
@Service
public class OpenCompanyBillingProfileService implements OpenCompanyBillingProfileUseCase {

    private final CompanyBillingProfileRepository repository;
    private final CityQueryPort cityQueryPort;
    private final Clock clock;

    public OpenCompanyBillingProfileService(CompanyBillingProfileRepository repository,
            CityQueryPort cityQueryPort, Clock clock) {
        this.repository = repository;
        this.cityQueryPort = cityQueryPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CompanyBillingProfileDto execute(OpenCompanyBillingProfileCommand command) {
        if (repository.findCurrentByCompanyId(command.companyId()).isPresent())
            throw new CompanyBillingProfileAlreadyOpenException(command.companyId());

        CityRef city = cityQueryPort.findById(command.cityId()).orElseThrow(
                () -> new IllegalArgumentException("City not found: " + command.cityId()));

        CompanyBillingProfile profile = CompanyBillingProfile.open(command.companyId(),
                command.personKind(), command.taxIdKind(), command.taxId(),
                command.verificationDigit(), command.legalName(), command.firstName(),
                command.middleName(), command.lastName(), command.secondLastName(),
                command.address(), city, command.billingEmail(), command.taxRegime(),
                command.withholdingAgent(), command.validFrom(), LocalDateTime.now(clock));

        return CompanyBillingProfileDto.from(repository.save(profile));
    }
}
