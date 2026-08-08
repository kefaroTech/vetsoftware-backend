package com.vetsoftware.app.animalcolor.application.command;

public record UpdateAnimalColorCommand(Long id, String name, Long specieId) {
}
