package com.vetsoftware.app.revenuerecognitionline.application.command;

import com.vetsoftware.app.revenuerecognitionline.domain.RecognitionMethod;
import java.math.BigDecimal;

/**
 * Registrar un renglon de reconocimiento de ingreso.
 *
 * <p>
 * <strong>No trae {@code postingPeriod}, y esa ausencia es la
 * decision.</strong> El periodo contable en que se registra no lo elige quien
 * llama: lo resuelve el caso de uso buscando el <b>primer periodo abierto</b> a
 * partir del mes de imputacion. Es la regla de §6.4 de la especificacion —la
 * que la base no puede imponer sola porque exigiria una consulta de conjunto
 * dentro del {@code INSERT}— y dejarla en manos del llamador seria abrir la
 * puerta a imputar a un mes ya declarado.
 *
 * <p>
 * <strong>Si trae {@code companyId}, y aqui no es una fuga.</strong> Este
 * command no llega nunca desde un {@code @RequestBody} —no hay endpoint de
 * alta, es un libro derivado— sino del proceso que factura, que ya sabe de que
 * clinica es el cargo. Su puerto va cerrado a {@code hasRole('SYSTEM')} a
 * secas: no hay principal de empleado que pueda alcanzarlo.
 *
 * @param recognizedAmount
 *            puede ser negativo: una correccion es otra fila que compensa,
 *            nunca una edicion. Lo unico prohibido es el cero
 */
public record RecordRevenueRecognitionCommand(Long companyId, Long chargeId, String periodKey,
        BigDecimal recognizedAmount, RecognitionMethod method) {
}
