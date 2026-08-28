package com.vetsoftware.app.entitlement.application.port.out;

/**
 * Deja escrito el <strong>portazo</strong>: a esta clinica se le nego crear
 * algo porque se le habia acabado el cupo.
 *
 * <p>
 * <strong>Es el hecho, no el log.</strong> Un log se rota y se pierde; este
 * hecho es la prueba ante una reclamacion --"a mi nunca me avisaron"-- y, a la
 * vez, la mejor senal de venta que tiene el producto: una clinica que choca
 * doce veces contra su tope de mascotas en una semana es una ampliacion que se
 * cierra sola. Lanzar la excepcion sin escribirlo tira las dos cosas a la vez.
 *
 * <p>
 * <strong>Tiene que sobrevivir a la vuelta atras.</strong> La negacion aborta
 * la operacion que la provoco, asi que escribir el hecho en la misma
 * transaccion equivale a no escribirlo: se va con el {@code rollback}. Quien lo
 * implemente esta obligado a escribirlo en una transaccion propia.
 */
public interface LimitDenialPort {

    /**
     * @param requestedDelta
     *            cuanto se intento subir el contador. Va entero y no como "uno"
     *            porque una importacion masiva pide muchos de golpe, y "pidio 400
     *            sobre un techo de 100" es una conversacion comercial distinta de
     *            "pidio uno".
     */
    void limitDenied(Long companyId, Long limitDimensionId, int limitQuantity, int usedQuantity,
            int requestedDelta);
}
