# Skill · project-rules

> Reglas globales y **obligatorias** para todo el código de `bike-garage`. Este skill tiene prioridad sobre cualquier otro. Ante conflicto con un patrón del workspace, prevalece este documento.

## 0. Principio rector: consistencia antes que creatividad

1. Antes de generar cualquier clase, **analiza los demás proyectos Spring Boot abiertos en el workspace** (p. ej. `vida-util-piezas-api`) y localiza el equivalente más cercano.
2. Replica su estructura, estilo de anotaciones, nombres y forma de documentar.
3. Solo innova cuando no exista patrón previo, y documéntalo como ADR en el diseño.
4. Los ejemplos de implementación **no** se incluyen en este skill: deben extraerse del workspace y, una vez validados, guardarse en `references/`.

## 1. Stack obligatorio

| Tecnología | Uso |
|------------|-----|
| Java 21 | Records, `var` local con moderación, pattern matching, text blocks, switch expressions |
| Spring Boot 3.x | Web, Validation, Data JPA, Actuator (si el workspace lo usa) |
| Maven | Build. Versiones alineadas con el `pom.xml` del workspace |
| PostgreSQL | Base de datos |
| Spring Data JPA | Persistencia |
| Lombok | Reducción de boilerplate (con restricciones, ver §3) |
| MapStruct | Conversión entidad ↔ DTO |
| JUnit 5 + Mockito | Tests |
| springdoc-openapi | Documentación OpenAPI 3 |

Prohibido añadir dependencias fuera de esta lista sin ADR.

## 2. Organización de paquetes

```
com.<org>.bikegarage
├── BikeGarageApplication.java
├── config        → @Configuration (OpenAPI, JPA auditing, Jackson, seguridad)
├── controller    → @RestController
├── service       → interfaces de servicio
│   └── impl      → implementaciones (solo si el workspace separa interfaz/impl)
├── repository    → interfaces Spring Data
│   └── specification → Specifications JPA
├── entity        → @Entity, @Embeddable, enums persistidos, entidad base
├── dto           → records de request/response (subpaquetes por agregado si el workspace lo hace)
├── mapper        → @Mapper MapStruct
└── exception     → excepciones de dominio + GlobalExceptionHandler + ErrorResponse
```

- `<org>`: tomar del `groupId` de los proyectos del workspace.
- Tests replican el mismo paquete en `src/test/java`.

## 3. Convenciones Java

- **J1.** Java 21: usa `record` para DTOs inmutables; `switch` expressions; `instanceof` con pattern matching.
- **J2.** Campos `private final` siempre que sea posible.
- **J3.** **No usar field injection** (`@Autowired` sobre campos o setters). **Prohibido.**
- **J4.** **Preferir constructor injection** mediante `@RequiredArgsConstructor` de Lombok.
- **J5.** **No usar `@Data`** en ninguna clase (genera `equals/hashCode/toString` peligrosos en entidades JPA).
- **J6.** Lombok permitido: `@Getter`, `@Setter`, `@RequiredArgsConstructor`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`, `@Slf4j`, `@ToString(onlyExplicitlyIncluded = true)`.
- **J7.** No devolver `null` en colecciones: devolver colección vacía. No devolver `null` en búsquedas por id: usar `Optional` en repositorio y excepción en servicio.
- **J8.** Inmutabilidad: `List.copyOf`, `Set.copyOf` al exponer colecciones.
- **J9.** Fechas con `java.time` (`LocalDate`, `Instant`, `OffsetDateTime`). Prohibido `java.util.Date`.
- **J10.** Importes/medidas con `BigDecimal` cuando se requiera precisión.
- **J11.** Sin wildcard imports. Sin código comentado. Sin `System.out`.
- **J12.** Logs mediante **`@Slf4j`**, con placeholders: `log.info("Bike created id={}", id)`. Nunca concatenar ni loguear datos personales.

## 4. Convenciones Spring Boot

- **S1.** Controllers devuelven siempre **`ResponseEntity<T>`**.
- **S2.** Entradas validadas con **Bean Validation** (`jakarta.validation`) y `@Valid` / `@Validated`.
- **S3.** `@Transactional` solo en la capa de servicio. Clase con `@Transactional(readOnly = true)` y métodos de escritura con `@Transactional`.
- **S4.** Configuración en `application.yml` con perfiles (`local`, `test`, `prod`). Secretos por variables de entorno (`${DB_PASSWORD}`).
- **S5.** `spring.jpa.open-in-view=false`.
- **S6.** `spring.jpa.hibernate.ddl-auto=validate` (o `none`) fuera de local; esquema gestionado por migraciones si el workspace usa Flyway/Liquibase.
- **S7.** Propiedades propias con `@ConfigurationProperties` + record, no `@Value` disperso.

## 5. Nomenclatura

| Elemento | Patrón | Ejemplo |
|----------|--------|---------|
| Entidad | `<Concepto>` singular | `Bike` |
| Tabla | snake_case plural | `bikes`, `bike_components` |
| Columna | snake_case | `purchase_date` |
| Repositorio | `<Entidad>Repository` | `BikeRepository` |
| Servicio | `<Entidad>Service` / `<Entidad>ServiceImpl` | `BikeService` |
| Controller | `<Entidad>Controller` | `BikeController` |
| Mapper | `<Entidad>Mapper` | `BikeMapper` |
| DTO alta | `<Entidad>CreateRequest` | `BikeCreateRequest` |
| DTO modificación | `<Entidad>UpdateRequest` | `BikeUpdateRequest` |
| DTO salida | `<Entidad>Response` | `BikeResponse` |
| DTO filtro | `<Entidad>Filter` | `BikeFilter` |
| Excepción | `<Motivo>Exception` | `ResourceNotFoundException` |
| Specification | `<Entidad>Specifications` | `BikeSpecifications` |
| Test unitario | `<Clase>Test` | `BikeServiceImplTest` |
| Test integración | `<Clase>IT` | `BikeControllerIT` |
| Rutas REST | `/api/v1/<recurso-plural-kebab>` | `/api/v1/bikes` |
| Métodos servicio | `create`, `update`, `delete`, `findById`, `findAll` | — |

Si el workspace usa otra convención (p. ej. `XxxDto`), **se adopta la del workspace** y se documenta aquí.

## 6. Gestión de excepciones

- **E1.** Excepciones de dominio en `exception`, extienden `RuntimeException` (o una base `BikeGarageException`).
- **E2.** Mínimo: `ResourceNotFoundException` (404), `BusinessRuleException` (422 o 400 según workspace), `ConflictException` (409).
- **E3.** Un único `@RestControllerAdvice` `GlobalExceptionHandler` mapea excepciones → `ErrorResponse`.
- **E4.** Formato de error: reutilizar el del workspace; si no existe, `ProblemDetail` (RFC 9457) de Spring.
- **E5.** `MethodArgumentNotValidException` → 400 con lista de errores por campo.
- **E6.** `Exception` genérica → 500 con mensaje neutro; log `error` con stack trace solo en servidor.
- **E7.** Nunca capturar y silenciar excepciones. Nunca usar excepciones para control de flujo normal.

## 7. Estrategia DTO

- **D1.** Las entidades **nunca** cruzan la frontera del servicio.
- **D2.** DTOs separados por intención: `CreateRequest`, `UpdateRequest`, `Response`, opcional `SummaryResponse` para listados.
- **D3.** DTOs como `record` inmutables con Bean Validation en los de entrada.
- **D4.** Los DTOs de entrada no contienen `id` ni campos de auditoría.
- **D5.** Relaciones en request se expresan por id (`Long bikeId`), no por objeto anidado.

## 8. Estrategia MapStruct

- **M1.** `@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)` o un `@MapperConfig` común reutilizado del workspace.
- **M2.** Métodos estándar: `toEntity(CreateRequest)`, `toResponse(Entity)`, `updateEntity(UpdateRequest, @MappingTarget Entity)`.
- **M3.** Ignorar explícitamente `id`, auditoría y relaciones en `toEntity`/`updateEntity` (`@Mapping(target = "id", ignore = true)`).
- **M4.** Sin lógica de negocio ni acceso a repositorios en mappers.
- **M5.** Configurar `lombok-mapstruct-binding` en `annotationProcessorPaths`.

## 9. Estrategia de testing

- **T1.** JUnit 5 + Mockito + AssertJ.
- **T2.** Servicios: unit tests con `@ExtendWith(MockitoExtension.class)`.
- **T3.** Controllers: `@WebMvcTest` + `MockMvc` + `@MockitoBean`/`@MockBean` (según versión de Spring Boot del workspace).
- **T4.** Repositorios con consultas personalizadas: `@DataJpaTest` (Testcontainers PostgreSQL si el workspace lo usa).
- **T5.** Patrón Given / When / Then; nombres `should<Resultado>When<Condición>`.
- **T6.** Cobertura mínima de servicios: 80 %.

## 10. Checklist rápido antes de entregar

- [ ] Sin `@Autowired` en campos · Sin `@Data`
- [ ] `@RequiredArgsConstructor` + `private final`
- [ ] `ResponseEntity` en todos los endpoints
- [ ] Bean Validation + `@Valid`
- [ ] `@Slf4j` sin datos sensibles
- [ ] Entidades no expuestas
- [ ] Tests en verde

## 11. Referencias

`references/` debe contener (tras extraerlos del workspace): `pom.xml` de referencia, `application.yml` de referencia, ejemplos validados de cada tipo de clase y decisiones de convención adoptadas.
