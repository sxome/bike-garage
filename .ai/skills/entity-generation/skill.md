# Skill · entity-generation

> Cómo diseñar y generar entidades JPA en `bike-garage`.
> **Los ejemplos de implementación NO se incluyen aquí: deben obtenerse automáticamente analizando las entidades del resto de proyectos Spring Boot abiertos en el workspace.**

## 0. Procedimiento de descubrimiento de patrones (obligatorio)

Antes de generar una entidad, Copilot debe:

1. Buscar en el workspace ficheros con `@Entity`, `@MappedSuperclass`, `@Embeddable`, `@EntityListeners`.
2. Identificar:
   - ¿Existe una entidad base (`BaseEntity`, `AuditableEntity`)? → reutilizar su diseño.
   - Estrategia de ID (`IDENTITY`, `SEQUENCE`, `UUID`).
   - Uso de Lombok en entidades (`@Getter/@Setter`, `@Builder`, constructores).
   - Implementación de `equals`/`hashCode`.
   - Estrategia de auditoría (`@CreatedDate` + `AuditingEntityListener`, `@CreationTimestamp`, `@PrePersist`).
   - Persistencia de enums (`EnumType.STRING`).
   - Naming de tablas y columnas.
3. Copiar el patrón más consistente y anotarlo en `references/patterns.md` con la ruta de origen.
4. Si no hay patrón, aplicar las reglas de este skill.

## 1. Reglas generales

- **EN1.** Paquete `entity`. Una clase por fichero.
- **EN2.** `@Entity` + `@Table(name = "<plural_snake_case>")` siempre explícito.
- **EN3.** Lombok permitido: `@Getter`, `@Setter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`. **Prohibido `@Data`**, `@EqualsAndHashCode` sin configurar y `@ToString` que incluya relaciones.
- **EN4.** `@Column` explícito con `name`, `nullable`, `length`, `precision/scale` cuando aplique.
- **EN5.** Enums con `@Enumerated(EnumType.STRING)`. **Nunca** `ORDINAL`.
- **EN6.** Fechas con `java.time`. Dinero/distancias con `BigDecimal` (`precision`, `scale`).
- **EN7.** `equals`/`hashCode` basados en id con comprobación de nulo y `getClass()` consciente de proxies de Hibernate, o el patrón del workspace.
- **EN8.** Sin lógica de infraestructura en entidades. Métodos de dominio permitidos si encapsulan invariantes (p. ej. `markAsReplaced(LocalDate)`), si el workspace sigue ese estilo.
- **EN9.** Campo `@Version Long version` para bloqueo optimista en entidades editables concurrentemente.

## 2. Identificadores

- Seguir la estrategia del workspace. Por defecto:
  - `Long` + `@GeneratedValue(strategy = GenerationType.IDENTITY)`, o
  - `UUID` + `@GeneratedValue(strategy = GenerationType.UUID)` si la API expone ids públicos y se quiere evitar enumeración.
- Nunca exponer ids secuenciales sin control de acceso por propietario (ver `security-generation`).

## 3. Relaciones

| Tipo | Reglas |
|------|--------|
| `@ManyToOne` | `fetch = FetchType.LAZY`, `optional` acorde, `@JoinColumn(name = "<entidad>_id", nullable = ...)` |
| `@OneToMany` | `mappedBy`, `fetch = LAZY` (por defecto), colección inicializada `new ArrayList<>()`/`HashSet`; `cascade` y `orphanRemoval = true` **solo** para hijos cuyo ciclo de vida depende del padre |
| `@OneToOne` | `LAZY` con `@JoinColumn` en el lado propietario; considerar `@Embeddable` o `@MapsId` |
| `@ManyToMany` | Evitar. Preferir entidad intermedia explícita |

Reglas adicionales:
- **R1.** El lado propietario es el que tiene la FK (`@ManyToOne`).
- **R2.** Métodos helper para mantener ambos lados sincronizados: `addComponent(Component c)` / `removeComponent(Component c)`.
- **R3.** No usar `CascadeType.REMOVE`/`ALL` hacia agregados independientes (`User`, `Bike` desde hijos).
- **R4.** Exponer colecciones como no modificables en getters si el workspace lo hace.
- **R5.** Evitar N+1: las consultas que necesiten relaciones usan `@EntityGraph` o `JOIN FETCH` (ver `persistence-generation`).

## 4. Auditoría

- **A1.** Toda entidad hereda de una `@MappedSuperclass` de auditoría (reutilizar la del workspace) con:
  - `createdAt` (`@CreatedDate`, `updatable = false`)
  - `updatedAt` (`@LastModifiedDate`)
  - opcional `createdBy` / `lastModifiedBy` (`@CreatedBy`, `@LastModifiedBy`) si hay contexto de seguridad.
- **A2.** `@EntityListeners(AuditingEntityListener.class)` en la superclase.
- **A3.** `@EnableJpaAuditing` en una clase de `config` (no en la clase `Application`), con `AuditorAware` si se auditan usuarios.
- **A4.** Tipos `Instant` u `OffsetDateTime` para timestamps (UTC).

## 5. Validaciones

- **V1.** Validación de entrada → en DTOs (Bean Validation). Ver `api-generation`.
- **V2.** En la entidad: constraints de BD vía `@Column(nullable = false, length = n, unique = ...)` y `@Table(uniqueConstraints = ...)`, `@Index`.
- **V3.** Bean Validation en entidad (`@NotNull`, `@Size`) solo si el workspace lo hace; nunca como sustituto de la validación del DTO.
- **V4.** Invariantes de negocio complejas (RN-xx) → servicio o método de dominio, nunca en `@PrePersist`.
- **V5.** Unicidades compuestas (p. ej. nombre de bici por usuario) con `@UniqueConstraint` + comprobación previa en servicio que lance `ConflictException`.

## 6. Esquema y migraciones

- Si el workspace usa Flyway/Liquibase, generar el script junto con la entidad (`V<n>__<descripcion>.sql`).
- Nombres de constraints explícitos: `pk_<tabla>`, `fk_<tabla>_<ref>`, `uk_<tabla>_<campos>`, `idx_<tabla>_<campos>`.

## 7. Checklist por entidad

- [ ] `@Table` explícita, columnas snake_case
- [ ] Sin `@Data`; constructor protegido sin argumentos
- [ ] Hereda auditoría
- [ ] Relaciones `LAZY`, cascades justificados
- [ ] Enums `STRING`
- [ ] `equals/hashCode` seguros
- [ ] Constraints e índices declarados
- [ ] Patrón del workspace referenciado

## 8. Referencias

`references/` debe poblarse con: entidad base de auditoría, una entidad con relaciones y la configuración de auditoría **copiadas de proyectos del workspace**, indicando proyecto y ruta de origen.
