package com.vetsoftware.app.taxreturn.application.usecase;

import com.vetsoftware.app.taxreturn.application.command.FileTaxReturnCommand;
import com.vetsoftware.app.taxreturn.application.dto.TaxReturnDto;
import com.vetsoftware.app.taxreturn.application.port.in.FileTaxReturnUseCase;
import com.vetsoftware.app.taxreturn.application.port.out.TaxReturnRepository;
import com.vetsoftware.app.taxreturn.domain.TaxReturn;
import com.vetsoftware.app.taxreturn.domain.TaxReturnNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Presenta la declaracion.
 *
 * <p>
 * <strong>La fecha de presentacion la pone el reloj inyectado, no el
 * cliente.</strong> Es un dato probatorio: de el depende que
 * {@code firmezaUntil} sea posterior —lo exige {@code chk_tax_returns_filed}— y
 * por tanto toda la ventana de conservacion. Aceptarla por HTTP dejaria
 * antedatar una presentacion, y {@code RELOJ_INYECTADO_EN_VEZ_DE_NOW} rompe el
 * build ante un {@code now()} pelado.
 *
 * <p>
 * <strong>La fecha de firmeza si llega como dato, y eso es deliberado.</strong>
 * No se calcula aqui porque depende de si Lumbre compensa perdidas fiscales
 * —tres años (art. 714 ET) o cinco—, y esa pregunta <b>sigue abierta para un
 * contador</b>. Un calculo inventado aqui produciria una ventana de
 * conservacion equivocada en la tabla mas grande del diseño, y el fallo aparece
 * el dia que la DIAN pide los soportes.
 */
@Observed(name = "tax.return.file")
@Service
public class FileTaxReturnService implements FileTaxReturnUseCase {

    private final TaxReturnRepository repository;
    private final Clock clock;

    public FileTaxReturnService(TaxReturnRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public TaxReturnDto execute(FileTaxReturnCommand command) {
        TaxReturn taxReturn = repository.findById(command.id())
                .orElseThrow(() -> new TaxReturnNotFoundException(command.id()));
        return TaxReturnDto.from(repository
                .save(taxReturn.file(LocalDateTime.now(clock), command.filedBySystemUserId(),
                        command.receiptRef(), command.fileRef(), command.firmezaUntil())));
    }
}
