package com.vetsoftware.app.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static com.vetsoftware.app.architecture.VetSoftwareConditions.CodigoDeExencion.E1_APPEND_ONLY;
import static com.vetsoftware.app.architecture.VetSoftwareConditions.CodigoDeExencion.E2_TABLA_PUENTE;
import static com.vetsoftware.app.architecture.VetSoftwareConditions.CodigoDeExencion.E3_TOKEN;
import static com.vetsoftware.app.architecture.VetSoftwareConditions.CodigoDeExencion.E4_VISTA;
import static com.vetsoftware.app.architecture.VetSoftwareConditions.CodigoDeExencion.E5_SEMILLA;
import static com.vetsoftware.app.architecture.VetSoftwareConditions.CodigoDeExencion.E6_YA_PROTEGIDO;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import com.vetsoftware.app.architecture.VetSoftwareConditions.CodigoDeExencion;
import com.vetsoftware.app.architecture.VetSoftwareConditions.ExencionDeVersion;
import com.vetsoftware.app.shared.security.NoAuthorizationRequired;
import jakarta.persistence.Entity;
import java.util.List;
import org.hibernate.annotations.SQLDelete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

/**
 * Las reglas del {@code CLAUDE.md}, ejecutables.
 *
 * <p>
 * Hay dos clases de regla aquí. Las que el código ya cumple van tal cual:
 * cualquier violación rompe el build. Las que encontraron deuda preexistente
 * van envueltas en {@link FreezingArchRule}, que tolera lo que está registrado
 * en {@code config/archunit/violation-store} y falla ante cualquier violación
 * nueva. Congelar no es perdonar: el store se versiona, se ve en el diff y solo
 * puede encoger.
 *
 * <p>
 * Para bajar deuda basta con arreglar el código y volver a correr el test:
 * ArchUnit quita del store las violaciones resueltas. Cuando una regla llegue a
 * cero, se le quita el {@code FreezingArchRule.freeze(...)} y pasa a ser dura.
 */
@AnalyzeClasses(packages = "com.vetsoftware.app", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    // ── Reglas duras: el código ya las cumple ────────────────────────────────

    @ArchTest
    static final ArchRule DOMINIO_SIN_FRAMEWORK = noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta.persistence..",
                    "jakarta.validation..", "com.fasterxml.jackson..", "tools.jackson..")
            .because("el dominio debe poder testearse sin contexto y sin BD");

    // ignoreDependency(origen, destino): lo que se exceptua es depender DE shared,
    // no que shared dependa de otros. RF-11 traia los argumentos al reves.
    @ArchTest
    static final ArchRule SIN_CRUCE_DE_DOMINIOS = slices()
            .matching("com.vetsoftware.app.(*)..domain..").should().notDependOnEachOther()
            .ignoreDependency(alwaysTrue(), resideInAPackage("..shared.."))
            .because("cross-feature se resuelve con companion VO (XxxRef), no importando");

    /**
     * El cierre de BE-03: sin esta regla, un puerto nuevo sin gate queda alcanzable
     * por cualquier JWT válido y nada lo detecta. La única excepción admitida es
     * {@link NoAuthorizationRequired}, que obliga a escribir el motivo.
     */
    @ArchTest
    static final ArchRule PUERTOS_AUTORIZADOS = methods().that().areDeclaredInClassesThat()
            .resideInAPackage("..application.port.in..").and().areDeclaredInClassesThat()
            .areInterfaces().and().areDeclaredInClassesThat()
            .areNotAnnotatedWith(NoAuthorizationRequired.class).should()
            .beAnnotatedWith(PreAuthorize.class)
            .because("la autorizacion vive en el puerto; un puerto nuevo sin gate queda abierto");

    /**
     * BE-08, ya sin deuda. Comprueba las dos formas en que un puerto recibe la
     * empresa —dentro del command y como parámetro suelto— y ademas que la
     * referencia SpEL apunte a un parámetro real: un {@code #nombre} que no existe
     * se resuelve a null y deja el gate siempre en false.
     */
    @ArchTest
    static final ArchRule TENANT_DEFENSA_EN_PROFUNDIDAD = methods().that()
            .areDeclaredInClassesThat().resideInAPackage("..application.port.in..").and()
            .areDeclaredInClassesThat().areInterfaces().and().areDeclaredInClassesThat()
            .areNotAnnotatedWith(NoAuthorizationRequired.class)
            .should(VetSoftwareConditions.validarElTenantCuandoRecibeCompanyId())
            .because("un companyId que no viene del principal es una fuga entre empresas");

    /**
     * El cierre de BE-29, y la otra mitad de la regla anterior. Aquella cubre los
     * puertos que <em>reciben</em> un {@code companyId}; esta, los que no reciben
     * ninguno y por eso pasaban sin que nadie mirara lo que servían por debajo.
     *
     * <p>
     * La fuga concreta —{@code GET /medicaments} devolviendo el catálogo privado de
     * todas las empresas a cualquiera con {@code prescription.read}— se tapó a mano
     * al encontrarla de casualidad. Esta regla es lo que la habría encontrado sola,
     * y lo que impide que la siguiente entre por el mismo sitio.
     */
    @ArchTest
    static final ArchRule LISTADOS_SIN_EMPRESA_SOLO_SYSTEM = classes().that()
            .resideInAPackage("..application.usecase..")
            .should(VetSoftwareConditions.cerrarASystemLosListadosSinEmpresa())
            .because("un listado que no filtra por empresa devuelve filas de todas:"
                    + " solo puede servirlo un principal cross-tenant");

    /**
     * La tercera pieza del trío de tenancy, y el cierre de BE-COV.
     * {@link #TENANT_DEFENSA_EN_PROFUNDIDAD} mira los puertos que <em>reciben</em>
     * un {@code companyId}; {@link #LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}, los que no
     * reciben ninguno y devuelven <em>varias</em> filas. Entre las dos quedaba el
     * hueco por el que pasó una campaña entera: las operaciones que no reciben
     * empresa y actúan sobre <em>una</em> fila, señalada por un id que el cliente
     * escribe en la URL.
     *
     * <p>
     * La auditoría de cobertura encontró ~65 de esas en 27 de 94 features
     * —{@code DELETE /employee-roles/{id}} revocando el rol del administrador de
     * otra empresa, {@code PATCH /employees/{id}/enable} devolviéndole el acceso a
     * quien otro tenant despidió, {@code GET /laboratory-test-files/{id}/download}
     * entregando el PDF de un resultado de laboratorio ajeno— y ArchUnit pasó 13/13
     * en verde mientras todas existían. Esa es la razón de la regla: la lección de
     * BE-29 fue que tapar la fuga sin tocar el mecanismo que la dejó pasar la trae
     * de vuelta.
     */
    @ArchTest
    static final ArchRule OPERACIONES_POR_ID_SIN_EMPRESA_SOLO_SYSTEM = methods().that()
            .areDeclaredInClassesThat().resideInAPackage("..application.port.in..").and()
            .areDeclaredInClassesThat().areInterfaces().and().areDeclaredInClassesThat()
            .areNotAnnotatedWith(NoAuthorizationRequired.class)
            .should(VetSoftwareConditions.acotarPorEmpresaLasOperacionesPorId())
            .because("un id lo escribe el cliente en la URL: si la fila es de una empresa,"
                    + " el puerto tiene que recibir cual");

    /**
     * La misma familia de fugas vista desde el SQL, y su variante más grave. En un
     * {@code delete} o un {@code update} corriente hay una lectura previa que
     * valida la propiedad; en un {@code reactivate} no la hay —el servicio decide
     * si la fila existe mirando cuántas actualizó—, así que el {@code WHERE} es
     * toda la seguridad que hay.
     *
     * <p>
     * Un {@code UPDATE employee_roles SET enabled = true WHERE id = :id} reactivaba
     * una asignación de rol revocada a un empleado de otra empresa y le vaciaba la
     * caché de permisos: escalada de privilegios cross-tenant en cuatro líneas de
     * SQL, invisible para las otras dos reglas porque el puerto sí recibía su
     * {@code companyId} —y lo ignoraba.
     */
    @ArchTest
    static final ArchRule MUTACIONES_SQL_ACOTADAS_POR_EMPRESA = classes().that()
            .areAssignableTo(JpaRepository.class).and().haveSimpleNameEndingWith("JpaRepository")
            .should(VetSoftwareConditions.acotarPorEmpresaElSqlQueEscribe())
            .because("en un reactivate no hay lectura previa: el WHERE es la unica barrera");

    /**
     * La fuga que ninguna revisión humana ve, porque la anotación «se ve bien».
     * Doce {@code Update…UseCase} llevaban
     * {@code @authz.isMyCompany(#command.companyId)} y eran vulnerables igualmente:
     * esa anotación solo prueba que el atacante declara <em>su propia</em> empresa,
     * no de quién es la fila que el servicio está a punto de cargar. Con
     * {@code findById(command.id())} seguido de {@code entidad.update(…, company)},
     * el efecto no es un rechazo sino una <b>apropiación</b>: la fila de la empresa
     * B pasa a ser de A.
     *
     * <p>
     * No prohíbe {@code findById}: el camino SYSTEM legítimo es el ternario
     * {@code companyId == null ? findById(id) : findByIdAndCompanyId(id, companyId)},
     * que llama a las dos variantes. Lo que exige es que la clase llame
     * <em>también</em> a la acotada cuando el puerto la ofrece; la fuga es la clase
     * que solo conoce la variante ancha. Referencia: {@code UpdateSpaService}.
     *
     * <p>
     * <b>«La acotada» es una familia, no un nombre.</b> Los cuatro catálogos por
     * empresa declaran <em>dos</em> finders acotados con propósitos distintos:
     * {@code findByIdAndCompanyId} para leer (la fila propia o cualquiera de las
     * generales) y {@code findOwnedByIdAndCompanyId} para escribir (SOLO la propia,
     * porque editar una general la cambiaría para todos los tenants). Sus ocho
     * {@code Update…}/{@code Delete…} usan el segundo —el estrictamente más fuerte—
     * y la condición los marcaba por no llamar al primero: pedía cambiar código
     * correcto por código menos seguro. Ahora empareja por la cláusula {@code By…}
     * y el tipo de retorno, así que vale cualquier hermana que cargue lo mismo por
     * el mismo criterio con la empresa encima.
     *
     * <p>
     * <b>También está exento el servicio sin autorización de empleado.</b> Un
     * puerto anotado {@code @NoAuthorizationRequired} declara por escrito que su
     * autorización no es un JWT —el token de un solo uso del flujo público de
     * verificación, la firma HMAC del webhook del proveedor—, así que no hay
     * principal del que sacar una empresa; en el webhook la empresa es incluso una
     * <em>salida</em> de la búsqueda.
     * {@link #OPERACIONES_POR_ID_SIN_EMPRESA_SOLO_SYSTEM} ya lo trataba como
     * exención en su {@code .that()}: no mirarlo aquí era una asimetría entre dos
     * reglas de la misma familia, no una decisión.
     */
    @ArchTest
    static final ArchRule CARGA_POR_ID_ACOTADA_POR_EMPRESA = classes().that()
            .resideInAPackage("..application.usecase..")
            .should(VetSoftwareConditions.cargarPorIdAcotandoLaEmpresa())
            .because("cargar por id sin acotar y reescribir la empresa no rechaza: se apropia"
                    + " de la fila ajena");

    /**
     * La cuarta forma del defecto, y la que sobrevive a las otras tres. Con la
     * carga propia ya acotada, un {@code UpdateSurgeryService} no puede apropiarse
     * de una cirugía ajena; lo que sí puede es <b>reapuntar la suya a una entidad
     * de otro tenant</b>, porque resuelve el animal con
     * {@code animalQueryPort.findById(command.animalId())} y ese puerto no filtra
     * nada: una cirugía de mi empresa colgada del animal de la vecina, con su
     * historia clínica contaminada. Afecta a {@code laboratorytest},
     * {@code surgery}, {@code diagnosticimaging} y {@code daycare}; {@code spa},
     * {@code prescription} y {@code consultation} son el modelo.
     *
     * <p>
     * Es regla aparte y no una ampliación de
     * {@link #CARGA_POR_ID_ACOTADA_POR_EMPRESA} porque aquella ya mira estos
     * puertos —su filtro es el paquete, no el nombre del tipo— y no le sirve de
     * nada: el problema es que <b>no declaran</b> ninguna variante acotada que
     * exigirles llamar. «Declárala» y «llámala» son dos afirmaciones distintas y el
     * mensaje de fallo tiene que decir cuál es. Las dos quedan disjuntas por
     * construcción y se comprobó que no comparten ni un punto.
     *
     * <p>
     * <b>Solo mira servicios que ya tienen el {@code companyId} en la mano</b> —los
     * que llaman a alguna variante acotada en otra parte— y solo las llamadas que
     * <em>resuelven la referencia</em> ({@code find…} que devuelve un
     * {@code XxxRef}), no los predicados del mismo puerto ({@code isOpen},
     * {@code lockForUpdate}, {@code outstandingAmount}). Sin esos dos cortes la
     * condición marcaba 89 puntos en vez de 38, casi todos {@code Create…Service}:
     * ahí el defecto es el mismo pero la regla no puede distinguir un id que llega
     * del cliente de uno que llega del principal.
     *
     * <p>
     * <b>El falso positivo que se dejó visible, y cómo se cerró sin enumerar
     * puertos.</b> Siete servicios resolvían {@code EmployeeQueryPort.findById}
     * para guardar el <em>empleado autenticado</em> como {@code createdBy} /
     * {@code processedBy} / {@code suspendedBy}. Ahí el id viene del principal y no
     * hay nada que acotar, pero la primera versión de la regla los dejó dentro a
     * propósito: excluirlos exigía enumerar un puerto por su nombre —lo que este
     * fichero evita en todas las demás reglas— y eso taparía también el día que un
     * {@code employeeId} llegue de verdad en el request.
     *
     * <p>
     * La señal que los separa no es el nombre del puerto sino la <b>ausencia del
     * nombre de recurso</b>: si la referencia es a {@code EmployeeJpaEntity} y
     * ningún command ni parámetro del servicio declara un {@code employeeId}, el
     * servicio no tiene por dónde recibir «sobre qué empleado actúo». La condición
     * lo comprueba en {@code nombraLaReferenciaComoAutor}, y el día que alguien
     * añada ese campo al command la regla vuelve a marcarlo — la preocupación
     * original queda intacta. Discrimina de verdad: en
     * {@code CreateOpenAccountService} exime el {@code createdById} y deja rojo el
     * {@code OwnerQueryPort}, porque su command sí declara {@code ownerId}.
     */
    @ArchTest
    static final ArchRule REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA = classes().that()
            .resideInAPackage("..application.usecase..")
            .should(VetSoftwareConditions.acotarPorEmpresaLasReferenciasCrossFeature())
            .because("una referencia sin acotar no se apropia de la fila ajena: cuelga la propia"
                    + " de un padre de otro tenant");

    /**
     * El cierre de BE-21, y la regla sin la cual el hallazgo vuelve a crecer.
     *
     * <p>
     * «Una lista con su total y su número de página» es infraestructura de
     * colección, no semántica de negocio: por eso el vertical slicing —que sí
     * justifica duplicar tipos de dominio— no la cubre. Cuando cada feature declaró
     * la suya, la cuenta pasó de 12 a 36 en una semana, y no por descuido: paginar
     * una feature nueva copiando el {@code PageResult} del vecino era literalmente
     * lo que pedía la regla de no compartir DTOs.
     *
     * <p>
     * Quedan dos, uno a cada lado de la frontera y ambos únicos:
     * {@code shared.pagination.PageResult} para dentro e
     * {@code infrastructure.web.PageResponse} para el JSON. Esta regla es lo que
     * impide que entre el tercero con la próxima feature paginada.
     */
    @ArchTest
    static final ArchRule PAGINACION_CON_UN_SOLO_CONTRATO = classes()
            .that(VetSoftwareConditions.declaranElConceptoDePagina()).should()
            .resideInAnyPackage("com.vetsoftware.app.shared.pagination",
                    "com.vetsoftware.app.infrastructure.web")
            .because("la pagina es un tipo sin semantica de negocio: uno por lado"
                    + " de la frontera, no uno por feature");

    /**
     * La otra mitad de {@link #PAGINACION_CON_UN_SOLO_CONTRATO}: unificar el tipo
     * no sirve de nada si cada adaptador sigue traduciendo a mano lo que pide el
     * cliente. Ahí la copia no era solo ruido —tres adaptadores pasaban los
     * parámetros crudos a {@code PageRequest.of}, sin normalizar el índice ni topar
     * el tamaño, y un {@code ?pageSize=100000} deshacía el trabajo de paginar.
     */
    @ArchTest
    static final ArchRule PAGINA_ACOTADA_EN_UN_SOLO_SITIO = noClasses().that()
            .resideOutsideOfPackage("com.vetsoftware.app.shared.pagination").should()
            .callMethodWhere(VetSoftwareConditions.esUnPageRequestSinAcotar())
            .because("el tope de filas por pagina se decide en Pages.request(), no por adaptador");

    /**
     * Lo que mantiene a Spring Data fuera de {@code application}. El kernel tiene
     * dos piezas y solo una conoce el framework: {@code PageResult} lo cruza todo,
     * {@code Pages} se queda en el adaptador. Sin esta regla, un caso de uso que
     * llame a {@code Pages.request(...)} arrastra {@code PageRequest} a la capa que
     * el CLAUDE.md exige poder probar sin contexto — y no lo detectaría ninguna de
     * las otras dos, porque la llamada a {@code PageRequest.of} ocurre dentro del
     * kernel.
     */
    @ArchTest
    static final ArchRule PUENTE_DE_PAGINACION_SOLO_EN_PERSISTENCIA = noClasses().that()
            .resideOutsideOfPackages("..infrastructure.persistence..",
                    "com.vetsoftware.app.shared.pagination")
            .should().dependOnClassesThat()
            .haveFullyQualifiedName("com.vetsoftware.app.shared.pagination.Pages")
            .because("Pages habla Spring Data: application se queda con PageResult");

    /**
     * Dura: ya no queda ninguna. La búsqueda se detiene en los saltos
     * {@code @Async}, así que el envío de correos —asíncrono a propósito— no cuenta
     * como HTTP dentro de la transacción.
     */
    @ArchTest
    static final ArchRule SIN_IO_EXTERNO_EN_TRANSACCION = noMethods().that()
            .areAnnotatedWith(Transactional.class)
            .should(VetSoftwareConditions.alcanzarUnClienteHttp(RestClient.class))
            .because("una llamada HTTP retiene la conexion y los locks hasta el commit");

    /**
     * El cierre de la incidencia #135. Un {@code @RequestBody} sin {@code @Valid}
     * delante no se valida: el binder de Spring no dispara el validador, así que
     * las restricciones que el DTO declara están escritas y <b>no se evalúan
     * nunca</b>.
     *
     * <p>
     * Lo que hace al defecto invisible es que las tres señales que mira un humano
     * dicen que la validación existe. El compilador calla; la anotación se lee
     * perfecta en el diff; y el {@code api/openapi.json} sigue anunciando el
     * {@code maxLength} al front, porque springdoc lo deriva del {@code @Size} con
     * {@code @Valid} o sin él. Solo se nota en ejecución, y de una forma que parece
     * otro problema.
     *
     * <p>
     * El caso real: {@code PATCH /appointments/{id}/cancel} recibía
     * {@code CancelAppointmentRequest(@Size(max = 300) String reason)} sin
     * {@code @Valid}. A la base no entraba basura —{@code Appointment} vuelve a
     * medir la longitud y la columna es de 300—, así que el daño no era corrupción
     * sino <b>la forma del error</b>: en lugar del error de campo sobre
     * {@code reason} que el front sabe pintar bajo el textarea, salía una excepción
     * de dominio con otro {@code errorCode} y otra forma, y quien cancelaba la cita
     * leía un mensaje genérico sin saber qué corregir. El endpoint de la línea de
     * arriba, {@code changeStatus}, sí lo llevaba: era una omisión, no una
     * decisión.
     *
     * <p>
     * <b>Nace dura y no congelada</b> porque el predicado mira el tipo del
     * parámetro y no su nombre: de los tres {@code @RequestBody} sin {@code @Valid}
     * que quedaban en {@code src/main}, dos no declaran ninguna restricción —el
     * {@code String} crudo del webhook de la DIAN y el {@code RefreshTokenRequest},
     * que dejó de exigir su campo por escrito— y quedan legítimamente fuera. Con el
     * {@code @Valid} de {@code cancel} puesto, la cuenta queda en cero.
     *
     * <p>
     * <b>No lleva {@code .that(...)}</b>: el filtro es la propia condición, que
     * ignora todo método sin un parámetro {@code @RequestBody}. Acotarla a
     * {@code ..infrastructure.web..} habría dejado el hueco de un controller
     * escrito fuera de su paquete, que es justo el caso que nadie revisaría.
     */
    @ArchTest
    static final ArchRule CUERPO_CON_RESTRICCIONES_SE_VALIDA = methods()
            .should(VetSoftwareConditions.validarElCuerpoQueDeclaraRestricciones())
            .because("un @Size sin @Valid delante no lo evalua nadie: la peticion entra entera"
                    + " y el error sale del dominio con otra forma y otro errorCode,"
                    + " no como el error de campo que el front sabe pintar");

    /**
     * El primer anti-patrón de autorización del {@code CLAUDE.md} —«no aceptar
     * {@code companyId} en un {@code XxxRequest} para recursos scoped al usuario»—,
     * que hasta hoy no comprobaba ninguna de las veinticinco reglas de este
     * fichero. Se verificaron una por una: la familia «por id»
     * ({@link #TENANT_DEFENSA_EN_PROFUNDIDAD},
     * {@link #OPERACIONES_POR_ID_SIN_EMPRESA_SOLO_SYSTEM},
     * {@link #CARGA_POR_ID_ACOTADA_POR_EMPRESA},
     * {@link #MUTACIONES_SQL_ACOTADAS_POR_EMPRESA}) mira {@code port/in},
     * {@code application.usecase} y los {@code JpaRepository}, y la única que toca
     * un {@code @RequestBody} —{@link #CUERPO_CON_RESTRICCIONES_SE_VALIDA}— solo
     * comprueba que lleve {@code @Valid}: los componentes del tipo le dan igual.
     * <b>Ninguna miraba la capa web de entrada</b>, que es por donde el dato entra.
     *
     * <p>
     * <b>El ataque, que es lo que hay que entender si esto se pone rojo dentro de
     * un año.</b> Alguien añade {@code Long companyId} a
     * {@code CreateSurgeryRequest} para «cuadrar» con lo que el front ya mandaba,
     * el controller lo pasa al command, y un empleado de la empresa A hace
     * {@code POST /surgeries} con el identificador de la empresa B: <b>cirugía
     * creada en la historia clínica de otro tenant</b>. Lo que hace al defecto peor
     * que un olvido de gate es que el gate <em>está puesto y se lee bien</em>:
     * {@code @PreAuthorize("@authz.isMyCompany(#command.companyId)")} compara el
     * número que el atacante acaba de escribir <b>contra sí mismo</b> y devuelve
     * {@code true} siempre. Las tres señales que mira un revisor —hay
     * {@code @PreAuthorize}, nombra {@code companyId}, el SpEL apunta a un
     * parámetro real— dicen que la autorización existe. La empresa se toma del
     * principal, con {@code authz.currentCompanyId()}, y nunca del cuerpo.
     *
     * <p>
     * <b>Nace dura y en cero</b>, sin {@code freeze(...)}, con el criterio normal
     * del repositorio: el censo del árbol da cero. De los 162 ficheros de
     * {@code ..infrastructure.web.request..} ninguno declara un componente
     * {@code companyId}; las dos únicas apariciones textuales de la cadena son
     * legítimas y la regla las deja fuera <em>por construcción</em>, no por lista
     * —{@code RegisterUserRequest.companyIdentifier}, el NIT del registro público,
     * porque compara por nombre exacto; y una mención dentro de un comentario de
     * {@code RegisterPosSaleRequest}, que ArchUnit no ve porque lee bytecode—.
     *
     * <p>
     * <b>La llave es el parámetro, no el paquete</b>, igual que en
     * {@link #CUERPO_CON_RESTRICCIONES_SE_VALIDA} y por la misma razón: acotarla a
     * {@code ..infrastructure.web..} dejaría pasar el controller escrito fuera de
     * su paquete, que es justo el que nadie revisaría. Y desciende a los tipos
     * anidados y a los argumentos genéricos de las colecciones, porque un
     * {@code companyId} dentro de una línea de detalle de
     * {@code List<LineaRequest>} es el mismo defecto con menos visibilidad.
     *
     * <p>
     * <b>No lleva válvula de escape, y es una decisión, no un descuido.</b> El
     * repositorio ya tiene el patrón —{@link NoAuthorizationRequired} con
     * {@code reason()} obligatorio— y aquí se descartó por tres razones. La
     * primera: <em>la ruta legítima ya existe y es más fuerte</em>. El endpoint de
     * administración global que necesita nombrar una empresa la recibe en la URL
     * ({@code /companies/{id}/…}), que es lo que hace hoy todo el árbol, y ahí la
     * cubren {@link #TENANT_DEFENSA_EN_PROFUNDIDAD},
     * {@link #OPERACIONES_POR_ID_SIN_EMPRESA_SOLO_SYSTEM} y
     * {@link #GATE_COHERENTE_EN_FEATURE_DE_SYSTEM}, que le exigen
     * {@code hasRole('SYSTEM')} o validación del tenant. Una exención en el cuerpo
     * sería un camino por el que esa empresa <em>esquiva a las tres a la vez</em>,
     * sin que ninguna pueda verlo. La segunda: la anotación iría sobre el
     * {@code XxxRequest}, o sea sobre el artefacto cuya existencia <em>es</em> el
     * defecto, y sería exactamente lo que añadiría el PR que introduce la fuga;
     * {@code @NoAuthorizationRequired} funciona porque vive en un puerto, donde el
     * revisor ya va a buscar el gate. La tercera: el censo es cero, así que no hay
     * nada que indultar y una válvula estrenada por su primer usuario no es una
     * excepción, es la derogación de la regla.
     *
     * <p>
     * <b>Qué hacer si algún día hiciera falta de verdad</b>, para que nadie la
     * resuelva borrando esta regla: mover la empresa a un {@code @PathVariable} y
     * dejar que la cubra la familia «por id». Si ese camino no sirviera para un
     * caso concreto, la exención se escribe como las de BE-26 —lista enumerada en
     * <em>este</em> fichero, con su código y su motivo al lado del nombre, visible
     * en el diff del PR— y jamás como una anotación por DTO, que es invisible en
     * cuanto hay más de una.
     *
     * <p>
     * <b>Lo que esta regla no ve.</b> Solo mira el nombre {@code companyId}: un
     * {@code empresaId}, un {@code tenantId} o un {@code companyCode} pasarían sin
     * que nadie los toque. Es a propósito —el nombre es la convención única del
     * árbol y ampliarlo por sinónimos abre la puerta a falsos positivos como el
     * {@code companyIdentifier} de arriba—, pero queda escrito para que el día que
     * aparezca el primer sinónimo se sepa que hay que sumarlo aquí, no descubrirlo
     * en producción.
     */
    @ArchTest
    static final ArchRule EMPRESA_NO_VIAJA_EN_EL_CUERPO = methods()
            .should(VetSoftwareConditions.noRecibirLaEmpresaEnElCuerpo())
            .because("un companyId que el cliente escribe en el JSON convierte"
                    + " @authz.isMyCompany(#command.companyId) en una comparacion del numero"
                    + " consigo mismo: el gate se lee perfecto, siempre da true, y un empleado"
                    + " de una empresa crea la fila en la historia clinica de otra");

    // ── BE-26: quién no lleva @Version, y por qué ────────────────────────────

    /**
     * Las entidades que <b>no</b> llevan bloqueo optimista, con el código que lo
     * justifica y el motivo escrito al lado del nombre. No es una lista de
     * pendientes: es la decisión, y por eso vive aquí y no en un comentario dentro
     * de cada entidad —así el diff de un PR enseña de un vistazo a quién se le está
     * perdonando y con qué argumento.
     *
     * <p>
     * <b>La cuenta cierra al dígito, y esa es la prueba de que la lista es
     * exhaustiva y no una muestra</b>: 104 clases {@code @Entity} = 71 versionadas
     * (las 16 que ya lo estaban + 55 de la campaña de BE-26) + estas 33 exentas.
     * Cualquier entidad nueva desequilibra la suma y
     * {@link #ENTIDADES_CON_BLOQUEO_OPTIMISTA} la caza el mismo día.
     *
     * <p>
     * <b>Cómo se añade una entrada.</b> Nunca «para que pase el test». El código
     * dice qué clase de razón se está invocando ({@link CodigoDeExencion}) y el
     * motivo dice por qué esa razón aplica <em>a esta entidad</em>: qué la escribe,
     * qué la reescribe y quién gana si dos operadores llegan a la vez. Un motivo
     * que valdría igual para cualquier otra fila no es un motivo.
     *
     * <p>
     * <b>Cómo se quita.</b> Sola no se va: ponerle {@code @Version} a una entidad
     * exenta y olvidar su línea rompe {@link #EXENCIONES_DE_VERSION_AL_DIA}, que
     * está precisamente para eso.
     */
    private static final List<ExencionDeVersion> ENTIDADES_EXENTAS_DE_VERSION = List.of(

            // E1 — se insertan y no se vuelven a modificar.
            exenta("WeightRecordJpaEntity", E1_APPEND_ONLY,
                    "serie temporal de pesos: el dominio no expone ningún update()"),
            exenta("CashMovementJpaEntity", E1_APPEND_ONLY,
                    "asiento de libro mayor: se escribe una vez y no se corrige"),
            exenta("CashSessionCountJpaEntity", E1_APPEND_ONLY,
                    "conteo de arqueo: se inserta al cerrar la sesión y ahí acaba"),
            exenta("StockMovementJpaEntity", E1_APPEND_ONLY,
                    "asiento del kardex: la reversa emite un movimiento inverso nuevo"),
            exenta("SaleInventoryAllocationJpaEntity", E1_APPEND_ONLY,
                    "su repositorio no declara ninguna escritura sobre fila existente"),
            exenta("InventoryCountJpaEntity", E1_APPEND_ONLY,
                    "la sesión se construye entera y se guarda una vez; campos final"),
            exenta("InventoryCountLineJpaEntity", E1_APPEND_ONLY,
                    "línea de esa misma sesión, escrita en el mismo insert; campos final"),
            exenta("GoodsReceiptLineJpaEntity", E1_APPEND_ONLY,
                    "línea inmutable, reemplazada en bloque con orphanRemoval;"
                            + " el bloqueo vive en la cabecera goods_receipts, ya versionada"),
            exenta("SupplierInvoicePaymentJpaEntity", E1_APPEND_ONLY,
                    "pago inmutable; la cabecera supplier_invoices ya va versionada"),
            exenta("ProductBundleItemJpaEntity", E1_APPEND_ONLY,
                    "patrón borrar-todas-y-reinsertar; el bloqueo vive en product_bundles"),
            exenta("ElectronicDocumentLineJpaEntity", E1_APPEND_ONLY,
                    "documento electrónico ya emitido: inmutable por ley"),
            exenta("ElectronicDocumentPaymentJpaEntity", E1_APPEND_ONLY,
                    "documento electrónico ya emitido: inmutable por ley"),
            exenta("ElectronicDocumentTransmissionJpaEntity", E1_APPEND_ONLY,
                    "cada intento de transmisión es una fila nueva con attempt = count + 1"),
            exenta("LaboratoryTestFileJpaEntity", E1_APPEND_ONLY,
                    "adjunto: el dominio tiene los diez campos final y ningún mutador, no hay"
                            + " update ni reactivación, y el borrado es físico junto al objeto"
                            + " en S3 (por eso ni siquiera lleva @SQLDelete)"),

            // E2 — relación N:M pura: solo dos FK, insert + delete, par único en BD.
            exenta("RolePermissionJpaEntity", E2_TABLA_PUENTE,
                    "solo dos FK y ningún campo propio mutable; par único en BD"),
            exenta("BaseRolePermissionJpaEntity", E2_TABLA_PUENTE,
                    "solo dos FK y ningún campo propio mutable; par único en BD"),
            exenta("SystemUserPermissionJpaEntity", E2_TABLA_PUENTE,
                    "solo dos FK y ningún campo propio mutable; par único en BD"),
            exenta("EmployeeRoleJpaEntity", E2_TABLA_PUENTE,
                    "solo dos FK y ningún campo propio mutable; par único en BD"),
            exenta("EmployeeBranchJpaEntity", E2_TABLA_PUENTE,
                    "solo dos FK y ningún campo propio mutable; par único en BD"),
            exenta("MembershipSubModuleJpaEntity", E2_TABLA_PUENTE,
                    "solo dos FK y ningún campo propio mutable; par único en BD"),
            exenta("CompanyTaxProfileResponsibilityJpaEntity", E2_TABLA_PUENTE,
                    "responsabilidades DIAN del perfil, reemplazadas en bloque"),

            // E3 — un solo uso o vida corta: se emite, se consume y caduca.
            exenta("RefreshTokenJpaEntity", E3_TOKEN,
                    "token de sesión: se emite, se rota y se revoca; nadie lo edita"),
            exenta("PasswordResetTokenJpaEntity", E3_TOKEN,
                    "token de un solo uso con caducidad corta"),
            exenta("EmailVerificationTokenJpaEntity", E3_TOKEN,
                    "token de un solo uso con caducidad corta"),

            // E4 — no hay fila que actualizar.
            exenta("ClinicalEventViewJpaEntity", E4_VISTA,
                    "entidad @Immutable sobre la vista v_clinical_event: no se escribe"),

            // E5 — dato de referencia sembrado. Ojo: el motivo NO es que sean
            // inmutables —la versión anterior de esta justificación decía eso y era
            // falsa—, sino que no hay pantalla desde la que dos operadores editen la
            // misma fila a la vez.
            exenta("PermissionJpaEntity", E5_SEMILLA,
                    "catálogo sembrado: ningún front ofrece pantalla para editarlo"),
            exenta("SystemPermissionJpaEntity", E5_SEMILLA,
                    "catálogo sembrado: ningún front ofrece pantalla para editarlo"),
            exenta("CountryJpaEntity", E5_SEMILLA,
                    "geografía DIVIPOLA sembrada: ningún front la edita en concurrencia"),
            exenta("StateJpaEntity", E5_SEMILLA,
                    "geografía DIVIPOLA sembrada: ningún front la edita en concurrencia"),
            exenta("CityJpaEntity", E5_SEMILLA,
                    "geografía DIVIPOLA sembrada: ningún front la edita en concurrencia"),
            exenta("UnitMeasureCatalogJpaEntity", E5_SEMILLA,
                    "catálogo de unidades DIAN sembrado; no tiene controller siquiera"),

            // E6 — la concurrencia ya la resuelve algo más fuerte, nombrado aquí.
            exenta("PurchaseOrderLineJpaEntity", E6_YA_PROTEGIDO,
                    "quantityReceived sí muta, pero receiveLine/revertLine van siempre"
                            + " seguidos de un touch(updatedBy) sobre la cabecera"
                            + " purchase_orders, ya versionada, que fuerza el incremento"),
            exenta("NumberingResolutionJpaEntity", E6_YA_PROTEGIDO,
                    "el consecutivo fiscal se serializa con SELECT … FOR UPDATE en"
                            + " lockActiveForUpdate, dentro de un REQUIRES_NEW; añadirle"
                            + " @Version arriesgaría un 409 en mitad de una emisión"));

    private static ExencionDeVersion exenta(String entidad, CodigoDeExencion codigo,
            String motivo) {
        return new ExencionDeVersion(entidad, codigo, motivo);
    }

    /**
     * El cierre de BE-26. La auditoría encontró 104 {@code @Entity} y 16 con
     * {@code @Version}, y su conclusión no fue «póngaselo a las 88»: fue que la
     * ausencia de bloqueo optimista tiene que ser una <b>decisión escrita</b>. En
     * una entidad que dos operadores editan a la vez, sin {@code @Version} el
     * segundo {@code UPDATE} pisa al primero y nadie se entera —no hay excepción,
     * no hay log, solo un dato que desapareció—; en una tabla puente o en un
     * asiento contable, ponerlo es ruido y un 409 que el usuario no sabe resolver.
     * Las dos cosas son ciertas, y la que hacía falta no era el criterio sino el
     * registro de qué criterio se aplicó a cada una.
     *
     * <p>
     * De ahí la forma de la regla: o la entidad declara un campo {@code @Version},
     * o aparece en {@link #ENTIDADES_EXENTAS_DE_VERSION} con uno de los seis
     * códigos y el motivo <em>al lado del nombre</em>, no en un comentario suelto.
     * Así el diff de un PR enseña a quién se le está perdonando y por qué, que es
     * exactamente lo que no existía cuando el hallazgo se abrió.
     *
     * @see #EXENCIONES_DE_VERSION_AL_DIA para lo que impide que la lista se pudra
     */
    @ArchTest
    static final ArchRule ENTIDADES_CON_BLOQUEO_OPTIMISTA = classes().that()
            .areAnnotatedWith(Entity.class)
            .should(VetSoftwareConditions
                    .declararBloqueoOptimistaOEstarExenta(ENTIDADES_EXENTAS_DE_VERSION))
            .because("sin @Version dos ediciones simultáneas se pisan sin ruido:"
                    + " si no lo lleva, que sea porque alguien lo escribió");

    /**
     * La trampa de los dos parámetros, y la razón por la que esta regla existe
     * aparte de la anterior. En cuanto una entidad lleva {@code @Version},
     * Hibernate liga <b>dos</b> parámetros al SQL de su {@code @SQLDelete} —primero
     * el {@code id}, después la {@code version}—, así que el
     * {@code UPDATE … WHERE id = ?} que era correcto ayer se convierte hoy en un
     * borrado lógico roto en tiempo de ejecución. Ninguna revisión humana lo ve: la
     * anotación se lee perfecta y el error solo aparece al borrar.
     *
     * <p>
     * Es un riesgo real y no teórico: 62 entidades borran en lógico con
     * {@code @SQLDelete}, y versionar cualquiera de ellas sin tocar su SQL arma la
     * bomba. Las 16 que ya estaban versionadas lo reflejan —llevan
     * {@code AND version = ?}—, y el resto se acaba de versionar en bloque; esta
     * regla es lo que impide que la próxima entre sin él.
     *
     * <p>
     * El predicado mira la condición dentro del {@code WHERE}, no una subcadena
     * sobre el SQL entero: {@code employees} y {@code system_users} llevan
     * {@code auth_version = auth_version + 1} en el {@code SET} —invalidación de
     * sesión, nada que ver con el bloqueo optimista— y una comprobación ingenua las
     * daría por buenas sin comprobar nada. Ver
     * {@code VetSoftwareConditions.ligarLaVersionEnElBorradoLogico()}.
     */
    @ArchTest
    static final ArchRule BORRADO_LOGICO_RESPETA_LA_VERSION = classes().that()
            .areAnnotatedWith(Entity.class).and().areAnnotatedWith(SQLDelete.class)
            .should(VetSoftwareConditions.ligarLaVersionEnElBorradoLogico())
            .because("con @Version, Hibernate liga id y version al @SQLDelete:"
                    + " un WHERE con un solo ? deja el borrado logico roto en ejecucion");

    /**
     * La tercera pieza de BE-26, y la que decide si las otras dos siguen valiendo
     * algo dentro de un año. Una lista de exenciones que nadie limpia acaba
     * mintiendo por escrito, y de las dos formas de podrirse la peor es silenciosa:
     * si alguien versiona una entidad exenta y no borra su línea, el repositorio
     * sigue afirmando que esa entidad no necesita bloqueo optimista mientras el
     * código dice lo contrario. La otra —una entrada que ya no corresponde a
     * ninguna clase— es solo ruido, pero ruido que enseña a no leer la lista.
     *
     * <p>
     * Va como {@code ArchRule} y no como {@code @Test} suelto porque necesita
     * exactamente lo mismo que las otras dos: el censo de {@code @Entity} del árbol
     * de producción que ya importa este {@code @AnalyzeClasses}. Lo que cambia es
     * el sujeto —se juzga la lista, no cada entidad—, y por eso la condición
     * levanta el censo en {@code init(…)} y emite una violación por entrada podrida
     * en {@code finish(…)}.
     */
    @ArchTest
    static final ArchRule EXENCIONES_DE_VERSION_AL_DIA = classes().that()
            .areAnnotatedWith(Entity.class)
            .should(VetSoftwareConditions
                    .mantenerLaListaDeExencionesAlDia(ENTIDADES_EXENTAS_DE_VERSION))
            .because("una exencion que nadie limpia deja de ser una decision y pasa a ser"
                    + " una mentira firmada");

    // ── #53: la puerta de atrás del bloqueo optimista ────────────────────────

    /**
     * El cierre de la incidencia #53, y la grieta que BE-26 dejó abierta sin
     * saberlo. {@code @Version} protege <b>un</b> camino: el ciclo
     * leer-modificar-guardar de una entidad gestionada, donde Hibernate compara la
     * versión en el {@code WHERE} y la incrementa en el {@code SET}. Una
     * {@code @Query} de {@code UPDATE} no pasa por ahí: va directa a la base, ni
     * comprueba ni incrementa nada, y deja la fila modificada con su versión
     * intacta. El {@code save} concurrente que llegue después con la versión vieja
     * <b>casa igual</b> y pisa el cambio —sin excepción, sin log y sin 409—, que es
     * exactamente el fallo silencioso que las tres reglas de BE-26 creían haber
     * cerrado.
     *
     * <p>
     * <b>No es un riesgo teórico: ya cobró dos veces.</b> La revocación de sesión
     * que se deshacía sola (#54, arreglado) y la suspensión de programaciones de
     * medicación que se perdía. En los dos casos el {@code UPDATE} masivo hizo su
     * trabajo y un {@code save} que venía de una lectura anterior lo deshizo, y en
     * los dos el rastro en los logs fue ninguno. La campaña que acompaña a esta
     * regla añadió {@code version = version + 1} al {@code SET} de las ~70
     * consultas afectadas; la regla es lo que impide que entre la 71.
     *
     * <p>
     * <b>La versión va en el {@code SET}, nunca en el {@code WHERE}</b> —al revés
     * que en {@link #BORRADO_LOGICO_RESPETA_LA_VERSION}, y la diferencia es toda la
     * intención—. Aquel es el SQL <em>de</em> Hibernate para una entidad concreta
     * que alguien acaba de leer, y ahí la versión es el candado. Este es un
     * {@code UPDATE} de conjunto que nadie leyó antes: condicionarlo por versión
     * solo conseguiría que actualizara cero filas y que el servicio lo interpretara
     * como «no existe». Lo que hace falta no es rechazar la escritura, es
     * <em>invalidar</em> la copia que otros tengan en la mano. Por eso la regla
     * denuncia <b>las dos</b> mitades: la versión que falta en el {@code SET} y la
     * versión que sobra en el {@code WHERE}. La segunda no se da hoy en ninguna
     * consulta, y está vigilada precisamente porque es el error natural de quien
     * viene de leer {@code BORRADO_LOGICO_RESPETA_LA_VERSION} y aplica su receta
     * aquí.
     *
     * <p>
     * <b>Qué mira y qué no.</b> El mapa tabla → ¿versionada? sale del censo de
     * {@code @Entity} —{@code @Table(name = …)} para el SQL nativo, nombre simple
     * para el JPQL—, nunca de una lista literal: BE-26 ya enseñó lo que le pasa a
     * una lista que nadie mantiene, y aquí bastaría versionar una entidad para que
     * su tabla saliera de la vigilancia en silencio. Los {@code DELETE} quedan
     * fuera a propósito: se llevan la fila, no hay versión que proteger. Ver
     * {@code VetSoftwareConditions.moverLaVersionEnElUpdateMasivo()} para el
     * troceado del {@code SET} y la resolución de alias, que es lo que separa
     * {@code auth_version} de {@code version} y {@code role_permissions} de los
     * {@code roles} con los que hace {@code JOIN}.
     *
     * <p>
     * <b>Hoy no hay ninguna {@code @Query} perdonada, y por eso no hay lista de
     * exenciones.</b> No es un descuido: es que la única escritura masiva exenta
     * del repositorio no lleva anotación —ver la limitación de abajo—. Si algún día
     * hiciera falta perdonar una {@code @Query}, el código sería
     * {@code E6_YA_PROTEGIDO} del vocabulario de BE-26 —la concurrencia la resuelve
     * un mecanismo más fuerte, nombrado en el motivo— y la exención iría escrita
     * con el mismo formato que {@code ENTIDADES_EXENTAS_DE_VERSION}: entidad,
     * código y motivo al lado del nombre.
     *
     * <p>
     * <b>Limitación conocida, con nombre y apellido: el SQL crudo por
     * {@code JdbcTemplate}.</b> Esta regla escanea {@code @Query} y solo
     * {@code @Query}. {@code JdbcDianJobLeasePort.leaseByDianStatus} hace
     * {@code UPDATE electronic_documents SET dian_leased_until = ?} con
     * {@code jdbcTemplate.update(…)}, sin anotación ninguna, sobre una tabla que
     * <b>sí</b> va versionada — y la regla no lo ve ni lo verá. Ese caso concreto
     * es además una exención legítima: las filas vienen de un
     * {@code SELECT … FOR UPDATE SKIP LOCKED} en la misma transacción, o sea que ya
     * están serializadas por bloqueo pesimista y el optimista sobra.
     *
     * <p>
     * <b>¿Se puede cubrir ese camino? Por contenido, no</b>, y no por esfuerzo sino
     * por el modelo. ArchUnit expone el <em>valor</em> de una anotación —de ahí que
     * {@code @Query} sea automatizable— pero no los argumentos constantes de un
     * punto de llamada: el literal de {@code jdbcTemplate.update("UPDATE …")} vive
     * en el pool de constantes del llamador y {@code JavaMethodCall} solo modela
     * origen, destino y línea. Sacarlo a un {@code static final String} tampoco
     * ayuda. Se puede exigir que una escritura cruda esté <b>declarada</b>; jamás
     * que esté <b>bien escrita</b>.
     *
     * <p>
     * <b>La propuesta, medida y sin implementar a la espera de que se decida</b>:
     * un tripwire estructural al estilo BE-26 —nadie llama a
     * {@code JdbcTemplate.update}/{@code batchUpdate}/{@code execute} salvo las
     * clases de una lista, cada una con su código y su motivo al lado del nombre, y
     * con su gemela anti-podredumbre como {@link #EXENCIONES_DE_VERSION_AL_DIA}—.
     * Hoy tendría exactamente <b>dos</b> entradas, porque solo dos clases de
     * {@code src/main} usan {@code JdbcTemplate}: {@code JdbcDianJobLeasePort}
     * ({@code E6_YA_PROTEGIDO}, el lease con {@code SKIP LOCKED}) y
     * {@code TokenCleanupRepository} (solo {@code DELETE}). Ni
     * {@code createNativeQuery}, ni {@code JdbcClient}, ni
     * {@code NamedParameterJdbcTemplate}, ni {@code DataSource} directo en ningún
     * otro sitio. Eso no valida SQL, pero convierte un agujero invisible en uno
     * visible: la tercera escritura cruda rompe el build hasta que alguien escriba
     * por qué. Y si además se quisiera el chequeo <em>de contenido</em> sobre SQL
     * crudo, la herramienta no es ArchUnit sino un {@code RegexpMultiline} de
     * Checkstyle, que sí ve el literal esté donde esté y ya corre en
     * {@code mvn verify}.
     *
     * <p>
     * Mientras nada de eso exista, estos párrafos son la red: la regla dice lo que
     * no ve.
     */
    @ArchTest
    static final ArchRule UPDATE_MASIVO_MUEVE_LA_VERSION = classes().that()
            .areAssignableTo(JpaRepository.class).and().haveSimpleNameEndingWith("JpaRepository")
            .should(VetSoftwareConditions.moverLaVersionEnElUpdateMasivo())
            .because("una @Query de UPDATE va directa a la base: ni comprueba ni incrementa"
                    + " @Version, y el save concurrente que llega con la version vieja casa"
                    + " igual y pisa el cambio sin dejar rastro");

    /**
     * Cierre de la incidencia #209, y el hueco complementario de BE-COV: en una
     * feature cuyos puertos están todos cerrados a {@code ROLE_SYSTEM}, ninguno
     * puede abrirse por {@code hasAuthority} sin acotar la empresa.
     *
     * <p>
     * <b>El hallazgo.</b> Catorce {@code Reactivate…UseCase} de catálogos maestros
     * declaraban {@code hasRole('SYSTEM') or hasAuthority('X.update')} mientras
     * todos sus hermanos eran {@code hasRole('SYSTEM')} a secas. Bastaba sembrar
     * esa authority en un rol de empresa para reactivar filas de un catálogo
     * global: no es leer dato ajeno, es <em>escribir</em> en el dato que comparten
     * todos los tenants.
     *
     * <p>
     * <b>Por qué las cuatro reglas de BE-COV no lo cazan, y no por descuido.</b>
     * Todas llevan la guarda antifalsos positivos que excluye las features cuya
     * entidad no alcanza {@code CompanyJpaEntity} —es lo que las mantiene sin
     * ruido— y un catálogo maestro es exactamente lo que esa guarda excluye.
     * Aquellas preguntan <em>de quién es la fila</em>; esta pregunta <em>si el gate
     * desentona de sus hermanos</em>, que es una señal que no necesita saber nada
     * del esquema.
     *
     * <p>
     * <b>Nace dura y en cero</b>, sobre el árbol ya alineado: la campaña de los
     * catorce puertos cerró antes de escribirla, que es el criterio normal del
     * repositorio para no congelar. La barrida de comprobación sobre los 556
     * puertos de las 87 features da cero, y deja fuera los dos casos que tenían que
     * quedar fuera: {@code permission/ListPermissionsByCompanyUseCase} —que recibe
     * la empresa y la valida, el patrón que el {@code CLAUDE.md} prescribe— y
     * {@code company/ReactivateCompanyUseCase}, que no se alineó a propósito porque
     * sus cinco hermanos sí declaran {@code hasAuthority}. Vigila 19 features de
     * entre 4 y 6 puertos: los catálogos maestros, los {@code base_*} y los
     * {@code system_*}.
     *
     * <p>
     * Ver
     * {@code VetSoftwareConditions.noAbrirPorAuthorityLoQueLaFeatureCierraASystem()}
     * para las cuatro condiciones y el falso positivo concreto que paga cada una. Y
     * ojo con no mezclarla con la incidencia #208 —nadie comprueba de quién es la
     * empresa que señala el {@code id} en {@code company}—, que es un problema
     * distinto, peor, y que esta regla no cubre.
     */
    @ArchTest
    static final ArchRule GATE_COHERENTE_EN_FEATURE_DE_SYSTEM = methods().that()
            .areDeclaredInClassesThat().resideInAPackage("..application.port.in..").and()
            .areDeclaredInClassesThat().areInterfaces().and().areDeclaredInClassesThat()
            .areNotAnnotatedWith(NoAuthorizationRequired.class)
            .should(VetSoftwareConditions.noAbrirPorAuthorityLoQueLaFeatureCierraASystem())
            .because("en una feature cerrada a SYSTEM, una authority suelta es un endpoint que"
                    + " se abre sembrando un permiso, sobre un catalogo que comparten todos"
                    + " los tenants");

    /**
     * Cierre de la incidencia #196, y la red que le faltaba a #185: ninguna
     * {@code @Query} puede proyectar un literal booleano.
     *
     * <p>
     * <b>Nace dura y en cero.</b> No hay hoy ninguna violación —la última se
     * arregló al cerrar #185, sustituyendo el JPQL por una consulta derivada—, así
     * que congelarla sería fotografiar un store vacío. Las seis apariciones de
     * {@code CASE WHEN} que quedan en {@code src/main} son legítimas y la regla las
     * distingue por construcción, no por lista: dos {@code ORDER BY} de
     * {@code StockLotJpaRepository} —que van detrás del {@code FROM} y no son
     * columna de salida—, tres agregados de {@code OpenAccountJpaRepository}
     * —{@code COUNT(CASE WHEN …)} y {@code SUM(CASE WHEN …)}, que proyectan
     * números— y una mención en javadoc, que ArchUnit no ve.
     *
     * <p>
     * <b>Por qué el defecto sobrevivió.</b>
     * {@code SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END} compila, se
     * lee perfecto en un diff y con Hibernate 6 funcionaba. Con Hibernate 7 la
     * expresión se tipa como {@code Integer} al extraer el resultado y la coerción
     * del {@code Boolean} lanza siempre. Esa consulta era el cuerpo entero de
     * {@code JpaBillingEntitlementQueryPort.isElectronicInvoicingEnabled}, la
     * <b>primera</b> lectura a base de datos de toda emisión, transmisión,
     * reconciliación y webhook DIAN: facturación electrónica caída al 100 %, y
     * ningún test lo vio porque su único uso en el árbol de test era un mock.
     *
     * <p>
     * <b>La regla y la rodaja son complementarias, no alternativas.</b>
     * {@code MembershipSubModulePersistenceIT} ejecuta ya esa consulta contra MySQL
     * real —y {@code ADAPTADORES_JPA_CON_RODAJA} de {@code PiramideDeTestsTest}
     * exige que cada adaptador tenga la suya—, pero una rodaja solo cubre lo que
     * alguien se acordó de ejecutar; el defecto vivió meses justo porque nadie la
     * había escrito. Esta mira todas las {@code @Query} que existan, se ejecuten o
     * no.
     *
     * <p>
     * Ver {@code VetSoftwareConditions.proyectarSinLiteralBooleano()} para el
     * troceado de la lista de proyección —la profundidad de paréntesis, el salto de
     * los literales de texto y la exclusión de los argumentos de agregado—, que es
     * lo que separa el {@code THEN true} roto del {@code COUNT(CASE WHEN …)} bueno.
     */
    @ArchTest
    static final ArchRule PROYECCION_SIN_LITERAL_BOOLEANO = methods().that()
            .areAnnotatedWith(Query.class)
            .should(VetSoftwareConditions.proyectarSinLiteralBooleano())
            .because("un THEN true en la proyeccion revienta al extraer el resultado con"
                    + " Hibernate 7 y tuvo la facturacion electronica caida al 100%");

    // ── Reglas congeladas: deuda preexistente, cero violaciones nuevas ───────

    @ArchTest
    static final ArchRule APPLICATION_NO_CONOCE_INFRASTRUCTURE = FreezingArchRule
            .freeze(noClasses().that().resideInAPackage("..application..").should()
                    .dependOnClassesThat().resideInAPackage("..infrastructure..")
                    .because("infrastructure -> application -> domain, nunca al reves"));

    @ArchTest
    static final ArchRule REPOS_CON_ENTITYGRAPH = FreezingArchRule.freeze(classes().that()
            .areAssignableTo(JpaRepository.class).and().haveSimpleNameEndingWith("JpaRepository")
            .should(VetSoftwareConditions.declararEntityGraphEnLosFinders())
            .because("la regla del CLAUDE.md sobre N+1 debe ser verificable"));

    private static final DescribedPredicate<JavaMethodCall> FIND_ALL_SIN_ARGS = DescribedPredicate
            .describe("es findAll() sin argumentos",
                    call -> "findAll".equals(call.getTarget().getName())
                            && call.getTarget().getRawParameterTypes().isEmpty());

    @ArchTest
    static final ArchRule SIN_FINDALL_SIN_TENANT = FreezingArchRule.freeze(noClasses().that()
            .resideInAPackage("..application.usecase..").should().callMethodWhere(FIND_ALL_SIN_ARGS)
            .because("un findAll() sin companyId es una fuga entre empresas esperando a ocurrir"));

    /**
     * El cierre de BE-18, y el complemento exacto de
     * {@link #SIN_IO_EXTERNO_EN_TRANSACCION}: aquella protege la transacción del
     * I/O, esta protege al cliente de la transacción. Un efecto {@code @Async}
     * disparado antes del commit se entrega igual aunque la transacción revierta
     * después, y no hay forma de retirarlo: el correo ya está en la bandeja.
     *
     * <p>
     * Congelada porque el correo transaccional del proyecto nació así: la cita no
     * era el único sitio, solo el que se miró. El store registra las otras cuatro
     * notificaciones que hoy salen sin esperar al commit —invitación de empleado,
     * reenvío de invitación, restablecer contraseña y recuperar código—, todas con
     * la misma consecuencia: el correo llega y la operación no ocurrió.
     *
     * <p>
     * La forma de resolver una violación no es tocar la regla, sino diferir el
     * efecto: resolver los datos dentro de la transacción y registrar el envío en
     * {@code afterCommit}. {@code CreateAppointmentService.sendAfterCommit} y
     * {@code EmitElectronicDocumentOnCloseService} son las dos referencias.
     */
    @ArchTest
    static final ArchRule EFECTOS_ASINCRONOS_DESPUES_DEL_COMMIT = FreezingArchRule
            .freeze(noMethods().that().areAnnotatedWith(Transactional.class).or()
                    .areDeclaredInClassesThat().areAnnotatedWith(Transactional.class)
                    .should(VetSoftwareConditions.alcanzarUnEfectoAsincrono())
                    .because("lo que cruza de hilo antes del commit no vuelve si hay rollback"));

    /**
     * Las fábricas de {@code java.time} que leen el reloj del sistema. La lista es
     * cerrada porque el paquete lo es: son todas las clases de {@code java.time}
     * que declaran un {@code now()}.
     */
    private static final List<String> FABRICAS_DE_TIEMPO = List.of("java.time.Instant",
            "java.time.LocalDate", "java.time.LocalDateTime", "java.time.LocalTime",
            "java.time.OffsetDateTime", "java.time.OffsetTime", "java.time.ZonedDateTime",
            "java.time.Year", "java.time.YearMonth", "java.time.MonthDay");

    /**
     * Un {@code now()} que lee el reloj de la máquina. La sobrecarga
     * {@code now(Clock)} queda fuera <b>a propósito</b>: es exactamente la forma
     * correcta, la que un test puede fijar con {@code Clock.fixed(…)}, y marcarla
     * sería pedir que se deshaga la migración. {@code now(ZoneId)} sí entra: cambia
     * la zona, no la fuente del instante.
     */
    private static final DescribedPredicate<JavaMethodCall> RELOJ_DEL_SISTEMA = DescribedPredicate
            .describe("es un now() de java.time sin Clock inyectado",
                    call -> "now".equals(call.getTarget().getName())
                            && FABRICAS_DE_TIEMPO.contains(call.getTargetOwner().getFullName())
                            && call.getTarget().getRawParameterTypes().stream().noneMatch(
                                    tipo -> "java.time.Clock".equals(tipo.getFullName())));

    /**
     * Cierre de la incidencia #119: el código de producción no lee el reloj de la
     * máquina, lo recibe.
     *
     * <p>
     * <b>La regla primero, la migración después</b>, y en ese orden a propósito. La
     * sección «Determinismo» del {@code CLAUDE.md} lleva meses diciendo que se
     * inyecte {@code Clock} y nombra siete sitios concretos; ninguno estaba migrado
     * al escribirse esta regla, y la razón es que nada vigilaba la frontera:
     * mientras la deuda crece sola, migrarla es achicar agua. Congelada, la deuda
     * que hay se tolera y la 166.ª rompe el build, así que la campaña de migración
     * puede ir a su ritmo sin que el problema siga creciendo por debajo.
     *
     * <p>
     * <b>Qué se rompe cuando falta.</b> No es estética: un test que compara contra
     * {@code LocalDate.now()} se cae solo el día que el reloj cruza medianoche
     * entre dos líneas, y un caso de uso que llama a {@code LocalDateTime.now()}
     * por dentro no tiene forma de probar el vencimiento, la ventana por defecto ni
     * el borde de fin de mes: hay que aceptar el instante que salga. Por eso la
     * cobertura de estos servicios se queda siempre en el camino feliz.
     *
     * <p>
     * <b>Cómo se baja una violación</b>: se inyecta {@code java.time.Clock} por
     * constructor y se llama a {@code LocalDate.now(clock)}. El bean ya existe
     * —{@code ClockConfig}— y hay dos referencias vivas,
     * {@code ListAppointmentsService} y {@code ListCompanyClinicalEventsService}.
     * Al arreglar una, ArchUnit la quita del store sola.
     *
     * <p>
     * <b>Aviso sobre el store, que es donde está la trampa.</b> Esta regla se
     * añadió sin poder ejecutar Maven, así que su foto <b>no</b> se ha registrado
     * todavía: la escribirá sola la primera ejecución, tal como advierte
     * {@code archunit.properties}. Al revisar ese primer diff hay que contar las
     * líneas del fichero nuevo del store y contrastarlas con el censo del día en
     * que se escribió — <b>165</b> llamadas sin {@code Clock} (168 apariciones
     * textuales de {@code .now()} en {@code src/main} menos 3 que están en
     * comentario), repartidas en 134 ficheros. Un número mucho mayor significa que
     * el predicado muerde de más y hay que corregirlo <em>antes</em> de commitear
     * el store, no después.
     *
     * <p>
     * <b>Lo que esta regla no ve.</b> Solo mira {@code java.time}: un
     * {@code System.currentTimeMillis()}, un {@code new java.util.Date()} o un
     * {@code CURRENT_TIMESTAMP} dentro de una {@code @Query} pasan sin que nadie
     * los toque. Se dejó fuera para que el censo congelado coincida exactamente con
     * el que documenta la incidencia; ampliarlo después es sumar violaciones
     * nuevas, y eso sí rompería el build a propósito y con un diff legible.
     */
    @ArchTest
    static final ArchRule RELOJ_INYECTADO_EN_VEZ_DE_NOW = FreezingArchRule.freeze(noClasses().that()
            .resideInAPackage("com.vetsoftware.app..").should().callMethodWhere(RELOJ_DEL_SISTEMA)
            .because("un now() que lee el reloj de la maquina no se puede fijar desde un"
                    + " test: el caso que cruza medianoche solo aparece en CI y de noche"));
}
