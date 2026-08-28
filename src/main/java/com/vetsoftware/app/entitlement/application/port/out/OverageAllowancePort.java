package com.vetsoftware.app.entitlement.application.port.out;

import com.vetsoftware.app.entitlement.domain.OverageAllowance;
import java.time.LocalDate;
import java.util.Optional;

/**
 * <strong>Si esta empresa puede pasarse de ese cupo, y a que precio.</strong>
 *
 * <p>
 * El modo de excedente vive en {@code subscription_item_limits.enforcement}
 * desde el changeset 304 y hasta ahora <b>no lo leia nadie</b> en el camino del
 * contador: {@code AdjustCompanyCapacityUsageService} bloqueaba
 * incondicionalmente al pasar del techo, tambien a la clinica que habia
 * comprado el derecho a pasarse. Este puerto es lo unico que faltaba para que
 * esa decision se pueda tomar.
 *
 * <p>
 * <strong>Se consulta solo cuando el contador ya iba a negar</strong>, igual
 * que {@code SubscriptionQueryPort.findContractSignedOnByCompanyId} en la rama
 * de D-74: el camino feliz —el consumo que cabe bajo el techo— no paga esta
 * consulta.
 *
 * <p>
 * <strong>Acotado por empresa, siempre.</strong> El {@code companyId} no es
 * decorativo: {@code subscription_item_limits} lleva la empresa en sus claves
 * foraneas compuestas justo para que el techo de una clinica no cuelgue de la
 * linea de contrato de otra, y una consulta que solo mirara el eje devolveria
 * el permiso del vecino.
 */
public interface OverageAllowancePort {

    /**
     * El permiso de excedente vigente de una empresa sobre un eje, si lo hay.
     *
     * <p>
     * <strong>El dia entra por parametro y no lo pone la consulta.</strong> Una
     * linea de contrato tiene vigencia, asi que «¿puedo pasarme?» depende de cuando
     * se pregunte; resolverlo con {@code CURRENT_DATE} dentro del SQL pondria el
     * reloj del servidor de base de datos en una decision de dinero y dejaria el
     * caso de uso sin forma de probarlo con un {@code Clock} fijo
     * ({@code RELOJ_INYECTADO_EN_VEZ_DE_NOW}).
     *
     * @param on
     *            el dia para el que se pregunta, del reloj inyectado del caso de
     *            uso
     * @return vacio si la linea no declara {@code OVERAGE}, si esta deshabilitada,
     *         si su vigencia no cubre {@code on}, o si la empresa no tiene linea
     *         para ese eje. <b>Vacio significa bloquear</b>, que es el
     *         comportamiento de siempre
     */
    Optional<OverageAllowance> findAllowance(Long companyId, Long limitDimensionId, LocalDate on);
}
