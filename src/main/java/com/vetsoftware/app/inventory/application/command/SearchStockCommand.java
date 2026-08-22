package com.vetsoftware.app.inventory.application.command;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * Búsqueda paginada del saldo por (producto, sede). {@code branchId} null =
 * todas las sedes accesibles (el controller ya resolvió el alcance). {@code q}
 * filtra por nombre/código de producto. {@code lowStock}=true → solo bajo
 * mínimo. {@code productIds} acota la búsqueda a esos productos —lista vacía =
 * sin filtro—, para que el punto de venta pida los saldos de las líneas que
 * está pintando en vez del catálogo entero.
 *
 * <p>
 * El filtro por ids NO sustituye al alcance de empresa: es una condición más
 * que se suma a {@code company_id = :companyId} en el mismo WHERE, así que solo
 * puede reducir el conjunto que la empresa ya podía ver.
 */
public record SearchStockCommand(Long companyId, Long branchId, String q, boolean lowStock,
        List<Long> productIds, int page, int pageSize) {

    /**
     * Tope de identificadores por petición. Es el mismo número que el tope duro de
     * filas por página del proyecto ({@code Pages.MAX_SIZE} = 200, que no se
     * importa aquí porque {@code application} no conoce el puente de paginación):
     * pedir más ids de los que caben en una página no ahorra ni una petición, y en
     * cambio dejaría que un query param decidiera cuántos parámetros lleva el
     * {@code IN} —cada longitud distinta es un plan nuevo en la caché de sentencias
     * preparadas—.
     */
    public static final int MAX_PRODUCT_IDS = 200;

    public SearchStockCommand {
        productIds = normalizeProductIds(productIds);
    }

    /**
     * Normaliza el filtro: null o vacío = sin filtro; se rechaza por encima del
     * tope y con nulos dentro, y se deduplica conservando el orden de llegada.
     */
    private static List<Long> normalizeProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        if (productIds.size() > MAX_PRODUCT_IDS) {
            throw new IllegalArgumentException(
                    "productIds admite como maximo " + MAX_PRODUCT_IDS + " identificadores");
        }
        if (productIds.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("productIds no admite identificadores nulos");
        }
        return List.copyOf(new LinkedHashSet<>(productIds));
    }
}
