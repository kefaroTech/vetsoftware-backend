package com.vetsoftware.app.companylimitevent.infrastructure.persistence;

import com.vetsoftware.app.companylimitevent.application.port.out.RealUsageCountPort;
import java.util.OptionalInt;
import org.springframework.stereotype.Component;

/**
 * El registro de fuentes de verdad por eje.
 *
 * <p>
 * <strong>Es una lista explicita y no un {@code switch} con
 * {@code default}</strong>: un eje que no esta aqui devuelve vacio y el
 * recuento lo salta diciendolo, en vez de caer en una rama que devolveria cero.
 * La diferencia importa mucho --cero es una afirmacion, y afirmar cero sobre un
 * eje que no se sabe contar escribiria un desvio enorme contra un contador
 * correcto y le pondria el sello de comprobado--.
 *
 * <p>
 * Los codigos van como constantes de esta clase y no importados de la otra
 * rodaja: son valores de datos ({@code limit_dimensions.code}), no un
 * vocabulario compartido de codigo. El dia que se instrumenten los cinco que
 * faltan, cada uno entra aqui con su consulta.
 */
@Component
public class JpaRealUsageCountPort implements RealUsageCountPort {

    private static final String USER = "USER";
    private static final String BRANCH = "BRANCH";
    private static final String TERMINAL = "TERMINAL";

    private final RealUsageCountJpaRepository repository;

    public JpaRealUsageCountPort(RealUsageCountJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public OptionalInt countFor(Long companyId, String dimensionCode) {
        if (companyId == null || dimensionCode == null) {
            return OptionalInt.empty();
        }
        return switch (dimensionCode) {
            case USER -> OptionalInt.of(repository.countActiveUsers(companyId));
            case BRANCH -> OptionalInt.of(repository.countActiveBranches(companyId));
            case TERMINAL -> OptionalInt.of(repository.countActiveTerminals(companyId));
            // ANIMAL y OWNER: acumulativos con enfriamiento de treinta dias (D-61), y
            // ninguna tabla clinica guarda la fecha de borrado, asi que "las no
            // borradas mas las borradas dentro de la ventana" no se puede escribir.
            // APPOINTMENT e INVOICE: de flujo, y en facturas solo cuentan tres de los
            // caminos de emision (D-16). STORAGE_GB: los ficheros no guardan su tamano
            // todavia (R-LIMIT-24). Los cinco se saltan, y se saltan diciendolo.
            default -> OptionalInt.empty();
        };
    }
}
