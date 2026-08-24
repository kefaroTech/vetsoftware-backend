package com.vetsoftware.app.entitlement.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
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
 * </ol>
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
        LocalDateTime trialEnd = trialEndInstant(subscription);

        Set<Long> concedidosAMano = subModuleIdsOf(preserved);
        Map<Long, CompanyEntitlement> bySubModule = new LinkedHashMap<>();
        for (ModuleGrantLine line : currentLinesFirst(contract.moduleLines(), day)) {
            if (concedidosAMano.contains(line.subModule().id())) {
                continue;
            }
            bySubModule.computeIfAbsent(line.subModule().id(),
                    key -> grantFor(companyId, subscription, line, ceiling, trialEnd, now));
        }
        for (ModuleGrantLine line : endedLinesMostRecentFirst(contract.moduleLines(), day)) {
            if (concedidosAMano.contains(line.subModule().id())) {
                continue;
            }
            bySubModule.computeIfAbsent(line.subModule().id(),
                    key -> downgradeFor(companyId, subscription, line, ceiling, now));
        }

        List<CompanyEntitlement> entitlements = new ArrayList<>(bySubModule.values());
        entitlements.sort(Comparator.comparing(e -> e.getSubModule().id()));
        return new EntitlementRecalculation(entitlements,
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
        Map<CapacityUnit, CompanyCapacity> previous = new EnumMap<>(CapacityUnit.class);
        if (existing != null) {
            for (CompanyCapacity capacity : existing) {
                previous.put(capacity.getUnit(), capacity);
            }
        }
        Map<CapacityUnit, CompanyCapacity> merged = new EnumMap<>(CapacityUnit.class);
        if (computed != null) {
            for (CompanyCapacity capacity : computed) {
                merged.put(capacity.getUnit(),
                        capacity.reconciledFrom(previous.get(capacity.getUnit())));
            }
        }
        for (Map.Entry<CapacityUnit, CompanyCapacity> entry : previous.entrySet()) {
            if (!merged.containsKey(entry.getKey())) {
                merged.put(entry.getKey(), entry.getValue().withoutContract(null, now));
            }
        }
        return List.copyOf(merged.values());
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

    private static LocalDateTime trialEndInstant(SubscriptionRef subscription) {
        if (subscription.status() != ContractStatus.TRIALING || subscription.trialEndDate() == null)
            return null;
        // trial_end_date es el ultimo dia de prueba, inclusive: la ventana se cierra
        // al arrancar el dia siguiente. Sin esto la prueba moriria un dia antes.
        return subscription.trialEndDate().plusDays(1).atStartOfDay();
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

    private static CompanyEntitlement grantFor(Long companyId, SubscriptionRef subscription,
            ModuleGrantLine line, AccessLevel ceiling, LocalDateTime trialEnd, LocalDateTime now) {
        LocalDateTime validFrom = line.effectiveFrom().atStartOfDay();
        LocalDateTime validUntil = earliest(trialEnd,
                line.effectiveTo() == null ? null : line.effectiveTo().atStartOfDay());
        if (validUntil != null && !validUntil.isAfter(validFrom)) {
            // Ventana degenerada (la prueba caduco antes de que la linea arrancara): no
            // se emite un permiso imposible, se emite el permiso degradado.
            return downgradeFor(companyId, subscription, line, ceiling, now);
        }
        AccessLevel level = AccessLevel.FULL.restrictedTo(ceiling)
                .hiddenIfNotReadOnlyCapable(line.readOnlyCapable());
        return CompanyEntitlement.derived(companyId, line.subModule(), level,
                sourceFor(subscription, line), subscription.id(), line.subscriptionItemId(),
                validFrom, validUntil, now);
    }

    /**
     * La baja de un modulo: el maximo que queda es consultar e imprimir, y si el
     * submodulo no sabe hacerlo, desaparece del menu. La ventana queda abierta sin
     * fin: lo que el cliente escribio sigue siendo suyo.
     */
    private static CompanyEntitlement downgradeFor(Long companyId, SubscriptionRef subscription,
            ModuleGrantLine line, AccessLevel ceiling, LocalDateTime now) {
        AccessLevel level = AccessLevel.READ_ONLY.restrictedTo(ceiling)
                .hiddenIfNotReadOnlyCapable(line.readOnlyCapable());
        LocalDateTime validFrom = line.effectiveTo() == null
                ? line.effectiveFrom().atStartOfDay()
                : line.effectiveTo().atStartOfDay();
        EntitlementSource source = line.core()
                ? EntitlementSource.CORE
                : EntitlementSource.SUBSCRIPTION;
        return CompanyEntitlement.derived(companyId, line.subModule(), level, source,
                subscription.id(), line.subscriptionItemId(), validFrom, null, now);
    }

    private static EntitlementSource sourceFor(SubscriptionRef subscription, ModuleGrantLine line) {
        if (subscription.status() == ContractStatus.TRIALING)
            return EntitlementSource.TRIAL;
        return line.core() ? EntitlementSource.CORE : EntitlementSource.SUBSCRIPTION;
    }

    private static List<CompanyCapacity> capacities(Long companyId, SubscriptionRef subscription,
            List<CapacityGrantLine> lines, LocalDate day, LocalDateTime now) {
        // Un contrato que ya no esta vigente no sostiene ningun techo: las unidades
        // desaparecen del calculo y reconcile() las deja en cero conservando el
        // consumo.
        if (!subscription.status().isCurrent())
            return List.of();
        Map<CapacityUnit, Integer> ceilings = new EnumMap<>(CapacityUnit.class);
        for (CapacityGrantLine line : lines) {
            if (line.isCurrentOn(day)) {
                ceilings.merge(line.unit(), line.ceiling(), Integer::sum);
            }
        }
        List<CompanyCapacity> result = new ArrayList<>();
        for (Map.Entry<CapacityUnit, Integer> entry : ceilings.entrySet()) {
            result.add(CompanyCapacity.contracted(companyId, entry.getKey(), entry.getValue(),
                    subscription.id(), now));
        }
        return List.copyOf(result);
    }

    private static LocalDateTime earliest(LocalDateTime first, LocalDateTime second) {
        if (first == null)
            return second;
        if (second == null)
            return first;
        return first.isBefore(second) ? first : second;
    }
}
