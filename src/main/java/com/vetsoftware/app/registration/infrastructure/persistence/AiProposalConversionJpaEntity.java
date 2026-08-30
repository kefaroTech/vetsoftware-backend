package com.vetsoftware.app.registration.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * El puente propuesta &rarr; empresa: <strong>de que propuesta del asistente
 * nacio este cliente</strong> (changeset 386).
 *
 * <p>
 * <strong>&#9940; Donde vive esta clase es una decision medida, y las otras dos
 * opciones estan las dos descartadas por escrito.</strong>
 *
 * <p>
 * <strong>En {@code aiproposal} no puede estar</strong>, y lo dice el propio
 * changeset que creo la tabla: el discriminador de las cuatro reglas duras de
 * BE-COV es «alguna entidad JPA de la feature pertenece a una empresa», y una
 * sola entidad asi dentro de aquella rodaja encenderia las cuatro sobre
 * <em>todos</em> sus puertos —incluidos los del asistente, que sirven a un
 * prospecto anonimo que no tiene empresa ninguna— y dejaria ademas fuera de
 * norma el {@code UPDATE} de anonimizacion por fecha, que no puede acotarse por
 * empresa porque las propuestas no la tienen.
 *
 * <p>
 * <strong>En {@code company} tampoco, y esto costo un ArchUnit rojo.</strong>
 * El mecanismo real de la regla no son las asociaciones: {@code
 * VetSoftwareConditions.perteneceAUnaEmpresa} marca la entidad en cuanto
 * encuentra <em>un campo llamado {@code companyId}</em>, tenga o no
 * {@code @ManyToOne}. Aquella rodaja estaba fuera de BE-COV por una exencion
 * <em>estructural</em> —{@code CompanyJpaEntity} es el tenant, no pertenece a
 * ningun tenant, asi que la feature no tenia ni una entidad marcada— y meter
 * esta clase alli la rompia: de golpe {@code FindCompanyUseCase.findById(Long)}
 * y {@code UpdateCompanyUseCase.execute(...)} pasaban a estar bajo la regla y
 * la incumplian, aunque los dos estan <strong>correctamente</strong>
 * protegidos: su parametro {@code id} <em>es</em> el de la empresa y lo valida
 * {@code @authz.isMyCompany(#id)}. La regla no puede saberlo porque busca un
 * parametro llamado {@code companyId}, y en el agregado de la empresa esa
 * columna no existe — el id de la fila ya es el de la empresa.
 *
 * <p>
 * <strong>Aqui, en {@code registration}, no enciende nada</strong>, y esa es la
 * razon de estar: {@code EmailVerificationTokenJpaEntity} ya declara un campo
 * {@code companyId}, asi que esta rodaja lleva dentro del alcance de BE-COV
 * desde siempre, y sus dos unicos puertos de entrada
 * —{@code RegisterUserUseCase} y {@code VerifyEmailUseCase}— no señalan ninguna
 * fila por id, de modo que la regla ni los mira. Es ademas donde vive el unico
 * escritor de esta tabla, asi que el puente deja de cruzar de rodaja. El
 * changeset 386 ya admitia este sitio por escrito: «pertenece a la rodaja
 * {@code company}, o a la de registro».
 *
 * <p>
 * <strong>Lo que NO se hizo, y no se debe hacer.</strong> Renombrar el campo
 * para que la regla no lo vea. El campo guarda el id de una empresa; esconderlo
 * del discriminador dejaria la regla en verde y el invariante abandonado, que
 * es exactamente la forma en que una invariante se pierde sin que nadie lo
 * note.
 *
 * <p>
 * <strong>{@code companyId} y {@code proposalId} son {@code Long} pelados, sin
 * {@code @ManyToOne}.</strong> La integridad la sostienen las dos claves
 * foraneas que el 386 dejo puestas; esta fila solo se escribe, nunca se navega,
 * y una asociacion no aportaria ningun dato a cambio de arrastrar dos entidades
 * mas al grafo.
 *
 * <strong>Los dos unicos de la tabla dicen dos cosas distintas.</strong>
 * {@code uq_ai_proposal_conversions_proposal}: una propuesta convierte una sola
 * vez. {@code uq_ai_proposal_conversions_company}: una empresa nace de UNA sola
 * propuesta —sin el, dos propuestas podrian atribuirse la misma alta y el
 * embudo contaria doble—.
 *
 * <p>
 * <strong>Por que la tabla importa aunque solo la lea la analitica.</strong> No
 * solo la lee la analitica: tres consultas de la purga de retencion la
 * consultan con {@code NOT EXISTS} para <em>no</em> llevarse una propuesta que
 * acabo en cliente, y su clave foranea va {@code ON DELETE RESTRICT} para que
 * esa proteccion no dependa del {@code WHERE} del job. Hasta este cambio la
 * tabla no tenia ni un solo escritor, asi que esas tres guardas protegian un
 * hecho que el sistema no registraba nunca y la purga podia borrar una
 * propuesta convertida.
 *
 * <p>
 * <strong>Sin {@code @Version} y sin {@code @SQLDelete}</strong>: se inserta en
 * el momento de la conversion y no se vuelve a tocar, asi que no hay dos
 * ediciones concurrentes que puedan pisarse. Su entrada esta en
 * {@code ENTIDADES_EXENTAS_DE_VERSION} con {@code E1_APPEND_ONLY}, que es lo
 * que convierte esa ausencia en una decision escrita y no en un descuido.
 */
@Entity
@Table(name = "ai_proposal_conversions")
public class AiProposalConversionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "proposal_id", nullable = false)
    private Long proposalId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    /** Cuando se convirtio. Sale del reloj inyectado, nunca de la base. */
    @Column(name = "converted_at", nullable = false)
    private LocalDateTime convertedAt;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected AiProposalConversionJpaEntity() {
    }

    public AiProposalConversionJpaEntity(Long proposalId, Long companyId, LocalDateTime convertedAt,
            LocalDateTime createdDate) {
        this.proposalId = proposalId;
        this.companyId = companyId;
        this.convertedAt = convertedAt;
        this.createdDate = createdDate;
    }

    public Long getId() {
        return id;
    }

    public Long getProposalId() {
        return proposalId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public LocalDateTime getConvertedAt() {
        return convertedAt;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
