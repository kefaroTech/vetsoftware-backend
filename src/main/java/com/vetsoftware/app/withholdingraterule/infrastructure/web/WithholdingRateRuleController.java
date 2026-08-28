package com.vetsoftware.app.withholdingraterule.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.withholdingraterule.application.port.in.FindWithholdingRateRuleUseCase;
import com.vetsoftware.app.withholdingraterule.application.port.in.ListWithholdingRateRulesUseCase;
import com.vetsoftware.app.withholdingraterule.application.port.in.ResolveWithholdingRateRuleUseCase;
import com.vetsoftware.app.withholdingraterule.domain.ServiceNature;
import com.vetsoftware.app.withholdingraterule.domain.WithholdingType;
import com.vetsoftware.app.withholdingraterule.infrastructure.web.response.WithholdingRateRuleResponse;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * La cara de tenant del catalogo de retenciones, y es <strong>solo de
 * lectura</strong>.
 *
 * <p>
 * No es que las escrituras aun no esten hechas: no van aqui. El bloque
 * «Referencia fiscal y textos» del documento maestro lo escribe la plataforma y
 * lo leen los dos, asi que el alta y el cierre viven en
 * {@link SystemWithholdingRateRuleController} y este controller no tiene un
 * solo {@code @PostMapping}. La tarifa la fija la ley; una clinica que pudiera
 * escribirla estaria decidiendo cuanto cree que le van a girar.
 *
 * <p>
 * <strong>La empresa sale siempre de {@code authz.currentCompanyId()} y nunca
 * de la URL ni del cuerpo</strong>, aunque esta tabla no tenga empresa. No es
 * un filtro —el catalogo es global y devuelve lo mismo para todos— sino la
 * credencial que los tres puertos revalidan con
 * {@code @authz.isMyCompany(#companyId)}. Si alguien la aceptara por parametro
 * «para la consola», la comprobacion quedaria mirando un valor que escribe el
 * propio cliente.
 */
@RestController
@RequestMapping("/withholding-rate-rules")
public class WithholdingRateRuleController {

    private final FindWithholdingRateRuleUseCase findUseCase;
    private final ListWithholdingRateRulesUseCase listUseCase;
    private final ResolveWithholdingRateRuleUseCase resolveUseCase;
    private final Authz authz;

    public WithholdingRateRuleController(FindWithholdingRateRuleUseCase findUseCase,
            ListWithholdingRateRulesUseCase listUseCase,
            ResolveWithholdingRateRuleUseCase resolveUseCase, Authz authz) {
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.resolveUseCase = resolveUseCase;
        this.authz = authz;
    }

    @GetMapping("/{id}")
    public WithholdingRateRuleResponse findById(@PathVariable Long id) {
        return WithholdingRateRuleResponse.from(findUseCase.findById(id, authz.currentCompanyId()));
    }

    @GetMapping
    public PageResponse<WithholdingRateRuleResponse> listAvailable(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(
                listUseCase.listAvailable(authz.currentCompanyId(), page, pageSize),
                WithholdingRateRuleResponse::from);
    }

    /**
     * <strong>El endpoint por el que existe la feature</strong>: que tarifa aplica
     * a este supuesto en esta fecha, que es de donde sale cuanto se espera que
     * retenga el cliente.
     *
     * <p>
     * {@code municipalityCode} es opcional porque las retenciones nacionales se
     * archivan sin municipio; {@code on} tampoco es obligatorio, pero <b>quien
     * calcula una factura tiene que enviarlo</b>: la fecha que manda es la del
     * hecho economico, y recalcular una factura de diciembre con la tarifa de enero
     * descuadra una cartera ya cerrada.
     *
     * <p>
     * <strong>Cuando {@code on} no viene, el {@code null} viaja tal cual al caso de
     * uso y es EL quien decide que dia es hoy, con su {@code Clock}
     * inyectado.</strong> El controller no lo resuelve, y no es reparto de
     * responsabilidades sino una regla dura: un {@code LocalDate.now()} aqui es una
     * fecha que ningun test puede fijar —el caso quedaria dependiendo del dia en
     * que se ejecute— y {@code RELOJ_INYECTADO_EN_VEZ_DE_NOW} rompe el build por
     * ello.
     *
     * <p>
     * Si no hay tarifa para el supuesto sale 404 y no una respuesta vacia: el fallo
     * caro de este modelo no es un error, es un cero que nadie ve.
     */
    @GetMapping("/effective")
    public WithholdingRateRuleResponse resolve(@RequestParam WithholdingType withholdingType,
            @RequestParam ServiceNature serviceNature,
            @RequestParam(required = false) String municipalityCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate on) {
        return WithholdingRateRuleResponse.from(resolveUseCase.resolve(withholdingType,
                serviceNature, municipalityCode, on, authz.currentCompanyId()));
    }
}
