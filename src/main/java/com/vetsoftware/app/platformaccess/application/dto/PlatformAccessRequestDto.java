package com.vetsoftware.app.platformaccess.application.dto;

import com.vetsoftware.app.platformaccess.domain.PlatformAccessRequest;
import java.time.LocalDateTime;

/**
 * Lo que ve el aprobador al abrir el enlace, antes de decidir.
 * {@code requestedAt} viaja como instante crudo: el front lo formatea, el
 * backend no manda texto formateado.
 */
public record PlatformAccessRequestDto(String fullName, String email, String reason,
        LocalDateTime requestedAt) {

    public static PlatformAccessRequestDto from(PlatformAccessRequest request) {
        return new PlatformAccessRequestDto(request.getFullName(), request.getEmail(),
                request.getReason(), request.getCreatedDate());
    }
}
