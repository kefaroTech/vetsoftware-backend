package com.vetsoftware.app.catalogitemlimit.application.command;

import com.vetsoftware.app.catalogitemlimit.domain.LimitEnforcement;
import com.vetsoftware.app.catalogitemlimit.domain.LimitMode;
import com.vetsoftware.app.catalogitemlimit.domain.ResetPeriod;
import java.math.BigDecimal;

/**
 * Cambiar el techo de fábrica.
 *
 * <p>
 * <strong>Lleva {@code catalogItemId} además del {@code id} del techo</strong>,
 * igual que su hermano {@code UpdateBundleComponentCommand}, y esa es la
 * corrección de un hueco que estuvo abierto: sin él, el {@code {catalogItemId}}
 * de la ruta del {@code PUT} decoraba pero no se comprobaba, así que editar el
 * techo del artículo 7 entrando por la ruta del 9 funcionaba. No era una fuga
 * entre empresas —aquí no hay empresas y el gate es SYSTEM— pero sí una URL que
 * miente, y una URL que miente es una URL que alguien guarda y reutiliza. Con
 * el campo dentro, la carga se acota por el par y el desajuste responde 404.
 *
 * <p>
 * <strong>Cambiarlo aquí no cambia nada a quien ya firmó</strong>: los
 * contratos vivos leen su copia congelada. Propagar una <em>mejora</em> a los
 * contratos vivos es otra operación, deliberadamente separada (D-75).
 */
public record UpdateCatalogItemLimitCommand(Long catalogItemId, Long id, LimitMode mode,
        Integer limitQuantity, ResetPeriod resetPeriod, LimitEnforcement enforcement,
        BigDecimal overageUnitAmount, int warnThreshold, LimitMode trialMode,
        Integer trialLimitQuantity) {
}
