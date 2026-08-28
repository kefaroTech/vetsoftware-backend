package com.vetsoftware.app.companytaxprofile.application.usecase;

import com.vetsoftware.app.companytaxprofile.application.command.UpdateCompanyTaxProfileCommand;
import com.vetsoftware.app.companytaxprofile.application.dto.CompanyTaxProfileDto;
import com.vetsoftware.app.companytaxprofile.application.port.in.UpdateCompanyTaxProfileUseCase;
import com.vetsoftware.app.companytaxprofile.application.port.out.CompanyTaxProfileRepository;
import com.vetsoftware.app.companytaxprofile.application.port.out.EconomicActivityQueryPort;
import com.vetsoftware.app.companytaxprofile.domain.CompanyDocumentType;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfile;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfileNotFoundException;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfileResponsibility;
import com.vetsoftware.app.companytaxprofile.domain.EconomicActivityRef;
import com.vetsoftware.app.companytaxprofile.domain.NitVerificationDigit;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El cambio de datos fiscales: <strong>cierra el perfil vigente y abre su
 * sucesor</strong>, en una sola transaccion.
 *
 * <h2>Por que ya no es un update</h2>
 *
 * <p>
 * Reescribir la fila cambiaba hacia atras con que identidad se emitieron los
 * documentos anteriores. {@code electronic_documents} congela seis campos del
 * emisor desde 121/136, pero el <strong>nombre comercial</strong>, la
 * <strong>actividad economica</strong> y las <strong>responsabilidades
 * fiscales</strong> se leian siempre de la fila viva —y esos tres viajan en el
 * documento fiscal—. Cerrar y abrir deja las dos verdades: la de entonces y la
 * de ahora, y desde el changeset 364 el documento apunta a la fila exacta con
 * la que se emitio.
 *
 * <h2>El orden de las dos escrituras importa, y no por lo que parece</h2>
 *
 * <p>
 * Se cierra primero y se inserta despues, pero escribirlo en ese orden
 * <strong>no basta</strong>: Hibernate ejecuta todos los {@code INSERT} antes
 * que los {@code UPDATE} de la misma transaccion, asi que el sucesor entraria
 * mientras el anterior sigue vigente y {@code uq_company_tax_profiles_current}
 * pararia la operacion. Lo resuelven dos cosas escritas en el contrato del
 * puerto y no escondidas en el adaptador: {@code close} es un {@code UPDATE}
 * inmediato de una sola columna —no pasa por la cola de acciones— y
 * {@code save} vacia el buffer antes de devolver.
 *
 * <h2>Sucesion en el mismo dia</h2>
 *
 * <p>
 * No es representable: {@code chk_company_tax_profiles_validity} es
 * {@code valid_to > valid_from} estricto. El dominio la rechaza diciendo desde
 * cuando rige el vigente y cual es la primera fecha posible. <strong>No se
 * adelanta al dia siguiente por cuenta propia</strong>: esa fecha es la que
 * decide con que identidad se emitio un documento del intervalo.
 */
@Observed(name = "company.tax.profile.update")
@Service
public class UpdateCompanyTaxProfileService implements UpdateCompanyTaxProfileUseCase {
    private final CompanyTaxProfileRepository repository;
    private final EconomicActivityQueryPort economicActivityQueryPort;
    private final Clock clock;

    public UpdateCompanyTaxProfileService(CompanyTaxProfileRepository repository,
            EconomicActivityQueryPort economicActivityQueryPort, Clock clock) {
        this.repository = repository;
        this.economicActivityQueryPort = economicActivityQueryPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CompanyTaxProfileDto execute(UpdateCompanyTaxProfileCommand command) {
        CompanyTaxProfile current = repository.findCurrentByCompanyId(command.companyId())
                .orElseThrow(() -> new CompanyTaxProfileNotFoundException(command.companyId()));

        // La actividad economica se resuelve ANTES de cerrar nada: un id que no
        // existe tiene que dejar el perfil vigente intacto. Con la transaccion basta
        // para que el UPDATE no cuaje, pero resolver antes hace que el orden de las
        // escrituras no dependa del rollback.
        EconomicActivityRef economicActivity = command.economicActivityId() == null
                ? null
                : economicActivityQueryPort.findById(command.economicActivityId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Economic activity not found: " + command.economicActivityId()));
        List<CompanyTaxProfileResponsibility> responsibilities = toResponsibilities(
                command.responsibilityCodes());
        // El DV del NIT es determinístico (módulo 11): se autocalcula y es
        // autoritativo, ignorando cualquier valor entrante. Para otros tipos de
        // documento no aplica DV. Se calcula antes de cerrar por el mismo motivo que
        // la actividad: un NIT invalido no debe dejar a la empresa sin perfil vigente.
        String verificationDigit = command.companyDocumentType() == CompanyDocumentType.NIT
                ? NitVerificationDigit.calculate(command.companyDocumentId())
                : null;

        LocalDate desde = LocalDate.now(clock);
        current.closeOn(desde);
        // Cero filas afectadas = otra sucesion gano la carrera y esta ficha ya no era
        // la vigente. Seguir insertaria una SEGUNDA vigente, y quien la parara seria
        // uq_company_tax_profiles_current con un Duplicate entry sobre una columna
        // generada que no aparece en ningun sitio del codigo Java.
        if (repository.close(current) == 0)
            throw new IllegalStateException(
                    "El perfil fiscal " + current.getId() + " ya no es el vigente de la empresa "
                            + command.companyId() + ": otra sucesion se adelanto");

        CompanyTaxProfile successor = CompanyTaxProfile.open(current.getCompany(),
                command.companyDocumentType(), command.companyDocumentId(), verificationDigit,
                command.legalName(), command.taxRegime(), command.fiscalEmail(),
                command.commercialName(), economicActivity, responsibilities, desde,
                LocalDateTime.now(clock));
        return CompanyTaxProfileDto.from(repository.save(successor));
    }

    private static List<CompanyTaxProfileResponsibility> toResponsibilities(List<String> codes) {
        if (codes == null)
            return List.of();
        return codes.stream().map(CompanyTaxProfileResponsibility::new).toList();
    }
}
