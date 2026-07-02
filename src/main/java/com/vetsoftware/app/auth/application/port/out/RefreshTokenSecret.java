package com.vetsoftware.app.auth.application.port.out;

/** Genera el valor opaco del refresh token y calcula su hash para almacenamiento/lookup. */
public interface RefreshTokenSecret {
    /** Valor aleatorio en claro que se entrega al cliente (nunca se persiste). */
    String generateRaw();

    /** Hash determinista (SHA-256 hex) usado como clave de búsqueda y para persistir. */
    String hash(String raw);
}
