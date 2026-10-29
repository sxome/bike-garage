# Skill · persistence-generation

> Cómo generar la capa de persistencia con Spring Data JPA sobre PostgreSQL.
> **El patrón de repositorios, consultas y Specifications debe copiarse de otros proyectos Spring Boot del workspace.** Este skill define las reglas; los ejemplos se extraen del workspace.

## 0. Descubrimiento de patrones (obligatorio)

1. Buscar en el workspace interfaces que extiendan `JpaRepository`, `CrudRepository`, `JpaSpecificationExecutor`, `PagingAndSortingRepository`.
2. Buscar clases `*Specification*`, usos de `@Query`, `@EntityGraph`, `@Modifying`, proyecciones (`interface XxxView`, records DTO en JPQL).
3. Identificar: interfaz base usada, estilo de Specifications (clase utilitaria con métodos estáticos vs `Specification` builder), cómo se combinan filtros, tests de repositorio existentes.
4. Copiar el patrón y registrar origen en `references/patterns.md`.

## 1. Creación de repositorios

- **P1.** Paquete `repository`. Nombre `<Entidad>Repository`.
- **P2.** `interface XxxRepository extends JpaRepository<Xxx, ID>` (+ `JpaSpecificationExecutor<Xxx>` si hay filtros dinámicos).
- **P3.** Sin `@Repository` en interfaces Spring Data (redundante) salvo que el workspace lo use.
- **P4.** Sin implementaciones manuales salvo necesidad justificada (`XxxRepositoryCustom` + `XxxRepositoryImpl`).
- **P5.** Repositorios solo inyectados en servicios.
- **P6.** Métodos de búsqueda por id devuelven `Optional<Xxx>`.
- **P7.** Listados devuelven `Page<Xxx>` y reciben `Pageable`. Nunca `findAll()` sin paginar sobre tablas que crecen.

## 2. Consultas derivadas

Usar cuando la consulta sea simple (≤ 3 condiciones) y el nombre siga siendo legible.

| Necesidad | Método |
|-----------|--------|
| Buscar por propietario | `Page<Bike> findByOwnerId(Long ownerId, Pageable pageable)` |
| Acceso seguro por propietario | `Optional<Bike> findByIdAndOwnerId(Long id, Long ownerId)` |
| Unicidad | `boolean existsByOwnerIdAndNameIgnoreCase(Long ownerId, String name)` |
| Filtro por estado | `List<Component> findByBikeIdAndStatus(Long bikeId, ComponentStatus status)` |
| Conteo | `long countByBikeId(Long bikeId)` |

Reglas:
- **DQ1.** Preferir `existsBy...` a `findBy...().isPresent()`.
- **DQ2.** Si el nombre supera ~4 condiciones o es ilegible → JPQL o Specification.

## 3. JPQL (`@Query`)

Usar para joins, agregaciones, proyecciones o consultas de negocio fijas.

- **JQ1.** Siempre parámetros nombrados (`:bikeId`) con `@Param`. **Prohibido concatenar** entrada de usuario.
- **JQ2.** `JOIN FETCH` o `@EntityGraph(attributePaths = {...})` para evitar N+1. Con paginación + `JOIN FETCH` sobre colecciones, definir `countQuery` o usar dos consultas.
- **JQ3.** Proyecciones a record DTO con `select new ...` o interfaces de proyección para lecturas agregadas.
- **JQ4.** Operaciones masivas con `@Modifying(clearAutomatically = true, flushAutomatically = true)` y `@Transactional` en el servicio.
- **JQ5.** SQL nativo (`nativeQuery = true`) solo con justificación (funciones específicas de PostgreSQL) y con parámetros enlazados.
- **JQ6.** Agregaciones numéricas (`SUM(r.distanceKm)`) con `COALESCE` para evitar `null`.

## 4. Specifications (filtros dinámicos)

Usar cuando el endpoint de listado admite filtros opcionales combinables.

- **SP1.** Clase `repository.specification.<Entidad>Specifications` con métodos estáticos que devuelven `Specification<Entidad>` (o el estilo del workspace).
- **SP2.** Cada método devuelve `null` (o `Specification.where(null)`/`unrestricted()` según versión) si el criterio es nulo, para composición segura.
- **SP3.** Composición en servicio a partir de un `XxxFilter` (record):
  `Specification.where(ownedBy(userId)).and(hasType(filter.type())).and(nameContains(filter.name()))`.
- **SP4.** El filtro de propietario (`ownedBy`) es **siempre obligatorio** (mínimo privilegio).
- **SP5.** `like` siempre con `criteriaBuilder.lower(...)` y escape de comodines si procede.
- **SP6.** Usar el metamodelo JPA (`Bike_.name`) si el workspace lo genera; si no, constantes de nombre de atributo.

## 5. Paginación y ordenación

- **PG1.** Tamaño de página máximo configurado (`spring.data.web.pageable.max-page-size=100`).
- **PG2.** Ordenación por defecto explícita en el controller (`@PageableDefault(sort = "createdAt", direction = DESC)`).
- **PG3.** Validar/whitelistear campos de ordenación permitidos para evitar errores y fugas de información.

## 6. Transacciones (recordatorio)

- Los repositorios no llevan `@Transactional` propio salvo `@Modifying` justificado; la transacción la abre el servicio.
- `spring.jpa.open-in-view=false`: todo acceso a relaciones LAZY debe ocurrir dentro del servicio o mediante fetch explícito.

## 7. Checklist

- [ ] Repositorio por agregado, sin lógica de negocio
- [ ] Búsquedas protegidas por propietario
- [ ] Sin concatenación en consultas
- [ ] N+1 resuelto en listados
- [ ] Paginación con límite
- [ ] Test `@DataJpaTest` para JPQL/Specifications
- [ ] Patrón copiado del workspace y referenciado

## 8. Referencias

`references/` debe contener ejemplos reales extraídos del workspace: un repositorio con consultas derivadas, uno con `@Query` y una clase de Specifications, indicando proyecto y ruta de origen.
