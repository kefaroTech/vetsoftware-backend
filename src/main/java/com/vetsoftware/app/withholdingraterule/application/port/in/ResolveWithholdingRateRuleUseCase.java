package com.vetsoftware.app.withholdingraterule.application.port.in;

import com.vetsoftware.app.withholdingraterule.application.dto.WithholdingRateRuleDto;
import com.vetsoftware.app.withholdingraterule.domain.ServiceNature;
import com.vetsoftware.app.withholdingraterule.domain.WithholdingType;
import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ResolveWithholdingRateRuleUseCase {

    /**
     * <strong>LA consulta del negocio</strong>: que tarifa aplica a este supuesto
     * en esta fecha. De ella sale cuanto se espera que retenga el cliente y, por
     * tanto, de cuanto va a ser el giro. Sin ella cada retencion llega como una
     * sorpresa que deja la factura sin saldar y arranca el reloj de la mora contra
     * alguien que pago bien.
     *
     * <p>
     * Devuelve la regla con {@code validFrom <= on} y {@code validTo} nulo o
     * posterior a {@code on}. <b>El limite superior es estricto</b>: el dia escrito
     * en {@code validTo} es el primero en que la tarifa ya no aplica, de modo que
     * la regla que se cierra y la que la releva ese mismo dia no se pisan.
     *
     * <p>
     * <strong>Si no hay tarifa, lanza en vez de devolver vacio.</strong> Es una
     * decision, y la razon esta escrita en el changeset 317: el fallo caro de este
     * modelo no es un error, es un cero. Si {@code serviceNature} divergiera entre
     * {@code catalog_items} y esta tabla, la busqueda saldria vacia, la retencion
     * esperada seria cero y <em>nadie se enteraria</em> hasta que el cliente girara
     * de menos. Un {@code Optional} vacio reproduce exactamente esa forma de
     * fallar; un 404 con el supuesto completo en el mensaje la convierte en algo
     * que se ve en el momento. Ver {@code NoEffectiveWithholdingRateRuleException}.
     *
     * <p>
     * <strong>El {@code companyId} llega y no filtra.</strong> No filtra porque la
     * tarifa depende del supuesto fiscal y no del cliente. Llega porque es la
     * credencial: sin el no habria {@code @authz.isMyCompany(#companyId)} que
     * escribir y el permiso quedaria solo, alcanzable por un empleado de cualquier
     * empresa. Ver {@code ListWithholdingRateRulesUseCase#listAvailable}, donde
     * esta el razonamiento completo.
     *
     * <p>
     * Va el ultimo en la firma por eso mismo: los cuatro primeros describen el
     * supuesto que se consulta; el quinto describe a quien pregunta. El nombre
     * {@code companyId} tiene que sobrevivir intacto o el {@code #companyId} del
     * SpEL resuelve a {@code null} en silencio.
     *
     * @param municipalityCode
     *            codigo DIVIPOLA. Nulo para las retenciones nacionales, que es como
     *            se archivan: el centinela de la columna generada
     *            {@code municipality_key} lo traduce del lado del adaptador
     * @param on
     *            la fecha del hecho economico, no la de hoy. Recalcular una factura
     *            de diciembre con la tarifa de enero es como se descuadra una
     *            cartera ya cerrada. <b>Admite {@code null}</b>, y entonces el dia
     *            lo pone la implementacion con su {@code Clock} inyectado: el
     *            adaptador web no puede resolverlo, porque un
     *            {@code LocalDate.now()} en la capa web es una fecha que ningun
     *            test puede fijar y {@code RELOJ_INYECTADO_EN_VEZ_DE_NOW} rompe el
     *            build por ello
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('withholdingRateRule.read')"
            + " and @authz.isMyCompany(#companyId))")
    WithholdingRateRuleDto resolve(WithholdingType type, ServiceNature nature,
            String municipalityCode, LocalDate on, Long companyId);
}
