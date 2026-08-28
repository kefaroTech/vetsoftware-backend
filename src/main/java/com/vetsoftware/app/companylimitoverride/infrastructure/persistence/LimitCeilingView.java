package com.vetsoftware.app.companylimitoverride.infrastructure.persistence;

/**
 * Un techo candidato tal como sale de la base: su modo y su cantidad.
 *
 * <p>
 * <strong>Los dos campos juntos, y no solo la cantidad.</strong> Un modo
 * {@code FULL} lleva la cantidad vacía y significa «sin techo»; un modo
 * {@code LIMITED} con cantidad cero significa «no puede crear nada». Traer solo
 * la columna de cantidad haría indistinguibles el vacío del primero de un vacío
 * por error, y el resolutor trata «sin techo» como el candidato que gana a
 * todos los demás: la confusión no se quedaría en un número raro, elevaría a
 * ilimitado un cupo que alguien pagó.
 */
public interface LimitCeilingView {

    String getMode();

    Integer getLimitQuantity();
}
