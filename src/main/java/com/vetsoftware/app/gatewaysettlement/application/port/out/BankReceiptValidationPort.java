package com.vetsoftware.app.gatewaysettlement.application.port.out;

/**
 * Solo comprueba que la entrada del extracto existe.
 *
 * <p>
 * <strong>Es un {@code ValidationPort} y no un {@code QueryPort} porque esta
 * rodaja no necesita ni un dato de la entrada de banco</strong>: el dominio
 * guarda un {@code Long bankReceiptId} pelado, no un companion VO. Traer el
 * importe o la fecha del extracto para no usarlos seria el patron caro sin la
 * razon que lo justifica —y ademas ataria esta feature a la forma de la otra—.
 *
 * <p>
 * <strong>Devuelve {@code boolean} y no lanza.</strong> Quien decide como suena
 * el fallo es el caso de uso: el CLAUDE.md prohibe expresamente que un
 * adaptador de persistencia lance por una clave foranea que no encuentra.
 *
 * <p>
 * <strong>Sin variante acotada por empresa, y no falta</strong>:
 * {@code bank_receipts} tampoco tiene {@code company_id} —antes de identificar
 * una entrada no hay cliente— asi que no hay {@code findByIdAndCompanyId} que
 * {@code REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA} pudiera echar de
 * menos.
 */
public interface BankReceiptValidationPort {

    boolean exists(Long bankReceiptId);
}
