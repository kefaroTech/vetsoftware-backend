package com.vetsoftware.app.billingdocumentstatushistory.testsupport;

import com.vetsoftware.app.billingdocumentstatushistory.application.command.RecordBillingDocumentStatusChangeCommand;
import com.vetsoftware.app.billingdocumentstatushistory.application.dto.BillingDocumentStatusHistoryDto;
import com.vetsoftware.app.billingdocumentstatushistory.domain.BillingDocumentStatus;
import com.vetsoftware.app.billingdocumentstatushistory.domain.BillingDocumentStatusHistory;
import java.time.LocalDateTime;

/**
 * Fotogramas de ejemplo para los tests de esta rodaja.
 *
 * <p>
 * <b>Los dos instantes son distintos entre si a proposito.</b>
 * {@code occurredAt} y {@code createdDate} son los dos {@code LocalDateTime} y
 * van seguidos en el constructor: cruzarlos compila sin una queja, y con el
 * mismo valor en los dos un mapper cruzado pasaria en verde. Aqui difieren en
 * horas, asi que la confusion sale a la primera.
 *
 * <p>
 * <b>Los dos estados por defecto son el par que importa</b>: de {@code DRAFT} a
 * {@code AWAITING_EXTERNAL} es el movimiento que llena la bandeja de vigilancia
 * —el documento que espera factura externa— y por tanto el que sostiene la
 * consulta para la que existe la tabla.
 */
public final class BillingDocumentStatusHistoryMother {

    public static final Long EMPRESA = 900L;
    public static final Long OTRA_EMPRESA = 901L;
    public static final Long DOCUMENTO = 8500L;

    public static final String ACTOR_PERSONA = "Laura Restrepo";

    /**
     * El actor que justifica que la columna sea texto y no una FK a
     * {@code system_users}: un proceso no tiene fila alli.
     */
    public static final String ACTOR_PROCESO = "proceso-facturacion-automatica";

    public static final String MOTIVO = "Factura externa FE-1043 registrada";

    public static final LocalDateTime OCURRIO_EL = LocalDateTime.of(2026, 3, 5, 9, 30, 0);
    public static final LocalDateTime CREADO_EL = LocalDateTime.of(2026, 3, 5, 14, 15, 0);

    private BillingDocumentStatusHistoryMother() {
    }

    /** El movimiento que deja el documento esperando la factura externa. */
    public static BillingDocumentStatusHistory haciaEsperaExterna() {
        return BillingDocumentStatusHistory.register(EMPRESA, DOCUMENTO,
                BillingDocumentStatus.DRAFT, BillingDocumentStatus.AWAITING_EXTERNAL, OCURRIO_EL,
                ACTOR_PERSONA, MOTIVO, CREADO_EL);
    }

    /** El fotograma siguiente: ya llego la factura externa. */
    public static BillingDocumentStatusHistory haciaRegistroExterno() {
        return BillingDocumentStatusHistory.register(EMPRESA, DOCUMENTO,
                BillingDocumentStatus.AWAITING_EXTERNAL, BillingDocumentStatus.EXTERNAL_REGISTERED,
                OCURRIO_EL.plusDays(2), ACTOR_PROCESO, "Proveedor externo confirmo FE-1043",
                CREADO_EL.plusDays(2));
    }

    /** Fotograma ya escrito en la base, con su id. */
    public static BillingDocumentStatusHistory yaRegistrado(Long id) {
        return new BillingDocumentStatusHistory(id, EMPRESA, DOCUMENTO, BillingDocumentStatus.DRAFT,
                BillingDocumentStatus.AWAITING_EXTERNAL, OCURRIO_EL, ACTOR_PERSONA, MOTIVO,
                CREADO_EL);
    }

    public static RecordBillingDocumentStatusChangeCommand comando() {
        return new RecordBillingDocumentStatusChangeCommand(EMPRESA, DOCUMENTO,
                BillingDocumentStatus.DRAFT, BillingDocumentStatus.AWAITING_EXTERNAL, ACTOR_PERSONA,
                MOTIVO);
    }

    /** El comando que el dominio rechaza: origen y destino iguales. */
    public static RecordBillingDocumentStatusChangeCommand comandoSinCambio() {
        return new RecordBillingDocumentStatusChangeCommand(EMPRESA, DOCUMENTO,
                BillingDocumentStatus.DRAFT, BillingDocumentStatus.DRAFT, ACTOR_PERSONA, MOTIVO);
    }

    public static BillingDocumentStatusHistoryDto dto(Long id) {
        return new BillingDocumentStatusHistoryDto(id, EMPRESA, DOCUMENTO,
                BillingDocumentStatus.DRAFT, BillingDocumentStatus.AWAITING_EXTERNAL, OCURRIO_EL,
                ACTOR_PERSONA, MOTIVO, CREADO_EL);
    }
}
