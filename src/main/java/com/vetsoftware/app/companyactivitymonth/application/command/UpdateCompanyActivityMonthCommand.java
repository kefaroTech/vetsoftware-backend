package com.vetsoftware.app.companyactivitymonth.application.command;

import com.vetsoftware.app.companyactivitymonth.domain.CommercialState;
import java.math.BigDecimal;

/**
 * Recalcula el mes: los cinco numeros nuevos sobre la fila que ya existe.
 *
 * <p>
 * <strong>Sin {@code companyId} y sin {@code periodKey}, a proposito.</strong>
 * El par empresa-mes identifica la fila y no se puede mover: cambiarlo no seria
 * recalcular sino reescribir la historia —llevarse la actividad de una clinica
 * a la de otra, o la de agosto a la de julio—. Lo que se recalcula son los
 * cinco numeros; el par se queda donde estaba.
 *
 * <p>
 * Esa ausencia tiene ademas un efecto de autorizacion que conviene decir en voz
 * alta: este command entra en la familia «por id» de BE-COV —lleva un
 * {@code id} y no lleva empresa—, asi que su puerto de entrada solo puede estar
 * abierto a {@code hasRole('SYSTEM')} a secas. Es lo correcto aqui: la serie de
 * actividad es un instrumento de plataforma y ningun tenant la escribe.
 */
public record UpdateCompanyActivityMonthCommand(Long id, CommercialState commercialState,
        int activeDays, int activeUsers, int recordsCreated, BigDecimal mrrSnapshot) {
}
