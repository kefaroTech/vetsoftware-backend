package com.vetsoftware.app.platformtaxprofile.application.dto;

import com.vetsoftware.app.platformtaxprofile.domain.EconomicActivityRef;

/**
 * La actividad economica tal como la consume la aplicacion.
 *
 * <p>
 * Es el gemelo sin invariantes de {@link EconomicActivityRef}: aquel es un
 * value object del dominio y se niega a existir con un codigo vacio, este solo
 * transporta. La separacion es la que pide el CLAUDE.md para los companion VO,
 * y evita que un DTO de salida pueda lanzar al construirse — que es lo que
 * pasaria al mapear una identidad fiscal sin actividad economica, caso legitimo
 * porque {@code economic_activity_id} es nulable.
 */
public record PlatformEconomicActivitySummaryDto(Long id, String code, String name) {

    /** {@code null} entra y {@code null} sale: la actividad es opcional. */
    public static PlatformEconomicActivitySummaryDto from(EconomicActivityRef ref) {
        return ref == null
                ? null
                : new PlatformEconomicActivitySummaryDto(ref.id(), ref.code(), ref.name());
    }
}
