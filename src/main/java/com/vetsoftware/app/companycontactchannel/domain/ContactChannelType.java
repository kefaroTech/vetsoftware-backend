package com.vetsoftware.app.companycontactchannel.domain;

/**
 * Por que medio se puede contactar a la empresa. Dominio cerrado y espejo
 * <strong>literal</strong> de {@code chk_company_contact_channels_type}: los
 * cinco nombres se escriben aqui igual que en la constraint, porque
 * {@code @Enumerated(EnumType.STRING)} guarda el {@code name()} tal cual y un
 * valor que la comprobacion no admita lo rechaza la base con un error que no
 * menciona ni la columna ni el valor.
 *
 * <p>
 * <strong>Anadir una constante aqui no basta</strong>: sin el changeset que
 * amplie el {@code CHECK}, el alta compila, pasa los tests de dominio y
 * revienta en el {@code INSERT}.
 */
public enum ContactChannelType {

    /** Correo electronico. */
    EMAIL,

    /** Mensaje de texto al movil. */
    SMS,

    /** Mensajeria de WhatsApp. */
    WHATSAPP,

    /** Llamada telefonica. */
    PHONE,

    /** Aviso dentro de la propia aplicacion. */
    IN_APP
}
