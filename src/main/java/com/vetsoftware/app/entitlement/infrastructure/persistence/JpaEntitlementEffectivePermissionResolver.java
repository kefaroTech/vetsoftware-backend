package com.vetsoftware.app.entitlement.infrastructure.persistence;

import com.vetsoftware.app.auth.application.port.out.EffectivePermissionResolver;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Arbitraje contractual ejecutado en cada request de un EMPLOYEE. No lleva
 * anotacion de cache: el contrato vigente prevalece aunque la cache de permisos
 * base aun contenga una asignacion anterior.
 *
 * <p>
 * <strong>La ausencia de fila significa cero acceso, nunca acceso
 * ilimitado</strong>, y es el mismo criterio que ya se fijo para
 * {@code company_capacities}. Se escribe aqui porque es una decision que
 * alguien va a querer revertir por parecerle poco amable: si una empresa no
 * tiene entitlements, lo tentador es dejarla pasar \"mientras se arregla\". Eso
 * convertiria un proceso caido en una concesion silenciosa de todo el sistema a
 * un tenant sobre el que nadie decidio nada. El cruce es y sigue siendo
 * <em>fail-closed</em>.
 *
 * <p>
 * <strong>Lo que si se arreglo es el modo de fallo</strong> (#410). El conjunto
 * vacio colapsaba dos situaciones que no se parecen:
 * <ul>
 * <li><b>La empresa no tiene entitlements calculados.</b> Ni una fila. TODOS
 * sus empleados quedan sin authorities y ven 403 en cada endpoint; el login
 * funciona, {@code /auth/me} responde con la lista de permisos vacia y el
 * sintoma que llega a soporte es \"la interfaz se quedo en blanco\". Es un
 * proceso roto, no una decision, y ahora deja rastro: un {@code WARN} que
 * <b>nombra la empresa</b> y dice que hay que recalcularla, mas un contador
 * alertable.
 * <li><b>La empresa los tiene y este empleado no alcanza ninguno.</b> Legitimo
 * y frecuente --un recepcionista cuyos permisos base caen todos fuera de lo que
 * su clinica contrato--. Va a {@code DEBUG} y a su propio cubo del contador:
 * sin separarlo, la senal del caso roto se ahoga en el ruido del caso normal.
 * </ul>
 *
 * <p>
 * <strong>El coste extra solo lo paga el caso enfermo.</strong> La consulta de
 * desempate ({@link CompanyEntitlementJpaRepository#countByCompanyId}) solo se
 * lanza cuando el resultado ya vino vacio; una empresa sana nunca la ejecuta.
 *
 * <p>
 * <strong>Por que {@code WARN} y no {@code ERROR}.</strong> Esto se evalua en
 * <em>cada</em> peticion de la empresa afectada, y su front reintenta: un
 * {@code ERROR} por request seria una tormenta que degrada el canal justo
 * cuando hace falta leerlo. La senal alertable es el contador --una tasa, que
 * es lo que se sabe alertar--; el registro existe para que, una vez suena, se
 * sepa <em>que empresa</em> mirar. Bajar de {@code ERROR} aqui no es rebajar la
 * gravedad, es ponerla en el instrumento que la mide.
 *
 * <p>
 * <strong>Por que no se etiqueta por {@code companyId}.</strong> Es el error
 * clasico de cardinalidad: una serie de Prometheus por tenant crece sin techo y
 * el dato que hace falta --que empresa-- ya esta en el registro y en el
 * {@code MDC} de la request. Los dos cubos de {@code cause} son un conjunto
 * cerrado de dos valores.
 *
 * <p>
 * <strong>El login no necesita instrumentacion propia.</strong> Se valoro:
 * {@code LoginEmployeeService} no resuelve permisos --solo credenciales y
 * tokens--, asi que el diagnostico tendria que anadir alli una consulta y una
 * dependencia cruzada nuevas. No hace falta: la primera peticion autenticada
 * que hace el front tras el login es {@code GET /auth/me}, que pasa por el
 * {@code AuthFilter} y por tanto por este resolutor. La ventana entre \"el
 * login funciono\" y \"hay rastro del problema\" es de una peticion.
 */
@Component
public class JpaEntitlementEffectivePermissionResolver implements EffectivePermissionResolver {

    /**
     * Fuera del prefijo {@code vetsoftware.business.}: no es una metrica de negocio
     * sino de salud del arbitraje, y ese prefijo lo filtra la lista blanca de
     * cardinalidad. Convive con {@code vetsoftware.security.tokens.*}.
     */
    static final String RESOLUTION_EMPTY_METRIC = "vetsoftware.entitlement.resolution.empty";

    /** La empresa no tiene ni una fila en {@code company_entitlements}. */
    static final String CAUSE_COMPANY_WITHOUT_ENTITLEMENTS = "company_without_entitlements";

    /** Los tiene, y los permisos base de este empleado caen todos fuera. */
    static final String CAUSE_CONTRACT_RESTRICTED = "contract_restricted";

    private static final Logger log = LoggerFactory
            .getLogger(JpaEntitlementEffectivePermissionResolver.class);

    private final CompanyEntitlementJpaRepository repository;
    private final Counter companyWithoutEntitlements;
    private final Counter contractRestricted;

    public JpaEntitlementEffectivePermissionResolver(CompanyEntitlementJpaRepository repository,
            MeterRegistry meterRegistry) {
        this.repository = repository;
        // Se registran de forma ansiosa y en cero para que una alerta
        // `increase(...) > 0` funcione desde el primer scrape, en vez de depender de
        // que la serie aparezca por primera vez justo durante el incidente.
        this.companyWithoutEntitlements = counter(meterRegistry,
                CAUSE_COMPANY_WITHOUT_ENTITLEMENTS);
        this.contractRestricted = counter(meterRegistry, CAUSE_CONTRACT_RESTRICTED);
    }

    private static Counter counter(MeterRegistry meterRegistry, String cause) {
        return Counter.builder(RESOLUTION_EMPTY_METRIC)
                .description("Resoluciones de permisos efectivos que devolvieron el conjunto"
                        + " vacio, por causa; company_without_entitlements > 0 significa un"
                        + " recalculo de entitlements que no corrio")
                .tag("cause", cause).register(meterRegistry);
    }

    @Override
    public Set<String> resolveFor(Long companyId, Set<String> basePermissions) {
        if (companyId == null || basePermissions == null || basePermissions.isEmpty()) {
            // Tercer estado, y no es de esta feature: el empleado no tiene ni un permiso
            // base asignado. Diagnosticarlo aqui como si fuera un problema de
            // entitlements mandaria a soporte a mirar el contrato equivocado.
            return Set.of();
        }
        Set<String> effective = Set
                .copyOf(repository.findEffectivePermissionCodes(companyId, basePermissions));
        if (effective.isEmpty()) {
            diagnoseEmptyResult(companyId);
        }
        return effective;
    }

    /**
     * Separa los dos motivos por los que el cruce pudo quedar vacio. Solo se llama
     * con el resultado ya vacio, asi que la consulta extra no toca el camino sano.
     */
    private void diagnoseEmptyResult(Long companyId) {
        if (repository.countByCompanyId(companyId) == 0) {
            companyWithoutEntitlements.increment();
            log.warn("La empresa {} no tiene ninguna fila en company_entitlements: sus empleados"
                    + " quedan sin authorities y recibiran 403 en todos los endpoints. No es un"
                    + " problema de permisos del usuario: falta recalcular sus entitlements desde"
                    + " su contrato (RecalculateCompanyEntitlementsUseCase). Si la empresa tampoco"
                    + " tiene contrato, el alta quedo a medias.", companyId);
            return;
        }
        contractRestricted.increment();
        log.debug(
                "La empresa {} tiene entitlements calculados, pero ninguno de los permisos base"
                        + " de este empleado cae dentro de lo que su contrato concede hoy.",
                companyId);
    }
}
