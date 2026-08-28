package com.vetsoftware.app.externalinvoicingoutage.application.usecase;

import com.vetsoftware.app.externalinvoicingoutage.application.command.RegisterAffectedCompanyCommand;
import com.vetsoftware.app.externalinvoicingoutage.application.dto.OutageAffectedCompanyDto;
import com.vetsoftware.app.externalinvoicingoutage.application.port.in.RegisterAffectedCompanyUseCase;
import com.vetsoftware.app.externalinvoicingoutage.application.port.out.CompanyValidationPort;
import com.vetsoftware.app.externalinvoicingoutage.application.port.out.ExternalInvoicingOutageCompanyRepository;
import com.vetsoftware.app.externalinvoicingoutage.application.port.out.ExternalInvoicingOutageRepository;
import com.vetsoftware.app.externalinvoicingoutage.domain.AffectedCompanyAlreadyRegisteredException;
import com.vetsoftware.app.externalinvoicingoutage.domain.ExternalInvoicingOutageCompany;
import com.vetsoftware.app.externalinvoicingoutage.domain.ExternalInvoicingOutageNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mete a una clinica en el reparto de una caida.
 *
 * <p>
 * Comprueba tres cosas antes de escribir, y ninguna es una invariante de la
 * fila —esas viven en el constructor de
 * {@link ExternalInvoicingOutageCompany}—:
 *
 * <ol>
 * <li>Que la caida existe. Sin esto, la clave foranea {@code fk_eioc_outage}
 * rechazaria la fila como un error de integridad en vez de como el «esa caida
 * no existe» que corresponde.
 * <li>Que la clinica existe, por el mismo motivo y contra
 * {@code fk_eioc_company}.
 * <li>Que no estaba ya en el reparto.
 * </ol>
 *
 * <p>
 * <strong>La tercera es cortesia, no barandilla, y conviene no
 * confundirlas.</strong> Quien impide de verdad el duplicado es
 * {@code uq_eioc_pair}: dos peticiones concurrentes pasarian las dos por el
 * {@code exists} y solo una sobreviviria al indice. Esta comprobacion existe
 * porque el llamador normal es un proceso que arma el reparto y puede morir a
 * mitad de lote, asi que el reintento es el caso corriente y merece un 409
 * legible en vez de una violacion de integridad cruda.
 *
 * <p>
 * <strong>Y no sobrescribe la fila que ya esta.</strong> Su
 * {@code failed_document_count} es el numero que sostiene la reclamacion de esa
 * clinica; machacarlo con el de un reintento posterior —que puede haber contado
 * distinto— es perder el dato original sin dejar rastro.
 */
@Observed(name = "external.invoicing.outage.company.register")
@Service
public class RegisterAffectedCompanyService implements RegisterAffectedCompanyUseCase {

    private final ExternalInvoicingOutageCompanyRepository repository;
    private final ExternalInvoicingOutageRepository outageRepository;
    private final CompanyValidationPort companyValidationPort;

    public RegisterAffectedCompanyService(ExternalInvoicingOutageCompanyRepository repository,
            ExternalInvoicingOutageRepository outageRepository,
            CompanyValidationPort companyValidationPort) {
        this.repository = repository;
        this.outageRepository = outageRepository;
        this.companyValidationPort = companyValidationPort;
    }

    @Override
    @Transactional
    public OutageAffectedCompanyDto execute(RegisterAffectedCompanyCommand command) {
        if (outageRepository.findById(command.outageId()).isEmpty())
            throw new ExternalInvoicingOutageNotFoundException(command.outageId());
        if (!companyValidationPort.existsById(command.companyId()))
            throw new IllegalArgumentException("Company not found: " + command.companyId());
        if (repository.existsByOutageIdAndCompanyId(command.outageId(), command.companyId()))
            throw new AffectedCompanyAlreadyRegisteredException(command.outageId(),
                    command.companyId());
        ExternalInvoicingOutageCompany affected = ExternalInvoicingOutageCompany.register(
                command.outageId(), command.companyId(), command.failedDocumentCount(),
                command.resolvedBy());
        return OutageAffectedCompanyDto.from(repository.save(affected));
    }
}
