package com.vetsoftware.app.documentwithholding.application.usecase;

import com.vetsoftware.app.documentwithholding.application.command.RegisterDocumentWithholdingCommand;
import com.vetsoftware.app.documentwithholding.application.dto.DocumentWithholdingDto;
import com.vetsoftware.app.documentwithholding.application.port.in.RegisterDocumentWithholdingUseCase;
import com.vetsoftware.app.documentwithholding.application.port.out.BillingDocumentValidationPort;
import com.vetsoftware.app.documentwithholding.application.port.out.DocumentWithholdingRepository;
import com.vetsoftware.app.documentwithholding.application.port.out.MunicipalityValidationPort;
import com.vetsoftware.app.documentwithholding.domain.DocumentWithholding;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra la retencion que un cliente practico sobre una factura de cobro.
 *
 * <p>
 * <strong>Este servicio valida las claves foraneas y nada mas; las invariantes
 * viven en el constructor de {@link DocumentWithholding}.</strong> El reparto
 * no es de estilo: «la tarifa es un porcentaje entre 0 y 100» o «el periodo
 * tiene que casar con el tipo» son verdades de la retencion y valen aunque
 * nadie llame a este metodo, mientras que «ese documento de cobro existe y es
 * de esta empresa» solo se puede saber preguntando. Poner lo primero aqui lo
 * dejaria fuera de cualquier otro camino que construya la entidad —el mapper de
 * lectura, por ejemplo— y las filas mal formadas entrarian por ahi.
 *
 * <p>
 * <strong>Sin llave de idempotencia, y a diferencia de las devoluciones no hace
 * falta.</strong> Aqui la barandilla contra el doble registro la pone el motor:
 * {@code uq_document_withholdings_case} impide dos retenciones del mismo
 * documento, del mismo tipo y del mismo municipio. Es lo que hace que un doble
 * clic choque en vez de duplicar el importe retenido —y por eso el centinela
 * {@code municipality_key} importa tanto: sin el, las dos retenciones
 * nacionales habrian cabido, porque en un indice unico dos {@code NULL} no
 * chocan—.
 */
@Observed(name = "document.withholding.register")
@Service
public class RegisterDocumentWithholdingService implements RegisterDocumentWithholdingUseCase {

    private final DocumentWithholdingRepository repository;
    private final BillingDocumentValidationPort billingDocumentValidationPort;
    private final MunicipalityValidationPort municipalityValidationPort;
    private final Clock clock;

    public RegisterDocumentWithholdingService(DocumentWithholdingRepository repository,
            BillingDocumentValidationPort billingDocumentValidationPort,
            MunicipalityValidationPort municipalityValidationPort, Clock clock) {
        this.repository = repository;
        this.billingDocumentValidationPort = billingDocumentValidationPort;
        this.municipalityValidationPort = municipalityValidationPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public DocumentWithholdingDto execute(RegisterDocumentWithholdingCommand command) {
        validateBillingDocument(command);

        // La entidad se construye ANTES de preguntar por el municipio, y el orden
        // importa: es el dominio quien decide si este tipo de retencion puede llevar
        // municipio siquiera. Preguntando primero, un ICA con codigo invalido y un
        // IVA con codigo prohibido saldrian con el mismo error —«municipio no
        // encontrado»— y el segundo mentiria sobre lo que esta mal.
        DocumentWithholding withholding = DocumentWithholding.register(command.companyId(),
                command.billingDocumentId(), command.type(), command.taxableBase(),
                command.ratePercent(), command.amount(), command.municipalityCode(),
                command.fiscalYear(), command.fiscalPeriodKey(), command.practicedOn(),
                LocalDateTime.now(clock));

        validateMunicipality(withholding.getMunicipalityCode());
        return DocumentWithholdingDto.from(repository.save(withholding));
    }

    /**
     * El documento de cobro es obligatorio —una retencion sin factura no salda
     * nada— y tiene que ser de la misma empresa, o la FK compuesta lo rechazaria
     * mas tarde y como un error de integridad.
     */
    private void validateBillingDocument(RegisterDocumentWithholdingCommand command) {
        if (!billingDocumentValidationPort.existsByIdAndCompanyId(command.billingDocumentId(),
                command.companyId()))
            throw new IllegalArgumentException(
                    "Billing document not found: " + command.billingDocumentId());
    }

    /**
     * Solo se pregunta cuando hay municipio, que por el CHECK es exactamente cuando
     * el tipo es ICA. El dominio ya garantizo la coherencia entre los dos, asi que
     * aqui basta con que el codigo exista de verdad en el catalogo: un municipio
     * inventado deja una retencion imposible de cruzar contra la tarifa de nadie.
     */
    private void validateMunicipality(String municipalityCode) {
        if (municipalityCode == null)
            return;
        if (!municipalityValidationPort.existsByDaneCode(municipalityCode))
            throw new IllegalArgumentException("Municipality not found: " + municipalityCode);
    }
}
