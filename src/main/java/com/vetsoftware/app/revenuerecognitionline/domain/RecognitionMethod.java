package com.vetsoftware.app.revenuerecognitionline.domain;

/**
 * Como se reparte el ingreso en el tiempo. Dominio cerrado y <strong>espejo
 * exacto</strong> de {@code chk_rrl_method} (changeset 344).
 */
public enum RecognitionMethod {

    /**
     * Linea recta por dias: la suscripcion mensual se devenga dia a dia. Es el
     * metodo por el que existe la tabla.
     */
    STRAIGHT_LINE_DAYS,

    /**
     * En un instante: el cargo puntual —un excedente, una penalizacion— se devenga
     * entero en el mes en que ocurre.
     */
    POINT_IN_TIME,

    /**
     * Sobre la vida del cliente. <strong>Queda declarado y no se
     * implementa</strong>, y esa es la decision escrita en el changeset: hoy no se
     * cobra implantacion, que es el unico concepto que lo necesitaria. Esta aqui
     * porque el {@code CHECK} lo admite y un enum que no cubra la lista cerrada de
     * la base convierte una fila legal en un fallo de conversion al leerla.
     */
    OVER_CUSTOMER_LIFE
}
