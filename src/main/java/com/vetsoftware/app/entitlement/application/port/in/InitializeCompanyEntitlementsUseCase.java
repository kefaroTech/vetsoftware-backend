package com.vetsoftware.app.entitlement.application.port.in;

import com.vetsoftware.app.entitlement.application.command.InitializeCompanyEntitlementsCommand;
import com.vetsoftware.app.entitlement.application.dto.EntitlementRecalculationDto;
import com.vetsoftware.app.shared.security.NoAuthorizationRequired;

/**
 * Deriva los permisos de una empresa desde su contrato vigente, dentro de la
 * misma transaccion que lo escribe (R10: no existe una empresa sin contrato ni
 * sin permisos).
 *
 * <p>
 * <strong>Quien lo alcanza de verdad.</strong> El nombre dice "initialize" y se
 * queda corto: no hay dos llamadores, hay dos <em>familias</em>.
 * <ul>
 * <li>La orquestacion del alta de empresa
 * ({@code PlatformInitialContractProvisioningAdapter},
 * {@code PlatformCatalogSubscriptionCreator}), que es una ruta publica y
 * anonima.
 * <li><strong>Seis endpoints HTTP de {@code SubscriptionController}</strong>
 * --crear contrato, alta de linea, baja de linea, cambio de cantidad, cambio de
 * estado y cancelacion--, transitivamente: cada uno de sus casos de uso llama a
 * {@code SubscriptionChangedPort.subscriptionChanged(...)}, y su unico
 * implementador es {@code EntitlementRecalculationAdapter}, que invoca este
 * puerto. Es un salto por interfaz que un {@code grep} no ve.
 * </ul>
 *
 * <p>
 * <strong>Por que el gate no se pone aqui, aunque haya seis endpoints
 * detras.</strong> No es que sobre: es que <em>romperia</em> el camino del
 * tenant. Este recalculo corre <b>bajo el principal de quien disparo el cambio
 * de contrato</b>, que puede ser el administrador de la clinica modificando su
 * propio plan. Un {@code @PreAuthorize} que exigiera {@code SYSTEM}
 * --{@link RecalculateCompanyEntitlementsUseCase} lo exige-- lanzaria
 * {@code AccessDeniedException} <b>dentro de la transaccion y la revertiria
 * entera</b>: el cliente amplia su plan, recibe un 403 y no queda rastro de
 * nada. Es la regla transversal del bloque: <b>una escalada interna nunca puede
 * depender del principal de quien disparo la operacion</b>. Por eso son dos
 * puertos y no uno relajado; quien quiera forzar un recalculo <em>desde
 * fuera</em> usa el gateado.
 *
 * <p>
 * <strong>Por que sigue siendo seguro.</strong> Este caso de uso
 * <strong>deriva</strong>, no concede: todo lo que escribe sale del contrato
 * vigente de esa empresa, asi que invocarlo no otorga ni un permiso que el
 * contrato no sostenga, y devuelve contadores, no datos. Y el {@code companyId}
 * nunca viene de la peticion: en el camino del alta lo produce la propia
 * transaccion que crea la empresa, y en los seis endpoints sale del contrato
 * que se esta modificando, que el controller ya acoto con
 * {@code authz.currentCompanyId()}.
 *
 * <p>
 * <strong>Lo que hay que vigilar al anadir un llamador.</strong> Aqui no hay
 * {@code @PreAuthorize} que revalidar --esa es la excepcion--, asi que un
 * septimo llamador que reciba el {@code companyId} por otra via (un
 * {@code @PathVariable}, un campo del cuerpo) reescribiria
 * {@code company_entitlements} de la empresa que le digan: el recalculo borra
 * los derivados y reinserta. Quien anada un llamador tiene que garantizar que
 * su {@code companyId} sale del contrato o del principal, nunca del cliente.
 */
@NoAuthorizationRequired(reason = "Lo alcanzan DOS caminos, y ninguno admite gate. (1) La "
        + "orquestacion del alta de empresa, que es publica y anonima: en ese punto no hay "
        + "principal del que derivar empresa ni rol, y exigir autorizacion impediria que "
        + "ninguna empresa nueva entrara. (2) Los seis endpoints de mutacion de contrato de "
        + "SubscriptionController, transitivamente por SubscriptionChangedPort -> "
        + "EntitlementRecalculationAdapter; ahi el recalculo corre bajo el principal del "
        + "cliente, asi que un gate SYSTEM lanzaria AccessDeniedException dentro de la "
        + "transaccion y la revertiria entera: el tenant amplia su plan, recibe un 403 y no "
        + "queda rastro. Una escalada interna no puede depender del principal que disparo la "
        + "operacion. Sigue siendo seguro porque deriva en vez de conceder --todo lo que escribe "
        + "sale del contrato vigente de esa empresa-- y porque el companyId nunca viene de la "
        + "peticion: sale del contrato que se esta modificando o de la transaccion del alta.")
public interface InitializeCompanyEntitlementsUseCase {

    EntitlementRecalculationDto execute(InitializeCompanyEntitlementsCommand command);
}
