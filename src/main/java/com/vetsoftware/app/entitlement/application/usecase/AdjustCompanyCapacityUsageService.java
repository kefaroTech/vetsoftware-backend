package com.vetsoftware.app.entitlement.application.usecase;

import com.vetsoftware.app.entitlement.application.command.AdjustCompanyCapacityUsageCommand;
import com.vetsoftware.app.entitlement.application.dto.CompanyCapacityDto;
import com.vetsoftware.app.entitlement.application.port.in.AdjustCompanyCapacityUsageUseCase;
import com.vetsoftware.app.entitlement.application.port.out.CompanyCapacityRepository;
import com.vetsoftware.app.entitlement.application.port.out.LimitDenialPort;
import com.vetsoftware.app.entitlement.application.port.out.LimitDimensionQueryPort;
import com.vetsoftware.app.entitlement.application.port.out.OverageAllowancePort;
import com.vetsoftware.app.entitlement.application.port.out.OverageChargePort;
import com.vetsoftware.app.entitlement.application.port.out.SubscriptionQueryPort;
import com.vetsoftware.app.entitlement.domain.CompanyCapacity;
import com.vetsoftware.app.entitlement.domain.CompanyCapacityLimitExceededException;
import com.vetsoftware.app.entitlement.domain.CompanyCapacityNotFoundException;
import com.vetsoftware.app.entitlement.domain.CompanyCapacityUnderflowException;
import com.vetsoftware.app.entitlement.domain.LimitDimensionRef;
import com.vetsoftware.app.entitlement.domain.OverageAllowance;
import com.vetsoftware.app.entitlement.domain.PeriodKey;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mueve el consumo con una sola sentencia atomica y despues lee el contador
 * para devolverlo. La lectura va <em>despues</em> a proposito: leer antes para
 * decidir y escribir despues es exactamente la carrera que el
 * {@code UPDATE ... SET x = x + ?} evita (R-LIMIT-01). Ese mecanismo no se
 * reimplementa aqui: se conserva tal cual estaba.
 *
 * <p>
 * Lo unico que se le antepone es resolver el eje contra el catalogo, que es lo
 * que sustituye a la lista cerrada de cuatro unidades.
 *
 * <p>
 * <strong>Cero filas afectadas tiene ahora cinco causas y se distinguen las
 * cinco</strong>, porque el mensaje generico "no se pudo actualizar" es lo que
 * convierte un error de configuracion en una hora de depuracion --y porque dos
 * de ellas <em>no son un error en absoluto</em>--:
 *
 * <ol>
 * <li>La fila existe y el movimiento la dejaria en negativo: subdesbordamiento.
 * <li>La fila existe y la reserva pasaria del techo. <strong>Y aqui hay una
 * bifurcacion, no un portazo</strong>: si el contrato declaro modo de excedente
 * ({@code enforcement = OVERAGE}) el consumo pasa y se devenga el cargo; solo
 * si no lo declaro se bloquea. Ver {@link #chargeOverageIfAllowed}.
 * <li>La fila <strong>no existe todavia porque acaba de entrar un periodo
 * nuevo</strong> de un eje de flujo. No es un tope: es el reinicio de
 * R-LIMIT-04 y hay que hacer nacer la fila. Ver {@link #openPeriodAndRetry}.
 * <li>La fila no existe y <strong>el eje nacio despues de que esta empresa
 * firmara</strong>: D-74. Para ella ese limite no aplica y el consumo pasa sin
 * techo.
 * <li>La fila no existe y ninguna de las dos anteriores: techo cero, que es la
 * regla de siempre.
 * </ol>
 *
 * <p>
 * <strong>El orden entre la tercera y la cuarta importa.</strong> Un cupo de
 * flujo cuyo periodo entra tiene un periodo anterior del que heredar; un eje
 * posterior a la firma no tiene ninguno. Probar primero el nacimiento y caer
 * despues a D-74 distingue las dos sin preguntar nada de mas.
 */
@Observed(name = "entitlement.capacity.adjust")
@Service
public class AdjustCompanyCapacityUsageService implements AdjustCompanyCapacityUsageUseCase {

    private final CompanyCapacityRepository repository;
    private final LimitDimensionQueryPort limitDimensionQueryPort;
    private final SubscriptionQueryPort subscriptionQueryPort;
    private final LimitDenialPort limitDenialPort;
    private final OverageAllowancePort overageAllowancePort;
    private final OverageChargePort overageChargePort;
    private final Clock clock;

    public AdjustCompanyCapacityUsageService(CompanyCapacityRepository repository,
            LimitDimensionQueryPort limitDimensionQueryPort,
            SubscriptionQueryPort subscriptionQueryPort, LimitDenialPort limitDenialPort,
            OverageAllowancePort overageAllowancePort, OverageChargePort overageChargePort,
            Clock clock) {
        this.repository = repository;
        this.limitDimensionQueryPort = limitDimensionQueryPort;
        this.subscriptionQueryPort = subscriptionQueryPort;
        this.limitDenialPort = limitDenialPort;
        this.overageAllowancePort = overageAllowancePort;
        this.overageChargePort = overageChargePort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CompanyCapacityDto execute(AdjustCompanyCapacityUsageCommand command) {
        LimitDimensionRef dimension = limitDimensionQueryPort.findByCode(command.dimensionCode())
                .orElseThrow(() -> unknownDimension(command));
        // El eje decide si la clave es un periodo real o el centinela, y rechaza la
        // combinacion imposible en las dos direcciones (R-LIMIT-05).
        PeriodKey periodKey = PeriodKey.forMeasure(dimension.measureKind(), command.periodKey());

        int updated = repository.addUsage(command.companyId(), dimension.id(), periodKey.value(),
                command.delta());
        Optional<CompanyCapacity> capacity = repository.findByCompanyIdAndDimension(
                command.companyId(), dimension.id(), periodKey.value());

        if (updated == 0 && capacity.isEmpty() && dimension.measureKind().requiresPeriodKey()) {
            Retry retry = openPeriodAndRetry(command, dimension, periodKey);
            updated = retry.updated();
            capacity = retry.capacity();
        }

        if (updated == 0) {
            if (capacity.isEmpty()) {
                return notContractedOrUncapped(command, dimension, periodKey);
            }
            CompanyCapacity current = capacity.get();
            long requestedUsage = (long) current.getUsedQuantity() + command.delta();
            if (requestedUsage < 0) {
                throw new CompanyCapacityUnderflowException(command.companyId(), dimension.code(),
                        current.getUsedQuantity(), command.delta());
            }
            if (command.delta() > 0 && requestedUsage > current.getLimitQuantity()) {
                // EL EXCEDENTE SE MIRA ANTES DE NEGAR. Si el contrato compro el
                // derecho a pasarse, negar aqui seria bloquear a quien paga.
                Optional<CompanyCapacityDto> excedente = chargeOverageIfAllowed(command, dimension,
                        periodKey, current);
                if (excedente.isPresent()) {
                    return excedente.get();
                }
                // EL PORTAZO SE ESCRIBE ANTES DE NEGARLO. El hecho es la prueba ante
                // una reclamacion --"a mi nunca me avisaron"-- y la mejor senal de
                // venta que tiene el producto: doce choques contra el tope en una
                // semana son una ampliacion que se cierra sola. Lanzar la excepcion
                // sin escribirlo tira las dos cosas.
                //
                // El puerto escribe en transaccion propia (REQUIRES_NEW) porque esta
                // se va a revertir en cuanto salga la excepcion de dos lineas mas
                // abajo: escribirlo aqui dentro seria escribirlo y borrarlo en el
                // mismo suspiro, y la bitacora quedaria vacia justo en los casos que
                // existe para registrar.
                limitDenialPort.limitDenied(command.companyId(), dimension.id(),
                        current.getLimitQuantity(), current.getUsedQuantity(), command.delta());
                throw new CompanyCapacityLimitExceededException(command.companyId(),
                        dimension.code(), current.getLimitQuantity(), current.getUsedQuantity(),
                        command.delta());
            }
            throw new IllegalStateException("Capacity update affected no rows for company "
                    + command.companyId() + ", dimension " + dimension.code() + " and period "
                    + periodKey.value());
        }
        return CompanyCapacityDto
                .from(capacity.orElseThrow(() -> notContracted(command, periodKey)));
    }

    /**
     * <strong>La rama del excedente: si el contrato compro el derecho a pasarse, no
     * se bloquea.</strong>
     *
     * <p>
     * Hasta aqui esta clase negaba <em>incondicionalmente</em> al pasar del techo.
     * El modo estaba declarado desde el changeset 304
     * ({@code subscription_item_limits.enforcement = 'OVERAGE'}, con su precio por
     * unidad) y
     * {@link com.vetsoftware.app.subscriptionitemlimit.domain.SubscriptionItemLimit}
     * lo validaba, pero <b>nadie lo leia en el camino del contador</b> y no existia
     * clase de cargo a la que mandar el exceso. El resultado medido: se bloqueaba a
     * un cliente que estaba dispuesto a pagar el excedente y que el modelo dice que
     * debe poder pasarse.
     *
     * <p>
     * <strong>El orden es: permiso, consumo, cargo — y los tres en la misma
     * transaccion.</strong> Si el cargo no se puede escribir, el consumo por encima
     * del techo tampoco puede quedar: consumir de mas y cobrarlo son un solo hecho.
     * Por eso {@link OverageChargePort} no se traga excepciones, al reves que
     * {@link com.vetsoftware.app.entitlement.application.port.out.LimitDenialPort}.
     *
     * <p>
     * <strong>Solo se cobran las unidades que estan por encima del techo</strong>,
     * no el delta entero. Un cliente con el cupo en 98 de 100 que pide 5 paga 3, no
     * 5; y el que ya iba en 103 paga los 5, porque todas caen fuera. La resta lo
     * expresa sola: {@code max(usado + delta, techo) - max(usado, techo)}.
     *
     * <p>
     * <strong>Sin permiso, devuelve vacio y el llamador niega como
     * siempre.</strong> Vacio es la respuesta por defecto y el unico modo seguro:
     * un fallo al resolver el permiso no puede convertirse en «pase usted».
     *
     * @return el contador ya movido si el excedente se aplico; vacio si no hay
     *         permiso y hay que bloquear
     */
    private Optional<CompanyCapacityDto> chargeOverageIfAllowed(
            AdjustCompanyCapacityUsageCommand command, LimitDimensionRef dimension,
            PeriodKey periodKey, CompanyCapacity current) {
        LocalDate today = LocalDate.now(clock);
        Optional<OverageAllowance> allowance = overageAllowancePort
                .findAllowance(command.companyId(), dimension.id(), today);
        if (allowance.isEmpty()) {
            return Optional.empty();
        }
        int techo = current.getLimitQuantity();
        int usado = current.getUsedQuantity();
        int unidadesDeExceso = Math.max(usado + command.delta(), techo) - Math.max(usado, techo);
        if (unidadesDeExceso <= 0) {
            // No deberia ocurrir --el llamador ya comprobo que se pasa-- pero cobrar
            // cero unidades reventaria en OverageAllowance.amountFor y convertiria una
            // aritmetica inesperada en un 500 sobre una operacion legitima.
            return Optional.empty();
        }
        int movidas = repository.addUsageAllowingOverage(command.companyId(), dimension.id(),
                periodKey.value(), command.delta());
        if (movidas == 0) {
            // La fila existia hace dos lineas: si ahora no se mueve, la causa ya no es
            // el techo sino el signo o una carrera. Se deja caer al camino de siempre,
            // que sabe distinguirlas.
            return Optional.empty();
        }
        OverageAllowance permiso = allowance.get();
        overageChargePort.chargeOverage(command.companyId(), permiso.subscriptionId(),
                permiso.subscriptionItemId(), dimension.code(), unidadesDeExceso,
                permiso.unitAmount(), servicePeriodStart(periodKey, today),
                servicePeriodEnd(periodKey, today));
        return Optional.of(CompanyCapacityDto.from(repository
                .findByCompanyIdAndDimension(command.companyId(), dimension.id(), periodKey.value())
                .orElseThrow(() -> notContracted(command, periodKey))));
    }

    /**
     * El tramo de servicio que ampara el cargo. Un cupo de flujo lo saca de su
     * propia clave; uno que no lo es lleva el centinela {@code ALLTIME}, que no es
     * un periodo de calendario, y entonces el tramo es el dia en que se consumio.
     *
     * <p>
     * Derivarlo de la clave —y no del reloj— es lo que hace que el mismo excedente
     * caiga siempre en el mismo mes contable, lo devengue quien lo devengue.
     */
    private static LocalDate servicePeriodStart(PeriodKey periodKey, LocalDate today) {
        return periodKey.isRealPeriod() ? periodKey.periodStart() : today;
    }

    private static LocalDate servicePeriodEnd(PeriodKey periodKey, LocalDate today) {
        return periodKey.isRealPeriod() ? periodKey.periodEnd() : today;
    }

    /**
     * Hace nacer la fila del periodo que entra y vuelve a intentar el movimiento
     * <strong>una sola vez</strong> (R-LIMIT-04).
     *
     * <p>
     * Este es el punto entero del arreglo. Hasta aqui, a las 00:00 del primer dia
     * del periodo la clave cambiaba, no habia fila para la nueva, el {@code UPDATE}
     * afectaba a cero filas y eso era <em>indistinguible de haber topado con el
     * techo</em>: el cupo de flujo no se reiniciaba, se <strong>cerraba</strong>, y
     * la agenda de la clinica quedaba bloqueada al 100&nbsp;% hasta que alguien
     * recalculara a mano --y el recalculo tampoco la habria creado, porque deriva
     * del contrato y el contrato no habla de periodos--.
     *
     * <p>
     * <strong>Nace sin proceso programado</strong>, que es lo que la regla exige
     * literalmente: la fila del periodo nuevo aparece la primera vez que alguien
     * consume contra el. Y nace <strong>heredando el techo ya resuelto</strong> del
     * periodo anterior, no resolviendolo: cruzar el contrato y el catalogo aqui
     * pondria tres tablas en el camino mas caliente del sistema, que es justo lo
     * que la fila resuelta existe para evitar.
     *
     * <p>
     * <strong>Un solo reintento, y no un bucle.</strong> Despues del nacimiento la
     * fila existe --la haya escrito esta peticion o la que gano la carrera--, asi
     * que si el segundo intento tampoco mueve nada, la causa ya no es la ausencia
     * de fila sino el techo o el signo, y eso lo sabe leer el llamador. Un bucle
     * solo podria girar para siempre.
     */
    private Retry openPeriodAndRetry(AdjustCompanyCapacityUsageCommand command,
            LimitDimensionRef dimension, PeriodKey periodKey) {
        int born = repository.openPeriod(command.companyId(), dimension.id(), periodKey.value(),
                LocalDateTime.now(clock));
        if (born == 0) {
            // No habia periodo anterior del que heredar: la serie no existe todavia y
            // quien la abre es el recalculo. Se devuelve el estado tal cual estaba
            // para que la decision caiga en D-74 o en techo cero, que es donde toca.
            return new Retry(0, Optional.empty());
        }
        int updated = repository.addUsage(command.companyId(), dimension.id(), periodKey.value(),
                command.delta());
        return new Retry(updated, repository.findByCompanyIdAndDimension(command.companyId(),
                dimension.id(), periodKey.value()));
    }

    /**
     * Sin fila, y la respuesta depende de <strong>por que</strong> no la hay
     * (D-74).
     *
     * <p>
     * «Sin fila porque no se vendio» y «sin fila porque el eje no existia cuando se
     * firmo» tienen respuestas <em>opuestas</em>. La primera es techo cero. La
     * segunda es sin techo: quien firmo antes de que el eje existiera no acepto ese
     * limite y no puede quedar sujeto a el. Sin esta distincion, añadir un eje de
     * citas en abril deja bloqueadas en el primer recalculo las cuarenta agendas de
     * los contratos firmados en enero, y el sintoma que ve soporte es «no puedo
     * agendar», sin ninguna relacion aparente con un cambio de catalogo.
     *
     * <p>
     * La fecha de firma se consulta <strong>aqui y no antes</strong>: esta rama
     * solo se pisa cuando el contador ya iba a fallar, asi que la consulta no toca
     * el camino feliz.
     */
    private CompanyCapacityDto notContractedOrUncapped(AdjustCompanyCapacityUsageCommand command,
            LimitDimensionRef dimension, PeriodKey periodKey) {
        LocalDate signedOn = subscriptionQueryPort
                .findContractSignedOnByCompanyId(command.companyId()).orElse(null);
        if (dimension.postdates(signedOn)) {
            return CompanyCapacityDto.uncapped(command.companyId(), dimension, periodKey);
        }
        throw notContracted(command, periodKey);
    }

    /**
     * Un codigo de eje que no esta en el catalogo es un error de programacion o una
     * siembra incompleta, nunca un cupo. Se distingue de "no contratado" a
     * proposito: leerlo como techo cero convertiria una fila que falta en
     * {@code limit_dimensions} en un bloqueo silencioso de una funcion entera.
     */
    private static IllegalArgumentException unknownDimension(
            AdjustCompanyCapacityUsageCommand command) {
        return new IllegalArgumentException("Unknown limit dimension code: "
                + command.dimensionCode() + ". The counter resolves axes against"
                + " limit_dimensions; seed the row before consuming it");
    }

    /**
     * Sin fila no hay techo contratado, y eso se lee como <strong>limite
     * cero</strong> --salvo el caso de D-74, que resuelve
     * {@link #notContractedOrUncapped} antes de llegar aqui--. El razonamiento
     * completo esta en {@link CompanyCapacityNotFoundException}. El mensaje nombra
     * el eje y el periodo y apunta al recalculo porque la causa mas probable no es
     * que el cliente no lo haya contratado, sino que sus contadores todavia no se
     * han derivado.
     */
    private static CompanyCapacityNotFoundException notContracted(
            AdjustCompanyCapacityUsageCommand command, PeriodKey periodKey) {
        return new CompanyCapacityNotFoundException(
                "Company " + command.companyId() + " has no contracted capacity for dimension "
                        + command.dimensionCode() + " and period " + periodKey.value()
                        + ": an absent row means limit zero, not unlimited, unless the dimension"
                        + " itself was born after the contract was signed. Recalculate the company"
                        + " entitlements if its contract does include this dimension");
    }

    /** El estado despues de hacer nacer el periodo y reintentar. */
    private record Retry(int updated, Optional<CompanyCapacity> capacity) {
    }
}
