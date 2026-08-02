package com.vetsoftware.app.hospitalizationprogressnote.application.command;

public record CreateHospitalizationProgressNoteCommand(String description, Long hospitalizationId,
        Long createdById) {
}
