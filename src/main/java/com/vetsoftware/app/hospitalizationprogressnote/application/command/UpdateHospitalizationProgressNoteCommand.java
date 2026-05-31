package com.vetsoftware.app.hospitalizationprogressnote.application.command;

public record UpdateHospitalizationProgressNoteCommand(
        Long id,
        String description
) {}
