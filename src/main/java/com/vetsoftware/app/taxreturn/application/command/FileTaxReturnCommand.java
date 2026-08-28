package com.vetsoftware.app.taxreturn.application.command;

import java.time.LocalDate;

/**
 * Presentar la declaracion.
 *
 * @param filedBySystemUserId
 *            quien la presenta. <strong>Lo pone el controller desde
 *            {@code authz.currentSystemUserId()}, nunca el cuerpo</strong>
 * @param receiptRef
 *            el radicado. Obligatorio: sin el no hay prueba de la presentacion
 * @param fileRef
 *            donde esta la copia de lo presentado. Conservarla es obligacion
 *            expresa del art. 632 ET
 * @param firmezaUntil
 *            hasta cuando pueden revisarla. <strong>Es el dato del que cuelga
 *            toda la politica de conservacion de soportes</strong> (art. 632 ET
 *            modificado por el art. 46 de la Ley 962 de 2005: el termino de
 *            conservacion <em>es</em> el de firmeza de la renta que soportan).
 *            Llega como dato y no se calcula aqui porque depende de si
 *            VetSoftware compensa perdidas fiscales —tres años (art. 714 ET) o
 *            cinco—, y eso <b>sigue pendiente de confirmar con un contador</b>
 */
public record FileTaxReturnCommand(Long id, Long filedBySystemUserId, String receiptRef,
        String fileRef, LocalDate firmezaUntil) {
}
