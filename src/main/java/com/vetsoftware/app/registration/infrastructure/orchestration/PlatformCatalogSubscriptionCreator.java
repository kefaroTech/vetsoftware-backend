package com.vetsoftware.app.registration.infrastructure.orchestration;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.entitlement.application.command.InitializeCompanyEntitlementsCommand;
import com.vetsoftware.app.entitlement.application.port.in.InitializeCompanyEntitlementsUseCase;
import com.vetsoftware.app.registration.application.port.out.InitialSubscriptionCreator;
import com.vetsoftware.app.registration.domain.PlatformCatalogNotConfiguredException;
import com.vetsoftware.app.subscription.application.command.CreateInitialSubscriptionCommand;
import com.vetsoftware.app.subscription.application.port.in.CreateInitialSubscriptionUseCase;
import com.vetsoftware.app.subscription.domain.PlatformCatalogNotConfiguredForSubscriptionException;
import org.springframework.stereotype.Component;

/**
 * Crea el contrato con el que nace una empresa <b>y deriva sus permisos</b>,
 * dentro de la misma transaccion del alta.
 *
 * <p>
 * <b>La regla es dura: toda empresa nace con un contrato.</b> Una empresa sin
 * contrato no tiene {@code company_entitlements}, entra al sistema y no puede
 * hacer nada, sin ningun mensaje que lo explique; se investiga como un problema
 * de permisos del usuario y hay que borrarla a mano de la base. Por eso este
 * adaptador solo tiene dos desenlaces, y ninguno es «empresa a medias»: o queda
 * contrato con sus permisos derivados, o revierte el alta entera.
 *
 * <p>
 * <b>Los dos pasos, en este orden y sin nada en medio.</b>
 *
 * <ol>
 * <li>{@code CreateInitialSubscriptionUseCase} resuelve el minimo estructural
 * de la plataforma —articulo {@code CORE} activo, su enlace a un submodulo, la
 * lista publicada por defecto, el precio de ese articulo para el ciclo, y la
 * fila de {@code platform_billing_config}—, <b>congela</b> precio, IVA y
 * cantidad incluida en una {@code subscription_items} de
 * {@code origin = 'INITIAL'}, y crea la {@code subscriptions}.
 * <li>{@code InitializeCompanyEntitlementsUseCase} deriva
 * {@code company_entitlements} de ese contrato recien creado.
 * </ol>
 *
 * <p>
 * <b>El orden no es preferencia.</b> Lo que viene despues en el alta —el
 * reparto de roles base— filtra los permisos por los submodulos concedidos a la
 * empresa, asi que si los entitlements todavia no existen cuando corre, lanza.
 * El fallo seria ruidoso, pero seria un fallo.
 *
 * <p>
 * <b>Y no envolver el paso 2 en nada que cambie su propagacion.</b>
 * {@code InitializeCompanyEntitlementsService} es {@code @Transactional} con
 * {@code REQUIRED} a proposito, para unirse a la transaccion del alta: con
 * {@code REQUIRES_NEW} quedaria una empresa creada y sin permisos, que es
 * exactamente el estado que esta regla existe para impedir.
 *
 * <p>
 * <b>El paso 2 SI va bajo {@link SystemAuthRunner}, y antes no hacia falta.</b>
 * Durante mucho tiempo basto con dejarlo desnudo: el puerto es interno, lleva
 * {@code @NoAuthorizationRequired} y no necesitaba principal. Esa premisa era
 * cierta mientras el recalculo solo tocaba puertos de salida, y dejo de serlo
 * en cuanto {@code CompanyEntitlementRecalculator} empezo a escribir la foto
 * del recalculo por {@code RecordEntitlementSnapshotUseCase}, que es un puerto
 * de entrada gateado: exige rol {@code SYSTEM} o que la empresa del command sea
 * la del principal. En el alta publica no hay ninguna de las dos cosas —no hay
 * token, luego no hay SYSTEM y no hay empresa propia—, asi que el alta entera
 * moria en un <b>403</b> que ni siquiera mencionaba a las suscripciones: el
 * usuario pedia registrarse y le contestaban «Acceso denegado». Un gate anadido
 * tres slices mas abajo invalido en silencio la premisa escrita aqui arriba; el
 * envoltorio es lo que la vuelve a hacer cierta, y ademas es lo que ya hacen
 * los otros ocho adaptadores del alta.
 *
 * <p>
 * Se envuelve <b>aqui</b> y no se abre el gate del snapshot: ese gate es
 * correcto —admite a plataforma y al propio tenant, que es quien puede pedir el
 * recalculo de reparacion— y relajarlo por comodidad del alta abriria la foto
 * de permisos de cualquier empresa a un anonimo. {@link SystemAuthRunner} solo
 * cambia el {@code SecurityContext} y lo restaura en un {@code finally}: no
 * toca la propagacion transaccional, asi que el parrafo de arriba sigue en pie.
 *
 * <p>
 * <b>Por que el paso 1 si va bajo {@link SystemAuthRunner}.</b> El alta es un
 * flujo publico sin token: en ese instante no hay principal, y su puerto exige
 * {@code hasRole('SYSTEM')}. Es el mismo cableado que usan
 * {@link CreateCompanyAdapter} y {@link CreateBranchAdapter}, y acuñar el
 * primer contrato de una empresa es un acto de plataforma, porque el inquilino
 * todavia no existe.
 *
 * <p>
 * <b>Si falta catalogo, sigue fallando entero.</b> Se traduce la señal del
 * slice {@code subscription} —que no conoce a esta empresa por su nombre— al
 * mensaje enumerado de {@link PlatformCatalogNotConfiguredException}, que si
 * dice cual de las cinco piezas hay que sembrar. No se degrada a un contrato
 * vacio: una fila en {@code subscriptions} que no corresponde a ningun articulo
 * comprado corrompe el dato del que cuelga toda la facturacion. Razonado en el
 * issue <b>#364</b>.
 */
@Component
public class PlatformCatalogSubscriptionCreator implements InitialSubscriptionCreator {

    private final CreateInitialSubscriptionUseCase createInitialSubscriptionUseCase;
    private final InitializeCompanyEntitlementsUseCase initializeCompanyEntitlementsUseCase;
    private final SystemAuthRunner systemAuthRunner;

    public PlatformCatalogSubscriptionCreator(
            CreateInitialSubscriptionUseCase createInitialSubscriptionUseCase,
            InitializeCompanyEntitlementsUseCase initializeCompanyEntitlementsUseCase,
            SystemAuthRunner systemAuthRunner) {
        this.createInitialSubscriptionUseCase = createInitialSubscriptionUseCase;
        this.initializeCompanyEntitlementsUseCase = initializeCompanyEntitlementsUseCase;
        this.systemAuthRunner = systemAuthRunner;
    }

    @Override
    public void createInitialContract(Long companyId, String companyName) {
        try {
            systemAuthRunner.run(() -> createInitialSubscriptionUseCase
                    .execute(new CreateInitialSubscriptionCommand(companyId, null, null)));
        } catch (PlatformCatalogNotConfiguredForSubscriptionException exception) {
            throw new PlatformCatalogNotConfiguredException(companyName);
        }

        // El contrato ya existe: derivar sus permisos es lo unico que falta para que la
        // empresa pueda hacer algo. Se descarta lo que devuelve —son contadores— para
        // no importar un DTO de aplicacion de otra feature.
        //
        // Bajo SystemAuthRunner porque el recalculo escribe su foto por un puerto
        // gateado (RecordEntitlementSnapshotUseCase) y aqui todavia no hay principal:
        // ver el javadoc de la clase. Sin esto, el alta publica devuelve 403.
        systemAuthRunner.run(() -> initializeCompanyEntitlementsUseCase
                .execute(new InitializeCompanyEntitlementsCommand(companyId)));
    }
}
