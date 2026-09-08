package com.vetsoftware.app.subscription.infrastructure.scheduling;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.companytrialgrant.application.dto.CompanyTrialGrantDto;
import com.vetsoftware.app.companytrialgrant.application.port.in.ListExpiredTrialGrantsUseCase;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobCatalog;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry.Outcome;
import com.vetsoftware.app.subscription.application.usecase.TrialLineExpirationWorker;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Vence cada línea {@code TRIAL} por su propia fecha, nunca por
 * {@code subscriptions.status} (R-TRIAL-13/15).
 *
 * <p>
 * Con candado distribuido, a diferencia de {@code SubscriptionLifecycleJob}:
 * mueve el modo de cobro y consume la concesión, y dos réplicas duplicarían el
 * cierre de una concesión la misma noche. Corre a las 03:15, después del
 * lifecycle (03:10), que mueve {@code TRIALING} a {@code ACTIVE} leyendo la
 * misma fecha: así este job no toca el estado del contrato.
 */
@Component
public class TrialLineExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(TrialLineExpirationJob.class);
    private static final ScheduledJobCatalog JOB = ScheduledJobCatalog.SUBSCRIPTION_TRIAL_EXPIRATION;

    private final ListExpiredTrialGrantsUseCase listExpiredTrialGrants;
    private final TrialLineExpirationWorker worker;
    private final SystemAuthRunner systemAuthRunner;
    private final ScheduledJobTelemetry telemetry;
    private final Clock clock;

    public TrialLineExpirationJob(ListExpiredTrialGrantsUseCase listExpiredTrialGrants,
            TrialLineExpirationWorker worker, SystemAuthRunner systemAuthRunner,
            ScheduledJobTelemetry telemetry, Clock clock) {
        this.listExpiredTrialGrants = listExpiredTrialGrants;
        this.worker = worker;
        this.systemAuthRunner = systemAuthRunner;
        this.telemetry = telemetry;
        this.clock = clock;
    }

    @Scheduled(cron = "${subscription.trial.expiration.cron:0 15 3 * * *}", zone = ScheduledJobCatalog.ZONE)
    @SchedulerLock(name = "subscription.trial.expiration", lockAtMostFor = "PT20M", lockAtLeastFor = "PT1M")
    public void runTrialExpiration() {
        telemetry.observe(JOB, this::executeExpiration);
    }

    /**
     * Una empresa por intento: el fallo de una no revierte ni bloquea el
     * vencimiento de las demás.
     */
    private Outcome executeExpiration() {
        LocalDate today = LocalDate.now(clock);
        List<CompanyTrialGrantDto> expired = systemAuthRunner
                .call(() -> listExpiredTrialGrants.listLiveExpiredOn(today));
        if (expired.isEmpty())
            return Outcome.NO_WORK;

        Map<Long, List<CompanyTrialGrantDto>> byCompany = expired.stream()
                .collect(Collectors.groupingBy(CompanyTrialGrantDto::companyId));
        int companies = 0;
        int lines = 0;
        int failures = 0;
        for (Map.Entry<Long, List<CompanyTrialGrantDto>> entry : byCompany.entrySet()) {
            Long companyId = entry.getKey();
            List<CompanyTrialGrantDto> grants = entry.getValue();
            try {
                lines += systemAuthRunner.call(() -> worker.processCompany(companyId, grants));
                companies++;
            } catch (RuntimeException exception) {
                failures++;
                log.error("Vencimiento de prueba fallido para la empresa {}: {}", companyId,
                        exception.getMessage());
            }
        }
        log.info("Vencimiento de pruebas finalizado: {} empresa(s) procesada(s),"
                + " {} linea(s) sucedida(s), {} fallo(s)", companies, lines, failures);
        return Outcome.from(companies + failures, failures);
    }
}
