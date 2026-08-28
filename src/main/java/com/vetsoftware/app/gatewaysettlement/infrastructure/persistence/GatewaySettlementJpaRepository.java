package com.vetsoftware.app.gatewaysettlement.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * <strong>Sin una sola {@code @Query} de escritura.</strong> Las dos consultas
 * derivadas y el contador nativo son todos {@code SELECT}, asi que aqui no hay
 * SQL que pueda olvidarse de mover la {@code version} en su {@code SET}
 * ({@code UPDATE_MASIVO_MUEVE_LA_VERSION}, #53) ni
 * {@code UPDATE}/{@code DELETE} al que
 * {@code MUTACIONES_SQL_ACOTADAS_POR_EMPRESA} pudiera pedirle un filtro de
 * empresa que la tabla no tiene. Toda escritura pasa por el ciclo
 * leer-modificar-guardar, que es el unico camino que {@code @Version} protege.
 *
 * <p>
 * <strong>Sin {@code @EntityGraph}</strong>, y no por descuido: la entidad no
 * tiene ni una asociacion —la entrada de banco viaja como {@code Long}—, asi
 * que no hay N+1 que evitar.
 */
public interface GatewaySettlementJpaRepository
        extends
            JpaRepository<GatewaySettlementJpaEntity, Long> {

    /**
     * <strong>La igualdad la resuelve el motor bajo la colacion de las columnas,
     * que es {@code ascii_bin}</strong> — es decir, byte a byte. Por eso este
     * {@code exists} no descarta como duplicado el lote {@code LOTE-9F2A} cuando ya
     * entro {@code lote-9f2a}: para MySQL son dos cadenas distintas, igual que para
     * {@code uq_gateway_settlements_reference}. Si alguien devolviera las columnas
     * a la colacion heredada del esquema, este metodo empezaria a mentir sin
     * cambiar una linea de Java.
     */
    boolean existsByGatewayAndSettlementReference(String gateway, String settlementReference);

    /**
     * Cuantos cobros estan atados a este lote por la clave hacia atras
     * {@code fk_subscription_payments_settlement}.
     *
     * <p>
     * <strong>Es nativa por una razon concreta y comprobable, no por
     * comodidad.</strong> {@code subscription_payments.settlement_reference} y
     * {@code settled_on} <b>existen en el esquema (changeset 252) pero NO estan
     * mapeadas</b> en {@code SubscriptionPaymentJpaEntity}, que solo declara
     * {@code gateway} y {@code gateway_reference}. Sin campo mapeado no hay
     * consulta derivada ni JPQL que pueda nombrar la columna: JPQL habla del modelo
     * y esa columna no esta en el modelo. Mientras siga sin mapear, esta es la
     * unica forma de contar. <b>Si algun dia se mapea, este metodo debe sustituirse
     * por una consulta derivada en el repositorio de aquella rodaja</b> —el SQL
     * nativo se queda fuera de la validacion de arranque de Hibernate y un
     * renombrado de columna no lo rompe hasta produccion—.
     *
     * <p>
     * <strong>Cuenta y no devuelve filas, a proposito.</strong> Los cobros de un
     * lote son de decenas de empresas distintas; un {@code SELECT *} aqui pondria
     * el detalle de quien cobro y cuanto al alcance de una rodaja que no acota por
     * empresa. Con un {@code COUNT} no hay nada que se pueda filtrar por descuido a
     * una respuesta.
     *
     * <p>
     * Va en este repositorio y no en el de la otra rodaja porque el cruce permitido
     * por el vertical slicing es de {@code infrastructure/persistence} hacia
     * {@code infrastructure/persistence}: la consulta pertenece a quien la
     * necesita, no a quien guarda la tabla.
     */
    @Query(value = """
            SELECT COUNT(*) FROM subscription_payments
             WHERE gateway = :gateway AND settlement_reference = :settlementReference
            """, nativeQuery = true)
    long countSettledPayments(@Param("gateway") String gateway,
            @Param("settlementReference") String settlementReference);
}
