package com.vetsoftware.app.subscriptionmodule.application.usecase;

import com.vetsoftware.app.subscriptionmodule.application.command.ListModuleShowcaseQuery;
import com.vetsoftware.app.subscriptionmodule.application.dto.ModuleCeilingDto;
import com.vetsoftware.app.subscriptionmodule.application.dto.ModuleShowcaseDto;
import com.vetsoftware.app.subscriptionmodule.application.port.in.ListModuleShowcaseUseCase;
import com.vetsoftware.app.subscriptionmodule.application.port.out.CompanyModuleLineQueryPort;
import com.vetsoftware.app.subscriptionmodule.application.port.out.CompanyModuleLineQueryPort.CompanyModuleLine;
import com.vetsoftware.app.subscriptionmodule.application.port.out.CompanyUsageSnapshotQueryPort;
import com.vetsoftware.app.subscriptionmodule.application.port.out.CompanyUsageSnapshotQueryPort.CapacitySnapshot;
import com.vetsoftware.app.subscriptionmodule.application.port.out.EmployeeAdminCheckPort;
import com.vetsoftware.app.subscriptionmodule.application.port.out.ModuleCatalogQueryPort;
import com.vetsoftware.app.subscriptionmodule.application.port.out.ModuleCatalogQueryPort.ModuleCatalogEntry;
import com.vetsoftware.app.subscriptionmodule.application.port.out.ModuleCatalogQueryPort.ModuleCeilingDefinition;
import com.vetsoftware.app.subscriptionmodule.domain.ModuleState;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ListModuleShowcaseService implements ListModuleShowcaseUseCase {

    private static final String NEVER_FREE = "NEVER_FREE";
    private static final List<String> CUMULATIVE_DIMENSIONS = List.of("ANIMAL", "OWNER");
    private static final List<String> FLOW_DIMENSIONS = List.of("APPOINTMENT", "GROOMING_SERVICE");
    private static final String SERVICE_ITEM_DIMENSION = "SERVICE_ITEM";
    private static final List<String> CAPACITY_DIMENSIONS = List.of("USER", "BRANCH");

    private final ModuleCatalogQueryPort catalogQueryPort;
    private final CompanyModuleLineQueryPort lineQueryPort;
    private final CompanyUsageSnapshotQueryPort usageQueryPort;
    private final EmployeeAdminCheckPort adminCheckPort;
    private final Clock clock;

    public ListModuleShowcaseService(ModuleCatalogQueryPort catalogQueryPort,
            CompanyModuleLineQueryPort lineQueryPort, CompanyUsageSnapshotQueryPort usageQueryPort,
            EmployeeAdminCheckPort adminCheckPort, Clock clock) {
        this.catalogQueryPort = catalogQueryPort;
        this.lineQueryPort = lineQueryPort;
        this.usageQueryPort = usageQueryPort;
        this.adminCheckPort = adminCheckPort;
        this.clock = clock;
    }

    @Override
    public List<ModuleShowcaseDto> execute(ListModuleShowcaseQuery query) {
        LocalDate today = LocalDate.now(clock);
        boolean isAdmin = adminCheckPort.isCompanyAdmin(query.employeeId(), query.companyId());
        return catalogQueryPort.listActiveModules(today).stream()
                .map(entry -> toDto(query.companyId(), entry, isAdmin)).toList();
    }

    private ModuleShowcaseDto toDto(Long companyId, ModuleCatalogEntry entry, boolean isAdmin) {
        Optional<CompanyModuleLine> currentLine = lineQueryPort.findCurrentLine(companyId,
                entry.catalogItemId());
        ModuleState state = resolveState(entry, currentLine);
        LocalDate trialEndDate = currentLine.map(CompanyModuleLine::trialEndDate).orElse(null);
        List<ModuleCeilingDto> ceilings = (state == ModuleState.TRIAL
                || state == ModuleState.FREE_LIMITED)
                        ? ceilingsOf(companyId, entry.catalogItemId())
                        : List.of();
        boolean purchasable = entry.selfService() && (state == ModuleState.TRIAL
                || state == ModuleState.FREE_LIMITED || state == ModuleState.EXPIRED_READ_ONLY
                || state == ModuleState.NEVER_FREE);
        return new ModuleShowcaseDto(entry.code(), entry.name(), entry.shortDescription(), state,
                trialEndDate, ceilings, entry.monthlyPrice(), entry.annualPrice(), purchasable,
                purchasable && isAdmin);
    }

    /**
     * Sin línea, el desenlace depende SOLO de si el artículo pudo probarse alguna
     * vez: {@code NEVER_FREE} para facturación electrónica (R-TRIAL-12),
     * {@code NOT_INCLUDED} para cualquier otro caso — que hoy no debería darse,
     * porque el alta prueba todo lo {@code ELIGIBLE}, pero un artículo retirado del
     * catálogo tras la venta cae aquí en vez de romper la pantalla.
     */
    private static ModuleState resolveState(ModuleCatalogEntry entry,
            Optional<CompanyModuleLine> currentLine) {
        return currentLine.map(line -> switch (line.chargeMode()) {
            case "TRIAL" -> ModuleState.TRIAL;
            case "PAID" -> ModuleState.PAID;
            case "FREE_LIMITED" -> ModuleState.FREE_LIMITED;
            case "EXPIRED_READ_ONLY" -> ModuleState.EXPIRED_READ_ONLY;
            default -> ModuleState.NOT_INCLUDED;
        }).orElseGet(() -> NEVER_FREE.equals(entry.trialEligibility())
                ? ModuleState.NEVER_FREE
                : ModuleState.NOT_INCLUDED);
    }

    private List<ModuleCeilingDto> ceilingsOf(Long companyId, Long catalogItemId) {
        return catalogQueryPort.findCeilings(catalogItemId).stream()
                .map(definition -> toCeilingDto(companyId, definition)).toList();
    }

    private ModuleCeilingDto toCeilingDto(Long companyId, ModuleCeilingDefinition definition) {
        String dimension = definition.dimensionCode();
        int used;
        int limit = definition.limitQuantity();
        if (CAPACITY_DIMENSIONS.contains(dimension)) {
            Optional<CapacitySnapshot> capacity = usageQueryPort.findCapacity(companyId, dimension);
            used = capacity.map(CapacitySnapshot::usedQuantity).orElse(0);
            limit = capacity.map(CapacitySnapshot::limitQuantity).orElse(limit);
        } else if (SERVICE_ITEM_DIMENSION.equals(dimension)) {
            used = usageQueryPort.countActiveServices(companyId);
        } else if (FLOW_DIMENSIONS.contains(dimension)) {
            used = usageQueryPort.countCurrentMonth(companyId, dimension);
        } else if (CUMULATIVE_DIMENSIONS.contains(dimension)) {
            used = usageQueryPort.countCumulative(companyId, dimension);
        } else {
            used = 0;
        }
        return new ModuleCeilingDto(dimension, definition.measureKind(), used, limit,
                definition.warnThreshold(), definition.enforcement());
    }
}
