package com.vetsoftware.app.companybillingprofile.application.dto;

import com.vetsoftware.app.companybillingprofile.domain.CityRef;

/**
 * El municipio tal como lo consume la aplicacion.
 *
 * <p>
 * Es el gemelo sin invariantes de {@link CityRef}: aquel es un value object del
 * dominio y se niega a existir con un nombre vacio, este solo transporta. La
 * separacion es la que pide el CLAUDE.md para los companion VO, y evita que un
 * DTO de salida pueda lanzar al construirse.
 */
public record CitySummaryDto(Long id, String name) {

    public static CitySummaryDto from(CityRef city) {
        return new CitySummaryDto(city.id(), city.name());
    }
}
