package com.vetsoftware.app.breed.application.command;

public record UpdateBreedCommand(Long id, String name, Long specieId) {}
