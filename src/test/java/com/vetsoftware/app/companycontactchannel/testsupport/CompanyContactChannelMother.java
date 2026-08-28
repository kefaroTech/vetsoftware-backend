package com.vetsoftware.app.companycontactchannel.testsupport;

import com.vetsoftware.app.companycontactchannel.application.dto.CompanyContactChannelDto;
import com.vetsoftware.app.companycontactchannel.domain.CompanyContactChannel;
import com.vetsoftware.app.companycontactchannel.domain.ContactChannelType;
import com.vetsoftware.app.companycontactchannel.domain.ContactPurpose;
import java.time.LocalDateTime;

/**
 * Canales de contacto listos para usar.
 *
 * <p>
 * <b>Los tres instantes son deliberadamente distintos entre si</b> —cuando se
 * autorizo, cuando se creo la fila y cuando se revoco—. Con el mismo valor en
 * los tres, un mapper que cruzara {@code authorized_at} con
 * {@code created_date} pasaria todas las aserciones; con estos, cae.
 *
 * <p>
 * <b>La empresa por defecto es la del seed de persistencia</b>
 * ({@code SchemaSeed.COMPANY_ID}) y la ajena su vecina, para que la misma
 * factoria valga en la rodaja de MySQL y en los tests puros.
 */
public final class CompanyContactChannelMother {

    public static final Long COMPANY_ID = 900L;
    public static final Long OTRA_COMPANY_ID = 901L;

    public static final String CORREO = "facturacion@clinicasanroque.co";
    public static final String MOVIL = "+573001234567";

    public static final String EVIDENCIA = "Clausula 7 del contrato firmado el 2026-01-15";

    public static final LocalDateTime AUTORIZADO_EL = LocalDateTime.of(2026, 3, 5, 9, 30, 0);
    public static final LocalDateTime CREADO_EL = LocalDateTime.of(2026, 3, 7, 8, 45, 0);
    public static final LocalDateTime REVOCADO_EL = LocalDateTime.of(2026, 6, 18, 14, 5, 45);

    public static final String MOTIVO = "El cliente retiro el consentimiento por escrito";

    private CompanyContactChannelMother() {
    }

    /** Recien autorizado por el caso de uso: sin id, sin version y no primario. */
    public static CompanyContactChannel nuevo() {
        return CompanyContactChannel.authorize(COMPANY_ID, ContactChannelType.EMAIL, CORREO,
                ContactPurpose.BILLING, EVIDENCIA, AUTORIZADO_EL);
    }

    /** Ya persistido: con id y con version, que es lo que ve una lectura. */
    public static CompanyContactChannel vivo(Long id) {
        return canal(id, COMPANY_ID, ContactPurpose.BILLING, false, null, null);
    }

    /** Vivo y ocupando el hueco de primario de su proposito. */
    public static CompanyContactChannel primario(Long id) {
        return canal(id, COMPANY_ID, ContactPurpose.BILLING, true, null, null);
    }

    /** Vivo, primario y del proposito que se pida. */
    public static CompanyContactChannel primarioDe(Long id, ContactPurpose proposito) {
        return canal(id, COMPANY_ID, proposito, true, null, null);
    }

    /** Cerrado: con fecha y motivo, y con la fila intacta. */
    public static CompanyContactChannel revocado(Long id) {
        return canal(id, COMPANY_ID, ContactPurpose.BILLING, false, REVOCADO_EL, MOTIVO);
    }

    /**
     * Cerrado <b>conservando</b> el marcador de primario. Es el estado que la
     * revocacion deja de verdad: el hueco lo libera la columna generada, no un
     * {@code is_primary = FALSE}.
     */
    public static CompanyContactChannel revocadoQueFuePrimario(Long id) {
        return canal(id, COMPANY_ID, ContactPurpose.BILLING, true, REVOCADO_EL, MOTIVO);
    }

    /** De la clinica vecina. Sirve para los casos de aislamiento. */
    public static CompanyContactChannel deOtraEmpresa(Long id) {
        return canal(id, OTRA_COMPANY_ID, ContactPurpose.BILLING, false, null, null);
    }

    public static CompanyContactChannel canal(Long id, Long companyId, ContactPurpose proposito,
            boolean primario, LocalDateTime revocadoEl, String motivo) {
        return new CompanyContactChannel(id, companyId, ContactChannelType.EMAIL, CORREO, proposito,
                AUTORIZADO_EL, EVIDENCIA, revocadoEl, motivo, primario, CREADO_EL, 0L);
    }

    public static CompanyContactChannelDto dto(Long id) {
        return CompanyContactChannelDto.from(vivo(id));
    }

    public static CompanyContactChannelDto dtoRevocado(Long id) {
        return CompanyContactChannelDto.from(revocado(id));
    }
}
