package com.vetsoftware.app.companybillingprofile.infrastructure.web.response;

import com.vetsoftware.app.companybillingprofile.application.dto.CitySummaryDto;
import com.vetsoftware.app.companybillingprofile.application.dto.CompanyBillingProfileDto;
import com.vetsoftware.app.companybillingprofile.domain.PersonKind;
import com.vetsoftware.app.companybillingprofile.domain.TaxIdKind;
import com.vetsoftware.app.companybillingprofile.domain.TaxRegime;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * La ficha de facturacion tal como sale por HTTP.
 *
 * <p>
 * <strong>Los campos de nombre van los cinco, y solo uno de los dos juegos trae
 * valor.</strong> Cual depende de {@code personKind}, y ninguno lleva
 * {@code REQUIRED}: marcarlos obligatorios haria que el tipo generado para los
 * fronts prometiera un {@code legalName} que la mitad de las fichas no tiene.
 * Juntarlos aqui en un solo campo «nombre» seria mas comodo para pintar y
 * exactamente lo que obliga a partirlos otra vez cuando toca reportar la
 * informacion exogena.
 *
 * <p>
 * <strong>{@code validTo} nulo significa vigente</strong>, y por eso tampoco va
 * {@code REQUIRED}. Es la unica señal que el front necesita para saber si esta
 * mirando la ficha de hoy o una del historico.
 *
 * <p>
 * <strong>Sin {@code companyId} y sin {@code enabled}.</strong> El primero
 * porque el cliente ya sabe de que empresa es —el backend lo deriva del token,
 * y devolverlo invita a reenviarlo—; el segundo porque no hay forma de
 * cambiarlo: publicar una bandera que siempre vale {@code true} sugiere una
 * baja logica que en esta feature no existe, cuando lo que cierra una ficha es
 * {@code validTo}.
 */
public record CompanyBillingProfileResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PersonKind personKind,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TaxIdKind taxIdKind,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String taxId, String verificationDigit,
        String legalName, String firstName, String middleName, String lastName,
        String secondLastName, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String address,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CitySummary city,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String billingEmail,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TaxRegime taxRegime,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean withholdingAgent,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate validFrom, LocalDate validTo,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate) {

    public static CompanyBillingProfileResponse from(CompanyBillingProfileDto dto) {
        CitySummaryDto city = dto.city();
        return new CompanyBillingProfileResponse(dto.id(), dto.personKind(), dto.taxIdKind(),
                dto.taxId(), dto.verificationDigit(), dto.legalName(), dto.firstName(),
                dto.middleName(), dto.lastName(), dto.secondLastName(), dto.address(),
                new CitySummary(city.id(), city.name()), dto.billingEmail(), dto.taxRegime(),
                dto.withholdingAgent(), dto.validFrom(), dto.validTo(), dto.createdDate());
    }
}
