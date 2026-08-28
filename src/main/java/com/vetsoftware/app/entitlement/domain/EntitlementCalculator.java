package com.vetsoftware.app.entitlement.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * El calculo entero de "que puede usar esta empresa ahora mismo", en un sitio y
 * sin framework.
 *
 * <p>
 * Vive en {@code domain} porque no tiene ninguna dependencia de infraestructura
 * y porque es la unica pieza del slice donde hay reglas de negocio de verdad;
 * el caso de uso solo orquesta puertos alrededor de esta funcion. Es
 * <strong>pura y determinista</strong>: mismo contrato y mismo instante, mismas
 * filas, en el mismo orden. Eso es lo que hace que recalcular dos veces sea
 * idempotente.
 *
 * <p>
 * Las tres decisiones que lo gobiernan, y que son las que se rompen en silencio
 * si se hacen mal:
 * <ol>
 * <li><strong>Vigente no es "sin fecha de fin"</strong>, es "ya empezo y
 * todavia no ha terminado". Ver {@link ModuleGrantLine#isCurrentOn(LocalDate)}.
 * <li><strong>Dar de baja un modulo nunca borra nada</strong>: baja a
 * {@link AccessLevel#READ_ONLY}, y si el submodulo no admite solo lectura queda
 * {@link AccessLevel#NONE} --oculto-- en vez de mostrar pantallas rotas.
 * <li><strong>No existe el corte total.</strong> Ni el contrato cancelado ni la
 * mora bajan de {@code READ_ONLY}: {@link ContractStatus#maxAccessLevel()} es
 * el unico techo, y su minimo es {@code READ_ONLY}.
 * <li><strong>Al vencer la prueba el acceso BAJA, no desaparece</strong>
 * (R-ENT-01). Una linea en prueba emite <em>dos</em> filas de una sola vez: la
 * de prueba, que se cierra en su fecha, y <strong>la que la sucede</strong>,
 * escrita el mismo dia y esperando su turno. Emitir solo la primera --ponerle
 * fecha de caducidad y no escribir nada detras-- deja a la clinica sin ninguna
 * fila vigente el dia del vencimiento, que es exactamente "sin acceso". La
 * sucesora arranca donde la otra acaba, y por eso
 * {@code uq_company_entitlements} incluye {@code valid_from}: sin esa tercera
 * columna solo cabria una de las dos.
 * </ol>
 *
 * <p>
 * <strong>La prueba es de la linea, no del contrato.</strong> El modo de cobro
 * y el fin de prueba viajan en cada {@link ModuleGrantLine}, asi que cada linea
 * vence por su cuenta y el estado del contrato no decide nada de esto
 * (R-TRIAL-13, R-TRIAL-15). Mirar {@code subscriptions.status} para saber si
 * algo esta en prueba es la trampa que D-01 obliga a desactivar: hace que un
 * solo dia de mora mate la prueba de los tres modulos a la vez y para siempre.
 */
public final class EntitlementCalculator {

    private EntitlementCalculator() {
    }

    /**
     * Reconstruye las filas derivadas de una empresa a partir de su contrato,
     * cuando no hay ninguna concesion manual que respetar.
     */
    public static EntitlementRecalculation recalculate(Long companyId, ContractSnapshot contract,
            LocalDateTime now) {
        return recalculate(companyId, contract, now, List.of());
    }

    /**
     * Reconstruye las filas <strong>derivadas</strong> de una empresa
     * --{@code SUBSCRIPTION}, {@code TRIAL}, {@code CORE}-- dejando fuera del
     * calculo los submodulos que ya tienen una concesion manual.
     *
     * <p>
     * <strong>Un {@code MANUAL_GRANT} no es derivable y por eso no se
     * toca.</strong> El modelo lo define como "se lo diste tu a mano, y queda
     * constancia de que fue a mano", y una fila que el siguiente recalculo borra no
     * deja constancia de nada. Peor: la borraria un cambio de cantidad en otra
     * linea del contrato, asi que quien la concedio no tendria forma de relacionar
     * las dos cosas. La capa derivada se sigue reconstruyendo entera desde los
     * contratos --que son la verdad-- y lo que no sale de un contrato sobrevive
     * intacto.
     *
     * <p>
     * Excluirlos del calculo no es solo semantico: {@code uq_company_entitlements}
     * es unico por {@code (company_id, sub_module_id)}, asi que emitir una fila
     * derivada para un submodulo que ya tiene la manual reventaria el
     * {@code INSERT} en mitad de la transaccion de recalculo.
     *
     * @param preserved
     *            las concesiones manuales vigentes de la empresa, que el llamador
     *            lee antes de borrar y no vuelve a escribir
     */
    public static EntitlementRecalculation recalculate(Long companyId, ContractSnapshot contract,
            LocalDateTime now, List<CompanyEntitlement> preserved) {
        if (companyId == null)
            throw new IllegalArgumentException("company id is required");
        if (contract == null)
            throw new IllegalArgumentException("contract snapshot is required");
        if (now == null)
            throw new IllegalArgumentException("recalculation instant is required");

        LocalDate day = now.toLocalDate();
        SubscriptionRef subscription = contract.subscription();
        AccessLevel ceiling = subscription.status().maxAccessLevel();

        Set<Long> concedidosAMano = subModuleIdsOf(preserved);
        // Un submodulo produce UNA entrada, pero esa entrada puede llevar dos filas
        // --la prueba y su sucesora--. Sigue ganando la primera linea que lo abre:
        // computeIfAbsent no se dispara dos veces para el mismo submodulo.
        Map<Long, List<CompanyEntitlement>> bySubModule = new LinkedHashMap<>();
        for (ModuleGrantLine line : currentLinesFirst(contract.moduleLines(), day)) {
            if (concedidosAMano.contains(line.subModule().id())) {
                continue;
            }
            bySubModule.computeIfAbsent(line.subModule().id(),
                    key -> grantFor(companyId, subscription, line, ceiling, now));
        }
        for (ModuleGrantLine line : endedLinesMostRecentFirst(contract.moduleLines(), day)) {
            if (concedidosAMano.contains(line.subModule().id())) {
                continue;
            }
            bySubModule.computeIfAbsent(line.subModule().id(),
                    key -> List.of(downgradeFor(companyId, subscription, line, ceiling, now)));
        }

        List<CompanyEntitlement> entitlements = new ArrayList<>();
        for (List<CompanyEntitlement> filas : bySubModule.values()) {
            entitlements.addAll(filas);
        }
        // El desempate por valid_from no es cosmetico: con dos filas por submodulo,
        // un orden inestable haria que dos recalculos identicos escribieran en
        // distinto orden y que un diff de auditoria mintiera.
        entitlements.sort(Comparator.<CompanyEntitlement, Long>comparing(e -> e.getSubModule().id())
                .thenComparing(CompanyEntitlement::getValidFrom));
        return new EntitlementRecalculation(List.copyOf(entitlements),
                capacities(companyId, subscription, contract.capacityLines(), day, now));
    }

    /**
     * Cruza los contadores recien derivados con los que ya existian.
     *
     * <p>
     * <strong>{@code used_quantity} nunca se recalcula</strong>: es un dato del
     * mundo real --cuantos usuarios hay dados de alta-- y no una derivacion del
     * contrato. Una unidad que deja de estar contratada baja su techo a cero y
     * conserva el consumo, que es exactamente el estado "5 usuarios con un techo de
     * 3" que el modelo admite a proposito.
     */
    public static List<CompanyCapacity> reconcile(List<CompanyCapacity> existing,
            List<CompanyCapacity> computed, LocalDateTime now) {
        Map<CounterKey, CompanyCapacity> previous = new LinkedHashMap<>();
        if (existing != null) {
            for (CompanyCapacity capacity : existing) {
                previous.put(CounterKey.of(capacity), capacity);
            }
        }
        Map<CounterKey, CompanyCapacity> merged = new LinkedHashMap<>();
        if (computed != null) {
            for (CompanyCapacity capacity : computed) {
                CounterKey key = CounterKey.of(capacity);
                merged.put(key, capacity.reconciledFrom(previous.get(key)));
            }
        }
        for (Map.Entry<CounterKey, CompanyCapacity> entry : previous.entrySet()) {
            if (!merged.containsKey(entry.getKey())) {
                merged.put(entry.getKey(), entry.getValue().withoutContract(null, now));
            }
        }
        return List.copyOf(merged.values());
    }

    /**
     * La identidad de un contador: el eje y el periodo, que es exactamente lo que
     * declara {@code uq_company_capacities (company_id, limit_dimension_id,
     * period_key)} --la empresa es la misma en todo el recalculo--.
     *
     * <p>
     * Antes esto era un {@code EnumMap} sobre la lista cerrada de cuatro unidades.
     * Al pasar el eje a ser una fila del catalogo, la clave deja de ser un
     * enumerado; y tiene que incluir el periodo, porque un eje de flujo tiene un
     * contador por periodo y cruzarlos por el eje solo los fundiria en uno.
     */
    private record CounterKey(Long dimensionId, String periodKey) {

        static CounterKey of(CompanyCapacity capacity) {
            return new CounterKey(capacity.getDimension().id(), capacity.getPeriodKey().value());
        }
    }

    private static Set<Long> subModuleIdsOf(List<CompanyEntitlement> entitlements) {
        if (entitlements == null || entitlements.isEmpty()) {
            return Set.of();
        }
        Set<Long> ids = new HashSet<>();
        for (CompanyEntitlement entitlement : entitlements) {
            ids.add(entitlement.getSubModule().id());
        }
        return ids;
    }

    private static List<ModuleGrantLine> currentLinesFirst(List<ModuleGrantLine> lines,
            LocalDate day) {
        List<ModuleGrantLine> current = new ArrayList<>(
                lines.stream().filter(line -> line.isCurrentOn(day)).toList());
        current.sort(Comparator.comparing(ModuleGrantLine::subscriptionItemId));
        return current;
    }

    private static List<ModuleGrantLine> endedLinesMostRecentFirst(List<ModuleGrantLine> lines,
            LocalDate day) {
        List<ModuleGrantLine> ended = new ArrayList<>(
                lines.stream().filter(line -> line.hasEndedOn(day)).toList());
        // La baja mas reciente es la que explica el estado actual del submodulo.
        Comparator<ModuleGrantLine> byEffectiveTo = Comparator
                .comparing(ModuleGrantLine::effectiveTo);
        Comparator<ModuleGrantLine> byItemId = Comparator
                .comparing(ModuleGrantLine::subscriptionItemId);
        ended.sort(byEffectiveTo.reversed().thenComparing(byItemId.reversed()));
        return ended;
    }

    /**
     * Las filas que abre una linea vigente. Son <strong>dos</strong> si la linea
     * esta en prueba --la de prueba y la que la sucede-- y una en cualquier otro
     * caso.
     *
     * <p>
     * Las dos se escriben <strong>en el mismo recalculo</strong>, no el dia del
     * vencimiento. Eso es lo que hace que el acceso baje solo, por dato, aunque el
     * barrido nocturno no llegue a correr nunca: la de prueba se cierra sola a su
     * fecha y la sucesora se abre sola en ese mismo instante, sin ningun proceso en
     * medio que se pueda olvidar de correr. Dejar la fila de prueba caducando a
     * solas deja a la clinica sin ninguna fila vigente ese dia, que es exactamente
     * "sin acceso": el bloqueante que este metodo existe para cerrar.
     */
    private static List<CompanyEntitlement> grantFor(Long companyId, SubscriptionRef subscription,
            ModuleGrantLine line, AccessLevel ceiling, LocalDateTime now) {
        LocalDateTime validFrom = line.effectiveFrom().atStartOfDay();
        LocalDateTime lineEnds = line.effectiveTo() == null
                ? null
                : line.effectiveTo().atStartOfDay();
        AccessLevel techo = ceilingFor(line, ceiling);

        if (!line.chargeMode().isTrial()) {
            if (lineEnds != null && !lineEnds.isAfter(validFrom)) {
                return List.of(downgradeFor(companyId, subscription, line, ceiling, now));
            }
            AccessLevel level = levelFor(line, line.chargeMode().accessLevel(), techo);
            return List.of(CompanyEntitlement.derived(companyId, line.subModule(), level,
                    line.chargeMode().entitlementSource(line.core()), subscription.id(),
                    line.subscriptionItemId(), validFrom, lineEnds, now));
        }

        LocalDateTime trialCloses = line.trialClosesAt();
        LocalDateTime trialUntil = earliest(trialCloses, lineEnds);
        if (trialUntil != null && !trialUntil.isAfter(validFrom)) {
            // Ventana degenerada (la prueba caduco antes de que la linea arrancara): no
            // se emite un permiso imposible, se emite el permiso degradado.
            return List.of(downgradeFor(companyId, subscription, line, ceiling, now));
        }

        List<CompanyEntitlement> filas = new ArrayList<>(2);
        filas.add(CompanyEntitlement.derived(companyId, line.subModule(),
                levelFor(line, AccessLevel.FULL, techo), EntitlementSource.TRIAL, subscription.id(),
                line.subscriptionItemId(), validFrom, trialUntil, now));

        // La sucesora solo tiene sitio si la linea sigue viva despues del
        // vencimiento. Si la baja del modulo llega antes, no hay nada que suceder:
        // manda la baja, y de eso ya se encarga la rama de lineas terminadas.
        if (lineEnds == null || lineEnds.isAfter(trialCloses)) {
            TrialOutcomePolicy desenlace = line.trialOutcome();
            filas.add(CompanyEntitlement.derived(companyId, line.subModule(),
                    levelFor(line, desenlace.accessLevel(), techo),
                    desenlace.entitlementSource(line.core()), subscription.id(),
                    line.subscriptionItemId(), trialCloses, lineEnds, now));
        }
        return List.copyOf(filas);
    }

    /**
     * El techo que se le aplica a esta linea. Un submodulo
     * {@code degradation_immune} <strong>no se degrada jamas</strong> --ni por
     * mora, ni por cupo, ni por baja (R-ENT-05)--, asi que el techo del contrato no
     * le llega: una cuenta en {@code READ_ONLY} por mora sigue pudiendo emitir sus
     * facturas electronicas. Es la unica barandilla entre una discusion comercial y
     * una clinica que no puede facturar.
     */
    private static AccessLevel ceilingFor(ModuleGrantLine line, AccessLevel ceiling) {
        return line.degradationImmune() ? AccessLevel.FULL : ceiling;
    }

    /** Nivel propio, recortado por el techo y ocultado si no sabe solo lectura. */
    private static AccessLevel levelFor(ModuleGrantLine line, AccessLevel propio,
            AccessLevel techo) {
        if (line.degradationImmune()) {
            return AccessLevel.FULL;
        }
        return propio.restrictedTo(techo).hiddenIfNotReadOnlyCapable(line.readOnlyCapable());
    }

    /**
     * La baja de un modulo: el maximo que queda es consultar e imprimir, y si el
     * submodulo no sabe hacerlo, desaparece del menu. La ventana queda abierta sin
     * fin: lo que el cliente escribio sigue siendo suyo.
     */
    private static CompanyEntitlement downgradeFor(Long companyId, SubscriptionRef subscription,
            ModuleGrantLine line, AccessLevel ceiling, LocalDateTime now) {
        AccessLevel level = levelFor(line, AccessLevel.READ_ONLY, ceilingFor(line, ceiling));
        LocalDateTime validFrom = line.effectiveTo() == null
                ? line.effectiveFrom().atStartOfDay()
                : line.effectiveTo().atStartOfDay();
        EntitlementSource source = line.core()
                ? EntitlementSource.CORE
                : EntitlementSource.SUBSCRIPTION;
        return CompanyEntitlement.derived(companyId, line.subModule(), level, source,
                subscription.id(), line.subscriptionItemId(), validFrom, null, now);
    }

    private static List<CompanyCapacity> capacities(Long companyId, SubscriptionRef subscription,
            List<CapacityGrantLine> lines, LocalDate day, LocalDateTime now) {
        // Un contrato que ya no esta vigente no sostiene ningun techo: las unidades
        // desaparecen del calculo y reconcile() las deja en cero conservando el
        // consumo.
        if (!subscription.status().isCurrent())
            return List.of();
        Map<Long, LimitDimensionRef> dimensions = new LinkedHashMap<>();
        Map<Long, Integer> ceilings = new LinkedHashMap<>();
        Map<Long, ResetPeriod> resets = new LinkedHashMap<>();
        for (CapacityGrantLine line : lines) {
            if (line.isCurrentOn(day)) {
                Long dimensionId = line.dimension().id();
                dimensions.putIfAbsent(dimensionId, line.dimension());
                ceilings.merge(dimensionId, line.ceiling(), Integer::sum);
                mergeReset(resets, line);
            }
        }
        List<CompanyCapacity> result = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : ceilings.entrySet()) {
            LimitDimensionRef dimension = dimensions.get(entry.getKey());
            // El periodo lo decide el eje, no el llamador. Un eje de existencias
            // lleva el centinela; uno de flujo, la clave del periodo en curso segun
            // la granularidad que la venta congelo. Esta es la fila de la que nace
            // toda la serie del contador de flujo: las de los periodos siguientes
            // heredan de ella su techo ya resuelto (R-LIMIT-04), sin volver a cruzar
            // el contrato.
            result.add(CompanyCapacity.contracted(companyId, dimension,
                    PeriodKey.forContract(dimension.measureKind(), resets.get(entry.getKey()), day),
                    entry.getValue(), subscription.id(), now));
        }
        return List.copyOf(result);
    }

    /**
     * Una serie de contador tiene <strong>una sola granularidad</strong>.
     *
     * <p>
     * Dos lineas vigentes que vendan el mismo eje de flujo con periodos distintos
     * --una mensual y otra trimestral-- no producen un techo mayor: producen dos
     * series que se pisan, porque {@code 2026-03} y {@code 2026-Q1} son claves
     * distintas para el mismo consumo. Sumar sus techos y quedarse con una de las
     * dos claves repartiria el consumo entre dos filas segun quien lo escribiera, y
     * el cliente veria su cupo entero o vacio segun la hora. Se rechaza en voz
     * alta: R-LIMIT-37 exige cerrar la serie y abrir otra con el hecho que lo
     * documenta, y eso es una operacion, no un efecto colateral de un recalculo.
     */
    private static void mergeReset(Map<Long, ResetPeriod> resets, CapacityGrantLine line) {
        ResetPeriod incoming = line.resetPeriod();
        ResetPeriod known = resets.get(line.dimension().id());
        if (known == null) {
            if (incoming != null)
                resets.put(line.dimension().id(), incoming);
            return;
        }
        if (incoming != null && incoming != known)
            throw new IllegalStateException("Contract grants dimension " + line.dimension().code()
                    + " with two different reset periods at once (" + known + " and " + incoming
                    + "): a counter series has a single granularity, and changing it has to close"
                    + " the current series and open another one (R-LIMIT-37)");
    }

    private static LocalDateTime earliest(LocalDateTime first, LocalDateTime second) {
        if (first == null)
            return second;
        if (second == null)
            return first;
        return first.isBefore(second) ? first : second;
    }
}
