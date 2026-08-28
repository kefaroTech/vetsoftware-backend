package com.vetsoftware.app.companybillingprofile.application.usecase;

import com.vetsoftware.app.companybillingprofile.application.command.SucceedCompanyBillingProfileCommand;
import com.vetsoftware.app.companybillingprofile.application.dto.CompanyBillingProfileDto;
import com.vetsoftware.app.companybillingprofile.application.port.in.SucceedCompanyBillingProfileUseCase;
import com.vetsoftware.app.companybillingprofile.application.port.out.CityQueryPort;
import com.vetsoftware.app.companybillingprofile.application.port.out.CompanyBillingProfileRepository;
import com.vetsoftware.app.companybillingprofile.domain.CityRef;
import com.vetsoftware.app.companybillingprofile.domain.CompanyBillingProfile;
import com.vetsoftware.app.companybillingprofile.domain.CompanyBillingProfileNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El cambio de datos de facturacion: cierra la ficha vigente y abre su
 * sucesora, <strong>en una sola transaccion</strong>.
 *
 * <h2>Por que no es un update</h2>
 *
 * <p>
 * Reescribir la fila cambiaria hacia atras a quien se le emitieron las facturas
 * anteriores. {@code subscription_billing_documents} apunta a la ficha por
 * {@code (company_id, billing_profile_id)}, asi que la factura del año pasado
 * seguiria enlazada a la misma fila y esa fila diria otra cosa. Cerrar y abrir
 * deja las dos verdades: la de entonces y la de ahora.
 *
 * <h2>El orden de las dos escrituras importa, y no por lo que parece</h2>
 *
 * <p>
 * Se cierra primero y se inserta despues, pero escribirlo en ese orden
 * <strong>no basta</strong>: Hibernate ejecuta todos los {@code INSERT} antes
 * que los {@code UPDATE} de la misma transaccion, asi que la sucesora entraria
 * mientras la anterior sigue vigente y
 * {@code uq_company_billing_profiles_current} pararia la operacion. Lo que lo
 * resuelve es que {@code CompanyBillingProfileRepository.save} vacia el buffer
 * antes de devolver, y por eso ese flush esta escrito en el contrato del puerto
 * y no escondido en el adaptador.
 *
 * <h2>Sucesion en el mismo dia</h2>
 *
 * <p>
 * No es representable: {@code chk_company_billing_profiles_validity} es
 * {@code valid_to > valid_from} estricto. El dominio la rechaza con
 * {@code BillingProfileSuccessionNotAfterCurrentException}, que dice desde
 * cuando rige la vigente y cual es la primera fecha posible. <strong>No se
 * adelanta al dia siguiente por cuenta propia</strong>: esa fecha es la que
 * decide a que ficha apunta una factura emitida en el intervalo.
 */
@Observed(name = "company.billing.profile.succeed")
@Service
public class SucceedCompanyBillingProfileService implements SucceedCompanyBillingProfileUseCase {

    private final CompanyBillingProfileRepository repository;
    private final CityQueryPort cityQueryPort;
    private final Clock clock;

    public SucceedCompanyBillingProfileService(CompanyBillingProfileRepository repository,
            CityQueryPort cityQueryPort, Clock clock) {
        this.repository = repository;
        this.cityQueryPort = cityQueryPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CompanyBillingProfileDto execute(SucceedCompanyBillingProfileCommand command) {
        CompanyBillingProfile current = repository.findCurrentByCompanyId(command.companyId())
                .orElseThrow(() -> CompanyBillingProfileNotFoundException
                        .withoutCurrentProfile(command.companyId()));

        // El municipio se resuelve ANTES de cerrar nada: un id de ciudad que no
        // existe tiene que dejar la ficha vigente intacta. Con la transaccion basta
        // para que el UPDATE no cuaje, pero resolver antes hace que el orden de las
        // escrituras no dependa del rollback.
        CityRef city = cityQueryPort.findById(command.cityId()).orElseThrow(
                () -> new IllegalArgumentException("City not found: " + command.cityId()));

        current.closeOn(command.effectiveFrom());
        repository.save(current);

        CompanyBillingProfile successor = CompanyBillingProfile.open(command.companyId(),
                command.personKind(), command.taxIdKind(), command.taxId(),
                command.verificationDigit(), command.legalName(), command.firstName(),
                command.middleName(), command.lastName(), command.secondLastName(),
                command.address(), city, command.billingEmail(), command.taxRegime(),
                command.withholdingAgent(), command.effectiveFrom(), LocalDateTime.now(clock));

        return CompanyBillingProfileDto.from(repository.save(successor));
    }
}
