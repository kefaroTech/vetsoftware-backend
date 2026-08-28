package com.vetsoftware.app.withholdingraterule.application.usecase;

import com.vetsoftware.app.withholdingraterule.application.command.CloseWithholdingRateRuleCommand;
import com.vetsoftware.app.withholdingraterule.application.dto.WithholdingRateRuleDto;
import com.vetsoftware.app.withholdingraterule.application.port.in.CloseWithholdingRateRuleUseCase;
import com.vetsoftware.app.withholdingraterule.application.port.out.WithholdingRateRuleRepository;
import com.vetsoftware.app.withholdingraterule.domain.WithholdingRateRule;
import com.vetsoftware.app.withholdingraterule.domain.WithholdingRateRuleNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pone fecha de fin a una vigencia abierta.
 *
 * <p>
 * <strong>Cerrar no es borrar.</strong> La tarifa que dejo de aplicarse el 1 de
 * enero sigue siendo la correcta para una factura de diciembre, asi que la fila
 * se queda y lo unico que cambia es hasta cuando vale. Por eso no hay aqui un
 * {@code delete} ni un {@code disable}: cualquiera de los dos dejaria sin
 * explicacion las retenciones ya calculadas.
 *
 * <p>
 * <strong>Leer, cerrar y guardar es un ciclo con bloqueo optimista, y de eso
 * depende que no se pierda un cierre.</strong> La entidad lleva
 * {@code @Version}, el dominio conserva ese numero al construir la instancia
 * cerrada y el {@code save} vuelve con el en el {@code WHERE}: dos cierres
 * concurrentes no se pisan, el segundo se lleva un fallo de bloqueo en vez de
 * machacar la fecha del primero. Todo dentro de una transaccion, o la lectura y
 * la escritura serian dos operaciones sin nada en medio.
 *
 * <p>
 * Que la regla no estuviera ya cerrada lo decide el dominio, no este metodo: es
 * una invariante de la regla y la base <b>no la cuida</b> —el marcador de
 * vigencia abierta vale {@code NULL} en una regla cerrada y la unicidad sobre
 * columna nula no restringe nada—.
 */
@Observed(name = "withholding.rate.rule.close")
@Service
public class CloseWithholdingRateRuleService implements CloseWithholdingRateRuleUseCase {

    private final WithholdingRateRuleRepository repository;

    public CloseWithholdingRateRuleService(WithholdingRateRuleRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public WithholdingRateRuleDto execute(CloseWithholdingRateRuleCommand command) {
        WithholdingRateRule rule = repository.findById(command.id())
                .orElseThrow(() -> new WithholdingRateRuleNotFoundException(command.id()));
        return WithholdingRateRuleDto.from(repository.save(rule.close(command.validTo())));
    }
}
