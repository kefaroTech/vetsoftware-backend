package com.vetsoftware.app.city.application.command;

public record UpdateCityCommand(Long id, String name, Long stateId, String daneCode) {}
