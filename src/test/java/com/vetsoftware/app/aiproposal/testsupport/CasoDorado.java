package com.vetsoftware.app.aiproposal.testsupport;

import com.vetsoftware.app.aiproposal.domain.LineVerdict;
import com.vetsoftware.app.aiproposal.domain.ModelProposalPayload;
import com.vetsoftware.app.aiproposal.domain.ProposalPresentation;
import java.util.List;
import java.util.Map;

/**
 * Un caso del golden set: <b>un texto de prospecto y la propuesta que tiene que
 * salir</b>.
 *
 * <p>
 * &#9940; <b>Lo que fija el caso es {@link #aceptados}, no
 * {@link #lectura}.</b> La lectura es como se representa hoy lo que el modelo
 * entendio del texto —contra el doble, porque
 * {@code ModelAccessNotEnabledInvoker} devuelve {@code isAvailable() == false}
 * y ningun borrador lleva lineas—. El dia que se habilite Bedrock, esa lectura
 * la producira el modelo de verdad y <b>este fichero no cambia</b>: lo que se
 * compara sigue siendo la propuesta. Por eso la lectura vive aqui como dato y
 * no como un {@code when(...)} dentro de un test.
 *
 * <p>
 * <b>Lo aceptado y lo recomendado son cosas distintas y por eso son dos
 * campos.</b> Un recomendado pasa el filtro del catalogo igual que un
 * necesario, pero <b>no entra al carrito ni dispara el cierre de
 * {@code REQUIRES}</b>: fundirlos convertiria un carrito de seis lineas en uno
 * de diez, que es el upsell que el plan prohibe.
 *
 * @param nombre
 *            como se lee el caso en el informe de fallos
 * @param texto
 *            lo que escribio el prospecto, tal cual: con faltas, abreviaturas y
 *            sin estructura
 * @param lectura
 *            lo que el modelo entendio de ese texto
 * @param aceptados
 *            los codigos que la propuesta tiene que cotizar, incluidos el
 *            nucleo y los que arrastra el cierre de dependencias
 * @param recomendados
 *            los que salen aparte y no suman al total
 * @param rechazados
 *            codigo &rarr; veredicto de lo que el motor descarta, que es la
 *            senal con la que se mide la calidad del modelo
 * @param pantalla
 *            cual de las cuatro pantallas ve el prospecto
 */
public record CasoDorado(String nombre, String texto, ModelProposalPayload lectura,
        List<String> aceptados, List<String> recomendados, Map<String, LineVerdict> rechazados,
        ProposalPresentation pantalla) {

    public CasoDorado {
        aceptados = List.copyOf(aceptados);
        recomendados = List.copyOf(recomendados);
        rechazados = Map.copyOf(rechazados);
    }

    /** Es lo que JUnit imprime como nombre del caso parametrizado. */
    @Override
    public String toString() {
        return nombre;
    }
}
