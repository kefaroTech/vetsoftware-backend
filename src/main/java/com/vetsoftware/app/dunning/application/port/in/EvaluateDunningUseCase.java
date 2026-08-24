package com.vetsoftware.app.dunning.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Reevaluacion de la mora de un contrato a partir de una factura suya.
 *
 * <p>
 * <b>Es el unico punto del motor que decide bajar un contrato a
 * {@code READ_ONLY}, y hasta ahora no tenia gate.</b> Llevaba
 * {@code @NoAuthorizationRequired(reason = "Orquestacion interna posterior a
 * pagos y barrido SYSTEM")}, que es exactamente el argumento que el javadoc de
 * {@link ProcessDunningBatchUseCase} declara insuficiente: describe <em>quien
 * llama hoy</em>, no una propiedad del codigo. Y aqui pesa mas que en aquel,
 * porque este puerto <b>recibe un {@code companyId} y no lo contrastaba con el
 * principal</b>: la regla dura que obliga a hacerlo
 * ({@code PUERTO_CON_COMPANYID_VALIDA_EL_TENANT}) exime a los puertos anotados
 * {@code @NoAuthorizationRequired}, asi que la exencion escrita era tambien lo
 * que apagaba la vigilancia.
 *
 * <p>
 * <b>El escenario concreto.</b> Rio abajo, {@code JpaDunningSubscriptionPort}
 * escala a {@code ROLE_SYSTEM} con {@code SystemAuthRunner} para poder mover el
 * estado del contrato (#412). Esa escalada es correcta y necesaria —sin ella un
 * pago de tenant se revierte con 403 dentro de su propia transaccion—, pero
 * convierte a este puerto en un amplificador: el dia que alguien lo inyecte en
 * un controller, cualquier autenticado podria pasar el {@code companyId} de
 * otra clinica y el motor, ya escalado a SYSTEM, le moveria el contrato sin
 * rechistar.
 *
 * <p>
 * <b>Por que esta expresion y no {@code hasRole('SYSTEM')} a secas.</b> Los dos
 * llamadores reales pasan: el barrido nocturno entra bajo {@code ROLE_SYSTEM},
 * y la reevaluacion posterior a un pago siempre se pregunta por la empresa del
 * documento que se acaba de saldar —la del propio pagador—, que es justo lo que
 * {@code @authz.isMyCompany} acepta. Cerrarlo solo a SYSTEM funcionaria hoy y
 * reventaria el dia que el registro de pagos se abra a un principal de tenant,
 * que es el camino que el modelo ya contempla.
 */
public interface EvaluateDunningUseCase {

    @PreAuthorize("hasRole('SYSTEM') or @authz.isMyCompany(#companyId)")
    void evaluate(Long billingDocumentId, Long companyId);
}
