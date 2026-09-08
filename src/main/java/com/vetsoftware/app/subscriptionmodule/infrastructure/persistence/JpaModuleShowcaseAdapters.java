package com.vetsoftware.app.subscriptionmodule.infrastructure.persistence;

import com.vetsoftware.app.subscriptionmodule.application.port.out.CompanyModuleLineQueryPort;
import com.vetsoftware.app.subscriptionmodule.application.port.out.CompanyUsageSnapshotQueryPort;
import com.vetsoftware.app.subscriptionmodule.application.port.out.EmployeeAdminCheckPort;
import com.vetsoftware.app.subscriptionmodule.application.port.out.ModuleCatalogQueryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Los cuatro adaptadores de lectura del escaparate, todos por SQL nativo contra
 * el esquema de otras rodajas ({@code catalog_items}, {@code catalog_prices},
 * {@code catalog_item_limits}, {@code subscriptions},
 * {@code subscription_items}, {@code company_usage_events}, {@code services},
 * {@code company_capacities}, {@code employee_roles}, {@code roles}). Mismo
 * patrón que {@code quote.infrastructure.persistence.JpaCatalogQueryPorts}: el
 * contrato es el esquema, no las clases Java de la feature vecina, y ninguna de
 * estas consultas escribe.
 */
public final class JpaModuleShowcaseAdapters {

    private static final DateTimeFormatter MONTH_KEY = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String CUMULATIVE_PERIOD_KEY = "ALLTIME";

    private JpaModuleShowcaseAdapters() {
    }

    private static BigDecimal amount(Object value) {
        return value == null ? null : new BigDecimal(String.valueOf(value));
    }

    private static int count(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    @Component
    public static class JpaModuleCatalogQueryPort implements ModuleCatalogQueryPort {

        private static final String SQL_ACTIVE_MODULES = """
                SELECT ci.id, ci.code, ci.name, ci.short_description, ci.trial_eligibility,
                       ci.self_service,
                       (SELECT p.unit_amount FROM catalog_prices p
                         WHERE p.catalog_item_id = ci.id AND p.price_list_id = :priceListId
                           AND p.billing_cycle = 'MONTHLY' AND p.tier_min = 1
                           AND p.enabled = TRUE LIMIT 1) AS monthly_price,
                       (SELECT p.unit_amount FROM catalog_prices p
                         WHERE p.catalog_item_id = ci.id AND p.price_list_id = :priceListId
                           AND p.billing_cycle = 'ANNUAL' AND p.tier_min = 1
                           AND p.enabled = TRUE LIMIT 1) AS annual_price
                  FROM catalog_items ci
                 WHERE ci.item_type = 'MODULE'
                   AND ci.status = 'ACTIVE'
                   AND ci.enabled = TRUE
                 ORDER BY ci.sort_order, ci.code
                """;

        private static final String SQL_CURRENT_PRICE_LIST = """
                SELECT id
                  FROM price_lists
                 WHERE status = 'PUBLISHED'
                   AND enabled = TRUE
                   AND valid_from <= :today
                   AND (valid_to IS NULL OR valid_to >= :today)
                 ORDER BY valid_from DESC, id DESC
                """;

        private static final String SQL_CEILINGS = """
                SELECT ld.code, ld.measure_kind, cil.limit_quantity, cil.warn_threshold,
                       cil.enforcement
                  FROM catalog_item_limits cil
                  JOIN limit_dimensions ld ON ld.id = cil.limit_dimension_id
                 WHERE cil.catalog_item_id = :catalogItemId
                   AND cil.enabled = TRUE
                 ORDER BY ld.code
                """;

        private final EntityManager entityManager;

        public JpaModuleCatalogQueryPort(EntityManager entityManager) {
            this.entityManager = entityManager;
        }

        @Override
        public List<ModuleCatalogEntry> listActiveModules(LocalDate today) {
            Long priceListId = currentPriceListId(today);
            Query query = entityManager.createNativeQuery(SQL_ACTIVE_MODULES)
                    .setParameter("priceListId", priceListId);
            List<ModuleCatalogEntry> modules = new ArrayList<>();
            for (Object row : query.getResultList()) {
                Object[] columns = (Object[]) row;
                modules.add(new ModuleCatalogEntry(((Number) columns[0]).longValue(),
                        String.valueOf(columns[1]), String.valueOf(columns[2]),
                        columns[3] == null ? null : String.valueOf(columns[3]),
                        String.valueOf(columns[4]),
                        Boolean.TRUE.equals(columns[5]) || Integer.valueOf(1).equals(columns[5]),
                        amount(columns[6]), amount(columns[7])));
            }
            return List.copyOf(modules);
        }

        private Long currentPriceListId(LocalDate today) {
            List<?> rows = entityManager.createNativeQuery(SQL_CURRENT_PRICE_LIST)
                    .setParameter("today", today).setMaxResults(1).getResultList();
            return rows.isEmpty() ? null : ((Number) rows.get(0)).longValue();
        }

        @Override
        public List<ModuleCeilingDefinition> findCeilings(Long catalogItemId) {
            Query query = entityManager.createNativeQuery(SQL_CEILINGS)
                    .setParameter("catalogItemId", catalogItemId);
            List<ModuleCeilingDefinition> ceilings = new ArrayList<>();
            for (Object row : query.getResultList()) {
                Object[] columns = (Object[]) row;
                ceilings.add(new ModuleCeilingDefinition(String.valueOf(columns[0]),
                        String.valueOf(columns[1]), count(columns[2]), count(columns[3]),
                        String.valueOf(columns[4])));
            }
            return List.copyOf(ceilings);
        }
    }

    @Component
    public static class JpaCompanyModuleLineQueryPort implements CompanyModuleLineQueryPort {

        private static final String SQL_CURRENT_LINE = """
                SELECT i.charge_mode, i.trial_end_date
                  FROM subscription_items i
                  JOIN subscriptions s ON s.id = i.subscription_id
                 WHERE i.company_id = :companyId
                   AND i.catalog_item_id = :catalogItemId
                   AND i.effective_to IS NULL
                   AND i.enabled = TRUE
                   AND s.enabled = TRUE
                 LIMIT 1
                """;

        private final EntityManager entityManager;

        public JpaCompanyModuleLineQueryPort(EntityManager entityManager) {
            this.entityManager = entityManager;
        }

        @Override
        public Optional<CompanyModuleLine> findCurrentLine(Long companyId, Long catalogItemId) {
            // SQL_CURRENT_LINE ya lleva LIMIT 1: setMaxResults(1) aqui duplicaba la
            // clausula y MySQL rechazaba el "LIMIT 1 limit ?" resultante.
            List<?> rows = entityManager.createNativeQuery(SQL_CURRENT_LINE)
                    .setParameter("companyId", companyId)
                    .setParameter("catalogItemId", catalogItemId).getResultList();
            if (rows.isEmpty())
                return Optional.empty();
            Object[] columns = (Object[]) rows.get(0);
            LocalDate trialEndDate = columns[1] == null
                    ? null
                    : ((java.sql.Date) columns[1]).toLocalDate();
            return Optional.of(new CompanyModuleLine(String.valueOf(columns[0]), trialEndDate));
        }
    }

    @Component
    public static class JpaCompanyUsageSnapshotQueryPort implements CompanyUsageSnapshotQueryPort {

        private static final String SQL_COUNT_EVENTS = """
                SELECT COUNT(*) FROM company_usage_events
                 WHERE company_id = :companyId
                   AND limit_dimension_code = :dimensionCode
                   AND period_key = :periodKey
                """;

        private static final String SQL_COUNT_SERVICES = """
                SELECT COUNT(*) FROM services WHERE company_id = :companyId AND enabled = TRUE
                """;

        private static final String SQL_CAPACITY = """
                SELECT cc.limit_quantity, cc.used_quantity
                  FROM company_capacities cc
                  JOIN limit_dimensions ld ON ld.id = cc.limit_dimension_id
                 WHERE cc.company_id = :companyId
                   AND ld.code = :dimensionCode
                 ORDER BY cc.id DESC
                 LIMIT 1
                """;

        private final EntityManager entityManager;
        private final Clock clock;

        public JpaCompanyUsageSnapshotQueryPort(EntityManager entityManager, Clock clock) {
            this.entityManager = entityManager;
            this.clock = clock;
        }

        @Override
        public int countCumulative(Long companyId, String dimensionCode) {
            return countEvents(companyId, dimensionCode, CUMULATIVE_PERIOD_KEY);
        }

        @Override
        public int countCurrentMonth(Long companyId, String dimensionCode) {
            return countEvents(companyId, dimensionCode, YearMonth.now(clock).format(MONTH_KEY));
        }

        private int countEvents(Long companyId, String dimensionCode, String periodKey) {
            Object result = entityManager.createNativeQuery(SQL_COUNT_EVENTS)
                    .setParameter("companyId", companyId)
                    .setParameter("dimensionCode", dimensionCode)
                    .setParameter("periodKey", periodKey).getSingleResult();
            return count(result);
        }

        @Override
        public int countActiveServices(Long companyId) {
            Object result = entityManager.createNativeQuery(SQL_COUNT_SERVICES)
                    .setParameter("companyId", companyId).getSingleResult();
            return count(result);
        }

        @Override
        public Optional<CapacitySnapshot> findCapacity(Long companyId, String dimensionCode) {
            // SQL_CAPACITY ya lleva su propio LIMIT 1: ver la nota en findCurrentLine.
            List<?> rows = entityManager.createNativeQuery(SQL_CAPACITY)
                    .setParameter("companyId", companyId)
                    .setParameter("dimensionCode", dimensionCode).getResultList();
            if (rows.isEmpty())
                return Optional.empty();
            Object[] columns = (Object[]) rows.get(0);
            return Optional.of(new CapacitySnapshot(count(columns[0]), count(columns[1])));
        }
    }

    @Component
    public static class JpaEmployeeAdminCheckPort implements EmployeeAdminCheckPort {

        private static final String SQL_IS_ADMIN = """
                SELECT COUNT(*)
                  FROM employee_roles er
                  JOIN roles r ON r.id = er.role_id
                 WHERE er.employee_id = :employeeId
                   AND r.company_id = :companyId
                   AND r.code = 'ADMIN'
                   AND er.enabled = TRUE
                   AND r.enabled = TRUE
                """;

        private final EntityManager entityManager;

        public JpaEmployeeAdminCheckPort(EntityManager entityManager) {
            this.entityManager = entityManager;
        }

        @Override
        public boolean isCompanyAdmin(Long employeeId, Long companyId) {
            if (employeeId == null || companyId == null)
                return false;
            Object result = entityManager.createNativeQuery(SQL_IS_ADMIN)
                    .setParameter("employeeId", employeeId).setParameter("companyId", companyId)
                    .getSingleResult();
            return count(result) > 0;
        }
    }
}
