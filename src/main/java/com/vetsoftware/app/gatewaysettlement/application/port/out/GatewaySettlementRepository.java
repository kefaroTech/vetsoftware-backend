package com.vetsoftware.app.gatewaysettlement.application.port.out;

import com.vetsoftware.app.gatewaysettlement.domain.GatewaySettlement;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.Optional;

/**
 * <strong>No hay variante acotada por empresa de nada, y no falta
 * ninguna.</strong> La tabla no tiene {@code company_id}: no existe la consulta
 * acotada que {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA} echaria de menos, ni el
 * filtro que {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} exigiria. Lo que sostiene
 * el aislamiento aqui no es un {@code WHERE} —no puede serlo, porque el lote es
 * de todas las empresas a la vez— sino que los seis puertos de entrada estan
 * cerrados a {@code hasRole('SYSTEM')} a secas.
 *
 * <p>
 * <strong>Y no hay borrado, ni logico ni fisico.</strong> Ni {@code delete}, ni
 * {@code disable}, ni reactivacion: la clave hacia atras desde
 * {@code subscription_payments} es {@code ON DELETE RESTRICT} porque un lote
 * que desaparece deja los cobros que colgaban de el sin explicacion.
 */
public interface GatewaySettlementRepository {

    GatewaySettlement save(GatewaySettlement settlement);

    Optional<GatewaySettlement> findById(Long id);

    /**
     * Si ese par pasarela + referencia ya entro.
     *
     * <p>
     * Se consulta <strong>antes</strong> de insertar porque
     * {@code uq_gateway_settlements_reference} convierte el duplicado en un error
     * del driver, y recargar dos veces el mismo informe de liquidacion —que se
     * descarga a mano de la consola de la pasarela— merece un conflicto legible y
     * no un 500.
     *
     * <p>
     * <strong>La comparacion es exacta</strong>, por la colacion {@code ascii_bin}
     * de las dos columnas.
     */
    boolean existsByGatewayAndSettlementReference(String gateway, String settlementReference);

    /** Barrido completo de lotes. Solo lo consume un puerto SYSTEM. */
    PageResult<GatewaySettlement> findAll(int page, int pageSize);
}
