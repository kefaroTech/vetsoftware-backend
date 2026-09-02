package com.vetsoftware.app.entitlement.infrastructure.persistence;

import com.vetsoftware.app.subscription.infrastructure.persistence.SubscriptionItemJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Las lineas del contrato, ya cruzadas con el catalogo. Dos consultas para todo
 * el recalculo de una empresa: una de modulos y otra de capacidades.
 */
public interface ContractItemJpaRepository extends Repository<SubscriptionItemJpaEntity, Long> {

    /**
     * Todas las lineas de modulo del contrato, <strong>vigentes y
     * terminadas</strong>, proyectadas sobre los submodulos que abren.
     *
     * <p>
     * Las terminadas viajan a proposito: son las que producen la bajada a solo
     * lectura. Filtrarlas aqui por {@code effective_to IS NULL} --el error
     * clasico-- haria que dar de baja un modulo borrara el permiso en vez de
     * degradarlo, y con el la unica forma que tiene el cliente de consultar lo que
     * ya escribio.
     *
     * <p>
     * <strong>D-76: la composicion se lee CONGELADA, jamas la vigente</strong>
     * (R-CAT-03). Esta consulta cruzaba {@code catalog_item_sub_modules} y
     * {@code bundle_components} en vivo, y eso significaba que quitar un submodulo
     * del catalogo se lo quitaba a todas las clinicas que lo pagan: sin otrosi, sin
     * aviso, sin bajarles el precio y <em>ni siquiera en solo lectura</em>, porque
     * la degradacion solo alcanza a lo que esta consulta devuelve y eso ya no lo
     * devolvia. Con un agravante de reloj: ningun cambio de catalogo dispara
     * recalculo, asi que las cuarenta clinicas no lo perdian el dia del cambio sino
     * de una en una, meses despues, cada una el dia que le tocara recalculo por
     * cualquier otro motivo. Un incidente sin fecha y sin correlacion con su causa.
     *
     * <p>
     * Ahora se lee {@code subscription_item_sub_modules}, la foto que
     * {@code SubscriptionItemCompositionPort} escribe al firmar. <strong>Y con ella
     * desaparece el {@code UNION}</strong>: la expansion de los paquetes -un
     * {@code BUNDLE} ata submodulos a traves de sus componentes- ya se hizo al
     * congelar, asi que aqui no quedan dos ramas que mantener sincronizadas.
     *
     * <p>
     * <strong>Lo que sigue leyendose VIVO, y a proposito, es
     * {@code sub_modules}</strong>: {@code read_only_capable} y
     * {@code degradation_immune} son propiedades del submodulo y no de la venta, y
     * congelarlas seria lo contrario de lo que D-76 decide. Igual que
     * {@code structural_minimum} del articulo.
     *
     * <p>
     * {@code enabled = TRUE} va explicito en las cuatro tablas: una consulta nativa
     * no pasa por el {@code @SQLRestriction} de las entidades.
     *
     * <p>
     * <strong>El modo de cobro, el fin de prueba y el desenlace congelado viajan
     * con la linea</strong>, que es lo que permite que cada una venza por su cuenta
     * y que el recalculo escriba la fila sucesora (R-ENT-01, R-TRIAL-15). El
     * {@code JOIN} contra {@code company_trial_grants} es {@code LEFT} y no lleva
     * {@code enabled}: esa tabla no tiene esa columna a proposito --una prueba
     * concedida no se puede desconceder (R-TRIAL-22)--, y con un {@code INNER}
     * desapareceria del recalculo toda linea que nunca fue prueba, que son casi
     * todas. La union por {@code (company_id, catalog_item_id)} es exactamente la
     * clave unica de la concesion, asi que trae una fila como maximo.
     */
    @Query(value = """
            SELECT i.id AS subscriptionItemId, sm.id AS subModuleId, sm.code AS subModuleCode,
                   sm.name AS subModuleName, sm.read_only_capable AS readOnlyCapable,
                   i.effective_from AS effectiveFrom, i.effective_to AS effectiveTo,
                   ci.structural_minimum AS core, i.charge_mode AS chargeMode,
                   i.trial_end_date AS trialEndDate, g.policy_trial_outcome AS trialOutcome,
                   sm.degradation_immune AS degradationImmune
            FROM subscription_items i
            JOIN catalog_items ci ON ci.id = i.catalog_item_id AND ci.enabled = TRUE
            JOIN subscription_item_sub_modules sism ON sism.subscription_item_id = i.id
                 AND sism.company_id = i.company_id AND sism.enabled = TRUE
            JOIN sub_modules sm ON sm.id = sism.sub_module_id AND sm.enabled = TRUE
            LEFT JOIN company_trial_grants g ON g.company_id = i.company_id
                 AND g.catalog_item_id = i.catalog_item_id
            WHERE i.company_id = :companyId
              AND i.subscription_id = :subscriptionId
              AND i.enabled = TRUE
              AND i.item_type IN ('MODULE', 'BUNDLE', 'ONE_TIME')
            """, nativeQuery = true)
    List<ContractModuleLineView> findModuleLines(@Param("companyId") Long companyId,
            @Param("subscriptionId") Long subscriptionId);

    /**
     * Las lineas de capacidad del contrato, ya resueltas contra el catalogo de
     * ejes. El techo de cada una es {@code included_quantity + quantity}: lo
     * incluido viene congelado al firmar y lo comprado aparte es la cantidad,
     * porque el configurador ya resto lo incluido antes de fijarla (R15).
     *
     * <p>
     * {@code subscription_items.capacity_unit} guarda el <strong>codigo</strong>
     * del eje, y aqui se cruza con {@code limit_dimensions} para traer su id y su
     * tipo de medida, que son los dos datos que el contador copia.
     *
     * <p>
     * <strong>El {@code JOIN} es {@code LEFT} a proposito.</strong> Con un
     * {@code INNER}, un eje vendido que no estuviera sembrado en el catalogo haria
     * desaparecer la linea entera: la empresa nace sin ese contador, lo que se lee
     * como techo cero, y el dueño se queda sin poder invitar a nadie sin un solo
     * mensaje que lo explique. Trayendo los nulos, el fallo de siembra se denuncia
     * en {@code JpaSubscriptionQueryPort} nombrando el codigo que falta.
     *
     * <p>
     * <strong>{@code available_from} viaja con la linea</strong> porque es lo que
     * decide D-74: sin ella, «esta empresa no tiene fila de este eje» se lee
     * siempre como techo cero, tambien cuando el eje nacio despues de que el
     * cliente firmara.
     *
     * <p>
     * <strong>Y la granularidad se resuelve aqui, con su precedencia.</strong> Cada
     * cuanto vuelve a empezar un cupo de flujo es propiedad de la venta, no del
     * eje: manda el techo <em>congelado al firmar</em>
     * ({@code subscription_item_limits}) y, si esa fila no existe todavia, el techo
     * de fabrica del articulo ({@code catalog_item_limits}). Es la precedencia de
     * R-LIMIT-06 menos la excepcion negociada, que no declara granularidad. Las dos
     * uniones son {@code LEFT} por la misma razon que la de arriba: una linea de
     * flujo sin granularidad en ninguna de las dos tablas tiene que llegar arriba
     * para que se denuncie por su nombre, no desaparecer.
     */
    @Query(value = """
            SELECT i.id AS subscriptionItemId, i.capacity_unit AS capacityUnit,
                   ld.id AS limitDimensionId, ld.measure_kind AS measureKind,
                   ld.available_from AS availableFrom,
                   COALESCE(sil.reset_period, cil.reset_period) AS resetPeriod,
                   i.quantity AS quantity, i.included_quantity AS includedQuantity,
                   i.effective_from AS effectiveFrom, i.effective_to AS effectiveTo
            FROM subscription_items i
            LEFT JOIN limit_dimensions ld ON ld.code = i.capacity_unit AND ld.enabled = TRUE
            LEFT JOIN subscription_item_limits sil ON sil.subscription_item_id = i.id
                 AND sil.limit_dimension_id = ld.id AND sil.company_id = i.company_id
                 AND sil.enabled = TRUE
            LEFT JOIN catalog_item_limits cil ON cil.catalog_item_id = i.catalog_item_id
                 AND cil.limit_dimension_id = ld.id AND cil.enabled = TRUE
            WHERE i.company_id = :companyId
              AND i.subscription_id = :subscriptionId
              AND i.enabled = TRUE
              AND i.item_type = 'CAPACITY'
              AND i.capacity_unit IS NOT NULL
            """, nativeQuery = true)
    List<ContractCapacityLineView> findCapacityLines(@Param("companyId") Long companyId,
            @Param("subscriptionId") Long subscriptionId);
}
