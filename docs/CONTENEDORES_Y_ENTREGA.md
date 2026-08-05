# Contenedores y entrega

## Stack local completo

El archivo `docker-compose.override.yml` agrega backend y los dos fronts al stack de infraestructura. El perfil `application` evita construirlos cuando solo se necesitan las dependencias.

Desde este repositorio:

```powershell
$env:GRAFANA_PASSWORD = "use-un-valor-local-seguro"
docker compose --profile application up --build
```

Servicios de aplicación:

- API: `http://localhost:8080/api/v1`
- Front privado: `http://localhost:5173`
- Front público: `http://localhost:5174`
- Readiness del backend: `http://localhost:8080/api/v1/actuator/health/readiness`

El backend usa los nombres DNS de Compose (`mysql`, `redis`, `localstack` y `otel-collector`). Prometheus raspa `backend:8080`; ya no depende de una JVM ejecutada en el host.

Para iniciar únicamente las dependencias:

```powershell
$env:GRAFANA_PASSWORD = "use-un-valor-local-seguro"
docker compose up
```

## Imagen del backend

El `Dockerfile` compila con Maven y Java 25, y ejecuta con Eclipse Temurin JRE 25 sobre Ubuntu Noble. La imagen final:

- se construye para `linux/arm64`, igual que la task definition de ECS;
- corre como UID/GID `10001`, sin privilegios de root;
- incluye fuentes Noto y Fontconfig para OpenHTMLToPDF;
- usa el mismo endpoint de readiness que Docker, ECS y el target group;
- fija por digest las imágenes base.

El CI construye la imagen de forma efímera sin publicarla en ECR. Una release SemVer aprobada en el environment `production` publica los tags inmutables `X.Y.Z` y `sha-<12 caracteres>`, resuelve el digest `sha256`, espera el escaneo ECR y bloquea la release ante hallazgos High o Critical. Un despacho manual solo puede ejecutarse desde `main`.

Cada entorno publica su propia imagen y ninguno depende del otro:

| Workflow | Rama | Environment | Repositorio ECR | Tags | Retención |
|---|---|---|---|---|---|
| `publish-dev-image.yml` | `develop` | `development` | `vetsoftware-dev-backend` | `X.Y.Z-dev.N` y `dev-<12 caracteres>` | 10 imágenes |
| `publish-release.yml` | `main` | `production` | `vetsoftware-backend` | `X.Y.Z` y `sha-<12 caracteres>` | 30 imágenes |

Son roles IAM distintos con trust policies distintas: el publicador de releases solo confía en el environment `production` y el de desarrollo solo en `development`. Ninguna credencial se comparte entre los dos ciclos.

Este repositorio publica el artefacto y ahí termina su responsabilidad. **No dispara ningún despliegue.** Quien opera la infraestructura ejecuta `Deploy backend image dev` o `Deploy backend image prod` desde `VetSoftwareIaC`, con la aprobación de su propio environment, y **el único dato que escribe es la versión**: el digest, el commit y el run de origen los resuelve el propio despliegue desde ECR —el digest por el tag, y el commit y el run desde los labels OCI que la imagen ya trae—. El Summary del run de publicación y, en desarrollo, un comentario en el pull request que originó el merge dicen qué versión salió.

No existe GitHub App, token cruzado ni despacho remoto. La confianza va en una sola dirección: el repositorio IaC concede permiso OIDC de mínimo privilegio para publicar en ECR; el backend no obtiene la capacidad inversa de iniciar un cambio en la infraestructura.

## Versión de la imagen de desarrollo

Cada merge a `develop` calcula la versión siguiente en formato `X.Y.Z-dev.N`, la commitea en `pom.xml`, `package.json` y `package-lock.json`, y construye la imagen **desde ese commit**: el artefacto y el repositorio declaran lo mismo. El mapeo de tipo de commit a dígito está en `CLAUDE.md` y lo implementa `.github/scripts/dev-version.mjs`.

`-dev.N` es un identificador de pre-release de SemVer, así que su orden lo define la propia especificación: `1.1.0-dev.1 < 1.1.0-dev.2 < 1.1.0-dev.10 < 1.1.0`. De ahí sale una guarda gratis, porque **la forma de la versión dice a qué ambiente pertenece**:

| Workflow de despliegue | Acepta | Rechaza |
|---|---|---|
| `Deploy backend image dev` | `X.Y.Z-dev.N` | una release: no se despliega producción en dev por error |
| `Deploy backend image prod` | `X.Y.Z` | cualquier cosa con `-dev.`: no se cuela un build de develop en producción |

El orden dentro del workflow no es negociable: **primero el commit de versión, después el build.** Al revés la imagen saldría de un commit cuyo `pom.xml` todavía declara la versión anterior.

## Quién puede escribir en `develop`

El versionado automático de desarrollo necesita que la publicación commitee la versión calculada en `develop` antes de construir la imagen. La decisión de gobierno es un **bypass acotado**: se autoriza a un único actor, `github-actions[bot]`, a escribir en esa rama, y solo desde el workflow que publica la imagen de desarrollo.

Cómo está materializado hoy:

| Capa | Estado | Consecuencia |
|---|---|---|
| Ruleset / branch protection de `develop` | No existe. La organización está en plan Free y el repositorio es privado, así que GitHub no ofrece la función (`/rules` y `/branches/develop/protection` responden 403) | No hay lista de bypass que configurar: `develop` acepta push directo de cualquiera con permiso de escritura |
| Default de permisos de Actions en el repositorio | `read` | Ningún workflow hereda escritura por omisión |
| `publish-dev-image.yml` | `contents: write` + `pull-requests: write` declarados en el propio workflow | Es el único punto del repositorio donde el bot puede commitear y comentar |

La política GitFlow —todo cambio entra a `develop` por pull request— sigue vigente como **convención de equipo**, no como regla aplicada por la plataforma. El bump automático es su única excepción declarada.

**Si la organización sube a un plan con rulesets**, la regla que exija pull request sobre `develop` debe crearse ya con `github-actions[bot]` en la lista de bypass; sin esa entrada, el bump falla al hacer push y ningún merge produce imagen.

### Por qué `GITHUB_TOKEN` y no una GitHub App

El push del bump usa el `GITHUB_TOKEN` efímero del run, no un PAT ni una App instalada. Eso mantiene en pie el principio de la sección anterior —ninguna credencial de larga vida cruza repositorios— y trae un efecto secundario deliberado: **los push hechos con `GITHUB_TOKEN` no disparan nuevos workflow runs**. De ahí se siguen dos cosas:

- El bucle de bump→push→bump es imposible por construcción. El guard `if: github.actor != 'github-actions[bot]'` se mantiene igualmente, porque deja de ser gratis en cuanto alguien cambie el token.
- `Backend CI` no se vuelve a ejecutar sobre el commit de versión. Lo que se valida sigue siendo el commit que revisó el PR; lo que se empaqueta es su hijo, que difiere solo en `pom.xml`, `package.json` y `package-lock.json`. Si en algún momento se quiere CI sobre el commit exacto que produjo la imagen, hay que cambiar a una GitHub App dedicada y aceptar la doble ejecución.
