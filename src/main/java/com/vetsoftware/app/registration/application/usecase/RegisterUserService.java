package com.vetsoftware.app.registration.application.usecase;

import com.vetsoftware.app.registration.application.command.RegisterUserCommand;
import com.vetsoftware.app.registration.application.dto.RegistrationDto;
import com.vetsoftware.app.registration.application.port.in.RegisterUserUseCase;
import com.vetsoftware.app.registration.application.port.out.BaseRoleProvider;
import com.vetsoftware.app.registration.application.port.out.BaseRoleProvider.BaseRoleData;
import com.vetsoftware.app.registration.application.port.out.BranchCreator;
import com.vetsoftware.app.registration.application.port.out.CaptchaVerifier;
import com.vetsoftware.app.registration.application.port.out.CompanyCreator;
import com.vetsoftware.app.registration.application.port.out.CompanyCreator.CompanyResult;
import com.vetsoftware.app.registration.application.port.out.CompanyIdentifierChecker;
import com.vetsoftware.app.registration.application.port.out.CompanyTaxProfileCreator;
import com.vetsoftware.app.registration.application.port.out.EmailVerificationTokenRepository;
import com.vetsoftware.app.registration.application.port.out.EmployeeCodeChecker;
import com.vetsoftware.app.registration.application.port.out.EmployeeCreator;
import com.vetsoftware.app.registration.application.port.out.EmployeeCreator.EmployeeResult;
import com.vetsoftware.app.registration.application.port.out.EmployeeRoleAssigner;
import com.vetsoftware.app.registration.application.port.out.InitialSubscriptionCreator;
import com.vetsoftware.app.registration.application.port.out.OwnerBranchAssigner;
import com.vetsoftware.app.registration.application.port.out.ProposalConversionRecorder;
import com.vetsoftware.app.registration.application.port.out.ProposalConverter;
import com.vetsoftware.app.registration.application.port.out.RoleCreator;
import com.vetsoftware.app.registration.application.port.out.RoleCreator.RoleResult;
import com.vetsoftware.app.registration.application.port.out.RolePermissionInitializationPort;
import com.vetsoftware.app.registration.application.port.out.VerificationEmailSender;
import com.vetsoftware.app.registration.domain.EmailVerificationToken;
import com.vetsoftware.app.registration.domain.EmployeeCodeAlreadyExistsException;
import com.vetsoftware.app.registration.domain.OwnerWithoutBranchException;
import com.vetsoftware.app.registration.domain.PlatformRoleCatalogNotConfiguredException;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Observed(name = "registration.register")
@Service
public class RegisterUserService implements RegisterUserUseCase {

    private static final String STATUS_PENDING_VERIFICATION = "PENDING_VERIFICATION";

    private final CaptchaVerifier captchaVerifier;
    private final CompanyCreator companyCreator;
    private final CompanyTaxProfileCreator companyTaxProfileCreator;
    private final BranchCreator branchCreator;
    private final EmployeeCreator employeeCreator;
    private final CompanyIdentifierChecker companyIdentifierChecker;
    private final EmployeeCodeChecker employeeCodeChecker;
    private final BaseRoleProvider baseRoleProvider;
    private final RoleCreator roleCreator;
    private final EmployeeRoleAssigner employeeRoleAssigner;
    private final OwnerBranchAssigner ownerBranchAssigner;
    private final InitialSubscriptionCreator initialSubscriptionCreator;
    private final RolePermissionInitializationPort rolePermissionInitializationPort;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final VerificationEmailSender verificationEmailSender;
    private final ProposalConverter proposalConverter;
    private final ProposalConversionRecorder proposalConversionRecorder;
    private final TransactionTemplate transactionTemplate;
    private final long verificationTtlHours;

    public RegisterUserService(CaptchaVerifier captchaVerifier, CompanyCreator companyCreator,
            CompanyTaxProfileCreator companyTaxProfileCreator, BranchCreator branchCreator,
            EmployeeCreator employeeCreator, CompanyIdentifierChecker companyIdentifierChecker,
            EmployeeCodeChecker employeeCodeChecker, BaseRoleProvider baseRoleProvider,
            RoleCreator roleCreator, EmployeeRoleAssigner employeeRoleAssigner,
            OwnerBranchAssigner ownerBranchAssigner,
            InitialSubscriptionCreator initialSubscriptionCreator,
            RolePermissionInitializationPort rolePermissionInitializationPort,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            VerificationEmailSender verificationEmailSender, ProposalConverter proposalConverter,
            ProposalConversionRecorder proposalConversionRecorder,
            TransactionTemplate transactionTemplate,
            @Value("${vetsoftware.registration.verification-token-ttl-hours:24}") long verificationTtlHours) {
        this.captchaVerifier = captchaVerifier;
        this.companyCreator = companyCreator;
        this.companyTaxProfileCreator = companyTaxProfileCreator;
        this.branchCreator = branchCreator;
        this.employeeCreator = employeeCreator;
        this.companyIdentifierChecker = companyIdentifierChecker;
        this.employeeCodeChecker = employeeCodeChecker;
        this.baseRoleProvider = baseRoleProvider;
        this.roleCreator = roleCreator;
        this.employeeRoleAssigner = employeeRoleAssigner;
        this.ownerBranchAssigner = ownerBranchAssigner;
        this.initialSubscriptionCreator = initialSubscriptionCreator;
        this.rolePermissionInitializationPort = rolePermissionInitializationPort;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.verificationEmailSender = verificationEmailSender;
        this.proposalConverter = proposalConverter;
        this.proposalConversionRecorder = proposalConversionRecorder;
        this.transactionTemplate = transactionTemplate;
        this.verificationTtlHours = verificationTtlHours;
    }

    /**
     * El captcha corre ANTES de abrir la transaccion, no dentro.
     *
     * <p>
     * Verificarlo es un POST a Google: con {@code @Transactional} sobre todo el
     * metodo, ese HTTP quedaba dentro de la transaccion del registro y retenia su
     * conexion del pool mientras esperaba respuesta. Un pico de registros —o un
     * siteverify lento— consumia conexiones de Hikari sin haber tocado todavia una
     * sola fila. Es el mismo patron de BE-02, en pequeño.
     *
     * <p>
     * Con {@link TransactionTemplate} el limite transaccional se abre recien en
     * {@link #register}, ya con el captcha resuelto. Programatico y no anotado a
     * proposito: la alternativa —extraer el cuerpo a otro bean— o se hace bien o
     * cae en la auto-invocacion, donde el proxy no aplica y no se abre transaccion
     * ninguna.
     */
    @Override
    public RegistrationDto execute(RegisterUserCommand command) {
        // 1) Anti-abuso: captcha antes de tocar la BD (no-op si el captcha esta
        // deshabilitado por
        // config).
        captchaVerifier.verify(command.recaptchaToken(), command.remoteIp());

        return transactionTemplate.execute(status -> register(command));
    }

    private RegistrationDto register(RegisterUserCommand command) {
        if (companyIdentifierChecker.exists(command.companyIdentifier())) {
            throw new IllegalArgumentException(
                    "Company identifier already in use: " + command.companyIdentifier());
        }

        CompanyResult company = companyCreator.create(command.companyName(),
                command.companyIdentifier(), command.companyAddress(),
                command.companyContactNumber(), command.cityId());

        // Toda empresa nace con un contrato, y nace con el AQUI: el alta y la creacion
        // de la suscripcion ocurren en la misma transaccion. Si el contrato no se puede
        // crear —hoy, porque el catalogo comercial todavia no esta sembrado— esto lanza
        // y revierte el alta entera. Es deliberado: una empresa sin contrato no tiene
        // company_entitlements, entra al sistema y no puede hacer nada, sin ningun
        // mensaje que lo explique.
        initialSubscriptionCreator.createInitialContract(company.id(), company.name());

        // DC-2 · el puente propuesta -> empresa, y el unico escritor que
        // `ai_proposal_conversions` ha tenido nunca.
        //
        // Va DENTRO de la transaccion del alta a proposito: tres consultas de la purga
        // de retencion de `aiproposal` descartan con NOT EXISTS las propuestas que
        // tienen fila aqui, asi que si el alta confirmara y esto no, la propuesta que
        // acaba de convertir quedaria expuesta al siguiente barrido. Y la FK va
        // ON DELETE RESTRICT para que esa proteccion no dependa del WHERE del job.
        //
        // Tolerante a lo desconocido y en ese orden: se marca la propuesta y solo si
        // existe se escribe el puente. Un token de un enlace viejo -o de una propuesta
        // que la retencion ya se llevo, que es una obligacion legal y corre sola- deja
        // el Optional vacio y el alta sigue. Perder la atribucion de un embudo es
        // analitica; negarle el registro a quien viene a pagar, no.
        proposalConverter.markConverted(command.aiProposalToken()).ifPresent(
                proposalId -> proposalConversionRecorder.record(proposalId, company.id()));

        // Identidad fiscal del emisor: toda venta (incluido el tiquete POS) la
        // requiere. Tipo NIT,
        // número =
        // identificador de la empresa, DV autocalculado, razón social = nombre; régimen
        // y correo del
        // signup.
        companyTaxProfileCreator.create(company.id(), command.documentType(), company.identifier(),
                company.name(), command.taxRegime(), command.fiscalEmail());

        // Multi-sucursal: toda empresa nace con su sede "Principal" (invariante: ≥1
        // sede por empresa).
        // Hereda ciudad/dirección/teléfono del registro, igual que el backfill de las
        // empresas
        // existentes.
        branchCreator.create("Principal", "PRINCIPAL", command.companyAddress(),
                command.companyContactNumber(), command.cityId(), company.id());

        // El usuario de acceso ES el correo del administrador (un email = una
        // veterinaria). Como
        // employee_code
        // es único global, si el correo ya está registrado se rechaza aquí. El dueño se
        // crea SIN
        // verificar (Opción B).
        String employeeCode = command.employeeEmail().trim();
        if (employeeCodeChecker.exists(employeeCode)) {
            throw new EmployeeCodeAlreadyExistsException(employeeCode);
        }
        // Se pasa la contraseña CRUDA: CreateEmployeeService la hashea una sola vez
        // (igual que el alta
        // de
        // staff por el admin). No pre-hashear aquí, o quedaría doble-hasheada y el
        // login nunca haría
        // match.
        EmployeeResult employee = employeeCreator.create(employeeCode, command.rawPassword(),
                command.employeeName(), command.employeeEmail(), company.id());

        // Se instancian TODOS los base roles en la empresa (permisos filtrados por
        // los submódulos que el CONTRATO de la empresa le concede, vía
        // company_entitlements). El dueño solo se auto-asigna a los mandatory (ADMIN);
        // el resto quedan como plantillas listas para asignar al personal.
        List<BaseRoleData> baseRoles = baseRoleProvider.findAll();
        requireOwnerRole(company.name(), baseRoles);
        for (BaseRoleData baseRole : baseRoles) {
            RoleResult role = roleCreator.create(baseRole.name(), baseRole.code(), company.id());
            rolePermissionInitializationPort.initializeForRole(role.id(), company.id(),
                    baseRole.id());
            if (baseRole.mandatory()) {
                employeeRoleAssigner.assign(employee.id(), role.id());
            }
        }

        // La otra mitad de la linea de arriba, y la que faltaba: el rol dice QUE puede
        // hacer el dueño, la sede dice DONDE. Mismo orden que InviteEmployeeService
        // —roles y despues sedes, en la misma transaccion—, y por el mismo camino que
        // usa el alta de staff, no con un INSERT propio: ver
        // SetEmployeeBranchesAdapter.
        List<Long> ownerBranchIds = ownerBranchAssigner.assignAllCompanyBranches(employee.id(),
                company.id());
        requireOwnerBranch(company.name(), ownerBranchIds);

        // 2) Token de verificación de un solo uso: se guarda el HASH, se envía el valor
        // plano por
        // correo.
        // El envío NO bloquea el registro (best-effort).
        String rawToken = VerificationTokens.generateRawToken();
        emailVerificationTokenRepository.save(EmailVerificationToken.issue(employee.id(),
                company.id(), VerificationTokens.hash(rawToken),
                LocalDateTime.now().plusHours(verificationTtlHours)));
        verificationEmailSender.send(command.employeeEmail(), command.employeeName(),
                command.companyName(), rawToken);

        return new RegistrationDto(company.id(), employee.id(), command.employeeEmail(),
                STATUS_PENDING_VERIFICATION);
    }

    /**
     * Una empresa cuyo dueño no puede recibir ningun rol no nace.
     *
     * <p>
     * <strong>Es la misma guarda que
     * {@code initialSubscriptionCreator.createInitialContract}, cuarenta lineas mas
     * arriba, en la otra mitad del mismo problema.</strong> Aquella declara que la
     * plataforma sin catalogo comercial no puede dar de alta a nadie y lanza
     * enumerando las siete piezas que hay que sembrar; esta declara lo mismo del
     * catalogo de roles. Sin ella, el bucle de abajo sobre una lista vacia es un
     * <em>no-op</em> silencioso: no se crea ningun {@code roles}, no se llama a
     * {@code initializeForRole}, {@code employeeRoleAssigner.assign} no se ejecuta
     * jamas, nada lanza y el alta devuelve <strong>201</strong>. El estado que
     * produce es palabra por palabra el que la guarda del contrato existe para
     * impedir —una cuenta que entra al sistema y no puede hacer nada, sin ningun
     * mensaje que lo explique—, y encima se investiga literalmente como un problema
     * de permisos del usuario. Razonado en el issue <b>#500</b>.
     *
     * <p>
     * <strong>Se comprueba antes de iterar, y no dentro del bucle.</strong> Es el
     * mismo criterio de
     * {@code CreateInitialSubscriptionService.requireOperableMinimum}: se falla
     * mientras no hay nada creado, no a mitad del reparto. La transaccion
     * revertiria igual —esto corre dentro del {@code TransactionTemplate} del
     * alta—, pero un fallo a mitad de reparto describe el sintoma del rol numero
     * tres en vez de la causa, que es que el catalogo no esta sembrado.
     *
     * <p>
     * <strong>Y mira los obligatorios, no solo que la lista traiga algo.</strong>
     * Una tabla con roles pero sin ninguno {@code mandatory} produce exactamente el
     * mismo desenlace —plantillas creadas, dueño con cero roles, 201—, asi que
     * comprobar unicamente {@code isEmpty()} dejaria la mitad del defecto viva.
     */
    private static void requireOwnerRole(String companyName, List<BaseRoleData> baseRoles) {
        if (baseRoles.isEmpty()) {
            throw new PlatformRoleCatalogNotConfiguredException(companyName);
        }
        if (baseRoles.stream().noneMatch(BaseRoleData::mandatory)) {
            throw new PlatformRoleCatalogNotConfiguredException(companyName,
                    baseRoles.stream().map(BaseRoleData::code).toList());
        }
    }

    /**
     * Una empresa cuyo dueño no puede operar en ninguna sede tampoco nace.
     *
     * <p>
     * <strong>Es exactamente la misma guarda que {@link #requireOwnerRole}, quince
     * lineas mas arriba, sobre la otra dimension del mismo acceso.</strong> Aquella
     * declara que el dueño sin un solo rol no es un dueño; esta declara lo mismo
     * del dueño sin una sola sede. Las dos protegen el mismo desenlace —un
     * <b>201</b> que entrega una cuenta que no puede trabajar y que se investiga
     * como un problema de permisos del usuario— y las dos lo hacen en el alta, que
     * es donde el estado se produce, en vez de dejarlo salir y fallar tres
     * pantallas despues.
     *
     * <p>
     * <strong>Lo que cambia es de donde sale el dato que se comprueba, y ese es
     * todo el punto.</strong> {@code requireOwnerRole} valida una <em>entrada</em>
     * —el catalogo que acaba de leer, antes de repartir nada— porque alli el fallo
     * posible es que falte la semilla. Aqui la entrada no sirve: la sede y el
     * empleado los acaba de crear este mismo metodo, asi que mirarlos solo
     * confirmaria lo que ya sabemos. Lo que puede fallar es la <em>union</em>, y el
     * {@code INSERT … SELECT} que la escribe no produce ninguna fila —y no lanza—
     * si alguna de las dos puntas no es de la empresa. Por eso esto valida la
     * <em>salida</em> releida de {@code employee_branches}, que es la unica fuente
     * de la que {@code Authz.currentBranchIds()} construye el conjunto del 403.
     * Comprobar lo que se pidio en vez de lo que quedo escrito seria el «verde que
     * miente» de siempre. Razonado en el issue <b>#510</b>.
     */
    private static void requireOwnerBranch(String companyName, List<Long> ownerBranchIds) {
        if (ownerBranchIds == null || ownerBranchIds.isEmpty()) {
            throw new OwnerWithoutBranchException(companyName);
        }
    }
}
