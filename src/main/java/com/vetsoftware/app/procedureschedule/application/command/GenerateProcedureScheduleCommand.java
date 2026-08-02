package com.vetsoftware.app.procedureschedule.application.command;

public record GenerateProcedureScheduleCommand(Long hospitalizationProcedureId, Long createdById) {
}
