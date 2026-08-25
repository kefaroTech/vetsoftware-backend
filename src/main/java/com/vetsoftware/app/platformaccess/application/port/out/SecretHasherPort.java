package com.vetsoftware.app.platformaccess.application.port.out;

/**
 * Hashing de los dos secretos que esta feature verifica contra una fila: el
 * codigo de 6 digitos y la contrasena elegida al aceptar.
 *
 * <p>
 * <b>Es un puerto propio y no el {@code PasswordHasher} de infraestructura</b>
 * porque la capa de aplicacion no conoce infraestructura. El adaptador delega
 * en el mismo bcrypt del resto del sistema.
 *
 * <p>
 * Para el codigo, bcrypt no es una concesion: 6 digitos son 10^6 combinaciones,
 * unos 20 bits, y con SHA-256 recorrer el millon de hashes de un volcado es
 * cuestion de milisegundos. Ademas su comparacion no cortocircuita en el primer
 * byte distinto, asi que no filtra por latencia cuantos digitos se acertaron
 * —un {@code String.equals} si lo haria—. Como bcrypt lleva salt, la fila NO se
 * puede localizar por el hash del codigo: se localiza siempre por el hash del
 * token de 256 bits y el codigo se verifica contra esa fila. Esa ordenacion es
 * la que hace que 20 bits basten.
 */
public interface SecretHasherPort {

    String hash(String rawSecret);

    boolean matches(String rawSecret, String storedHash);
}
