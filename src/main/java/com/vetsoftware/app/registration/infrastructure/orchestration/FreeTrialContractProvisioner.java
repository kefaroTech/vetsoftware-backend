package com.vetsoftware.app.registration.infrastructure.orchestration;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.companytrialgrant.application.command.GrantTrialCommand;
import com.vetsoftware.app.companytrialgrant.application.port.in.GrantTrialUseCase;
import com.vetsoftware.app.companytrialgrant.domain.TrialPolicyOutcome;
import com.vetsoftware.app.companytrialwindow.application.command.OpenTrialWindowCommand;
import com.vetsoftware.app.companytrialwindow.application.dto.CompanyTrialWindowDto;
import com.vetsoftware.app.companytrialwindow.application.port.in.OpenTrialWindowUseCase;
import com.vetsoftware.app.entitlement.application.command.InitializeCompanyEntitlementsCommand;
import com.vetsoftware.app.entitlement.application.port.in.InitializeCompanyEntitlementsUseCase;
import com.vetsoftware.app.registration.application.port.out.EligibleTrialCatalogItemsPort;
import com.vetsoftware.app.registration.application.port.out.EligibleTrialCatalogItemsPort.EligibleTrialCatalogItem;
import com.vetsoftware.app.registration.domain.PlatformCatalogNotConfiguredException;
import com.vetsoftware.app.subscription.application.command.CreateInitialSubscriptionCommand;
import com.vetsoftware.app.subscription.application.port.in.CreateInitialSubscriptionUseCase;
import com.vetsoftware.app.subscription.domain.BillingCycle;
import com.vetsoftware.app.subscription.domain.PlatformCatalogNotConfiguredForSubscriptionException;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Toda empresa nace gratis y con su propia ventana de prueba, sin importar por
 * qué puerta entró: el alta pública
 * ({@code PlatformCatalogSubscriptionCreator}) y el alta desde la consola de
 * plataforma
 * ({@code company.infrastructure.orchestration.PlatformInitialContractProvisioningAdapter})
 * llaman a este mismo componente, así que las dos rutas quedan exactamente
 * iguales por construcción y no por disciplina de mantener dos copias
 * sincronizadas.
 *
 * <p>
 * <b>Los cuatro pasos, en este orden y sin nada en medio.</b>
 *
 * <ol>
 * <li>{@code OpenTrialWindowUseCase} abre el reloj de la empresa,
 * {@code origin = 'SIGNUP'} y sin cotización: ninguna de las dos rutas negocia
 * nada.
 * <li>{@code GrantTrialUseCase} concede, por cada artículo
 * {@code trial_eligibility = 'ELIGIBLE'} del catálogo, una
 * {@code company_trial_grants} con el mismo {@code origin = 'SIGNUP'}. Va
 * <b>antes</b> del paso 3 a propósito:
 * {@code fk_subscription_items_trial_grant} exige que la concesión ya exista
 * cuando se firme la línea que la referencia.
 * <li>{@code CreateInitialSubscriptionUseCase} firma una línea
 * {@code charge_mode = 'TRIAL'} por cada uno de esos mismos artículos —no solo
 * {@code CORE} y las capacidades del mínimo estructural—, con
 * {@code trial_end_date} igual al fin de LA ventana abierta en el paso 1, y
 * crea la {@code subscriptions} en {@code TRIALING}.
 * <li>{@code InitializeCompanyEntitlementsUseCase} deriva
 * {@code company_entitlements} de ese contrato recién creado.
 * </ol>
 *
 * <p>
 * <b>El orden no es preferencia.</b> Lo que viene después en el alta pública
 * —el reparto de roles base— filtra los permisos por los submódulos concedidos
 * a la empresa, así que si los entitlements todavía no existen cuando corre,
 * lanza. El fallo sería ruidoso, pero sería un fallo.
 *
 * <p>
 * <b>Y no envolver el último paso en nada que cambie su propagación.</b>
 * {@code InitializeCompanyEntitlementsService} es {@code @Transactional} con
 * {@code REQUIRED} a propósito, para unirse a la transacción del alta: con
 * {@code REQUIRES_NEW} quedaría una empresa creada y sin permisos, que es
 * exactamente el estado que esta regla existe para impedir.
 *
 * <p>
 * <b>Los cuatro puertos van bajo {@link SystemAuthRunner}.</b> Los dos
 * llamantes de este componente son flujos sin token en ese instante —el alta
 * pública nunca tuvo uno, y el alta por consola actúa sobre una empresa que
 * todavía no tiene principal propio—, y los cuatro puertos exigen
 * {@code hasRole('SYSTEM')}: abrir ventana, conceder pruebas y firmar el
 * contrato son decisiones comerciales de plataforma, y derivar entitlements
 * escribe su foto por un puerto de entrada gateado
 * ({@code RecordEntitlementSnapshotUseCase}). Sin el envoltorio, cualquiera de
 * los cuatro pasos devuelve <b>403</b> sin mencionar el motivo real.
 *
 * <p>
 * <b>Si falta catálogo, sigue fallando entero.</b> Se traduce la señal del
 * slice {@code subscription} —que no conoce a esta empresa por su nombre— al
 * mensaje enumerado de {@link PlatformCatalogNotConfiguredException}, que sí
 * dice cuál de las piezas hay que sembrar. No se degrada a un contrato vacío:
 * una fila en {@code subscriptions} que no corresponde a ningún artículo
 * comprado corrompe el dato del que cuelga toda la facturación. Razonado en el
 * issue <b>#364</b>.
 */
@Component
public class FreeTrialContractProvisioner {

    /** Treinta días para todo artículo {@code ELIGIBLE}, sin excepción. */
    private static final int TRIAL_WINDOW_DAYS = 30;

    private final OpenTrialWindowUseCase openTrialWindowUseCase;
    private final GrantTrialUseCase grantTrialUseCase;
    private final EligibleTrialCatalogItemsPort eligibleTrialCatalogItemsPort;
    private final CreateInitialSubscriptionUseCase createInitialSubscriptionUseCase;
    private final InitializeCompanyEntitlementsUseCase initializeCompanyEntitlementsUseCase;
    private final SystemAuthRunner systemAuthRunner;
    private final Clock clock;

    public FreeTrialContractProvisioner(OpenTrialWindowUseCase openTrialWindowUseCase,
            GrantTrialUseCase grantTrialUseCase,
            EligibleTrialCatalogItemsPort eligibleTrialCatalogItemsPort,
            CreateInitialSubscriptionUseCase createInitialSubscriptionUseCase,
            InitializeCompanyEntitlementsUseCase initializeCompanyEntitlementsUseCase,
            SystemAuthRunner systemAuthRunner, Clock clock) {
        this.openTrialWindowUseCase = openTrialWindowUseCase;
        this.grantTrialUseCase = grantTrialUseCase;
        this.eligibleTrialCatalogItemsPort = eligibleTrialCatalogItemsPort;
        this.createInitialSubscriptionUseCase = createInitialSubscriptionUseCase;
        this.initializeCompanyEntitlementsUseCase = initializeCompanyEntitlementsUseCase;
        this.systemAuthRunner = systemAuthRunner;
        this.clock = clock;
    }

    /**
     * @param companyId
     *            la empresa recién creada, dentro de la misma transacción del alta
     *            que la disparó
     * @param companyName
     *            solo para que, si falta catálogo, el mensaje de
     *            {@link PlatformCatalogNotConfiguredException} sea legible por un
     *            humano
     */
    public void provision(Long companyId, String companyName) {
        LocalDate today = LocalDate.now(clock);
        CompanyTrialWindowDto window = systemAuthRunner.call(() -> openTrialWindowUseCase
                .execute(new OpenTrialWindowCommand(companyId, today, TRIAL_WINDOW_DAYS, null)));

        // daysGranted = policyTrialDays: ninguna de las dos rutas de alta negocia una
        // campana que baje de la politica del catalogo.
        for (EligibleTrialCatalogItem item : eligibleTrialCatalogItemsPort
                .findAll(BillingCycle.MONTHLY)) {
            systemAuthRunner.run(() -> grantTrialUseCase.execute(new GrantTrialCommand(companyId,
                    item.catalogItemId(), today, item.defaultTrialDays(), item.defaultTrialDays(),
                    TrialPolicyOutcome.valueOf(item.trialOutcome()), null, null)));
        }

        try {
            systemAuthRunner.run(() -> createInitialSubscriptionUseCase
                    .execute(new CreateInitialSubscriptionCommand(companyId, null, today,
                            window.endDate())));
        } catch (PlatformCatalogNotConfiguredForSubscriptionException exception) {
            throw new PlatformCatalogNotConfiguredException(companyName);
        }

        // Se descarta lo que devuelve -son contadores- para no importar un DTO de
        // aplicacion de otra feature.
        systemAuthRunner.run(() -> initializeCompanyEntitlementsUseCase
                .execute(new InitializeCompanyEntitlementsCommand(companyId)));
    }
}
