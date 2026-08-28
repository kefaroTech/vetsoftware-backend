package com.vetsoftware.app.withholdingraterule.application.usecase;

import com.vetsoftware.app.withholdingraterule.application.dto.WithholdingRateRuleDto;
import com.vetsoftware.app.withholdingraterule.application.port.in.ResolveWithholdingRateRuleUseCase;
import com.vetsoftware.app.withholdingraterule.application.port.out.WithholdingRateRuleRepository;
import com.vetsoftware.app.withholdingraterule.domain.NoEffectiveWithholdingRateRuleException;
import com.vetsoftware.app.withholdingraterule.domain.ServiceNature;
import com.vetsoftware.app.withholdingraterule.domain.WithholdingType;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

/**
 * La tarifa vigente para un supuesto en una fecha: <strong>la consulta por la
 * que existe toda esta feature</strong>.
 *
 * <p>
 * <strong>Lanza en vez de devolver vacio, y ese {@code orElseThrow} es la unica
 * linea interesante de la clase.</strong> El comentario del changeset 317 lo
 * escribe con nombre y apellido: si {@code service_nature} divergiera entre
 * {@code catalog_items} y {@code withholding_rate_rules} —un plural de mas, un
 * sinonimo— la busqueda saldria vacia, la retencion esperada seria cero y <b>no
 * habria error</b>. La factura se emite, el cliente retiene igual y gira de
 * menos, y el saldo queda abierto contra alguien que pago bien. Nadie se entera
 * hasta que se cuadra la cartera.
 *
 * <p>
 * Devolver un {@code Optional} vacio desde aqui reproduciria exactamente esa
 * forma de fallar: el llamador que no lo mire tratara la ausencia como un cero.
 * La excepcion lleva el supuesto completo en el mensaje —tipo, naturaleza,
 * municipio y fecha— para que quien la lea sepa <em>que</em> tarifa falta
 * configurar y no solo que algo no salio.
 */
@Observed(name = "withholding.rate.rule.resolve")
@Service
public class ResolveWithholdingRateRuleService implements ResolveWithholdingRateRuleUseCase {

    private final WithholdingRateRuleRepository repository;
    private final Clock clock;

    public ResolveWithholdingRateRuleService(WithholdingRateRuleRepository repository,
            Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * <strong>El dia por defecto lo pone este metodo con el {@code Clock}
     * inyectado, no el controller.</strong> Un {@code LocalDate.now()} en la capa
     * web seria una fecha que ningun test puede fijar —el caso pasaria o fallaria
     * segun el dia en que se ejecute— y {@code RELOJ_INYECTADO_EN_VEZ_DE_NOW} rompe
     * el build por ello. Aqui el reloj es una dependencia mas y la resolucion «de
     * hoy» se puede probar.
     */
    @Override
    public WithholdingRateRuleDto resolve(WithholdingType type, ServiceNature nature,
            String municipalityCode, LocalDate on, Long companyId) {
        LocalDate effectiveOn = on == null ? LocalDate.now(clock) : on;
        return repository.findEffective(type, nature, municipalityCode, effectiveOn)
                .map(WithholdingRateRuleDto::from)
                .orElseThrow(() -> new NoEffectiveWithholdingRateRuleException(type, nature,
                        municipalityCode, effectiveOn));
    }
}
