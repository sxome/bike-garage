# Skill · security-generation

> Reglas de seguridad para la API REST de `bike-garage`.
> **Antes de aplicar, analizar la configuración de seguridad de los proyectos del workspace** (`SecurityFilterChain`, JWT/OAuth2 Resource Server, CORS, manejo de 401/403) y reutilizar su patrón. Este skill define los mínimos obligatorios.

## 0. Descubrimiento de patrones (obligatorio)

1. Buscar: `SecurityFilterChain`, `@EnableWebSecurity`, `@EnableMethodSecurity`, `@PreAuthorize`, `oauth2ResourceServer`, `JwtDecoder`, `CorsConfigurationSource`, `AuthenticationEntryPoint`, `AccessDeniedHandler`.
2. Si el workspace usa seguridad, replicarla. Si no usa ninguna, el Arquitecto debe registrar un ADR (p. ej. OAuth2 Resource Server con JWT) — `spring-boot-starter-security` y `oauth2-resource-server` se consideran parte del ecosistema Spring Boot permitido.
3. Registrar en `references/patterns.md`.

## 1. Seguridad de la API REST

- **SEC1.** API **stateless**: `SessionCreationPolicy.STATELESS`; CSRF deshabilitado **solo** si la autenticación es por token Bearer.
- **SEC2.** Autenticación por JWT (Resource Server) o el mecanismo del workspace. Nunca autenticación casera.
- **SEC3.** Por defecto **todo autenticado** (`anyRequest().authenticated()`); whitelist explícita mínima (`/actuator/health`, docs OpenAPI solo en no-prod).
- **SEC4.** CORS con orígenes explícitos por configuración; prohibido `*` con credenciales.
- **SEC5.** Cabeceras de seguridad por defecto de Spring Security activas (HSTS en prod, `X-Content-Type-Options`, `X-Frame-Options`).
- **SEC6.** Actuator: exponer solo `health` e `info` públicamente.
- **SEC7.** El usuario actual se obtiene del `SecurityContext` (p. ej. componente `CurrentUserProvider`), **nunca** de un parámetro de la petición.

## 2. Validaciones de entrada

- **VAL1.** Toda entrada (body, path, query, headers usados) se valida con Bean Validation.
- **VAL2.** Longitud máxima en todos los `String`; rangos en numéricos; listas con `@Size(max)`.
- **VAL3.** Whitelist sobre blacklist: enums y `@Pattern` para valores acotados.
- **VAL4.** Campos de ordenación de `Pageable` validados contra lista permitida.
- **VAL5.** Tamaño máximo de petición y de página configurados.
- **VAL6.** Jackson: `FAIL_ON_UNKNOWN_PROPERTIES = true` si el workspace lo aplica (evita mass assignment); en cualquier caso los DTO no contienen campos sensibles (propietario, rol, id).

## 3. OWASP Top 10 (2021) aplicado

| ID | Riesgo | Medida obligatoria en bike-garage |
|----|--------|-----------------------------------|
| A01 | Broken Access Control | Toda consulta filtrada por propietario (`findByIdAndOwnerId`, Specification `ownedBy`). Recurso ajeno → **404** (no revelar existencia). `@PreAuthorize` para roles. |
| A02 | Cryptographic Failures | TLS en prod; sin secretos en código ni repositorio; contraseñas (si existen) con `BCryptPasswordEncoder`/`DelegatingPasswordEncoder`. |
| A03 | Injection | JPQL/Criteria con parámetros; prohibida concatenación; SQL nativo solo parametrizado. |
| A04 | Insecure Design | Reglas de negocio en servicio; límites de uso (paginación, tamaño). |
| A05 | Security Misconfiguration | `open-in-view=false`, Swagger off en prod, errores sin detalles, actuator restringido. |
| A06 | Vulnerable Components | Versiones alineadas con el BOM de Spring Boot; revisar CVEs (dependency-check si el workspace lo usa). |
| A07 | Identification & Auth Failures | Delegar en Spring Security / IdP; validar `iss`, `aud`, expiración del JWT. |
| A08 | Software & Data Integrity | Dependencias desde repositorios confiables; sin deserialización de tipos polimórficos arbitrarios. |
| A09 | Logging & Monitoring Failures | Log de eventos de seguridad (accesos denegados, errores de autenticación) sin datos sensibles; correlación con traceId. |
| A10 | SSRF | No realizar peticiones a URLs proporcionadas por el usuario; si es imprescindible, whitelist de hosts. |

## 4. Gestión de errores segura

- **ERR1.** Respuestas de error homogéneas vía `GlobalExceptionHandler` (ver `api-generation`).
- **ERR2.** Nunca exponer stack traces, mensajes de excepción técnicos, SQL, rutas internas o versiones (`server.error.include-stacktrace=never`, `include-message=never`).
- **ERR3.** 401 y 403 gestionados por `AuthenticationEntryPoint` y `AccessDeniedHandler` con el mismo formato de error.
- **ERR4.** Mensajes que no permitan enumeración (p. ej. no distinguir "no existe" de "no es tuyo").
- **ERR5.** Logs: nunca tokens, contraseñas, emails completos ni datos personales; enmascarar si es imprescindible.

## 5. Principio de mínimo privilegio

- **MP1.** Endpoints cerrados por defecto; se abren explícitamente.
- **MP2.** Roles mínimos (`USER`, `ADMIN`); operaciones administrativas con `@PreAuthorize("hasRole('ADMIN')")`.
- **MP3.** Cada servicio verifica la propiedad del recurso antes de leer/modificar (defensa en profundidad, no solo en controller).
- **MP4.** Usuario de BD de la aplicación sin permisos DDL en prod; migraciones con usuario separado.
- **MP5.** DTOs de respuesta exponen el mínimo de datos necesarios.
- **MP6.** Configuración sensible por variables de entorno o gestor de secretos.

## 6. Checklist

- [ ] Endpoints autenticados por defecto
- [ ] Filtro por propietario en todas las consultas
- [ ] Bean Validation completa con límites
- [ ] Sin concatenación en consultas
- [ ] Errores sin fugas de información
- [ ] Sin secretos ni datos personales en código/logs
- [ ] Tests de 401/403/404 para recursos ajenos
- [ ] Patrón de seguridad del workspace referenciado

## 7. Referencias

`references/` debe contener, extraídos del workspace: `SecurityConfig`, manejadores 401/403 y el proveedor de usuario actual, indicando proyecto y ruta de origen.
