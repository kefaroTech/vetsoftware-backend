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

| Workflow | Rama | Environment | Tags ECR | Retención |
|---|---|---|---|---|
| `publish-dev-image.yml` | `develop` | `development` | `dev-<12 caracteres>` | 10 imágenes |
| `publish-release.yml` | `main` | `production` | `X.Y.Z` y `sha-<12 caracteres>` | 30 imágenes |

Son roles IAM distintos con trust policies distintas: el publicador de releases solo confía en el environment `production` y el de desarrollo solo en `development`. Ninguna credencial se comparte entre los dos ciclos.

Este repositorio publica el artefacto y ahí termina su responsabilidad. **No dispara ningún despliegue.** Ambos workflows dejan en el Summary de su run los cuatro datos auditables —identificador de la imagen, digest, commit completo y URL del run— para que quien opera la infraestructura ejecute `Deploy backend image dev` o `Deploy backend image prod` desde `VetSoftwareIaC`, con la aprobación de su propio environment.

No existe GitHub App, token cruzado ni despacho remoto. La confianza va en una sola dirección: el repositorio IaC concede permiso OIDC de mínimo privilegio para publicar en ECR; el backend no obtiene la capacidad inversa de iniciar un cambio en la infraestructura.
