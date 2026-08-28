package com.vetsoftware.app.taxreturn.application.usecase;

import com.vetsoftware.app.taxreturn.application.command.CorrectTaxReturnCommand;
import com.vetsoftware.app.taxreturn.application.dto.TaxReturnDto;
import com.vetsoftware.app.taxreturn.application.port.in.CorrectTaxReturnUseCase;
import com.vetsoftware.app.taxreturn.application.port.out.TaxReturnRepository;
import com.vetsoftware.app.taxreturn.domain.TaxReturn;
import com.vetsoftware.app.taxreturn.domain.TaxReturnNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abre la correccion de una declaracion presentada.
 *
 * <h2>El orden de las dos escrituras no es negociable</h2>
 *
 * <p>
 * Primero la anterior pasa a {@code CORRECTED} —lo que vacia su
 * {@code current_return_marker}— y solo entonces cabe la nueva. Al reves,
 * {@code uq_tax_returns_current} rechazaria el {@code INSERT}: mientras la
 * anterior siga en {@code FILED}, el marcador vale
 * {@code impuesto|periodo|municipio} y solo puede haber uno.
 *
 * <p>
 * <strong>Y las dos van en la misma transaccion.</strong> Partirlas dejaria una
 * ventana en la que el periodo <em>no tiene ninguna declaracion vigente</em>, y
 * si la segunda escritura fallara, el periodo se quedaria sin vigente para
 * siempre.
 *
 * <h2>La invariante que la base no puede imponer</h2>
 *
 * <p>
 * «Una declaracion no se corrige a si misma» no cabe en un {@code CHECK}: el
 * manual de MySQL prohibe referenciar una columna {@code AUTO_INCREMENT} dentro
 * de uno. Aqui se cumple por construccion —la correccion nace sin id— y el
 * constructor de {@link TaxReturn} la comprueba igualmente, que es la red para
 * el dia que alguien añada un camino que reescriba {@code correctsReturnId}.
 */
@Observed(name = "tax.return.correct")
@Service
public class CorrectTaxReturnService implements CorrectTaxReturnUseCase {

    private final TaxReturnRepository repository;
    private final Clock clock;

    public CorrectTaxReturnService(TaxReturnRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public TaxReturnDto execute(CorrectTaxReturnCommand command) {
        TaxReturn corrected = repository.findById(command.id())
                .orElseThrow(() -> new TaxReturnNotFoundException(command.id()));
        TaxReturn correction = corrected.correctionDraft(command.totalGenerated(),
                command.totalDeductible(), command.balancePayable(), command.balanceCredit(),
                LocalDateTime.now(clock));
        repository.save(corrected.markCorrected());
        return TaxReturnDto.from(repository.save(correction));
    }
}
