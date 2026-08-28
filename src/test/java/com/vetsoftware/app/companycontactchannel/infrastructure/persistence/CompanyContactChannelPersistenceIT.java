package com.vetsoftware.app.companycontactchannel.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.companycontactchannel.domain.CompanyContactChannel;
import com.vetsoftware.app.companycontactchannel.domain.ContactChannelType;
import com.vetsoftware.app.companycontactchannel.domain.ContactPurpose;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.EngineConstraint;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de {@code JpaCompanyContactChannelRepository} contra MySQL real.
 *
 * <p>
 * <b>Lo que esta clase existe para vigilar es la barandilla que solo el motor
 * tiene</b>, y es la mas interesante del esquema: el canal primario es unico
 * por empresa <b>Y PROPOSITO</b>, no por empresa. Eso no lo puede probar ningun
 * test de dominio ni de servicio —la regla vive en un indice unico de dos
 * columnas sobre una columna generada— y es exactamente el sitio donde se
 * equivocaria quien copiara el patron de al lado: con {@code primary_marker}
 * sola en el indice habria un unico primario por empresa en total, y el correo
 * de facturacion y el movil de mora no podrian convivir.
 *
 * <p>
 * <b>El adaptador se construye a mano y no se inyecta.</b>
 * {@code PersistenceSliceConfig} enumera los adaptadores de cada rodaja y esta
 * feature todavia no figura ahi; anadirla es una edicion de infraestructura de
 * test compartida, que esta tarea tiene prohibido tocar. Construirlo con
 * {@code new} sobre el {@code CompanyContactChannelJpaRepository} que
 * {@code @DataJpaTest} ya registra da el mismo comportamiento —el SQL que se
 * ejercita es el real— y ademas <b>no cambia la clave del
 * {@code MergedContextConfiguration}</b>: el {@code @Import} sigue siendo
 * unicamente {@code PersistenceSliceConfig}, asi que esta clase comparte el
 * contexto cacheado con las demas rodajas en vez de pagar un arranque entero.
 * Queda pendiente registrarlo alli.
 *
 * <p>
 * <b>El seed basta y sobra.</b> La unica clave foranea de la tabla es
 * {@code companies}, y {@code SchemaSeed} ya siembra dos empresas: la propia y
 * la vecina, que es todo lo que hacen falta para los casos de aislamiento.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaCompanyContactChannelRepository — canales de contacto contra MySQL real")
class CompanyContactChannelPersistenceIT extends AbstractDataJpaTest {

    private static final LocalDateTime AUTORIZADO_EL = LocalDateTime.of(2026, 3, 5, 9, 30, 0);
    private static final LocalDateTime CREADO_EL = LocalDateTime.of(2026, 3, 7, 8, 45, 0);
    private static final LocalDateTime REVOCADO_EL = LocalDateTime.of(2026, 6, 18, 14, 5, 45);
    private static final String MOTIVO = "El cliente retiro el consentimiento por escrito";
    private static final String EVIDENCIA = "Clausula 7 del contrato firmado el 2026-01-15";

    /** Empresa inexistente, para el caso de la clave foranea. */
    private static final Long EMPRESA_FANTASMA = 8_499_000L;

    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private CompanyContactChannelJpaRepository jpaRepository;

    private JpaCompanyContactChannelRepository repository;
    private CompanyContactChannelJpaMapper mapper;

    @BeforeEach
    void seed() {
        mapper = new CompanyContactChannelJpaMapper();
        repository = new JpaCompanyContactChannelRepository(jpaRepository, mapper);
        SchemaSeed.seed(entityManager);
        entityManager.flush();
    }

    private static CompanyContactChannel canal(Long companyId, ContactPurpose proposito,
            String direccion, boolean primario) {
        return new CompanyContactChannel(null, companyId, ContactChannelType.EMAIL, direccion,
                proposito, AUTORIZADO_EL, EVIDENCIA, null, null, primario, CREADO_EL, null);
    }

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("guarda el canal y lo recupera con cada campo y cada fecha en su sitio")
        void guarda_el_canal_y_lo_recupera_campo_a_campo() {
            CompanyContactChannel guardado = repository.save(canal(SchemaSeed.COMPANY_ID,
                    ContactPurpose.BILLING, "facturacion@sanroque.co", false));
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(guardado.getId(), SchemaSeed.COMPANY_ID))
                    .get().satisfies(recuperado -> {
                        assertThat(recuperado.getCompanyId()).isEqualTo(SchemaSeed.COMPANY_ID);
                        assertThat(recuperado.getChannelType()).isEqualTo(ContactChannelType.EMAIL);
                        assertThat(recuperado.getAddress()).isEqualTo("facturacion@sanroque.co");
                        assertThat(recuperado.getPurpose()).isEqualTo(ContactPurpose.BILLING);
                        assertThat(recuperado.getAuthorizedAt()).isEqualTo(AUTORIZADO_EL);
                        assertThat(recuperado.getAuthorizationEvidence()).isEqualTo(EVIDENCIA);
                        assertThat(recuperado.getCreatedDate()).isEqualTo(CREADO_EL);
                        assertThat(recuperado.getRevokedAt()).isNull();
                        assertThat(recuperado.getRevokedReason()).isNull();
                        assertThat(recuperado.isPrimary()).isFalse();
                    });
        }

        @Test
        @DisplayName("el booleano is_primary sobrevive el viaje: TINYINT pelado, no BIT")
        void el_booleano_sobrevive_el_viaje() {
            // Si la columna se hubiera declarado TINYINT(1), el driver la reportaria
            // como BIT y ddl-auto: validate ni siquiera dejaria arrancar el contexto de
            // esta clase. Que este caso se ejecute ya es media prueba; la otra media es
            // que el valor vuelva como se guardo.
            CompanyContactChannel guardado = repository.save(
                    canal(SchemaSeed.COMPANY_ID, ContactPurpose.BILLING, "primario@x.co", true));
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(guardado.getId(), SchemaSeed.COMPANY_ID))
                    .get().satisfies(recuperado -> assertThat(recuperado.isPrimary()).isTrue());
        }

        @Test
        @DisplayName("revocar es un UPDATE que mueve la version, no un INSERT")
        void revocar_mueve_la_version() {
            // Si el mapper perdiera la version, Hibernate insertaria una fila nueva y el
            // canal original seguiria vivo para siempre: la bitacora diria lo contrario
            // de lo que paso.
            CompanyContactChannel vivo = repository.save(canal(SchemaSeed.COMPANY_ID,
                    ContactPurpose.DUNNING, "mora@sanroque.co", false));
            entityManager.clear();

            vivo.revoke(REVOCADO_EL, MOTIVO);
            CompanyContactChannel revocado = repository.save(vivo);
            entityManager.clear();

            assertThat(revocado.getId()).isEqualTo(vivo.getId());
            assertThat(revocado.getVersion()).isGreaterThan(0L);
            assertThat(repository.findAllByCompanyId(SchemaSeed.COMPANY_ID, 0, 20).totalElements())
                    .isEqualTo(1L);
            assertThat(repository.findByIdAndCompanyId(revocado.getId(), SchemaSeed.COMPANY_ID))
                    .get().satisfies(recuperado -> {
                        assertThat(recuperado.getRevokedAt()).isEqualTo(REVOCADO_EL);
                        assertThat(recuperado.getRevokedReason()).isEqualTo(MOTIVO);
                    });
        }
    }

    @Nested
    @DisplayName("Canal primario")
    class CanalPrimario {

        @Test
        @DisplayName("dos primarios de PROPOSITOS DISTINTOS de la misma empresa conviven")
        void dos_primarios_de_propositos_distintos_conviven() {
            // ESTE es el caso que justifica que el indice unico lleve dos columnas. Con
            // primary_marker sola habria un unico primario por empresa EN TOTAL, y el
            // segundo de estos dos fallaria: la clinica no podria tener a la vez un
            // correo primario de facturacion y un movil primario de mora.
            repository.save(
                    canal(SchemaSeed.COMPANY_ID, ContactPurpose.BILLING, "facturas@x.co", true));
            repository
                    .save(canal(SchemaSeed.COMPANY_ID, ContactPurpose.DUNNING, "mora@x.co", true));
            entityManager.clear();

            assertThat(repository.findPrimaryByCompanyIdAndPurpose(SchemaSeed.COMPANY_ID,
                    ContactPurpose.BILLING)).get()
                    .satisfies(p -> assertThat(p.getAddress()).isEqualTo("facturas@x.co"));
            assertThat(repository.findPrimaryByCompanyIdAndPurpose(SchemaSeed.COMPANY_ID,
                    ContactPurpose.DUNNING)).get()
                    .satisfies(p -> assertThat(p.getAddress()).isEqualTo("mora@x.co"));
        }

        @Test
        @DisplayName("dos primarios del MISMO proposito chocan contra el indice unico")
        void dos_primarios_del_mismo_proposito_chocan() {
            repository.save(
                    canal(SchemaSeed.COMPANY_ID, ContactPurpose.BILLING, "facturas@x.co", true));

            EngineConstraint.assertViolates("uq_company_contact_channels_primary", () -> repository
                    .save(canal(SchemaSeed.COMPANY_ID, ContactPurpose.BILLING, "otro@x.co", true)));
        }

        @Test
        @DisplayName("revocar el primario libera el hueco para el siguiente")
        void revocar_el_primario_libera_el_hueco() {
            // La fila revocada conserva is_primary = TRUE; lo que la aparta del indice es
            // primary_marker, que pasa a NULL en cuanto hay revoked_at. Si alguien
            // quitara la condicion de la revocacion de la columna generada, este caso se
            // pone rojo y la empresa se quedaria sin poder nombrar un canal de cobro
            // nuevo.
            CompanyContactChannel primario = repository
                    .save(canal(SchemaSeed.COMPANY_ID, ContactPurpose.BILLING, "viejo@x.co", true));
            entityManager.clear();

            primario.revoke(REVOCADO_EL, MOTIVO);
            repository.save(primario);
            entityManager.clear();

            CompanyContactChannel sucesor = repository
                    .save(canal(SchemaSeed.COMPANY_ID, ContactPurpose.BILLING, "nuevo@x.co", true));
            entityManager.clear();

            assertThat(sucesor.getId()).isNotNull();
            assertThat(repository.findPrimaryByCompanyIdAndPurpose(SchemaSeed.COMPANY_ID,
                    ContactPurpose.BILLING)).get()
                    .satisfies(p -> assertThat(p.getAddress()).isEqualTo("nuevo@x.co"));
        }

        @Test
        @DisplayName("el primario revocado conserva el marcador pero ya no ocupa el hueco")
        void el_primario_revocado_conserva_el_marcador() {
            CompanyContactChannel primario = repository.save(
                    canal(SchemaSeed.COMPANY_ID, ContactPurpose.MARKETING, "promos@x.co", true));
            entityManager.clear();

            primario.revoke(REVOCADO_EL, MOTIVO);
            repository.save(primario);
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(primario.getId(), SchemaSeed.COMPANY_ID))
                    .get().satisfies(recuperado -> assertThat(recuperado.isPrimary()).isTrue());
            assertThat(repository.findPrimaryByCompanyIdAndPurpose(SchemaSeed.COMPANY_ID,
                    ContactPurpose.MARKETING)).isEmpty();
        }

        @Test
        @DisplayName("cada empresa tiene su propio primario del mismo proposito")
        void cada_empresa_tiene_su_propio_primario() {
            // primary_marker vale company_id, no una constante: sin eso, la segunda
            // clinica del sistema no podria nombrar su canal de facturacion.
            repository.save(canal(SchemaSeed.COMPANY_ID, ContactPurpose.BILLING, "mia@x.co", true));
            repository.save(
                    canal(SchemaSeed.OTRA_COMPANY_ID, ContactPurpose.BILLING, "suya@x.co", true));
            entityManager.clear();

            assertThat(repository.findPrimaryByCompanyIdAndPurpose(SchemaSeed.COMPANY_ID,
                    ContactPurpose.BILLING)).isPresent();
            assertThat(repository.findPrimaryByCompanyIdAndPurpose(SchemaSeed.OTRA_COMPANY_ID,
                    ContactPurpose.BILLING)).isPresent();
        }

        @Test
        @DisplayName("varios canales NO primarios del mismo proposito conviven sin estorbarse")
        void varios_no_primarios_del_mismo_proposito_conviven() {
            // Los no primarios llevan primary_marker NULL, y en un indice unico de MySQL
            // dos NULL no chocan. Es la mitad silenciosa del diseno: sin ella, una
            // empresa solo podria autorizar un canal por proposito.
            repository
                    .save(canal(SchemaSeed.COMPANY_ID, ContactPurpose.BILLING, "uno@x.co", false));
            repository
                    .save(canal(SchemaSeed.COMPANY_ID, ContactPurpose.BILLING, "dos@x.co", false));
            repository
                    .save(canal(SchemaSeed.COMPANY_ID, ContactPurpose.BILLING, "tres@x.co", false));
            entityManager.clear();

            assertThat(repository.findAllUsableByCompanyIdAndPurpose(SchemaSeed.COMPANY_ID,
                    ContactPurpose.BILLING, 0, 20).totalElements()).isEqualTo(3L);
        }
    }

    @Nested
    @DisplayName("Consulta caliente")
    class ConsultaCaliente {

        @Test
        @DisplayName("los canales usables excluyen los revocados y los de otro proposito")
        void los_usables_excluyen_revocados_y_otros_propositos() {
            repository
                    .save(canal(SchemaSeed.COMPANY_ID, ContactPurpose.BILLING, "vivo@x.co", false));
            repository.save(
                    canal(SchemaSeed.COMPANY_ID, ContactPurpose.MARKETING, "promo@x.co", false));
            CompanyContactChannel cerrado = repository.save(
                    canal(SchemaSeed.COMPANY_ID, ContactPurpose.BILLING, "cerrado@x.co", false));
            entityManager.clear();

            cerrado.revoke(REVOCADO_EL, MOTIVO);
            repository.save(cerrado);
            entityManager.clear();

            PageResult<CompanyContactChannel> usables = repository
                    .findAllUsableByCompanyIdAndPurpose(SchemaSeed.COMPANY_ID,
                            ContactPurpose.BILLING, 0, 20);

            assertThat(usables.content()).extracting(CompanyContactChannel::getAddress)
                    .containsExactly("vivo@x.co");
        }

        @Test
        @DisplayName("el primario sale arriba del listado de usables")
        void el_primario_sale_arriba() {
            repository.save(
                    canal(SchemaSeed.COMPANY_ID, ContactPurpose.BILLING, "secundario@x.co", false));
            repository.save(
                    canal(SchemaSeed.COMPANY_ID, ContactPurpose.BILLING, "principal@x.co", true));
            entityManager.clear();

            PageResult<CompanyContactChannel> usables = repository
                    .findAllUsableByCompanyIdAndPurpose(SchemaSeed.COMPANY_ID,
                            ContactPurpose.BILLING, 0, 20);

            assertThat(usables.content()).extracting(CompanyContactChannel::getAddress)
                    .containsExactly("principal@x.co", "secundario@x.co");
        }

        @Test
        @DisplayName("la bitacora completa si trae los revocados")
        void la_bitacora_completa_trae_los_revocados() {
            CompanyContactChannel cerrado = repository.save(
                    canal(SchemaSeed.COMPANY_ID, ContactPurpose.BILLING, "cerrado@x.co", false));
            entityManager.clear();

            cerrado.revoke(REVOCADO_EL, MOTIVO);
            repository.save(cerrado);
            entityManager.clear();

            assertThat(repository.findAllByCompanyId(SchemaSeed.COMPANY_ID, 0, 20).content())
                    .extracting(CompanyContactChannel::getRevokedAt).containsExactly(REVOCADO_EL);
        }
    }

    @Nested
    @DisplayName("Validaciones del motor")
    class ValidacionesDelMotor {

        @Test
        @DisplayName("una revocacion a medias la para el CHECK de la bicondicional")
        void una_revocacion_a_medias_la_para_el_check() {
            // El dominio no deja construir esta fila, asi que se escribe la entidad JPA a
            // mano: es la unica forma de comprobar que la barandilla existe TAMBIEN en el
            // esquema. Si algun dia alguien escribe por otro camino —un script, una
            // migracion de datos—, la que responde es esta.
            CompanyContactChannelJpaEntity aMedias = mapper.toJpa(
                    canal(SchemaSeed.COMPANY_ID, ContactPurpose.BILLING, "amedias@x.co", false));
            aMedias.setRevokedAt(REVOCADO_EL);

            EngineConstraint.assertViolates("chk_company_contact_channels_revocation", () -> {
                entityManager.persist(aMedias);
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("cerrar el canal antes de abrirlo lo para el CHECK de la fecha")
        void cerrar_antes_de_abrir_lo_para_el_check() {
            CompanyContactChannelJpaEntity alReves = mapper.toJpa(
                    canal(SchemaSeed.COMPANY_ID, ContactPurpose.BILLING, "alreves@x.co", false));
            alReves.setRevokedAt(AUTORIZADO_EL.minusDays(1));
            alReves.setRevokedReason(MOTIVO);

            EngineConstraint.assertViolates("chk_company_contact_channels_revoked_after", () -> {
                entityManager.persist(alReves);
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("un canal de una empresa que no existe lo para la clave foranea")
        void un_canal_de_una_empresa_inexistente_lo_para_la_fk() {
            EngineConstraint.assertViolates("fk_company_contact_channels_company", () -> repository
                    .save(canal(EMPRESA_FANTASMA, ContactPurpose.BILLING, "fantasma@x.co", false)));
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("el canal de la vecina no se lee con el id y la empresa propia")
        void el_canal_de_la_vecina_no_se_lee() {
            CompanyContactChannel ajeno = repository.save(canal(SchemaSeed.OTRA_COMPANY_ID,
                    ContactPurpose.BILLING, "vecina@x.co", false));
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(ajeno.getId(), SchemaSeed.COMPANY_ID))
                    .isEmpty();
            assertThat(repository.findByIdAndCompanyId(ajeno.getId(), SchemaSeed.OTRA_COMPANY_ID))
                    .isPresent();
        }

        @Test
        @DisplayName("ni la bitacora ni la consulta caliente cruzan de empresa")
        void ninguna_consulta_cruza_de_empresa() {
            repository
                    .save(canal(SchemaSeed.COMPANY_ID, ContactPurpose.BILLING, "mia@x.co", false));
            repository.save(
                    canal(SchemaSeed.OTRA_COMPANY_ID, ContactPurpose.BILLING, "suya@x.co", false));
            entityManager.clear();

            assertThat(repository.findAllByCompanyId(SchemaSeed.COMPANY_ID, 0, 20).content())
                    .extracting(CompanyContactChannel::getAddress).containsExactly("mia@x.co");
            assertThat(repository.findAllUsableByCompanyIdAndPurpose(SchemaSeed.COMPANY_ID,
                    ContactPurpose.BILLING, 0, 20).content())
                    .extracting(CompanyContactChannel::getAddress).containsExactly("mia@x.co");
        }
    }
}
