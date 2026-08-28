package com.vetsoftware.app.subscription.application.port.out;

import com.vetsoftware.app.subscription.domain.PriceListRef;
import java.util.Optional;

/**
 * La tarifa con la que se firma la cabecera del contrato, <b>solo si esta
 * publicada</b>, y con su ventana de vigencia dentro. {@code price_lists} es
 * global de plataforma y no lleva {@code company_id}.
 *
 * <p>
 * <b>Sustituye al antiguo {@code PriceListValidationPort}, que era un
 * {@code existsById} pelado.</b> Con aquel se podia firmar un contrato cuya
 * cabecera apuntara a una lista en borrador o caducada: la comprobacion de
 * verdad solo ocurria despues, al congelar las lineas contra el catalogo
 * publicado, y salia con el mensaje equivocado —«Published catalog price not
 * found for item»—, que acusa al articulo cuando la culpable es la tarifa.
 * Quien leyera ese error se pondria a revisar el catalogo, que es el sitio que
 * no es. Como la cabecera pasa a comprobar estado y vigencia, ya no valida
 * existencia: valida lo mismo que las lineas.
 *
 * <p>
 * <b>Devuelve la ventana en vez de filtrar por ella</b>, igual que el puerto
 * gemelo de la cotizacion. Meter
 * {@code valid_from <= :hoy AND (valid_to IS NULL
 * OR :hoy <= valid_to)} en el WHERE seria mas corto y perderia justo lo que
 * importa cuando falla: una lista caducada volveria como
 * {@code Optional.empty()}, indistinguible de un id que no existe. Ademas
 * bajaria el reloj hasta el adaptador, donde la decision solo se puede probar
 * levantando la base de datos entera. Deciden aqui arriba, en el caso de uso,
 * que ya tiene el {@code Clock} zonado (D-81).
 */
public interface PriceListQueryPort {

    /**
     * @return la tarifa publicada y activa con ese id, con su ventana; vacio si no
     *         existe o si sigue en borrador/retirada. Un vacio y una ventana
     *         caducada son dos fallos distintos y se responden distinto
     */
    Optional<PriceListRef> findPublishedById(Long priceListId);
}
