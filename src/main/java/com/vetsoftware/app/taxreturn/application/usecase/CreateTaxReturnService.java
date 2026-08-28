package com.vetsoftware.app.taxreturn.application.usecase;

import com.vetsoftware.app.taxreturn.application.command.CreateTaxReturnCommand;
import com.vetsoftware.app.taxreturn.application.dto.TaxReturnDto;
import com.vetsoftware.app.taxreturn.application.port.in.CreateTaxReturnUseCase;
import com.vetsoftware.app.taxreturn.application.port.out.MunicipalityValidationPort;
import com.vetsoftware.app.taxreturn.application.port.out.TaxReturnRepository;
import com.vetsoftware.app.taxreturn.application.port.out.VatFilingPeriodValidationPort;
import com.vetsoftware.app.taxreturn.domain.TaxReturn;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abre el borrador de una declaracion inicial.
 *
 * <p>
 * <strong>El service solo comprueba lo que hay que preguntarle a otra
 * tabla</strong> —el municipio y la periodicidad de IVA del año— y sella la
 * fecha con el reloj inyectado. La forma de la clave de periodo segun el
 * impuesto, el municipio obligatorio solo en ICA, los importes no negativos y
 * el saldo unico son invariantes y viven en el constructor de
 * {@link TaxReturn}.
 *
 * <p>
 * <strong>La unicidad no se comprueba preguntando antes.</strong>
 * {@code uq_tax_returns_case} y {@code uq_tax_returns_current} las cuida la
 * base sobre una columna generada; un {@code exists} previo lo pasarian dos
 * peticiones concurrentes y dejarian dos declaraciones vigentes del mismo
 * periodo, que es lo que el segundo marcador existe para impedir.
 */
@Observed(name = "tax.return.create")
@Service
public class CreateTaxReturnService implements CreateTaxReturnUseCase {

    private final TaxReturnRepository repository;
    private final MunicipalityValidationPort municipalityValidationPort;
    private final VatFilingPeriodValidationPort vatFilingPeriodValidationPort;
    private final Clock clock;

    public CreateTaxReturnService(TaxReturnRepository repository,
            MunicipalityValidationPort municipalityValidationPort,
            VatFilingPeriodValidationPort vatFilingPeriodValidationPort, Clock clock) {
        this.repository = repository;
        this.municipalityValidationPort = municipalityValidationPort;
        this.vatFilingPeriodValidationPort = vatFilingPeriodValidationPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public TaxReturnDto execute(CreateTaxReturnCommand command) {
        validateMunicipality(command);
        validateVatFrequency(command);
        TaxReturn draft = TaxReturn.draft(command.taxKind(), command.fiscalYear(),
                command.fiscalPeriodKey(), command.municipalityCode(), command.vatFrequency(),
                command.totalGenerated(), command.totalDeductible(), command.balancePayable(),
                command.balanceCredit(), LocalDateTime.now(clock));
        return TaxReturnDto.from(repository.save(draft));
    }

    /**
     * {@code fk_tax_returns_municipality} es {@code RESTRICT}: sin esta
     * comprobacion un codigo DIVIPOLA inexistente saldria como error de integridad
     * en vez de como «ese municipio no existe». Que solo sea legitimo en ICA lo
     * comprueba el dominio.
     */
    private void validateMunicipality(CreateTaxReturnCommand command) {
        if (command.municipalityCode() == null)
            return;
        if (!municipalityValidationPort.existsByDaneCode(command.municipalityCode()))
            throw new IllegalArgumentException(
                    "Municipality not found: " + command.municipalityCode());
    }

    /**
     * La periodicidad del IVA es un <b>dato con vigencia</b> y no una formula:
     * {@code fk_tax_returns_vat_frequency} apunta a
     * {@code vat_filing_periods(fiscal_year, frequency)}. Preguntarlo antes hace
     * que «no hay periodicidad publicada para ese año» sea un mensaje y no una
     * violacion de clave foranea compuesta, que es de las mas dificiles de leer.
     */
    private void validateVatFrequency(CreateTaxReturnCommand command) {
        if (command.vatFrequency() == null)
            return;
        if (!vatFilingPeriodValidationPort.existsByFiscalYearAndFrequency(command.fiscalYear(),
                command.vatFrequency()))
            throw new IllegalArgumentException("No VAT filing period published for year "
                    + command.fiscalYear() + " with frequency " + command.vatFrequency());
    }
}
