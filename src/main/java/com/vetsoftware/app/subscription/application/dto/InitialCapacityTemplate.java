package com.vetsoftware.app.subscription.application.dto;

import com.vetsoftware.app.subscription.domain.CapacityUnit;
import com.vetsoftware.app.subscription.domain.TaxTreatment;
import java.math.BigDecimal;

/**
 * Una capacidad del minimo estructural, ya resuelta: el articulo
 * {@code CAPACITY} marcado como parte del nucleo y el tramo de tarifa que se le
 * congela al firmar.
 *
 * <p>
 * <strong>No repite {@code defaultGraceDays} ni
 * {@code defaultTrialDays}</strong> a diferencia de
 * {@link InitialContractTemplate}: esos dos valores son de la plataforma y no
 * de la linea, y traerlos aqui repetidos por cada capacidad seria invitar a que
 * alguien los leyera de la fila equivocada. El contrato solo tiene una
 * cabecera, y la cabecera la resuelve {@link InitialContractTemplate}.
 *
 * <p>
 * Es una <strong>foto</strong>, con la misma razon de ser que su hermana: el
 * precio, el IVA y —sobre todo— lo incluido se copian a la
 * {@code subscription_items} en el momento del alta y no se vuelven a leer del
 * catalogo.
 */
public record InitialCapacityTemplate(Long catalogItemId, String itemCode, String itemName,
        CapacityUnit capacityUnit, int includedQuantity, int minQuantity, BigDecimal unitAmount,
        BigDecimal taxRate, TaxTreatment taxTreatment) {
}
