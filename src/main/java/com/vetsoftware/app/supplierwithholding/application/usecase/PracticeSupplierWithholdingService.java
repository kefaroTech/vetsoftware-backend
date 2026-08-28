package com.vetsoftware.app.supplierwithholding.application.usecase;

import com.vetsoftware.app.supplierwithholding.application.command.PracticeSupplierWithholdingCommand;
import com.vetsoftware.app.supplierwithholding.application.dto.SupplierWithholdingDto;
import com.vetsoftware.app.supplierwithholding.application.port.in.PracticeSupplierWithholdingUseCase;
import com.vetsoftware.app.supplierwithholding.application.port.out.MunicipalityValidationPort;
import com.vetsoftware.app.supplierwithholding.application.port.out.SupplierWithholdingRepository;
import com.vetsoftware.app.supplierwithholding.domain.SupplierWithholding;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra una retencion practicada a un proveedor.
 *
 * <p>
 * <strong>El service hace exactamente dos cosas, y ninguna es validar la
 * retencion.</strong> Comprueba el municipio —un hecho externo, hay que
 * preguntarselo a otra tabla— y sella la fecha con el reloj inyectado. Que el
 * municipio solo venga con {@code ICA}, que el retenido no supere la base, que
 * la tarifa este entre cero y cien con seis decimales y que la clave de periodo
 * sea mensual o bimestral segun el tipo son invariantes y viven en el
 * constructor de {@link SupplierWithholding}.
 *
 * <p>
 * <strong>La unicidad no se comprueba preguntando antes.</strong>
 * {@code uq_supplier_withholdings_case} la cuida la base; un {@code exists}
 * previo lo pasarian dos peticiones concurrentes y se declararia dos veces la
 * misma retencion al mismo proveedor por el mismo soporte — que es justo lo que
 * duplicaria el reporte anual de terceros.
 */
@Observed(name = "supplier.withholding.practice")
@Service
public class PracticeSupplierWithholdingService implements PracticeSupplierWithholdingUseCase {

    private final SupplierWithholdingRepository repository;
    private final MunicipalityValidationPort municipalityValidationPort;
    private final Clock clock;

    public PracticeSupplierWithholdingService(SupplierWithholdingRepository repository,
            MunicipalityValidationPort municipalityValidationPort, Clock clock) {
        this.repository = repository;
        this.municipalityValidationPort = municipalityValidationPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SupplierWithholdingDto execute(PracticeSupplierWithholdingCommand command) {
        validateMunicipality(command);
        SupplierWithholding withholding = SupplierWithholding.practice(command.supplierTaxId(),
                command.supplierName(), command.supplierDocType(), command.supplierInvoiceRef(),
                command.withholdingType(), command.concept(), command.taxableBase(),
                command.ratePercent(), command.amount(), command.municipalityCode(),
                command.fiscalYear(), command.fiscalPeriodKey(), command.practicedOn(),
                LocalDateTime.now(clock));
        return SupplierWithholdingDto.from(repository.save(withholding));
    }

    /**
     * {@code fk_sw_municipality} es {@code RESTRICT}: sin esta comprobacion un
     * codigo DIVIPOLA inexistente saldria como error de integridad en vez de como
     * «ese municipio no existe». Que solo sea legitimo en {@code ICA} lo comprueba
     * el dominio.
     */
    private void validateMunicipality(PracticeSupplierWithholdingCommand command) {
        if (command.municipalityCode() == null)
            return;
        if (!municipalityValidationPort.existsByDaneCode(command.municipalityCode()))
            throw new IllegalArgumentException(
                    "Municipality not found: " + command.municipalityCode());
    }
}
