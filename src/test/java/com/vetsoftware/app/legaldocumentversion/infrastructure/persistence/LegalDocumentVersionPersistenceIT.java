package com.vetsoftware.app.legaldocumentversion.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.legaldocumentversion.domain.LegalDocumentKind;
import com.vetsoftware.app.legaldocumentversion.domain.LegalDocumentVersion;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.EngineConstraint;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(PersistenceSliceConfig.class)
@DisplayName("JpaLegalDocumentVersionRepository — el texto inmutable contra MySQL real")
class LegalDocumentVersionPersistenceIT extends AbstractDataJpaTest {

    private static final String CODE = "TERMS_OF_SERVICE_IT";
    private static final String TEXTO_V1 = "Version 1 de los terminos del servicio.";
    private static final String TEXTO_V2 = "Version 2 de los terminos del servicio.";
    private static final LocalDateTime PUBLICADO_EL = LocalDateTime.of(2026, 3, 1, 12, 0, 0);
    private static final LocalDateTime SUCEDIDO_EL = LocalDateTime.of(2026, 9, 1, 12, 0, 0);
    private static final LocalDate VIGE_DESDE = LocalDate.of(2026, 3, 15);

    @Autowired
    private LegalDocumentVersionJpaRepository springDataRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private JpaLegalDocumentVersionRepository repository;

    @BeforeEach
    void adaptador() {
        SchemaSeed.seed(entityManager);
        repository = new JpaLegalDocumentVersionRepository(springDataRepository,
                new LegalDocumentVersionJpaMapper());
    }

    private LegalDocumentVersion publicar(int numero, String texto, LocalDateTime cuando) {
        LegalDocumentVersion guardada = repository.save(LegalDocumentVersion.publish(CODE, numero,
                LegalDocumentKind.TERMS, "Terminos del servicio", texto, SchemaSeed.SYSTEM_USER_ID,
                VIGE_DESDE, cuando, cuando));
        entityManager.flush();
        entityManager.clear();
        return guardada;
    }

    @Nested
    @DisplayName("Publicacion")
    class Publicacion {

        @Test
        @DisplayName("guarda el texto con la huella que el dominio derivo de el")
        void guarda_el_texto_con_su_huella() {
            LegalDocumentVersion guardada = publicar(1, TEXTO_V1, PUBLICADO_EL);

            assertThat(repository.findById(guardada.getId())).get().satisfies(recuperada -> {
                assertThat(recuperada.getContent()).isEqualTo(TEXTO_V1);
                assertThat(recuperada.getContentHash())
                        .isEqualTo(LegalDocumentVersion.hashOf(TEXTO_V1)).matches("^[0-9a-f]{64}$");
                assertThat(recuperada.getDocumentVersion()).isEqualTo(1);
                assertThat(recuperada.getKind()).isEqualTo(LegalDocumentKind.TERMS);
                assertThat(recuperada.isCurrent()).isTrue();
                assertThat(recuperada.getVersion()).isZero();
            });
        }

        /**
         * <b>Este caso solo prueba la consulta, y antes decia que probaba la
         * restriccion.</b> Se llamaba «el mismo texto no se puede publicar dos veces» y
         * publicaba <em>una sola vez</em>: borrar {@code uq_ldv_content} del esquema lo
         * dejaba igual de verde. Quien de verdad ejercita la restriccion es
         * {@link #publicar_el_mismo_texto_otra_vez_lo_rechaza_el_motor}, aqui debajo.
         */
        @Test
        @DisplayName("existsByCodeAndContentHash distingue el texto publicado del que no")
        void exists_by_code_and_content_hash_distingue_el_texto_publicado() {
            publicar(1, TEXTO_V1, PUBLICADO_EL);

            assertThat(repository.existsByCodeAndContentHash(CODE,
                    LegalDocumentVersion.hashOf(TEXTO_V1))).isTrue();
            assertThat(repository.existsByCodeAndContentHash(CODE,
                    LegalDocumentVersion.hashOf(TEXTO_V2))).isFalse();
        }

        /**
         * <b>La restriccion de verdad, y disparada por el motivo correcto.</b> La tabla
         * tiene <em>tres</em> indices unicos y el escenario esta montado para que solo
         * pueda saltar el que se quiere probar:
         *
         * <ul>
         * <li>{@code uq_ldv_code_version (code, document_version)} no salta porque la
         * segunda publicacion lleva el numero 2, no el 1. Republicar con el mismo
         * numero habria chocado <b>antes</b> contra este indice y el caso habria pasado
         * en verde sin haber tocado nunca el que anuncia — el patron exacto del «verde
         * por el motivo equivocado».</li>
         * <li>{@code uq_ldv_current} no salta porque la primera version se cierra con
         * {@code supersede} antes de publicar la segunda, que es la misma precaucion
         * que ya documenta {@code la_huella_devuelve_la_version_historica}.</li>
         * <li>Queda {@code uq_ldv_content (code, content_hash)}, y
         * {@code EngineConstraint.assertViolates} exige que sea ese el nombre que
         * aparece en la cadena de causas: si mañana saltara otro, este caso se pone
         * rojo en vez de aplaudir.</li>
         * </ul>
         *
         * <p>
         * <b>Que se rompe si falta.</b> El texto legal se identifica ante el cliente
         * por su huella: dos filas con el mismo {@code content_hash} bajo el mismo
         * documento hacen que «que version aceptaste» tenga dos respuestas, y esa
         * pregunta se responde ante un juez.
         */
        @Test
        @DisplayName("republicar el mismo texto bajo el mismo documento lo rechaza el motor")
        void publicar_el_mismo_texto_otra_vez_lo_rechaza_el_motor() {
            LegalDocumentVersion primera = publicar(1, TEXTO_V1, PUBLICADO_EL);
            LegalDocumentVersion cargada = repository.findById(primera.getId()).orElseThrow();
            cargada.supersede(SUCEDIDO_EL);
            repository.supersede(cargada);
            entityManager.flush();

            EngineConstraint.assertViolates("uq_ldv_content",
                    () -> publicar(2, TEXTO_V1, SUCEDIDO_EL));
        }
    }

    @Nested
    @DisplayName("Inmutabilidad")
    class Inmutabilidad {

        @Test
        @DisplayName("el disparador rechaza cambiar el contenido de una version publicada")
        void el_disparador_rechaza_editar_el_contenido() {
            LegalDocumentVersion guardada = publicar(1, TEXTO_V1, PUBLICADO_EL);

            // El dominio no ofrece update(), asi que para llegar al disparador hay que
            // saltarselo escribiendo sobre la entidad JPA. Esto es exactamente lo que
            // ocurriria si alguien anadiera manana un caso de uso de edicion: el motor
            // lo aborta con SIGNAL SQLSTATE '45000'.
            LegalDocumentVersionJpaEntity fila = springDataRepository.findById(guardada.getId())
                    .orElseThrow();
            fila.setContent("Texto editado a mano");

            assertThatThrownBy(() -> {
                springDataRepository.save(fila);
                entityManager.flush();
            }).hasStackTraceContaining("una version publicada no se edita");
        }

        @Test
        @DisplayName("suceder si esta permitido: mueve superseded_at y nada mas")
        void suceder_si_esta_permitido() {
            LegalDocumentVersion guardada = publicar(1, TEXTO_V1, PUBLICADO_EL);

            LegalDocumentVersion vigente = repository.findById(guardada.getId()).orElseThrow();
            vigente.supersede(SUCEDIDO_EL);
            repository.supersede(vigente);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardada.getId())).get().satisfies(sucedida -> {
                assertThat(sucedida.getSupersededAt()).isEqualTo(SUCEDIDO_EL);
                assertThat(sucedida.isCurrent()).isFalse();
                assertThat(sucedida.getContent()).isEqualTo(TEXTO_V1);
                assertThat(sucedida.getVersion()).isEqualTo(1L);
            });
        }
    }

    @Nested
    @DisplayName("Lectura por huella y vigencia")
    class LecturaPorHuella {

        @Test
        @DisplayName("la huella devuelve la version historica, no la que rige hoy")
        void la_huella_devuelve_la_version_historica() {
            LegalDocumentVersion primera = publicar(1, TEXTO_V1, PUBLICADO_EL);
            LegalDocumentVersion cargada = repository.findById(primera.getId()).orElseThrow();
            cargada.supersede(SUCEDIDO_EL);
            // supersede() y no save(): el cierre tiene que estar escrito antes de que
            // entre la version nueva o uq_ldv_current ve dos vigentes. Con save() este
            // mismo test falla con «Duplicate entry ... for key uq_ldv_current», que es
            // como se encontro el defecto.
            repository.supersede(cargada);
            publicar(2, TEXTO_V2, SUCEDIDO_EL);

            // Lo que el cliente acepto fue la 1; hoy rige la 2. Su prueba tiene que
            // devolverle el texto que leyo, no el de ahora.
            assertThat(repository.findByCodeAndContentHash(CODE,
                    LegalDocumentVersion.hashOf(TEXTO_V1))).get().satisfies(aceptada -> {
                        assertThat(aceptada.getContent()).isEqualTo(TEXTO_V1);
                        assertThat(aceptada.isCurrent()).isFalse();
                    });
            assertThat(repository.findCurrentByCode(CODE)).get()
                    .extracting(LegalDocumentVersion::getContent).isEqualTo(TEXTO_V2);
        }

        @Test
        @DisplayName("findLastDocumentVersion da el numero desde el que sigue la siguiente")
        void find_last_document_version_da_el_ultimo_numero() {
            publicar(1, TEXTO_V1, PUBLICADO_EL);

            assertThat(repository.findLastDocumentVersion(CODE)).contains(1);
            assertThat(repository.findLastDocumentVersion("CODIGO_QUE_NO_EXISTE")).isEmpty();
        }

        @Test
        @DisplayName("una huella que no corresponde a este documento no devuelve nada")
        void una_huella_desconocida_no_devuelve_nada() {
            publicar(1, TEXTO_V1, PUBLICADO_EL);

            assertThat(repository.findByCodeAndContentHash(CODE,
                    LegalDocumentVersion.hashOf("un texto que nadie publico"))).isEmpty();
        }
    }
}
