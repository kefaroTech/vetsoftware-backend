package com.vetsoftware.app.state.application.command;

public record UpdateStateCommand(Long id, String name, Long countryId, String daneCode) {}
