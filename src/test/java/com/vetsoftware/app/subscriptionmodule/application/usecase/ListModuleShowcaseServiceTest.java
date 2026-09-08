package com.vetsoftware.app.subscriptionmodule.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.subscriptionmodule.application.command.ListModuleShowcaseQuery;
import com.vetsoftware.app.subscriptionmodule.application.dto.ModuleShowcaseDto;
import com.vetsoftware.app.subscriptionmodule.application.port.out.CompanyModuleLineQueryPort;
import com.vetsoftware.app.subscriptionmodule.application.port.out.CompanyModuleLineQueryPort.CompanyModuleLine;
import com.vetsoftware.app.subscriptionmodule.application.port.out.CompanyUsageSnapshotQueryPort;
import com.vetsoftware.app.subscriptionmodule.application.port.out.CompanyUsageSnapshotQueryPort.CapacitySnapshot;
import com.vetsoftware.app.subscriptionmodule.application.port.out.EmployeeAdminCheckPort;
import com.vetsoftware.app.subscriptionmodule.application.port.out.ModuleCatalogQueryPort;
import com.vetsoftware.app.subscriptionmodule.application.port.out.ModuleCatalogQueryPort.ModuleCatalogEntry;
import com.vetsoftware.app.subscriptionmodule.application.port.out.ModuleCatalogQueryPort.ModuleCeilingDefinition;
import com.vetsoftware.app.subscriptionmodule.domain.ModuleState;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListModuleShowcaseServiceTest {

    private static final Long COMPANY_ID = 9L;
    private static final Long EMPLOYEE_ID = 4L;
    private static final Long CATALOG_ITEM_ID = 30L;
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-08T00:00:00Z"),
            ZoneOffset.UTC);

    @Mock
    private ModuleCatalogQueryPort catalogQueryPort;
    @Mock
    private CompanyModuleLineQueryPort lineQueryPort;
    @Mock
    private CompanyUsageSnapshotQueryPort usageQueryPort;
    @Mock
    private EmployeeAdminCheckPort adminCheckPort;

    private ListModuleShowcaseService service;

    @BeforeEach
    void setUp() {
        service = new ListModuleShowcaseService(catalogQueryPort, lineQueryPort, usageQueryPort,
                adminCheckPort, CLOCK);
    }

    private static ModuleCatalogEntry grooming() {
        return new ModuleCatalogEntry(CATALOG_ITEM_ID, "GROOMING", "Spa y guardería",
                "Servicios de estética y hospedaje", "ELIGIBLE", true, new BigDecimal("59900"),
                new BigDecimal("599000"));
    }

    @Nested
    @DisplayName("Estado por módulo")
    class Estado {

        @Test
        @DisplayName("línea TRIAL vigente se pinta como EN_PRUEBA con su fecha de fin")
        void linea_trial_se_pinta_en_prueba() {
            when(catalogQueryPort.listActiveModules(any())).thenReturn(List.of(grooming()));
            LocalDate trialEnd = LocalDate.of(2026, 9, 30);
            when(lineQueryPort.findCurrentLine(COMPANY_ID, CATALOG_ITEM_ID))
                    .thenReturn(Optional.of(new CompanyModuleLine("TRIAL", trialEnd)));
            when(catalogQueryPort.findCeilings(CATALOG_ITEM_ID)).thenReturn(List
                    .of(new ModuleCeilingDefinition("GROOMING_SERVICE", "FLOW", 30, 60, "BLOCK")));
            when(usageQueryPort.countCurrentMonth(COMPANY_ID, "GROOMING_SERVICE")).thenReturn(5);
            when(adminCheckPort.isCompanyAdmin(EMPLOYEE_ID, COMPANY_ID)).thenReturn(true);

            List<ModuleShowcaseDto> result = service
                    .execute(new ListModuleShowcaseQuery(COMPANY_ID, EMPLOYEE_ID));

            assertThat(result).hasSize(1);
            ModuleShowcaseDto dto = result.get(0);
            assertThat(dto.state()).isEqualTo(ModuleState.TRIAL);
            assertThat(dto.trialEndDate()).isEqualTo(trialEnd);
            assertThat(dto.ceilings()).hasSize(1);
            assertThat(dto.ceilings().get(0).used()).isEqualTo(5);
            assertThat(dto.purchasable()).isTrue();
            assertThat(dto.canPurchase()).isTrue();
        }

        @Test
        @DisplayName("sin línea y artículo NEVER_FREE se pinta NUNCA_GRATIS, comprable")
        void sin_linea_never_free_se_pinta_nunca_gratis() {
            ModuleCatalogEntry invoicing = new ModuleCatalogEntry(31L, "ELECTRONIC_INVOICING",
                    "Facturación electrónica", null, "NEVER_FREE", true, new BigDecimal("39900"),
                    new BigDecimal("399000"));
            when(catalogQueryPort.listActiveModules(any())).thenReturn(List.of(invoicing));
            when(lineQueryPort.findCurrentLine(COMPANY_ID, 31L)).thenReturn(Optional.empty());
            when(adminCheckPort.isCompanyAdmin(EMPLOYEE_ID, COMPANY_ID)).thenReturn(false);

            ModuleShowcaseDto dto = service
                    .execute(new ListModuleShowcaseQuery(COMPANY_ID, EMPLOYEE_ID)).get(0);

            assertThat(dto.state()).isEqualTo(ModuleState.NEVER_FREE);
            assertThat(dto.ceilings()).isEmpty();
            assertThat(dto.purchasable()).isTrue();
            assertThat(dto.canPurchase()).isFalse();
        }

        @Test
        @DisplayName("línea PAID no lleva techo ni botón de compra")
        void linea_paid_no_es_comprable() {
            when(catalogQueryPort.listActiveModules(any())).thenReturn(List.of(grooming()));
            when(lineQueryPort.findCurrentLine(COMPANY_ID, CATALOG_ITEM_ID))
                    .thenReturn(Optional.of(new CompanyModuleLine("PAID", null)));
            when(adminCheckPort.isCompanyAdmin(EMPLOYEE_ID, COMPANY_ID)).thenReturn(true);

            ModuleShowcaseDto dto = service
                    .execute(new ListModuleShowcaseQuery(COMPANY_ID, EMPLOYEE_ID)).get(0);

            assertThat(dto.state()).isEqualTo(ModuleState.PAID);
            assertThat(dto.ceilings()).isEmpty();
            assertThat(dto.purchasable()).isFalse();
            assertThat(dto.canPurchase()).isFalse();
        }

        @Test
        @DisplayName("EXPIRED_READ_ONLY sigue siendo comprable para un ADMIN")
        void expirado_es_comprable_para_admin() {
            when(catalogQueryPort.listActiveModules(any())).thenReturn(List.of(grooming()));
            when(lineQueryPort.findCurrentLine(COMPANY_ID, CATALOG_ITEM_ID)).thenReturn(Optional
                    .of(new CompanyModuleLine("EXPIRED_READ_ONLY", LocalDate.of(2026, 8, 1))));
            when(adminCheckPort.isCompanyAdmin(EMPLOYEE_ID, COMPANY_ID)).thenReturn(true);

            ModuleShowcaseDto dto = service
                    .execute(new ListModuleShowcaseQuery(COMPANY_ID, EMPLOYEE_ID)).get(0);

            assertThat(dto.state()).isEqualTo(ModuleState.EXPIRED_READ_ONLY);
            assertThat(dto.purchasable()).isTrue();
            assertThat(dto.canPurchase()).isTrue();
        }
    }

    @Nested
    @DisplayName("Techos: qué tabla se consulta según el eje")
    class Techos {

        @Test
        @DisplayName("USER/BRANCH se leen de company_capacities, no de company_usage_events")
        void capacidad_se_lee_de_company_capacities() {
            when(catalogQueryPort.listActiveModules(any())).thenReturn(List.of(grooming()));
            when(lineQueryPort.findCurrentLine(COMPANY_ID, CATALOG_ITEM_ID))
                    .thenReturn(Optional.of(new CompanyModuleLine("FREE_LIMITED", null)));
            when(catalogQueryPort.findCeilings(CATALOG_ITEM_ID)).thenReturn(
                    List.of(new ModuleCeilingDefinition("USER", "STOCK", 1, 80, "BLOCK")));
            when(usageQueryPort.findCapacity(COMPANY_ID, "USER"))
                    .thenReturn(Optional.of(new CapacitySnapshot(1, 1)));
            when(adminCheckPort.isCompanyAdmin(EMPLOYEE_ID, COMPANY_ID)).thenReturn(true);

            ModuleShowcaseDto dto = service
                    .execute(new ListModuleShowcaseQuery(COMPANY_ID, EMPLOYEE_ID)).get(0);

            assertThat(dto.ceilings().get(0).used()).isEqualTo(1);
            assertThat(dto.ceilings().get(0).limit()).isEqualTo(1);
        }
    }
}
