package com.vetsoftware.app.subscriptionbilling.application.port.out;

import com.vetsoftware.app.subscriptionbilling.domain.BillableSubscriptionItem;
import java.time.LocalDate;
import java.util.List;

/**
 * Las líneas del contrato vigentes un día, <b>con su modo de cobro</b>.
 *
 * <p>
 * Devuelve <b>todas</b> las vigentes, incluidas las que no cobran, y el filtro
 * por {@link BillableSubscriptionItem#devenga} lo aplica el caso de uso. No es
 * indiferente dónde vive ese filtro: en el adaptador quedaría escrito como una
 * condición SQL que nadie vuelve a leer, y la regla que decide a quién se le
 * cobra tiene que estar en un sitio que se pueda probar sin base de datos.
 *
 * <p>
 * Solo variante acotada por empresa
 * ({@code REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA}).
 */
public interface BillableSubscriptionItemPort {

    List<BillableSubscriptionItem> findCurrentOn(Long companyId, Long subscriptionId,
            LocalDate day);
}
