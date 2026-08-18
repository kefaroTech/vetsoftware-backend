package com.vetsoftware.app.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.vetsoftware.app.shared.security.NoAuthorizationRequired;
import jakarta.persistence.ManyToOne;
import java.lang.reflect.Parameter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Condiciones de arquitectura propias del proyecto, las que no se expresan con
 * la API fluida de ArchUnit.
 */
final class VetSoftwareConditions {

    private static final String APP_PACKAGE = "com.vetsoftware.app";

    /**
     * Profundidad máxima de la búsqueda transitiva; evita recorrer el grafo entero.
     */
    private static final int MAX_DEPTH = 6;

    /** Captura la expresión que recibe {@code @authz.isMyCompany(#…)}. */
    private static final Pattern ISMYCOMPANY_REF = Pattern
            .compile("isMyCompany\\(\\s*#([A-Za-z0-9_.]+)");

    /** Disyunto que deja pasar al principal SYSTEM, cross-tenant por diseño. */
    private static final String SYSTEM_DISJUNCT = "hasRole('SYSTEM')";

    private static final String PORT_IN_PACKAGE = ".application.port.in";

    private static final String PORT_OUT_PACKAGE = ".application.port.out";

    /** Envoltorio de página de la capa de aplicación; hay exactamente uno. */
    private static final String PAGE_RESULT = "PageResult";

    /**
     * Los nombres con los que se ha declarado —o se podría volver a declarar— el
     * concepto «página». No es una lista de prohibidos: es la firma del hallazgo
     * BE-21, donde treinta y cinco features escribieron el mismo record con el
     * mismo nombre en su propio paquete.
     */
    private static final Set<String> NOMBRES_DE_PAGINA = Set.of(PAGE_RESULT, "PageResponse",
            "PagedResult", "PagedResponse", "PageDto", "Slice", "SliceResult");

    /** El {@code PageRequest} de Spring Data: el que hay que acotar. */
    private static final String PAGE_REQUEST = "org.springframework.data.domain.PageRequest";

    /** La API del commit: quien la toca sabe a qué lado del commit está. */
    private static final String TRANSACTION_SYNCHRONIZATION_MANAGER = TransactionSynchronizationManager.class
            .getName();

    private VetSoftwareConditions() {
    }

    /**
     * Las clases que declaran el concepto «página» por su nombre. Mira el nombre y
     * no la forma a propósito: la copia 37 se llamará igual que las 36 anteriores,
     * y para cuando difiera en un campo ya será otra deuda distinta.
     */
    static DescribedPredicate<JavaClass> declaranElConceptoDePagina() {
        return DescribedPredicate.describe("declaran el concepto «pagina»",
                clazz -> NOMBRES_DE_PAGINA.contains(clazz.getSimpleName()));
    }

    /**
     * Toda llamada a {@code PageRequest.of(...)}. Fuera del kernel de paginación no
     * hay ninguna legítima: el índice hay que normalizarlo y el tamaño hay que
     * toparlo, y eso ya lo hace {@code Pages.request(...)}.
     */
    static DescribedPredicate<JavaMethodCall> esUnPageRequestSinAcotar() {
        return DescribedPredicate.describe("construye un PageRequest fuera del kernel",
                call -> PAGE_REQUEST.equals(call.getTargetOwner().getFullName())
                        && "of".equals(call.getTarget().getName()));
    }

    /**
     * Detecta si un método alcanza —directamente o a través de la cadena de
     * llamadas del propio proyecto— un cliente HTTP externo.
     *
     * <p>
     * La búsqueda es transitiva a propósito. El caso real que motivó la regla
     * (BE-02) no llamaba al cliente HTTP desde el método transaccional, sino desde
     * un provider dos saltos más abajo; una regla de llamada directa no lo habría
     * visto. Cuando el salto es a una interfaz, se siguen también sus
     * implementaciones, que es donde vive la llamada.
     *
     * @param httpClientTypes
     *            tipos considerados cliente HTTP (p. ej. {@code RestClient})
     */
    static ArchCondition<JavaMethod> alcanzarUnClienteHttp(Class<?>... httpClientTypes) {
        Set<String> clientNames = new HashSet<>();
        for (Class<?> type : httpClientTypes) {
            clientNames.add(type.getName());
        }
        String description = "alcanzar un cliente HTTP externo (" + String.join(", ", clientNames)
                + ") directamente o a traves de la cadena de llamadas";

        return new ArchCondition<>(description) {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                List<String> path = rutaHasta(method,
                        call -> clientNames.contains(call.getTargetOwner().getFullName()),
                        VetSoftwareConditions::saltaDeHilo);
                if (!path.isEmpty()) {
                    events.add(SimpleConditionEvent.satisfied(method, method.getFullName()
                            + " alcanza un cliente HTTP: " + String.join(" -> ", path)));
                }
            }
        };
    }

    /**
     * La otra mitad de {@link #alcanzarUnClienteHttp(Class...)}, y el cierre de
     * BE-18: detecta el efecto {@code @Async} que se dispara <em>dentro</em> de una
     * transacción sin esperar a que confirme.
     *
     * <p>
     * Las dos reglas miran el mismo salto y sacan conclusiones opuestas, a
     * propósito. Aquella se detiene ahí porque lo que cruza de hilo ya no retiene
     * la conexión ni los locks del caller: deja de ser su problema. Esta empieza
     * justo ahí porque lo que cruza de hilo <b>tampoco vuelve</b>. El proxy encola
     * la tarea al instante y el pool la ejecuta cuando quiere —normalmente antes de
     * que el caller haya hecho flush—, así que un rollback posterior no deshace
     * nada: el correo ya salió.
     *
     * <p>
     * El caso real: {@code CreateAppointmentService.execute} enviaba la
     * confirmación desde dentro de la transacción, y {@code AppointmentJpaEntity}
     * tiene {@code @Version}, cuyo choque optimista salta en el flush,
     * <i>después</i> de la última línea del método. El cliente recibía la
     * confirmación de una cita que nunca existió.
     *
     * <p>
     * <b>Dónde se detiene la búsqueda.</b> En el primer método que habla con
     * {@code TransactionSynchronizationManager} —el origen incluido—. Ese método
     * está decidiendo explícitamente qué ocurre a cada lado del commit; si lo
     * decide mal, eso lo ve una revisión, no una regla de arquitectura. Sin este
     * corte la regla marcaría el propio patrón correcto: la rama de guarda de
     * {@code sendAfterCommit} —la que envía en el acto cuando no hay transacción
     * activa, porque {@code registerSynchronization} lanzaría ahí— es una llamada
     * directa al port como cualquier otra.
     *
     * <p>
     * <b>Por qué el callback no se comprueba.</b> No hace falta: el cuerpo de un
     * {@code afterCommit} vive en una clase anónima aparte, así que no cuelga del
     * método transaccional y el recorrido nunca llega. Es la misma razón por la que
     * el patrón es correcto en ejecución. El día que alguien difiera con un
     * <i>lambda</i>, ArchUnit atribuirá su cuerpo al método que lo declara y la
     * regla dará un falso positivo: la salida es extraer el diferido a su propio
     * método, como hacen hoy los dos que existen.
     */
    static ArchCondition<JavaMethod> alcanzarUnEfectoAsincrono() {
        return new ArchCondition<>("disparar un efecto @Async sin esperar al commit") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                if (difiereAlCommit(method)) {
                    return;
                }
                List<String> path = rutaHasta(method, VetSoftwareConditions::saltaAOtroHilo,
                        VetSoftwareConditions::difiereAlCommit);
                if (!path.isEmpty()) {
                    events.add(SimpleConditionEvent.satisfied(method,
                            method.getFullName() + " dispara un efecto @Async dentro de la"
                                    + " transaccion: " + String.join(" -> ", path)));
                }
            }
        };
    }

    /**
     * Recorre en anchura las llamadas salientes del proyecto y devuelve la primera
     * ruta que llega al destino, o una lista vacía si no hay ninguna.
     *
     * <p>
     * La búsqueda es transitiva a propósito. El caso real que motivó la primera
     * regla que la usa (BE-02) no llamaba al cliente HTTP desde el método
     * transaccional, sino desde un provider dos saltos más abajo; una regla de
     * llamada directa no lo habría visto. Cuando el salto es a una interfaz, se
     * siguen también sus implementaciones, que es donde vive la llamada.
     *
     * @param esElDestino
     *            qué llamada cierra la búsqueda
     * @param cortaLaBusqueda
     *            métodos en los que no se sigue bajando
     */
    private static List<String> rutaHasta(JavaMethod origin, Predicate<JavaMethodCall> esElDestino,
            Predicate<JavaMethod> cortaLaBusqueda) {
        Set<String> visited = new HashSet<>();
        Deque<List<JavaMethod>> queue = new ArrayDeque<>();
        queue.add(List.of(origin));
        visited.add(origin.getFullName());

        while (!queue.isEmpty()) {
            List<JavaMethod> path = queue.poll();
            JavaMethod current = path.get(path.size() - 1);

            for (JavaMethodCall call : current.getMethodCallsFromSelf()) {
                if (esElDestino.test(call)) {
                    return describe(path, call);
                }
                if (path.size() >= MAX_DEPTH || !isOwnCode(call.getTargetOwner())) {
                    continue;
                }
                for (JavaMethod next : resolveTargets(call)) {
                    if (cortaLaBusqueda.test(next) || !visited.add(next.getFullName())) {
                        continue;
                    }
                    List<JavaMethod> extended = new ArrayList<>(path);
                    extended.add(next);
                    queue.add(extended);
                }
            }
        }
        return List.of();
    }

    /**
     * {@code true} si la llamada entrega el trabajo a otro hilo vía {@code @Async}.
     */
    private static boolean saltaAOtroHilo(JavaMethodCall call) {
        return resolveTargets(call).stream().anyMatch(VetSoftwareConditions::saltaDeHilo);
    }

    /**
     * {@code true} si el método habla con
     * {@code TransactionSynchronizationManager}, en cualquiera de sus dos formas:
     * registrar el callback o preguntar si hay sincronización activa. Las dos
     * delatan a un método que sabe dónde está el commit.
     */
    private static boolean difiereAlCommit(JavaMethod method) {
        for (JavaMethodCall call : method.getMethodCallsFromSelf()) {
            if (TRANSACTION_SYNCHRONIZATION_MANAGER.equals(call.getTargetOwner().getFullName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resuelve el destino de una llamada. Si apunta a una interfaz, devuelve además
     * el método homónimo de cada implementación: es ahí donde está el cuerpo que
     * interesa.
     */
    private static List<JavaMethod> resolveTargets(JavaMethodCall call) {
        List<JavaMethod> targets = new ArrayList<>();
        Optional<JavaMethod> resolved = call.getTarget().resolveMember();
        if (resolved.isEmpty()) {
            return targets;
        }
        JavaMethod target = resolved.get();
        targets.add(target);

        JavaClass owner = target.getOwner();
        if (owner.isInterface()) {
            String[] parameterTypeNames = target.getRawParameterTypes().stream()
                    .map(JavaClass::getName).toArray(String[]::new);
            for (JavaClass implementation : owner.getAllSubclasses()) {
                if (!isOwnCode(implementation)) {
                    continue;
                }
                implementation.tryGetMethod(target.getName(), parameterTypeNames)
                        .ifPresent(targets::add);
            }
        }
        return targets;
    }

    private static boolean isOwnCode(JavaClass javaClass) {
        return javaClass.getPackageName().startsWith(APP_PACKAGE);
    }

    /**
     * {@code true} si la llamada cruza a otro hilo vía {@code @Async}.
     *
     * <p>
     * El proxy encola la ejecución en otro pool y devuelve inmediatamente, así que
     * lo que pase al otro lado ya no corre dentro de la transacción del caller. De
     * ahí que las dos reglas que usan este predicado lo lean al revés:
     * {@link #alcanzarUnClienteHttp(Class...)} <b>corta</b> aquí, porque más allá
     * del salto ya no se retiene la conexión ni los locks;
     * {@link #alcanzarUnEfectoAsincrono()} <b>reporta</b> aquí, porque más allá del
     * salto el efecto ya no se puede deshacer.
     */
    private static boolean saltaDeHilo(JavaMethod method) {
        return method.isAnnotatedWith(Async.class)
                || method.getOwner().isAnnotatedWith(Async.class);
    }

    /**
     * Exige que un puerto cuyo command transporta {@code companyId} valide el
     * tenant en su {@code @PreAuthorize}.
     *
     * <p>
     * El {@code companyId} lo inyecta el controller desde el principal, pero el
     * puerto es invocable por cualquier otro caller; sin {@code @authz.isMyCompany}
     * un caller que pase otra empresa opera sobre datos ajenos. Es la defensa en
     * profundidad que pide el CLAUDE.md.
     */
    static ArchCondition<JavaMethod> validarElTenantCuandoRecibeCompanyId() {
        return new ArchCondition<>("validar el tenant con @authz.isMyCompany") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                if (!transportaCompanyId(method)) {
                    return;
                }
                Optional<PreAuthorize> gate = method.tryGetAnnotationOfType(PreAuthorize.class);
                if (gate.isPresent() && soloAlcanzablePorSystem(gate.get().value())) {
                    return;
                }
                if (gate.isEmpty()) {
                    events.add(new SimpleConditionEvent(method, false,
                            method.getFullName() + " recibe companyId y no tiene @PreAuthorize"));
                    return;
                }
                String motivo = revisarGate(method, gate.get().value());
                events.add(new SimpleConditionEvent(method, motivo == null, method.getFullName()
                        + " recibe companyId pero su @PreAuthorize " + motivo));
            }
        };
    }

    /**
     * {@code null} si el gate valida el tenant correctamente; si no, qué le pasa.
     *
     * <p>
     * No basta con que aparezca {@code isMyCompany}: hay que comprobar a qué
     * apunta. SpEL resuelve un {@code #parametroInexistente} a {@code null} sin
     * avisar, {@code isMyCompany(null)} devuelve {@code false} y la regla queda
     * siempre falsa — el endpoint se cierra para todos y nadie entiende por qué. Es
     * la trampa que advierte el CLAUDE.md, y con decenas de anotaciones editadas a
     * la vez es fácil de sembrar.
     */
    private static String revisarGate(JavaMethod method, String expression) {
        Matcher reference = ISMYCOMPANY_REF.matcher(expression);
        if (!reference.find()) {
            return "no invoca @authz.isMyCompany";
        }
        String root = reference.group(1).split("\\.")[0];
        Optional<Set<String>> parameterNames = nombresDeParametro(method);
        if (parameterNames.isPresent() && !parameterNames.get().contains(root)) {
            return "invoca @authz.isMyCompany(#" + reference.group(1) + "), y '" + root
                    + "' no es un parametro del metodo: SpEL lo resuelve a null y el gate"
                    + " queda siempre en false";
        }
        return null;
    }

    /**
     * {@code true} si al gate solo llega un principal SYSTEM, que es cross-tenant
     * por diseño. Ahí el conjunto del tenant seria codigo muerto: ningun empleado
     * alcanza el metodo.
     */
    private static boolean soloAlcanzablePorSystem(String expression) {
        return Arrays.stream(expression.split("\\bor\\b")).map(part -> part.replaceAll("\\s", ""))
                .allMatch(part -> part.isEmpty() || SYSTEM_DISJUNCT.equals(part));
    }

    /**
     * Vacío si la clase no se puede reflexionar: sin nombres no hay nada que
     * validar.
     */
    private static Optional<Set<String>> nombresDeParametro(JavaMethod method) {
        try {
            Set<String> names = new HashSet<>();
            for (Parameter parameter : method.reflect().getParameters()) {
                names.add(parameter.getName());
            }
            return Optional.of(names);
        } catch (RuntimeException | NoClassDefFoundError ignored) {
            return Optional.empty();
        }
    }

    /**
     * {@code true} si el método recibe la empresa, de cualquiera de las dos formas
     * que se usan en el proyecto: dentro de un command
     * ({@code execute(CreateXxxCommand)}, con un campo {@code companyId}) o como
     * parámetro suelto ({@code find(Long companyId, Long id)}).
     *
     * <p>
     * Las dos cuentan. Mirar solo los commands deja fuera los Find/List/Delete, que
     * es justo donde estaban los hallazgos financieros y clínicos de BE-08; mirar
     * solo los parámetros sueltos deja fuera los Create/Update. Son conjuntos casi
     * disjuntos.
     */
    private static boolean transportaCompanyId(JavaMethod method) {
        for (JavaClass parameter : method.getRawParameterTypes()) {
            if (!isOwnCode(parameter)) {
                continue;
            }
            for (JavaField field : parameter.getAllFields()) {
                if ("companyId".equals(field.getName())) {
                    return true;
                }
            }
        }
        return tieneParametroLlamadoCompanyId(method);
    }

    /**
     * ArchUnit no expone los nombres de parámetro, así que se reflexiona la clase
     * real. Funciona porque Spring Boot compila con {@code -parameters}; si no
     * estuviera, los nombres serían {@code arg0}, {@code arg1}… y este chequeo
     * simplemente no encontraría nada.
     */
    private static boolean tieneParametroLlamadoCompanyId(JavaMethod method) {
        try {
            for (Parameter parameter : method.reflect().getParameters()) {
                if ("companyId".equals(parameter.getName())) {
                    return true;
                }
            }
        } catch (RuntimeException | NoClassDefFoundError ignored) {
            // La clase no se puede cargar; el chequeo por command sigue aplicando.
        }
        return false;
    }

    /**
     * Exige que un listado que no filtra por empresa quede cerrado a
     * {@code ROLE_SYSTEM}.
     *
     * <p>
     * Es el hueco por el que se coló BE-29.
     * {@link #validarElTenantCuandoRecibeCompanyId()} mira los puertos <em>que
     * reciben</em> un {@code companyId}: si el método no recibe ninguno no hay nada
     * que validar, así que el puerto pasa limpio aunque por debajo esté sirviendo
     * filas de todas las empresas. Desde esa regla, un catálogo global legítimo y
     * una fuga entre empresas se ven exactamente igual.
     *
     * <p>
     * Lo que los distingue es el repositorio. Si <strong>sabe</strong> filtrar por
     * empresa —declara algún método que recibe {@code companyId}, del tipo
     * {@code findByIdAndCompanyId}— entonces sus filas son de alguien, y servirlas
     * sin ese filtro a un permiso de empleado es una fuga. Los catálogos maestros
     * (ciudades, razas, especies) no declaran ningún método así y la regla ni los
     * mira.
     *
     * <p>
     * A propósito no se comprueba la <em>forma</em> de la llamada: da igual que sea
     * {@code findAll()}, {@code findAll(page, size)} o
     * {@code findAllByAnimalId(id, …)}. Paginar un listado no lo hace multi-tenant,
     * y acotarlo por una FK ajena tampoco: mientras el {@code WHERE} no nombre la
     * empresa, las filas que salen son de cualquiera.
     */
    static ArchCondition<JavaClass> cerrarASystemLosListadosSinEmpresa() {
        return new ArchCondition<>("cerrar a ROLE_SYSTEM los listados que no filtran por empresa") {
            @Override
            public void check(JavaClass service, ConditionEvents events) {
                for (JavaMethod method : service.getMethods()) {
                    Optional<JavaMethod> puerto = puertoQueImplementa(method);
                    if (puerto.isEmpty() || transportaCompanyId(puerto.get())) {
                        continue;
                    }
                    for (JavaMethodCall call : method.getMethodCallsFromSelf()) {
                        Optional<JavaMethod> listado = listadoSinEmpresa(call);
                        if (listado.isPresent()) {
                            events.add(evaluarElGate(puerto.get(), method, listado.get()));
                        }
                    }
                }
            }
        };
    }

    /**
     * El método homónimo del puerto de entrada que este service implementa, si lo
     * hay. Un método que no implementa ningún puerto no es alcanzable desde fuera:
     * su gate es el del puerto que acabe llamándolo.
     */
    private static Optional<JavaMethod> puertoQueImplementa(JavaMethod method) {
        String[] parameterTypeNames = method.getRawParameterTypes().stream().map(JavaClass::getName)
                .toArray(String[]::new);
        for (JavaClass implemented : method.getOwner().getAllRawInterfaces()) {
            if (!implemented.getPackageName().contains(PORT_IN_PACKAGE)) {
                continue;
            }
            Optional<JavaMethod> candidato = implemented.tryGetMethod(method.getName(),
                    parameterTypeNames);
            if (candidato.isPresent()) {
                return candidato;
            }
        }
        return Optional.empty();
    }

    /**
     * La llamada, si es a un listado sin empresa de un repositorio que sí sabe
     * filtrar por ella.
     */
    private static Optional<JavaMethod> listadoSinEmpresa(JavaMethodCall call) {
        JavaClass repositorio = call.getTargetOwner();
        if (!isOwnCode(repositorio) || !repositorio.getPackageName().contains(PORT_OUT_PACKAGE)) {
            return Optional.empty();
        }
        Optional<JavaMethod> destino = call.getTarget().resolveMember();
        if (destino.isEmpty() || !esUnFinderDeVariasFilas(destino.get())
                || filtraPorEmpresa(destino.get())) {
            return Optional.empty();
        }
        boolean sabeFiltrarPorEmpresa = repositorio.getMethods().stream()
                .anyMatch(VetSoftwareConditions::filtraPorEmpresa);
        return sabeFiltrarPorEmpresa ? destino : Optional.empty();
    }

    private static SimpleConditionEvent evaluarElGate(JavaMethod puerto, JavaMethod servicio,
            JavaMethod listado) {
        Optional<PreAuthorize> gate = puerto.tryGetAnnotationOfType(PreAuthorize.class);
        boolean cerrado = gate.isPresent() && soloAlcanzablePorSystem(gate.get().value());
        String hecho = servicio.getOwner().getSimpleName() + "." + servicio.getName() + "() sirve "
                + listado.getOwner().getSimpleName() + "." + listado.getName()
                + "(), que no filtra por empresa, y su puerto no recibe companyId";
        return new SimpleConditionEvent(servicio, cerrado,
                cerrado
                        ? hecho + ", pero solo lo alcanza ROLE_SYSTEM"
                        : hecho + " ni esta cerrado a ROLE_SYSTEM: "
                                + gate.map(PreAuthorize::value).orElse("sin @PreAuthorize"));
    }

    /**
     * {@code true} si el método lleva la empresa consigo. El nombre cuenta porque
     * los repositorios la declaran ahí ({@code findByIdAndCompanyId},
     * {@code findAllAvailableForCompany}) y así el chequeo no depende de que la
     * clase se pueda reflexionar para leer los nombres de parámetro.
     */
    private static boolean filtraPorEmpresa(JavaMethod method) {
        return transportaCompanyId(method) || method.getName().contains("Company");
    }

    /**
     * Solo los <em>finders</em> que devuelven varias filas. El nombre importa: un
     * {@code saveAll} también devuelve una colección y no es un listado.
     */
    private static boolean esUnFinderDeVariasFilas(JavaMethod method) {
        if (!method.getName().startsWith("find")) {
            return false;
        }
        JavaClass returnType = method.getRawReturnType();
        return returnType.isAssignableTo(Collection.class)
                || PAGE_RESULT.equals(returnType.getSimpleName());
    }

    /**
     * Exige {@code @EntityGraph} en todo finder que devuelva una entidad JPA con
     * asociaciones {@code @ManyToOne}: sin él, cada fila hidrata su proxy LAZY con
     * una consulta aparte (N+1).
     *
     * <p>
     * La regla del CLAUDE.md habla de {@code findAll}/{@code findById}, pero en
     * este código los repositorios multi-tenant no los sobreescriben —declaran su
     * equivalente por empresa, del tipo {@code findAllByCompany_Id}—. Mirar solo
     * los dos nombres canónicos dejaría fuera justamente los finders que se usan.
     * Una {@code @Query} propia también vale: ahí el {@code JOIN FETCH} se escribe
     * a mano.
     */
    static ArchCondition<JavaClass> declararEntityGraphEnLosFinders() {
        return new ArchCondition<>(
                "declarar @EntityGraph en los finders que devuelven la entidad") {
            @Override
            public void check(JavaClass repository, ConditionEvents events) {
                Optional<JavaClass> entity = entidadDe(repository);
                if (entity.isEmpty() || !tieneManyToOne(entity.get())) {
                    return;
                }
                for (JavaMethod finder : repository.getMethods()) {
                    if (!devuelveLaEntidad(finder, entity.get())) {
                        continue;
                    }
                    boolean resuelto = finder.isAnnotatedWith(EntityGraph.class)
                            || finder.isAnnotatedWith(Query.class);
                    events.add(new SimpleConditionEvent(finder, resuelto,
                            repository.getSimpleName() + "." + finder.getName() + "() devuelve "
                                    + entity.get().getSimpleName()
                                    + " (con @ManyToOne) sin @EntityGraph ni @Query: N+1"));
                }
            }
        };
    }

    /**
     * {@code true} si el método devuelve la entidad, sola o envuelta en
     * Optional/List/Page.
     */
    private static boolean devuelveLaEntidad(JavaMethod method, JavaClass entity) {
        JavaType returnType = method.getReturnType();
        if (returnType.toErasure().equals(entity)) {
            return true;
        }
        if (returnType instanceof JavaParameterizedType parameterized) {
            return parameterized.getActualTypeArguments().stream()
                    .anyMatch(argument -> argument.toErasure().equals(entity));
        }
        return false;
    }

    /**
     * Primer argumento genérico de {@code JpaRepository<Entidad, Id>}, si se
     * declara directo.
     */
    private static Optional<JavaClass> entidadDe(JavaClass repository) {
        for (JavaType implemented : repository.getInterfaces()) {
            if (!(implemented instanceof JavaParameterizedType parameterized)) {
                continue;
            }
            if (!parameterized.toErasure().isAssignableTo(JpaRepository.class)) {
                continue;
            }
            List<JavaType> arguments = parameterized.getActualTypeArguments();
            if (!arguments.isEmpty()) {
                return Optional.of(arguments.get(0).toErasure());
            }
        }
        return Optional.empty();
    }

    private static boolean tieneManyToOne(JavaClass entity) {
        return entity.getAllFields().stream().anyMatch(f -> f.isAnnotatedWith(ManyToOne.class));
    }

    // ── BE-COV: la familia de fugas «por id», hermana de BE-29 ───────────────

    /**
     * La entidad que <em>es</em> el tenant. Nada se acota contra sí mismo: un
     * {@code UPDATE companies … WHERE id = :id} no puede llevar {@code company_id}
     * porque esa columna no existe, y el id de la fila ya es el de la empresa.
     */
    private static final String COMPANY_ENTITY = "CompanyJpaEntity";

    private static final String SUFIJO_ENTIDAD_JPA = "JpaEntity";

    /**
     * Un statement que escribe. El {@code SELECT … FOR UPDATE} de
     * {@code NumberingResolutionJpaRepository} no empieza por aquí, que es
     * exactamente lo que se busca: bloquea filas, no las modifica.
     */
    private static final Pattern SENTENCIA_QUE_ESCRIBE = Pattern.compile("^(update|delete)\\b",
            Pattern.CASE_INSENSITIVE);

    /**
     * La empresa nombrada en la consulta, en cualquiera de las tres formas que usa
     * el proyecto: {@code company_id} en SQL nativo —también dentro de un
     * {@code EXISTS} correlacionado o tras un alias, {@code r.company_id}—,
     * {@code companyId} en JPQL y {@code company.id} en una ruta de propiedad.
     */
    private static final Pattern EMPRESA_EN_LA_CONSULTA = Pattern
            .compile("company_id|companyId|company\\.id", Pattern.CASE_INSENSITIVE);

    /** Sufijo con el que los puertos de salida declaran la variante acotada. */
    private static final String SUFIJO_ACOTADO = "AndCompanyId";

    /**
     * Saltos de asociación que se siguen buscando la empresa desde una entidad.
     * Cinco cubre la cadena más larga del esquema
     * ({@code medication_schedules → hospitalization_medications → hospitalizations
     * → companies}) con margen, y evita recorrer el grafo entero.
     */
    private static final int MAX_SALTOS_DE_ASOCIACION = 5;

    /**
     * Exige que un puerto de entrada que señala <em>una fila concreta por su
     * id</em> reciba también la empresa, salvo que solo lo alcance
     * {@code ROLE_SYSTEM}.
     *
     * <p>
     * Es la tercera pieza del trío, y la que faltaba.
     * {@link #validarElTenantCuandoRecibeCompanyId()} cubre los puertos que
     * <em>sí</em> reciben un {@code companyId} y obliga a validarlo;
     * {@link #cerrarASystemLosListadosSinEmpresa()} cubre los que no reciben
     * ninguno y devuelven <em>varias</em> filas. Entre las dos quedaba un hueco del
     * tamaño de una campaña: las operaciones que no reciben empresa y actúan sobre
     * <em>una</em> fila, señalada por un id que el atacante escribe en la URL.
     * ArchUnit pasó 13/13 en verde con sesenta y cinco de esas repartidas por
     * veintisiete features.
     *
     * <p>
     * <b>El defecto, con nombre.</b> {@code DELETE /employee-roles/{id}} revocaba
     * el rol del administrador de otra empresa;
     * {@code PATCH /employees/{id}/enable} le devolvía el login a quien otro tenant
     * había despedido; y {@code GET /laboratory-test-files/{id}/download} —una
     * lectura, no una escritura— entregaba el PDF de un resultado de laboratorio
     * ajeno. Los tres llevaban su {@code @PreAuthorize} con su permiso correcto: el
     * permiso dice <em>qué</em> puede hacer el empleado, nunca <em>sobre qué
     * filas</em>.
     *
     * <p>
     * <b>Por qué no se mira el verbo.</b> Una lista de prefijos ({@code Delete…},
     * {@code Reactivate…}, {@code Suspend…}) envejece mal: se rompe el día que
     * alguien escribe {@code ArchiveXxxUseCase}, y además dejaría fuera el
     * {@code Download…} y los dos
     * {@code List…ByHospitalization(Long hospitalizationId)}, que leen filas de
     * otro tenant sin escribir nada. Lo que se mira es la firma: un parámetro
     * {@code Long} —el CLAUDE.md fija que todo id de entidad es {@code Long}, así
     * que el {@code int page} de la paginación no cuenta— o un command/query con un
     * campo {@code id}.
     *
     * <p>
     * <b>El discriminador que evita los catálogos maestros.</b> Igual que en BE-29,
     * lo que separa una fuga de una operación global legítima no es el nombre sino
     * el dato: si ninguna entidad JPA de la feature alcanza
     * {@code CompanyJpaEntity} —ni directamente ni por asociaciones—, sus filas no
     * son de nadie y la regla ni las mira. Así {@code ReactivateCountryUseCase},
     * {@code ReactivateModuleUseCase} o {@code ReactivateSpaTypeUseCase} (los
     * {@code spa_types} son globales, no por empresa) quedan fuera sin enumerar
     * excepciones, mientras {@code DeleteDiagnosticImagingTypeUseCase} —cuya tabla
     * sí tiene {@code company_id}— entra.
     */
    static ArchCondition<JavaMethod> acotarPorEmpresaLasOperacionesPorId() {
        return new ArchCondition<>(
                "recibir la empresa cuando la operacion senala una fila" + " concreta por su id") {
            @Override
            public void check(JavaMethod puerto, ConditionEvents events) {
                if (transportaCompanyId(puerto) || !senalaUnaFilaPorId(puerto)) {
                    return;
                }
                Optional<PreAuthorize> gate = puerto.tryGetAnnotationOfType(PreAuthorize.class);
                if (gate.isPresent() && soloAlcanzablePorSystem(gate.get().value())) {
                    return;
                }
                if (!laFeatureTieneDatosDeEmpresa(puerto.getOwner())) {
                    return;
                }
                events.add(new SimpleConditionEvent(puerto, false,
                        puerto.getFullName()
                                + " senala una fila por id y no recibe companyId, pero sus filas"
                                + " pertenecen a una empresa y el gate deja entrar a un empleado: "
                                + gate.map(PreAuthorize::value).orElse("sin @PreAuthorize")));
            }
        };
    }

    /**
     * {@code true} si la firma basta para señalar una fila concreta: un
     * {@code Long} suelto —todo id de entidad lo es, por la regla del CLAUDE.md— o
     * un command/query con un campo {@code id}.
     *
     * <p>
     * El {@code int} queda fuera a propósito: en este proyecto los enteros
     * primitivos de un puerto son {@code page} y {@code pageSize}, nunca un id.
     */
    private static boolean senalaUnaFilaPorId(JavaMethod method) {
        for (JavaClass parameter : method.getRawParameterTypes()) {
            if (Long.class.getName().equals(parameter.getName())) {
                return true;
            }
            if (isOwnCode(parameter)
                    && parameter.getAllFields().stream().anyMatch(f -> "id".equals(f.getName()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@code true} si alguna entidad JPA de la feature a la que pertenece esta
     * clase alcanza la empresa. Es la pregunta «¿estas filas son de alguien?» hecha
     * al esquema en vez de al nombre del paquete.
     */
    private static boolean laFeatureTieneDatosDeEmpresa(JavaClass clazz) {
        return paqueteDeLaFeature(clazz).map(feature -> feature.getClassesInPackageTree().stream()
                .filter(c -> c.getSimpleName().endsWith(SUFIJO_ENTIDAD_JPA))
                .anyMatch(VetSoftwareConditions::perteneceAUnaEmpresa)).orElse(false);
    }

    /**
     * El paquete raíz de la rodaja vertical
     * ({@code com.vetsoftware.app.spa.application.port.in} →
     * {@code com.vetsoftware.app.spa}).
     */
    private static Optional<JavaPackage> paqueteDeLaFeature(JavaClass clazz) {
        String nombre = clazz.getPackageName();
        if (!nombre.startsWith(APP_PACKAGE + ".")) {
            return Optional.empty();
        }
        int fin = nombre.indexOf('.', APP_PACKAGE.length() + 1);
        String feature = fin < 0 ? nombre : nombre.substring(0, fin);
        JavaPackage paquete = clazz.getPackage();
        while (!paquete.getName().equals(feature)) {
            Optional<JavaPackage> padre = paquete.getParent();
            if (padre.isEmpty()) {
                return Optional.empty();
            }
            paquete = padre.get();
        }
        return Optional.of(paquete);
    }

    /**
     * {@code true} si las filas de esta entidad son de una empresa: o lleva la
     * empresa encima ({@code company}/{@code companyId}) o la alcanza siguiendo sus
     * asociaciones a otras entidades JPA.
     *
     * <p>
     * La búsqueda es transitiva porque el esquema no es plano: una
     * {@code medication_schedules} no tiene {@code company_id} —su empresa vive
     * tres saltos más arriba, en {@code hospitalizations}— y es justo una de las
     * tablas donde hubo fuga. Mirar solo la columna propia habría dado por «global»
     * a la mitad de las tablas clínicas.
     *
     * <p>
     * {@code CompanyJpaEntity} sale {@code false} y eso es correcto: la empresa no
     * pertenece a ninguna empresa, y sus operaciones son territorio de SYSTEM. Lo
     * mismo los catálogos maestros —{@code countries}, {@code states},
     * {@code cities}, {@code modules}, {@code memberships}, {@code base_roles}, los
     * {@code system_*}—, que no llegan a la empresa por ningún camino.
     */
    private static boolean perteneceAUnaEmpresa(JavaClass entidad) {
        Set<String> visitadas = new HashSet<>();
        Deque<JavaClass> nivel = new ArrayDeque<>();
        nivel.add(entidad);
        visitadas.add(entidad.getFullName());

        for (int salto = 0; salto < MAX_SALTOS_DE_ASOCIACION && !nivel.isEmpty(); salto++) {
            Deque<JavaClass> siguiente = new ArrayDeque<>();
            for (JavaClass actual : nivel) {
                for (JavaField campo : actual.getAllFields()) {
                    JavaClass tipo = campo.getRawType();
                    if (COMPANY_ENTITY.equals(tipo.getSimpleName())
                            || "companyId".equals(campo.getName())) {
                        return true;
                    }
                    if (isOwnCode(tipo) && tipo.getSimpleName().endsWith(SUFIJO_ENTIDAD_JPA)
                            && visitadas.add(tipo.getFullName())) {
                        siguiente.add(tipo);
                    }
                }
            }
            nivel = siguiente;
        }
        return false;
    }

    /**
     * Exige que toda {@code @Query} que escribe nombre la empresa, en los
     * repositorios cuyas filas son de alguien.
     *
     * <p>
     * <b>Por qué esta es la peor de las tres.</b> En un {@code delete} o un
     * {@code update} corriente hay una lectura previa que valida la propiedad, así
     * que el SQL es la segunda barrera. En un {@code reactivate} no la hay: el
     * servicio decide si la fila existe mirando <em>cuántas filas actualizó</em>,
     * de modo que el {@code WHERE} <b>es</b> toda la seguridad. Un
     * {@code UPDATE employee_roles SET enabled = true WHERE id = :id} devolvía el
     * rol revocado a un empleado de otra empresa y además invalidaba su caché de
     * permisos: escalada de privilegios cross-tenant escrita en cuatro líneas de
     * SQL.
     *
     * <p>
     * El valor de la anotación viaja en el bytecode, así que ArchUnit puede leer el
     * statement. Vale cualquiera de las formas correctas del proyecto: la columna
     * directa ({@code RoleJpaRepository}), el {@code JOIN} contra la tabla padre
     * ({@code RolePermissionJpaRepository}) y el {@code EXISTS} correlacionado para
     * cuando la fila no tiene {@code company_id} propio
     * ({@code DebtOpenAccountJpaRepository},
     * {@code HospitalizationProcedureJpaRepository}).
     *
     * <p>
     * <b>Las dos exenciones, y por qué son estructurales y no una lista.</b>
     * <ul>
     * <li><b>El repositorio cuyas filas no son de nadie.</b> Si su entidad no
     * alcanza {@code CompanyJpaEntity}, no hay empresa que nombrar:
     * {@code UPDATE countries …} o {@code UPDATE companies …} no pueden acotarse y
     * la regla ni los mira.</li>
     * <li><b>La hermana acotada del mismo nombre.</b> El camino SYSTEM necesita un
     * statement sin filtro, y el proyecto lo declara como sobrecarga:
     * {@code reactivate(id)} junto a {@code reactivate(id, companyId)}, elegidas
     * por el {@code companyId == null} del servicio. Sin esta exención la regla
     * marcaría el patrón corregido —{@code EmployeeJpaRepository} y
     * {@code RolePermissionJpaRepository} lo tienen ya— y nadie podría ponerla en
     * verde. Cuando la sobrecarga acotada <em>no</em> existe, el statement sin
     * filtro es el único camino y por él pasan todos, SYSTEM y empleado.</li>
     * </ul>
     */
    static ArchCondition<JavaClass> acotarPorEmpresaElSqlQueEscribe() {
        return new ArchCondition<>("nombrar la empresa en toda @Query que escribe") {
            @Override
            public void check(JavaClass repositorio, ConditionEvents events) {
                Optional<JavaClass> entidad = entidadDe(repositorio);
                if (entidad.isEmpty() || !perteneceAUnaEmpresa(entidad.get())) {
                    return;
                }
                for (JavaMethod metodo : repositorio.getMethods()) {
                    Optional<String> sentencia = sentenciaQueEscribe(metodo);
                    if (sentencia.isEmpty() || nombraLaEmpresa(sentencia.get())) {
                        continue;
                    }
                    boolean caminoSystem = tieneHermanaAcotada(repositorio, metodo);
                    String desenlace = caminoSystem
                            ? ", pero declara la sobrecarga acotada: es el camino SYSTEM"
                            : " y no hay sobrecarga acotada que lo separe del camino del"
                                    + " empleado: " + unaLinea(sentencia.get());
                    events.add(new SimpleConditionEvent(metodo, caminoSystem,
                            repositorio.getSimpleName() + "." + metodo.getName()
                                    + "() escribe sin nombrar la empresa" + desenlace));
                }
            }
        };
    }

    /** El statement de una {@code @Query} que escribe, si el método declara una. */
    private static Optional<String> sentenciaQueEscribe(JavaMethod method) {
        return method.tryGetAnnotationOfType(Query.class).map(Query::value).map(String::strip)
                .filter(sql -> SENTENCIA_QUE_ESCRIBE.matcher(sql).find());
    }

    private static boolean nombraLaEmpresa(String sentencia) {
        return EMPRESA_EN_LA_CONSULTA.matcher(sentencia).find();
    }

    /**
     * {@code true} si el repositorio declara otro método <em>del mismo nombre</em>
     * cuya {@code @Query} sí nombra la empresa. Esa sobrecarga es la prueba de que
     * el camino del tenant existe y va por otro sitio.
     */
    private static boolean tieneHermanaAcotada(JavaClass repositorio, JavaMethod metodo) {
        for (JavaMethod otro : repositorio.getMethods()) {
            if (otro.equals(metodo) || !otro.getName().equals(metodo.getName())) {
                continue;
            }
            if (sentenciaQueEscribe(otro).filter(VetSoftwareConditions::nombraLaEmpresa)
                    .isPresent()) {
                return true;
            }
        }
        return false;
    }

    private static String unaLinea(String sentencia) {
        return sentencia.replaceAll("\\s+", " ").strip();
    }

    /**
     * Exige que un caso de uso que carga por un puerto de salida use la variante
     * acotada por empresa <em>cuando el puerto la ofrece</em>.
     *
     * <p>
     * <b>Este es el que ningún humano ve en revisión.</b> Doce {@code Update…}
     * llevaban {@code @authz.isMyCompany(#command.companyId)} y eran vulnerables
     * igualmente. Esa anotación solo prueba que el atacante declara <em>su
     * propia</em> empresa —cosa que siempre hace, porque el controller la inyecta
     * desde el principal—; no dice nada sobre de quién es la fila que se va a
     * cargar. Como el servicio hacía {@code findById(command.id())} y después
     * {@code entidad.update(…, company)}, el efecto no era un rechazo sino una
     * <b>apropiación</b>: la fila de la empresa B pasaba a ser de A. La anotación
     * «se ve bien» y el defecto está debajo.
     *
     * <p>
     * <b>El matiz que distingue el patrón bueno de la fuga.</b> El camino SYSTEM
     * legítimo es un ternario —{@code companyId == null ? findById(id) :
     * findByIdAndCompanyId(id, companyId)}— que llama a las <em>dos</em> variantes.
     * Por eso la regla no prohíbe {@code findById}: exige que la clase llame además
     * a la acotada. La fuga es la clase que solo conoce la variante ancha.
     * Referencia canónica: {@code spa/application/usecase/UpdateSpaService}.
     *
     * <p>
     * Solo se mira si el <em>puerto</em> ofrece la variante acotada —misma
     * sobrecarga con {@code companyId}, o el mismo nombre con sufijo
     * {@code AndCompanyId}—. Cuando no la ofrece no hay nada que exigir aquí: ese
     * caso es de {@link #acotarPorEmpresaLasOperacionesPorId()} si la fila es de la
     * propia feature, o de {@link #acotarPorEmpresaLasReferenciasCrossFeature()} si
     * es una referencia a otra, y arreglarlo pasa por <em>crear</em> el finder
     * acotado (es lo que les faltaba a {@code medicationschedule} y
     * {@code procedureschedule}).
     *
     * <p>
     * <b>El servicio que solo alcanza SYSTEM está exento</b>, igual que en
     * {@link #validarElTenantCuandoRecibeCompanyId()}. Ahí la carga ancha no es un
     * descuido sino el comportamiento correcto: un principal SYSTEM no tiene
     * empresa, así que exigirle la variante acotada sería exigirle pasar
     * {@code null} y no encontrar nunca nada. El caso llegó solo: al añadir
     * {@code findByIdAndCompanyId} a {@code EmployeeRoleRepository} y
     * {@code PermissionRepository} —para arreglar otras fugas— seis servicios que
     * la regla antes ni miraba se volvieron visibles, y cinco de ellos
     * ({@code Find}/{@code Update}{@code EmployeeRole},
     * {@code Find}/{@code Update}/{@code DeletePermission}) son puertos de
     * administración de plataforma cerrados a {@code hasRole('SYSTEM')} a secas.
     * Registrarlos como deuda habría envenenado el indicador: el store solo vale si
     * cada línea es algo que alguien debería arreglar algún día.
     * {@code CreateEmployeeRoleService} <em>no</em> queda exento y sigue rojo,
     * porque su gate añade {@code hasAuthority('employee.create')}.
     */
    static ArchCondition<JavaClass> cargarPorIdAcotandoLaEmpresa() {
        return new ArchCondition<>(
                "usar la variante acotada por empresa cuando el puerto" + " de salida la ofrece") {
            @Override
            public void check(JavaClass servicio, ConditionEvents events) {
                if (sinEmpresaDeLaQueTirar(servicio)) {
                    return;
                }
                Set<String> invocadas = new HashSet<>();
                List<JavaMethodCall> aPuertos = new ArrayList<>();
                for (JavaMethod metodo : servicio.getMethods()) {
                    for (JavaMethodCall llamada : metodo.getMethodCallsFromSelf()) {
                        JavaClass puerto = llamada.getTargetOwner();
                        if (!isOwnCode(puerto)
                                || !puerto.getPackageName().contains(PORT_OUT_PACKAGE)) {
                            continue;
                        }
                        invocadas.add(clave(puerto, llamada.getTarget().getName()));
                        aPuertos.add(llamada);
                    }
                }
                Set<String> reportadas = new HashSet<>();
                for (JavaMethodCall llamada : aPuertos) {
                    Optional<JavaMethod> destino = llamada.getTarget().resolveMember();
                    if (destino.isEmpty() || filtraPorEmpresa(destino.get())) {
                        continue;
                    }
                    JavaClass puerto = llamada.getTargetOwner();
                    List<JavaMethod> acotadas = hermanasAcotadasDelPuerto(puerto, destino.get());
                    if (acotadas.isEmpty()
                            || !reportadas.add(clave(puerto, destino.get().getName()))) {
                        continue;
                    }
                    Optional<JavaMethod> usada = acotadas.stream()
                            .filter(a -> invocadas.contains(clave(puerto, a.getName())))
                            .findFirst();
                    String nombres = acotadas.stream().map(a -> a.getName() + "()")
                            .collect(Collectors.joining(" ni "));
                    events.add(new SimpleConditionEvent(servicio, usada.isPresent(),
                            servicio.getSimpleName() + " llama a " + puerto.getSimpleName() + "."
                                    + destino.get().getName() + "(), que no acota por empresa,"
                                    + (usada.isPresent()
                                            ? " pero tambien a " + usada.get().getName()
                                                    + "(): es el ternario del camino SYSTEM"
                                            : " y nunca a " + nombres
                                                    + ", que el puerto si ofrece: carga la fila"
                                                    + " de cualquier empresa")));
                }
            }
        };
    }

    private static String clave(JavaClass puerto, String metodo) {
        return puerto.getFullName() + "#" + metodo;
    }

    /**
     * {@code true} si a este caso de uso solo llega un principal SYSTEM: implementa
     * al menos un puerto de entrada y <em>todos</em> los que implementa están
     * cerrados a {@code hasRole('SYSTEM')} a secas.
     *
     * <p>
     * El criterio es por clase y no por método a propósito. La señal vive en el
     * {@code port/in} y la violación se detecta en el {@code usecase}, así que hay
     * que cruzar servicio → puerto que implementa; pero un servicio puede repartir
     * la carga entre el método público y un helper privado, y ahí el helper no
     * implementa nada. Exigir que <em>todos</em> los puertos de la clase sean
     * SYSTEM cubre las dos formas sin depender de cuál de sus métodos hizo la
     * llamada. En la práctica cada service implementa exactamente un caso de uso.
     *
     * <p>
     * Un servicio que no implementa ningún puerto <b>no</b> queda exento: no es
     * alcanzable desde fuera, así que su gate es el del puerto que acabe llamándolo
     * y esta clase no puede afirmar que sea SYSTEM.
     */
    /**
     * {@code true} si a este caso de uso no llega ningún principal del que sacar
     * una empresa, por cualquiera de las dos vías que el proyecto reconoce.
     *
     * <p>
     * La primera es {@link #soloAlcanzablePorSystem(JavaClass)}: un principal
     * SYSTEM es cross-tenant por diseño y no tiene empresa.
     *
     * <p>
     * La segunda es {@link NoAuthorizationRequired}, y estaba ya en el proyecto sin
     * que esta condición la mirara: {@link #acotarPorEmpresaLasOperacionesPorId()}
     * la trata como exención desde su primera línea —el propio
     * {@code HexagonalArchitectureTest} la excluye en el {@code .that()} de la
     * regla—, así que no mirarla aquí era una asimetría entre dos reglas de la
     * misma familia, no una decisión. Un puerto anotado así declara por escrito que
     * su autorización <em>no es un JWT de empleado</em>: el flujo público de
     * verificación de correo se autoriza por la posesión de un token de un solo uso
     * y el webhook del proveedor por su firma HMAC. En ninguno de los dos hay
     * empresa en la petición —en el webhook la empresa es una <em>salida</em> de la
     * búsqueda, se deriva del documento encontrado—, así que exigirles la variante
     * acotada es exigirles pasar {@code null} y no encontrar nunca nada, que es
     * exactamente el argumento con el que ya se eximió a SYSTEM.
     *
     * <p>
     * La anotación no es una vía silenciosa: exige escribir el motivo y la
     * comprueba otra regla dura. Se paga el mismo precio que en las demás
     * exenciones del fichero — que la salida esté declarada en el código y sea
     * legible en revisión.
     */
    private static boolean sinEmpresaDeLaQueTirar(JavaClass servicio) {
        return soloAlcanzablePorSystem(servicio) || sinAutorizacionDeEmpleado(servicio);
    }

    /**
     * {@code true} si el servicio implementa al menos un puerto de entrada y
     * <em>todos</em> los que implementa están anotados con
     * {@link NoAuthorizationRequired}. Mismo criterio por clase que
     * {@link #soloAlcanzablePorSystem(JavaClass)}, y por el mismo motivo: la señal
     * vive en el {@code port/in} y la violación se detecta en el {@code usecase}.
     */
    private static boolean sinAutorizacionDeEmpleado(JavaClass servicio) {
        boolean implementaAlguno = false;
        for (JavaMethod metodo : servicio.getMethods()) {
            Optional<JavaMethod> puerto = puertoQueImplementa(metodo);
            if (puerto.isEmpty()) {
                continue;
            }
            implementaAlguno = true;
            if (!puerto.get().getOwner().isAnnotatedWith(NoAuthorizationRequired.class)) {
                return false;
            }
        }
        return implementaAlguno;
    }

    private static boolean soloAlcanzablePorSystem(JavaClass servicio) {
        boolean implementaAlguno = false;
        for (JavaMethod metodo : servicio.getMethods()) {
            Optional<JavaMethod> puerto = puertoQueImplementa(metodo);
            if (puerto.isEmpty()) {
                continue;
            }
            implementaAlguno = true;
            Optional<PreAuthorize> gate = puerto.get().tryGetAnnotationOfType(PreAuthorize.class);
            if (gate.isEmpty() || !soloAlcanzablePorSystem(gate.get().value())) {
                return false;
            }
        }
        return implementaAlguno;
    }

    /**
     * Exige que el puerto con el que un caso de uso resuelve una referencia a
     * <em>otra</em> feature ofrezca —y use— la variante acotada por empresa.
     *
     * <p>
     * Es la cuarta forma del defecto, y la que sobrevive a las otras tres. Con la
     * carga propia ya acotada, un {@code UpdateSurgeryService} no puede apropiarse
     * de una cirugía ajena: el {@code findByIdAndCompanyId} de su repositorio lo
     * impide. Lo que sí puede es <b>reapuntar la suya a una entidad de otro
     * tenant</b>, porque resuelve el animal con
     * {@code animalQueryPort.findById(command.animalId())} y ese puerto no filtra
     * nada — una cirugía de mi empresa colgada del animal de la vecina, con su
     * historia clínica contaminada. Afecta a {@code laboratorytest},
     * {@code surgery}, {@code diagnosticimaging} y {@code daycare}; {@code spa},
     * {@code prescription} y {@code consultation} lo hacen bien y son el modelo.
     *
     * <p>
     * <b>Por qué es regla aparte y no una ampliación de
     * {@link #cargarPorIdAcotandoLaEmpresa()}.</b> Aquella ya mira los
     * {@code *QueryPort} —su filtro es el paquete {@code port.out}, no el nombre
     * del tipo—, así que ampliarla no habría añadido nada: el problema es que estos
     * puertos <b>no declaran</b> ninguna variante acotada que exigirles llamar, y
     * una condición que dice «llama a la acotada» no puede reportar «declárala
     * primero». Son dos afirmaciones distintas y el mensaje de fallo tiene que
     * decir cuál es. Las cuatro reglas quedan disjuntas por construcción: si el
     * puerto ofrece la acotada, el caso es de aquella; si no la ofrece y la fila es
     * de la propia feature, es de {@link #acotarPorEmpresaLasOperacionesPorId()};
     * esta se queda solo con las referencias cross-feature.
     *
     * <p>
     * <b>De dónde sale «esta referencia es de una empresa».</b> No del nombre. Se
     * sigue la cadena que el propio código ya declara: puerto → implementación
     * ({@code surgery.infrastructure.persistence.JpaAnimalQueryPort}) → el
     * {@code XxxJpaRepository} que inyecta → su entidad ({@code AnimalJpaEntity}) →
     * {@code perteneceAUnaEmpresa}. Se exige además que la entidad sea la del
     * núcleo del nombre del puerto ({@code AnimalQueryPort} →
     * {@code AnimalJpaEntity}), para que un adaptador que inyecte varios
     * repositorios no haga responder a la regla por el equivocado. Así
     * {@code CompanyQueryPort} (la empresa no pertenece a ninguna),
     * {@code SpaTypeQueryPort} y {@code SpecieQueryPort} (catálogos globales)
     * quedan fuera sin enumerar excepciones.
     */
    static ArchCondition<JavaClass> acotarPorEmpresaLasReferenciasCrossFeature() {
        return new ArchCondition<>("resolver las referencias a otra feature con la variante"
                + " acotada por empresa") {
            @Override
            public void check(JavaClass servicio, ConditionEvents events) {
                if (sinEmpresaDeLaQueTirar(servicio)) {
                    return;
                }
                Optional<JavaPackage> propia = paqueteDeLaFeature(servicio);
                if (propia.isEmpty() || !yaSabeAcotar(servicio)) {
                    return;
                }
                Set<String> reportadas = new HashSet<>();
                for (JavaMethod metodo : servicio.getMethods()) {
                    for (JavaMethodCall llamada : metodo.getMethodCallsFromSelf()) {
                        JavaClass puerto = llamada.getTargetOwner();
                        if (!isOwnCode(puerto)
                                || !puerto.getPackageName().contains(PORT_OUT_PACKAGE)) {
                            continue;
                        }
                        Optional<JavaMethod> destino = llamada.getTarget().resolveMember();
                        if (destino.isEmpty() || !resuelveUnaReferencia(destino.get())
                                || filtraPorEmpresa(destino.get())
                                || hermanaAcotadaDelPuerto(puerto, destino.get()).isPresent()) {
                            continue;
                        }
                        Optional<JavaClass> referida = entidadReferidaPor(puerto);
                        if (referida.isEmpty() || !perteneceAUnaEmpresa(referida.get())
                                || esDeLaMismaFeature(referida.get(), propia.get())
                                || nombraLaReferenciaComoAutor(servicio, referida.get())) {
                            continue;
                        }
                        if (!reportadas.add(clave(puerto, destino.get().getName()))) {
                            continue;
                        }
                        events.add(new SimpleConditionEvent(servicio, false,
                                servicio.getSimpleName() + " resuelve una referencia a "
                                        + referida.get().getSimpleName() + " con "
                                        + puerto.getSimpleName() + "." + destino.get().getName()
                                        + "(), que no acota por empresa y no ofrece variante"
                                        + " acotada: puede reapuntar su fila a una entidad de"
                                        + " otro tenant"));
                    }
                }
            }
        };
    }

    /**
     * {@code true} si el servicio demuestra en alguna parte que tiene la empresa en
     * la mano: llama a una variante acotada de algún puerto de salida.
     *
     * <p>
     * Es el discriminador que hace la regla usable, y no es un truco de recuento.
     * Sin él la condición marcaba 89 puntos, la mayoría {@code Create…Service}: ahí
     * la referencia sin acotar es el mismo defecto, pero la regla no puede
     * distinguir un id que llega del cliente de uno que llega del principal, así
     * que arrastraba consigo los quince {@code EmployeeQueryPort.findById} que
     * resuelven el <em>empleado autenticado</em> —imposibles de «arreglar», porque
     * ahí no hay nada roto—. Exigiendo que el servicio ya use una acotada, lo que
     * queda es la afirmación defendible y comprobable: <b>este servicio ya tiene el
     * {@code companyId} y ya demostró que sabe usarlo; que lo use también para la
     * referencia</b>.
     */
    /**
     * {@code true} si el servicio no recibe la entidad referida como un
     * <em>recurso</em>, sino solo como el <em>autor</em> de lo que está
     * escribiendo. Entonces el id no lo elige el cliente y no hay nada que acotar.
     *
     * <p>
     * <b>El falso positivo que esto cierra, y por qué no se cerró antes.</b> Siete
     * servicios resuelven {@code EmployeeQueryPort.findById(...)} para guardar
     * quién hizo la cosa —{@code createdById}, {@code processedById},
     * {@code suspendedById}—, y el controller rellena ese campo desde el principal
     * (`authz.currentEmployeeId…`), nunca desde el request. El autor de la regla
     * los dejó dentro a propósito: excluirlos <em>enumerando el puerto por su
     * nombre</em> habría taparlo también el día que un {@code employeeId} llegue de
     * verdad en el request, y este fichero evita las listas de nombres en todas las
     * demás reglas.
     *
     * <p>
     * <b>La señal estructural que los distingue.</b> No el nombre del puerto ni el
     * del campo de autoría, sino la <b>ausencia del nombre de recurso</b>: si la
     * referencia es a {@code EmployeeJpaEntity} y ningún command ni parámetro del
     * servicio declara un {@code employeeId}, el servicio no tiene por dónde
     * recibir «sobre qué empleado actúo» — los únicos ids de empleado que le llegan
     * son de autoría. En cuanto alguien añada {@code employeeId} al command, la
     * regla vuelve a marcarlo, que es literalmente la preocupación que el autor
     * quería no tapar.
     *
     * <p>
     * Y discrimina de verdad, no solo de recuento: en
     * {@code CreateOpenAccountService} exime el {@code createdById} y <b>deja
     * rojo</b> el {@code OwnerQueryPort}, porque {@code CreateOpenAccountCommand}
     * sí declara {@code ownerId}; en {@code employeerole}, cuyos commands declaran
     * {@code employeeId} porque ahí el empleado <em>es</em> el recurso, no exime
     * nada.
     */
    private static boolean nombraLaReferenciaComoAutor(JavaClass servicio, JavaClass referida) {
        String nombreDeRecurso = comoRecurso(referida.getSimpleName());
        for (JavaMethod metodo : servicio.getMethods()) {
            for (JavaClass parametro : metodo.getRawParameterTypes()) {
                if (!isOwnCode(parametro)) {
                    continue;
                }
                for (JavaField campo : parametro.getAllFields()) {
                    if (nombreDeRecurso.equals(campo.getName())) {
                        return false;
                    }
                }
            }
            if (nombresDeParametro(metodo).filter(n -> n.contains(nombreDeRecurso)).isPresent()) {
                return false;
            }
        }
        return true;
    }

    /** {@code EmployeeJpaEntity} → {@code employeeId}. */
    private static String comoRecurso(String nombreDeEntidad) {
        String nucleo = nombreDeEntidad.endsWith(SUFIJO_ENTIDAD_JPA)
                ? nombreDeEntidad.substring(0,
                        nombreDeEntidad.length() - SUFIJO_ENTIDAD_JPA.length())
                : nombreDeEntidad;
        return Character.toLowerCase(nucleo.charAt(0)) + nucleo.substring(1) + "Id";
    }

    private static boolean yaSabeAcotar(JavaClass servicio) {
        for (JavaMethod metodo : servicio.getMethods()) {
            for (JavaMethodCall llamada : metodo.getMethodCallsFromSelf()) {
                JavaClass puerto = llamada.getTargetOwner();
                if (!isOwnCode(puerto) || !puerto.getPackageName().contains(PORT_OUT_PACKAGE)) {
                    continue;
                }
                if (llamada.getTarget().getName().contains("Company")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * {@code true} si la llamada resuelve una referencia que va a quedar guardada
     * en la fila: un {@code find…} que devuelve un tipo del proyecto (el
     * {@code XxxRef} de la feature), envuelto o no en {@code Optional}.
     *
     * <p>
     * Deja fuera los predicados y las proyecciones del mismo puerto
     * —{@code isOpen()}, {@code lockForUpdate()}, {@code outstandingAmount()},
     * {@code findStoragePath()}—: no producen la referencia que se escribe, y
     * acotarlos es otra conversación.
     */
    private static boolean resuelveUnaReferencia(JavaMethod method) {
        if (!method.getName().startsWith("find")) {
            return false;
        }
        JavaType retorno = method.getReturnType();
        if (isOwnCode(retorno.toErasure())) {
            return true;
        }
        return retorno instanceof JavaParameterizedType parametrizado
                && parametrizado.getActualTypeArguments().stream()
                        .anyMatch(argumento -> isOwnCode(argumento.toErasure()));
    }

    /**
     * La entidad JPA que hay detrás de un puerto de salida: se busca en los
     * repositorios que inyectan sus implementaciones, y se acepta solo la que
     * coincide con el núcleo del nombre del puerto.
     */
    private static Optional<JavaClass> entidadReferidaPor(JavaClass puerto) {
        String esperada = nucleoDelPuerto(puerto.getSimpleName()) + SUFIJO_ENTIDAD_JPA;
        for (JavaClass adaptador : puerto.getAllSubclasses()) {
            if (!isOwnCode(adaptador)) {
                continue;
            }
            for (JavaField campo : adaptador.getAllFields()) {
                JavaClass tipo = campo.getRawType();
                if (!isOwnCode(tipo) || !tipo.isAssignableTo(JpaRepository.class)) {
                    continue;
                }
                Optional<JavaClass> entidad = entidadDe(tipo)
                        .filter(e -> esperada.equals(e.getSimpleName()));
                if (entidad.isPresent()) {
                    return entidad;
                }
            }
        }
        return Optional.empty();
    }

    /** {@code AnimalQueryPort} → {@code Animal}. El sufijo es ruido del naming. */
    private static String nucleoDelPuerto(String simpleName) {
        for (String sufijo : List.of("QueryPort", "ValidationPort", "Repository", "Port")) {
            if (simpleName.endsWith(sufijo)) {
                return simpleName.substring(0, simpleName.length() - sufijo.length());
            }
        }
        return simpleName;
    }

    private static boolean esDeLaMismaFeature(JavaClass clazz, JavaPackage feature) {
        return paqueteDeLaFeature(clazz).map(p -> p.getName().equals(feature.getName()))
                .orElse(false);
    }

    /**
     * Las variantes acotadas que el puerto declara para el mismo método. Son una
     * <em>familia</em> y no un nombre porque un puerto puede declarar más de una
     * forma de acotar la misma carga, y todas valen:
     *
     * <ul>
     * <li>La <b>sobrecarga homónima</b> que recibe {@code companyId}
     * ({@code reactivate(id)} / {@code reactivate(id, companyId)}).</li>
     * <li>La <b>del mismo criterio</b>: el nombre, quitado el sufijo
     * {@code AndCompanyId}, termina en la misma cláusula {@code By…} que el ancho.
     * Así {@code findById} empareja tanto con {@code findByIdAndCompanyId} como con
     * {@code findOwnedByIdAndCompanyId}, y {@code findByIdForUpdate} con
     * {@code findByIdForUpdateAndCompanyId}.</li>
     * </ul>
     *
     * <p>
     * <b>Por qué la familia y no el nombre exacto.</b> Los cuatro catálogos por
     * empresa —{@code surgerytype}, {@code vaccinationtype},
     * {@code laboratorytesttype}, {@code diagnosticimagingtype}— declaran
     * <em>dos</em> finders acotados con propósitos distintos, y su javadoc lo dice:
     * {@code findByIdAndCompanyId} es el de <b>lectura</b> (la fila propia o
     * cualquiera de las generales) y {@code findOwnedByIdAndCompanyId} el de
     * <b>escritura</b> (SOLO la propia, porque editar una general la cambiaría para
     * todos los tenants). Sus ocho {@code Update…}/{@code Delete…} usan el segundo,
     * que es el <em>estrictamente más fuerte</em>, y la condición los marcaba a los
     * ocho por no llamar al primero: exigía cambiar código correcto por código
     * menos seguro. La cláusula {@code By…} es la señal de que las dos cargan lo
     * mismo por el mismo criterio, con la empresa encima.
     *
     * <p>
     * La comparación exige además el <b>mismo tipo de retorno</b> y que la acotada
     * reciba exactamente un parámetro más, para que no empareje con un finder que
     * responde otra pregunta ({@code findByIdIncludingDisabledAndCompanyId} tiene
     * cláusula {@code ByIdIncludingDisabled} y no cuenta como hermana de
     * {@code findById}).
     */
    private static List<JavaMethod> hermanasAcotadasDelPuerto(JavaClass puerto, JavaMethod metodo) {
        List<JavaMethod> hermanas = new ArrayList<>();
        for (JavaMethod otro : puerto.getAllMethods()) {
            if (otro.equals(metodo) || !filtraPorEmpresa(otro)) {
                continue;
            }
            if (otro.getName().equals(metodo.getName())) {
                hermanas.add(otro);
                continue;
            }
            if (cargaLoMismoAcotando(otro, metodo)) {
                hermanas.add(otro);
            }
        }
        return hermanas;
    }

    private static Optional<JavaMethod> hermanaAcotadaDelPuerto(JavaClass puerto,
            JavaMethod metodo) {
        return hermanasAcotadasDelPuerto(puerto, metodo).stream().findFirst();
    }

    /**
     * {@code true} si {@code acotada} responde la misma pregunta que {@code ancha}
     * más la empresa: mismo tipo de retorno, un parámetro más, y la misma cláusula
     * {@code By…} una vez quitado el sufijo {@code AndCompanyId}.
     */
    private static boolean cargaLoMismoAcotando(JavaMethod acotada, JavaMethod ancha) {
        if (!acotada.getRawReturnType().equals(ancha.getRawReturnType())) {
            return false;
        }
        List<JavaClass> deLaAcotada = acotada.getRawParameterTypes();
        List<JavaClass> deLaAncha = ancha.getRawParameterTypes();
        if (deLaAcotada.size() != deLaAncha.size() + 1) {
            return false;
        }
        for (int i = 0; i < deLaAncha.size(); i++) {
            if (!deLaAcotada.get(i).equals(deLaAncha.get(i))) {
                return false;
            }
        }
        String clausula = clausulaBy(sinElSufijoAcotado(acotada.getName()));
        return !clausula.isEmpty() && clausula.equals(clausulaBy(ancha.getName()));
    }

    /**
     * El sufijo {@code AndCompanyId} es <b>opcional</b>, y esa es la diferencia
     * entre reconocer la hermana acotada y no reconocerla. El proyecto la escribe
     * de las dos formas: {@code findOwnedByIdAndCompanyId} la lleva en el nombre, y
     * {@code MedicamentQueryPort.findAvailableById(id, companyId)} la lleva solo en
     * la firma. Quien decide si acota es {@code filtraPorEmpresa}, que mira el
     * parámetro; el nombre solo sirve para comprobar que las dos cargan por el
     * mismo criterio.
     */
    private static String sinElSufijoAcotado(String nombre) {
        return nombre.endsWith(SUFIJO_ACOTADO)
                ? nombre.substring(0, nombre.length() - SUFIJO_ACOTADO.length())
                : nombre;
    }

    /** {@code findOwnedById} → {@code ById}; {@code save} → {@code ""}. */
    private static String clausulaBy(String nombre) {
        int desde = nombre.lastIndexOf("By");
        return desde < 0 ? "" : nombre.substring(desde);
    }

    private static List<String> describe(List<JavaMethod> path, JavaMethodCall finalCall) {
        List<String> steps = new ArrayList<>();
        for (JavaMethod method : path) {
            steps.add(method.getOwner().getSimpleName() + "." + method.getName() + "()");
        }
        steps.add(finalCall.getTargetOwner().getSimpleName() + "." + finalCall.getTarget().getName()
                + "()");
        return steps;
    }

    // ── BE-10: cada adaptador con su rodaja ──────────────────────────────────

    /** Sufijo de las rodajas de persistencia; failsafe solo recoge {@code *IT}. */
    private static final String SUFIJO_RODAJA_PERSISTENCIA = "IT";

    /**
     * Sufijo de las rodajas web; surefire solo recoge {@code *Test}/{@code *Tests}.
     */
    private static final String SUFIJO_RODAJA_WEB = "Test";

    /** Marca de {@code target/test-classes} en el URI de origen de una clase. */
    private static final String SALIDA_DE_TEST = "/test-classes/";

    /**
     * Deja fuera lo que no es código de producción. El {@code @AnalyzeClasses} de
     * {@link PiramideDeTestsTest} importa {@code src/test} a propósito —lo necesita
     * para <em>encontrar</em> las rodajas—, y eso mete en el universo cosas que
     * parecen adaptadores sin serlo.
     *
     * <p>
     * El caso real: {@code GlobalExceptionHandlerTest.BoomController}, un
     * {@code @RestController} anidado de juguete cuyas rutas lanzan cada excepción
     * que se quiere mapear. Sin este filtro la regla exigía un
     * {@code BoomControllerTest} —una rodaja para un fixture—, y esa violación
     * fantasma habría entrado al store congelada, contaminando el indicador desde
     * el primer día.
     *
     * <p>
     * Dos filtros, porque hacen falta los dos: el URI de origen descarta lo que
     * viene de {@code target/test-classes} (la misma señal que usa
     * {@code ImportOption.DoNotIncludeTests}), y {@code getEnclosingClass} descarta
     * las anidadas, que nunca son un adaptador ni un controller de producción.
     */
    static DescribedPredicate<JavaClass> sonCodigoDeProduccion() {
        return DescribedPredicate.describe("son codigo de produccion",
                VetSoftwareConditions::esCodigoDeProduccion);
    }

    private static boolean esCodigoDeProduccion(JavaClass javaClass) {
        return javaClass.getEnclosingClass().isEmpty() && !vieneDelArbolDeTest(javaClass);
    }

    /**
     * La mitad de {@link #sonCodigoDeProduccion()} que mira <em>de dónde salió el
     * {@code .class}</em>, sin el filtro de clases anidadas.
     *
     * <p>
     * Existe separada porque hay una regla —{@code DOBLE_DE_TEST_NO_ESCANEABLE}— a
     * la que el filtro de anidadas le sobra, y no en el sentido de que le dé igual:
     * las clases anidadas son <strong>el 100 %</strong> de lo que esa regla
     * persigue, así que negar {@code sonCodigoDeProduccion()} entero descartaría
     * exactamente los casos que hay que cazar. Extraída aquí para que las dos
     * reglas compartan la única constante que dice qué es
     * {@code target/test-classes}: dos copias de esa cadena es como una de las dos
     * se queda atrás.
     */
    static DescribedPredicate<JavaClass> vienenDelArbolDeTest() {
        return DescribedPredicate.describe("vienen del arbol de test",
                VetSoftwareConditions::vieneDelArbolDeTest);
    }

    /**
     * Sin origen conocido no se puede afirmar que sea de test: se trata como
     * producción, igual que hacía el chequeo del que se extrajo.
     */
    private static boolean vieneDelArbolDeTest(JavaClass javaClass) {
        return javaClass.getSource()
                .map(source -> source.getUri().toString().contains(SALIDA_DE_TEST)).orElse(false);
    }

    /**
     * Exige que todo adaptador {@code JpaXxxRepository} tenga en su mismo paquete
     * una rodaja de persistencia: una clase {@code *IT} cuyo nombre contenga el
     * núcleo del adaptador ({@code JpaStockLotRepository} →
     * {@code StockLotPersistenceIT}).
     *
     * <p>
     * <strong>Por qué el nombre y no solo el paquete.</strong> Bastaría con exigir
     * «algún {@code *IT} en el paquete», pero entonces una sola rodaja taparía los
     * dieciséis adaptadores de {@code animal.infrastructure.persistence} y el
     * indicador dejaría de contar lo que importa. Cruzar por el núcleo del nombre
     * hace que la unidad de medida sea el adaptador, que es la unidad de riesgo:
     * cada uno traduce su propio SQL y su propio {@code Sort}.
     *
     * <p>
     * <strong>Qué queda fuera, a propósito.</strong> Solo entran los adaptadores
     * que siguen el naming del CLAUDE.md ({@code Jpa} + entidad +
     * {@code Repository}). Los {@code JpaXxxQueryPort},
     * {@code JpaXxxValidationPort} y los adaptadores sueltos
     * ({@code NumberingAllocationAdapter}, {@code DianJobLeaseAdapter}) no se
     * exigen: son proyecciones de una consulta o de un lease, y su rodaja aporta
     * bastante menos que la del adaptador que escribe. Ampliar el alcance es
     * ampliar el predicado, no relajar esta condición.
     */
    static ArchCondition<JavaClass> tenerRodajaDePersistencia() {
        return new ArchCondition<>("tener una rodaja *IT en su mismo paquete") {
            @Override
            public void check(JavaClass adaptador, ConditionEvents events) {
                String nucleo = nucleoDelAdaptador(adaptador.getSimpleName());
                boolean cubierto = hermanasDePaquete(adaptador)
                        .anyMatch(nombre -> nombre.endsWith(SUFIJO_RODAJA_PERSISTENCIA)
                                && nombre.contains(nucleo));
                events.add(new SimpleConditionEvent(adaptador, cubierto,
                        adaptador.getSimpleName() + " no tiene rodaja de persistencia: falta "
                                + nucleo + "PersistenceIT (@DataJpaTest) en "
                                + adaptador.getPackageName()));
            }
        };
    }

    /**
     * Exige que todo {@code @RestController} tenga en su mismo paquete una rodaja
     * web: una clase {@code *Test} cuyo nombre empiece por el del controller
     * ({@code AnimalController} → {@code AnimalControllerTest}).
     *
     * <p>
     * <strong>Por qué el prefijo y no «algún {@code *Test} en el paquete».</strong>
     * Porque en {@code infrastructure/web} conviven rodajas y tests unitarios
     * corrientes: {@code RefreshTokenCookieTest}, {@code CashArqueoCsvTest} e
     * {@code InventoryCsvTest} son JUnit puro sobre un helper, no ejercitan ningún
     * endpoint, y un criterio por paquete los contaría como red que no existe.
     * Exigir el prefijo del controller elimina los tres falsos positivos sin
     * enumerar excepciones.
     *
     * <p>
     * <strong>El {@code GlobalExceptionHandler} no cuenta como controller</strong>:
     * es {@code @RestControllerAdvice}, una anotación distinta que no está
     * meta-anotada con {@code @RestController}, así que el predicado ni lo mira.
     */
    static ArchCondition<JavaClass> tenerRodajaWeb() {
        return new ArchCondition<>("tener una rodaja *Test en su mismo paquete") {
            @Override
            public void check(JavaClass controller, ConditionEvents events) {
                String esperado = controller.getSimpleName() + SUFIJO_RODAJA_WEB;
                boolean cubierto = hermanasDePaquete(controller)
                        .anyMatch(nombre -> nombre.endsWith(SUFIJO_RODAJA_WEB)
                                && nombre.startsWith(controller.getSimpleName()));
                events.add(new SimpleConditionEvent(controller, cubierto,
                        controller.getSimpleName() + " no tiene rodaja web: falta " + esperado
                                + " (@WebMvcTest) en " + controller.getPackageName()));
            }
        };
    }

    /**
     * {@code JpaStockLotRepository} → {@code StockLot}. El núcleo es lo que
     * comparten el adaptador y su rodaja; el prefijo {@code Jpa} y el sufijo
     * {@code Repository} son ruido del naming.
     */
    private static String nucleoDelAdaptador(String simpleName) {
        String nucleo = simpleName.substring("Jpa".length());
        return nucleo.substring(0, nucleo.length() - "Repository".length());
    }

    /**
     * Nombres simples de las clases de nivel superior del mismo paquete —de los dos
     * árboles, porque el {@code @AnalyzeClasses} de {@code PiramideDeTestsTest} sí
     * importa {@code src/test}—. Sin subpaquetes: un test de {@code web/request} no
     * es la rodaja del controller. Sin clases anidadas: las {@code @Nested} de una
     * rodaja no son rodajas.
     */
    private static Stream<String> hermanasDePaquete(JavaClass javaClass) {
        return javaClass.getPackage().getClasses().stream()
                .filter(hermana -> hermana.getEnclosingClass().isEmpty())
                .filter(hermana -> !hermana.equals(javaClass)).map(JavaClass::getSimpleName);
    }
}
