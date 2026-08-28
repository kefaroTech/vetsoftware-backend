package com.vetsoftware.app.paymentrefund.application.port.out;

/**
 * La FK {@code payment_refunds.authorized_by_system_user_id} contra
 * {@code system_users}: quien firma la salida de dinero.
 *
 * <p>
 * Es un {@code ValidationPort} porque de la firma solo hace falta que
 * <strong>exista</strong>. El nombre de quien autorizo lo resuelve la consola
 * de plataforma cuando pinta el expediente; copiarlo aqui lo congelaria mal el
 * dia que esa persona cambie de nombre.
 *
 * <p>
 * <strong>Sin variante acotada por empresa, y no es un descuido:</strong>
 * {@code system_users} es la tabla de la plataforma y no lleva
 * {@code company_id}. {@code REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA}
 * solo aplica cuando la entidad referida pertenece a una empresa, asi que aqui
 * no hay empresa por la que acotar.
 */
public interface SystemUserValidationPort {

    boolean existsById(Long systemUserId);
}
