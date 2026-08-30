package com.vetsoftware.app.aiproposal.infrastructure.persistence;

import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Las escrituras masivas de la retencion. <strong>Todas nativas y todas
 * acotadas.</strong>
 *
 * <p>
 * &#9940; <strong>Las tres tablas van versionadas, asi que todo {@code UPDATE}
 * de aqui lleva {@code version = version + 1} en el {@code SET}</strong>
 * ({@code UPDATE_MASIVO_MUEVE_LA_VERSION}, dura). No es ceremonia:
 * {@code @Version} solo protege el ciclo leer-modificar-guardar de una entidad
 * gestionada. Un {@code UPDATE} de conjunto va directo a la base y deja la fila
 * cambiada con su version intacta, asi que un {@code save} concurrente que
 * venga de una lectura anterior <strong>casa igual y pisa la
 * anonimizacion</strong> -sin excepcion, sin log y sin 409-. Es decir: el
 * prospecto reabre su enlace mientras corre el barrido, el {@code save} del
 * turno rehidrata el {@code contact_email} que se acababa de borrar, y la fila
 * queda con {@code anonymized_at} puesto y el correo dentro. El {@code CHECK}
 * {@code chk_ai_proposals_anonimizado} caza <em>ese</em> caso concreto, pero no
 * caza el mismo accidente sobre turnos ni sobre lineas.
 *
 * <p>
 * <strong>Y la version va en el {@code SET}, jamas en el
 * {@code WHERE}</strong>: al reves que en un {@code @SQLDelete}. Aqui nadie
 * leyo la fila antes, asi que condicionar por version solo conseguiria
 * actualizar cero filas y que el barrido lo interpretara como "ya no queda
 * trabajo".
 *
 * <p>
 * <strong>{@code ORDER BY id LIMIT} en todas las del barrido.</strong> MySQL
 * solo lo admite en mutaciones de una sola tabla, y por eso ninguna es
 * multitabla: el parentesco se expresa con subconsulta en el {@code WHERE}, no
 * con {@code JOIN} en el {@code UPDATE}. Sin el {@code LIMIT}, el primer
 * barrido despues de un pico de trafico escribiria cientos de miles de filas en
 * una sola transaccion.
 *
 * <p>
 * <strong>Sin {@code company_id} en ninguna</strong>, y es correcto: ni
 * {@code ai_proposals} ni sus dos hijas tienen empresa -una propuesta es
 * anonima por definicion, esa es la feature-, asi que
 * {@code MUTACIONES_SQL_ACOTADAS_POR_EMPRESA} no las mira. El dia que alguien
 * cuelgue una empresa de esta rodaja, las cuatro reglas de BE-COV se encienden
 * sobre la rodaja entera y estas consultas seran lo primero que se ponga rojo.
 */
public interface AiProposalRetentionJpaRepository extends JpaRepository<AiProposalJpaEntity, Long> {

    /**
     * Paso 1 del barrido. {@code contact_email} a {@code NULL} arrastra a
     * {@code contact_email_hash} -columna {@code GENERATED ... STORED}- y por eso
     * la supresion dirigida por hash tiene que ordenar sus pasos al reves que este
     * barrido. {@code idempotency_key} tambien se va: es la clave que el front
     * deriva del formulario y sirve para reidentificar.
     */
    @Modifying
    @Query(nativeQuery = true, value = """
            UPDATE ai_proposals
               SET contact_email = NULL,
                   idempotency_key = NULL,
                   anonymized_at = :ahora,
                   version = version + 1
             WHERE anonymized_at IS NULL
               AND last_activity_at < :inactivasDesde
             ORDER BY id
             LIMIT :tamanoDeLote
            """)
    int anonymizeProposals(@Param("inactivasDesde") LocalDateTime inactivasDesde,
            @Param("ahora") LocalDateTime ahora, @Param("tamanoDeLote") int tamanoDeLote);

    /**
     * Paso 2 del barrido. {@code input_text_chars} se conserva a proposito: es un
     * entero derivado, no dato personal, y es la unica señal que queda de si el
     * prospecto escribio cuatro palabras o cuatro parrafos.
     *
     * <p>
     * Ninguno de los ocho {@code CHECK} de la tabla exige {@code input_text} ni
     * {@code raw_response}: {@code chk_ai_proposal_turns_model_arc} solo ata
     * {@code model_id} y {@code prompt_version} al arco del modelo, y para
     * {@code CUSTOMER_EDIT} exige {@code raw_response IS NULL}, que este
     * {@code UPDATE} respeta por construccion.
     */
    @Modifying
    @Query(nativeQuery = true, value = """
            UPDATE ai_proposal_turns
               SET input_text = NULL,
                   raw_response = NULL,
                   version = version + 1
             WHERE (input_text IS NOT NULL OR raw_response IS NOT NULL)
               AND proposal_id IN (SELECT p.id FROM ai_proposals p
                                    WHERE p.anonymized_at IS NOT NULL)
             ORDER BY id
             LIMIT :tamanoDeLote
            """)
    int redactTurns(@Param("tamanoDeLote") int tamanoDeLote);

    /**
     * Paso 3 del barrido, el que faltaba. El marcador va <strong>en la propia
     * linea</strong> y no se lee {@code ai_proposals.anonymized_at}: MySQL no
     * permite que un {@code CHECK} referencie otra tabla, asi que la rama
     * {@code OR reason_redacted_at IS NOT NULL} de
     * {@code chk_ai_proposal_lines_model_reason} no seria expresable de otra forma.
     * Y el marcador por fila comprueba la invariante <strong>sin
     * {@code JOIN}</strong>, que es el mismo argumento por el que
     * {@code chk_ai_proposals_anonimizado} es la constraint mas valiosa de su
     * tabla.
     */
    @Modifying
    @Query(nativeQuery = true, value = """
            UPDATE ai_proposal_lines
               SET reason = NULL,
                   reason_redacted_at = :ahora,
                   version = version + 1
             WHERE reason IS NOT NULL
               AND turn_id IN (SELECT t.id FROM ai_proposal_turns t
                                JOIN ai_proposals p ON p.id = t.proposal_id
                               WHERE p.anonymized_at IS NOT NULL)
             ORDER BY id
             LIMIT :tamanoDeLote
            """)
    int redactLineReasons(@Param("ahora") LocalDateTime ahora,
            @Param("tamanoDeLote") int tamanoDeLote);

    /** Purga, paso 1 de 3: las lineas de los turnos purgables. */
    @Modifying
    @Query(nativeQuery = true, value = """
            DELETE FROM ai_proposal_lines
             WHERE turn_id IN (SELECT t.id FROM ai_proposal_turns t
                                JOIN ai_proposals p ON p.id = t.proposal_id
                               WHERE p.last_activity_at < :anterioresA
                                 AND NOT EXISTS (SELECT 1 FROM ai_proposal_conversions c
                                                  WHERE c.proposal_id = p.id))
             ORDER BY id
             LIMIT :tamanoDeLote
            """)
    int purgeLines(@Param("anterioresA") LocalDateTime anterioresA,
            @Param("tamanoDeLote") int tamanoDeLote);

    /** Purga, paso 2 de 3: los turnos ya vacios de las propuestas purgables. */
    @Modifying
    @Query(nativeQuery = true, value = """
            DELETE FROM ai_proposal_turns
             WHERE NOT EXISTS (SELECT 1 FROM ai_proposal_lines l
                                WHERE l.turn_id = ai_proposal_turns.id)
               AND proposal_id IN (SELECT p.id FROM ai_proposals p
                                    WHERE p.last_activity_at < :anterioresA
                                      AND NOT EXISTS (SELECT 1 FROM ai_proposal_conversions c
                                                       WHERE c.proposal_id = p.id))
             ORDER BY id
             LIMIT :tamanoDeLote
            """)
    int purgeTurns(@Param("anterioresA") LocalDateTime anterioresA,
            @Param("tamanoDeLote") int tamanoDeLote);

    /**
     * Purga, paso 3 de 3. &#9940; El {@code NOT EXISTS} sobre turnos no es
     * defensivo, es el orden: las FK de turnos y lineas van
     * {@code ON DELETE RESTRICT}, asi que borrar la cabecera antes de vaciar a sus
     * hijas es un error de integridad, no un borrado en cascada. Y la propuesta
     * convertida no se purga nunca -su fila de conversion la retiene-.
     */
    @Modifying
    @Query(nativeQuery = true, value = """
            DELETE FROM ai_proposals
             WHERE last_activity_at < :anterioresA
               AND NOT EXISTS (SELECT 1 FROM ai_proposal_turns t
                                WHERE t.proposal_id = ai_proposals.id)
               AND NOT EXISTS (SELECT 1 FROM ai_proposal_conversions c
                                WHERE c.proposal_id = ai_proposals.id)
             ORDER BY id
             LIMIT :tamanoDeLote
            """)
    int purgeProposals(@Param("anterioresA") LocalDateTime anterioresA,
            @Param("tamanoDeLote") int tamanoDeLote);

    /**
     * Supresion dirigida, paso 1 de 3: los motivos.
     *
     * <p>
     * &#9940; <strong>Va primero, al reves que en el barrido.</strong> El paso que
     * borra {@code contact_email} destruye {@code contact_email_hash} -es una
     * columna generada-, asi que ejecutarlo antes dejaria a los otros dos sin nada
     * por lo que buscar: los motivos del titular se quedarian escritos y el informe
     * diria que se le borro.
     *
     * <p>
     * Casa por hash y no por el texto del correo: la columna generada ya normaliza
     * a minusculas, asi que {@code Ana@X.com} y {@code ana@x.com} son el mismo
     * titular -que es lo que un requerimiento de supresion espera- y ademas
     * {@code ix_ai_proposals_email_hash} lo resuelve sin escanear la tabla.
     */
    @Modifying
    @Query(nativeQuery = true, value = """
            UPDATE ai_proposal_lines
               SET reason = NULL,
                   reason_redacted_at = :ahora,
                   version = version + 1
             WHERE reason IS NOT NULL
               AND turn_id IN (SELECT t.id FROM ai_proposal_turns t
                                JOIN ai_proposals p ON p.id = t.proposal_id
                               WHERE p.contact_email_hash
                                     = UNHEX(SHA2(LOWER(:contactEmail), 256)))
            """)
    int suppressLinesByEmail(@Param("contactEmail") String contactEmail,
            @Param("ahora") LocalDateTime ahora);

    /** Supresion dirigida, paso 2 de 3: el texto libre y la respuesta cruda. */
    @Modifying
    @Query(nativeQuery = true, value = """
            UPDATE ai_proposal_turns
               SET input_text = NULL,
                   raw_response = NULL,
                   version = version + 1
             WHERE (input_text IS NOT NULL OR raw_response IS NOT NULL)
               AND proposal_id IN (SELECT p.id FROM ai_proposals p
                                    WHERE p.contact_email_hash
                                          = UNHEX(SHA2(LOWER(:contactEmail), 256)))
            """)
    int suppressTurnsByEmail(@Param("contactEmail") String contactEmail);

    /**
     * Supresion dirigida, paso 3 de 3: el correo. {@code COALESCE} y no
     * {@code :ahora} a secas: si la fila ya estaba anonimizada por tiempo,
     * reescribir la fecha falsearia cuando se limpio.
     *
     * <p>
     * <strong>Sin {@code LIMIT} en los tres pasos, y es deliberado.</strong> Una
     * peticion de supresion es de un titular -unidades de filas, no millones- y
     * dejarla a medias porque se agoto un lote es incumplir el articulo 8 mientras
     * el job informa exito.
     */
    @Modifying
    @Query(nativeQuery = true, value = """
            UPDATE ai_proposals
               SET contact_email = NULL,
                   idempotency_key = NULL,
                   anonymized_at = COALESCE(anonymized_at, :ahora),
                   version = version + 1
             WHERE contact_email_hash = UNHEX(SHA2(LOWER(:contactEmail), 256))
            """)
    int suppressProposalsByEmail(@Param("contactEmail") String contactEmail,
            @Param("ahora") LocalDateTime ahora);
}
