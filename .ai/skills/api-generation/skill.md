# Skill · api-generation

> Cómo generar la capa REST: controllers, DTOs, requests, responses, manejo de errores y OpenAPI.
> **Copilot debe reutilizar los patrones de API detectados en otros proyectos del workspace** (estructura de controllers, formato de error, configuración OpenAPI, wrappers de paginación). Los ejemplos se obtienen de allí.

## 0. Descubrimiento de patrones (obligatorio)

1. Buscar en el workspace: `@RestController`, `@RestControllerAdvice`, `@ControllerAdvice`, `ProblemDetail`, `ErrorResponse`, `OpenAPI` bean, `@Tag`, `@Operation`, `springdoc` en `application.yml`.
2. Determinar:
   - Prefijo y versionado de rutas.
   - Si los DTOs son `record` o clases; sufijos usados (`Request/Response` vs `Dto`).
   - Formato de error estándar.
   - Si se devuelve `Page<T>` directamente o un wrapper (`PageResponse<T>`).
   - Si los controllers implementan una interfaz anotada con OpenAPI (`XxxApi`) o anotan directamente.
3. Replicar y registrar en `references/patterns.md`.

## 1. Controllers REST

- **C1.** `@RestController` + `@RequestMapping("/api/v1/<recursos>")` + `@RequiredArgsConstructor` + `@Validated` + `@Slf4j`.
- **C2.** Solo dependen de **servicios**. Sin repositorios, sin entidades, sin lógica de negocio.
- **C3.** Todos los métodos devuelven **`ResponseEntity<T>`**.
- **C4.** `@Valid` en cada `@RequestBody`; validaciones en `@PathVariable`/`@RequestParam` (`@Positive`, `@Size`...).
- **C5.** Semántica HTTP:

| Operación | Método | Ruta | Respuesta |
|-----------|--------|------|-----------|
| Crear | POST | `/api/v1/bikes` | `201 Created` + header `Location` + body |
| Obtener | GET | `/api/v1/bikes/{id}` | `200 OK` |
| Listar | GET | `/api/v1/bikes?page=&size=&sort=` | `200 OK` + página |
| Reemplazar | PUT | `/api/v1/bikes/{id}` | `200 OK` |
| Modificar parcial | PATCH | `/api/v1/bikes/{id}` | `200 OK` |
| Eliminar | DELETE | `/api/v1/bikes/{id}` | `204 No Content` |
| Acción de negocio | POST | `/api/v1/components/{id}/replacement` | `200/201` |

- **C6.** Recursos anidados solo un nivel: `/api/v1/bikes/{bikeId}/components`.
- **C7.** `Location` con `ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(id).toUri()`.
- **C8.** Paginación con `@ParameterObject @PageableDefault(...) Pageable pageable`.

## 2. DTOs

- **DT1.** Paquete `dto`. `record` inmutables.
- **DT2.** Tipos: `XxxCreateRequest`, `XxxUpdateRequest`, `XxxPatchRequest` (si hay PATCH), `XxxResponse`, `XxxSummaryResponse`, `XxxFilter`.
- **DT3.** Nunca reutilizar el mismo DTO para entrada y salida.
- **DT4.** Anotación `@Schema(description = ..., example = ...)` en cada campo relevante.

## 3. Requests

- **RQ1.** Bean Validation obligatoria: `@NotNull`, `@NotBlank`, `@Size(max = n)`, `@Positive`, `@PositiveOrZero`, `@PastOrPresent`, `@Email`, `@Pattern`, `@Digits`.
- **RQ2.** Validaciones cruzadas con anotación de clase personalizada (`@ValidDateRange`) en paquete `dto.validation` (o el del workspace).
- **RQ3.** No incluir `id`, propietario, auditoría ni campos calculados: el propietario se obtiene del contexto de seguridad.
- **RQ4.** Límites de tamaño en todos los `String` y colecciones (`@Size(max = ...)`).

## 4. Responses

- **RS1.** Exponer solo lo necesario; nunca campos internos (`version`, hashes, flags técnicos).
- **RS2.** Relaciones como id + nombre resumido, no grafos completos.
- **RS3.** Fechas en ISO-8601 (`Instant`/`LocalDate`); Jackson con `WRITE_DATES_AS_TIMESTAMPS=false`.
- **RS4.** Listados: `Page<XxxResponse>` o wrapper del workspace; considerar `PagedModel` / `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)` para un JSON estable.

## 5. Manejo de errores

- **ER1.** Un único `GlobalExceptionHandler` (`@RestControllerAdvice`) en `exception`.
- **ER2.** Formato: el del workspace; por defecto `ProblemDetail` (RFC 9457) con `type`, `title`, `status`, `detail`, `instance`, y propiedades `code`, `timestamp`, `errors[]`.
- **ER3.** Mapeo mínimo:

| Excepción | HTTP |
|-----------|------|
| `MethodArgumentNotValidException`, `ConstraintViolationException`, `HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException` | 400 |
| `AccessDeniedException` | 403 |
| `ResourceNotFoundException` | 404 |
| `ConflictException`, `DataIntegrityViolationException`, `OptimisticLockingFailureException` | 409 |
| `BusinessRuleException` | 422 (o 400 según workspace) |
| `Exception` | 500 (mensaje genérico) |

- **ER4.** Nunca devolver stack traces, nombres de clases, SQL ni mensajes internos.
- **ER5.** Log: `warn` para 4xx de negocio, `error` con stack trace para 5xx.

## 6. OpenAPI

- **OA1.** Dependencia `springdoc-openapi-starter-webmvc-ui` (versión del workspace).
- **OA2.** `config/OpenApiConfig` con bean `OpenAPI`: título "Bike Garage API", versión, descripción, contacto, `SecurityScheme` (bearer JWT) si hay seguridad.
- **OA3.** Cada controller con `@Tag(name = "Bikes", description = ...)`.
- **OA4.** Cada endpoint con `@Operation(summary, description)` y `@ApiResponse` para cada código posible, referenciando el esquema de error.
- **OA5.** Swagger UI deshabilitado en `prod` (`springdoc.swagger-ui.enabled=false`) salvo decisión explícita.
- **OA6.** Si el workspace usa enfoque *contract-first* (`openapi-generator-maven-plugin`), seguirlo en lugar de anotaciones.

## 7. Checklist

- [ ] Controller sin lógica ni repositorios
- [ ] `ResponseEntity` + códigos HTTP correctos
- [ ] `@Valid` y Bean Validation completa
- [ ] DTOs separados entrada/salida
- [ ] Errores con formato estándar, sin fugas
- [ ] OpenAPI completo
- [ ] Patrón del workspace referenciado

## 8. Referencias

`references/` debe contener, extraídos del workspace: un controller CRUD completo, `GlobalExceptionHandler`, formato de error y `OpenApiConfig`, indicando proyecto y ruta de origen.
