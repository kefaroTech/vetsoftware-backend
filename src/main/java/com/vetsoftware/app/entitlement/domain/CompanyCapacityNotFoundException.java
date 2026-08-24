package com.vetsoftware.app.entitlement.domain;

/**
 * La empresa no tiene contador de esa unidad.
 *
 * <p>
 * <strong>La decision, escrita para que nadie la revierta por parecerle poco
 * amable: la ausencia de fila significa capacidad NO contratada, es decir
 * limite cero. Nunca "ilimitado".</strong>
 *
 * <p>
 * El razonamiento sale del propio modelo. {@code company_capacities} es una
 * tabla <em>derivada</em> y {@code recalculated_at} esta declarado como
 * indicador de salud: una fila que falta no es una licencia, es la huella de un
 * proceso caido. Las dos lecturas posibles no cuestan lo mismo:
 *
 * <ul>
 * <li>Leerla como <em>ilimitado</em> convierte un fallo del recalculo en barra
 * libre de recursos facturables --usuarios, sedes, terminales-- que nadie cobra
 * y que nadie nota hasta que se audita la facturacion.
 * <li>Leerla como <em>cero</em> convierte el mismo fallo en un mensaje que
 * alguien lee y arregla el mismo dia.
 * </ul>
 *
 * <p>
 * Se falla cerrado, y el mensaje nombra la unidad y apunta al recalculo, porque
 * la causa mas probable de llegar aqui no es que el cliente no lo haya
 * contratado sino que sus contadores no se han derivado todavia.
 */
public class CompanyCapacityNotFoundException extends RuntimeException {

    public CompanyCapacityNotFoundException(String message) {
        super(message);
    }
}
