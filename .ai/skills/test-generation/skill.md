# Skill · test-generation

> Cómo generar tests unitarios y de integración.
> **Prioridad absoluta: seguir los estándares de testing encontrados en los proyectos existentes del workspace** (librerías, estructura, nombres, fixtures, uso de Testcontainers). Solo si no existen, aplicar las reglas por defecto de este skill.

## 0. Descubrimiento de estándares (obligatorio)

1. Analizar `src/test/java` de los proyectos del workspace:
   - `@ExtendWith(MockitoExtension.class)`, `@WebMvcTest`, `@DataJpaTest`, `@SpringBootTest`, `@MockBean` vs `@MockitoBean`.
   - AssertJ vs assertions de JUnit; Hamcrest en MockMvc.
   - Convención de nombres (`should...When...`, `@DisplayName`, `given_when_then`).
   - Uso de Testcontainers / H2 / perfiles `test`.
   - Builders o *fixtures* (`XxxTestDataFactory`, `XxxMother`).
   - Plugins: Surefire/Failsafe, JaCoCo y umbrales.
2. Replicar y documentar en `references/patterns.md`.

## 1. Reglas generales

- **TG1.** JUnit 5 + Mockito + AssertJ (`assertThat`).
- **TG2.** Estructura **Given / When / Then** con comentarios o bloques claros.
- **TG3.** Nombres: `should<ResultadoEsperado>When<Condición>` o el estándar del workspace; `@DisplayName` en español si el workspace lo usa.
- **TG4.** Un comportamiento por test. Sin lógica condicional en tests.
- **TG5.** Datos de prueba mediante factorías reutilizables en `src/test/java/.../fixtures` (o el paquete del workspace).
- **TG6.** Prohibido `Thread.sleep`; prohibido depender del orden de ejecución.
- **TG7.** Unit tests: sufijo `Test` (Surefire). Integración: sufijo `IT` (Failsafe) si el workspace los separa.

## 2. Unit tests (servicios, mappers, utilidades)

- **U1.** `@ExtendWith(MockitoExtension.class)`, `@Mock` para dependencias, `@InjectMocks` para la clase bajo test (o construcción manual).
- **U2.** Mappers MapStruct: instanciar la implementación generada (`Mappers.getMapper(...)`) o mockear según el workspace.
- **U3.** Cubrir por cada método de servicio:
  - Camino feliz.
  - Cada excepción de negocio (`ResourceNotFoundException`, `ConflictException`, `BusinessRuleException`).
  - Cada regla de negocio RN-xx del diseño (un test por regla como mínimo).
  - Acceso denegado a recursos de otro usuario.
- **U4.** Verificar interacciones relevantes: `verify(repository).save(captor.capture())` + `ArgumentCaptor` para comprobar estado guardado; `verifyNoMoreInteractions` solo cuando aporte valor.
- **U5.** `assertThatThrownBy(...)`.isInstanceOf(...).hasMessageContaining(...)` para excepciones.
- **U6.** Mockito estricto (por defecto con `MockitoExtension`): no dejar stubs innecesarios.

## 3. Tests de controller (MockMvc)

- **W1.** `@WebMvcTest(XxxController.class)` + `MockMvc` + mock del servicio (`@MockitoBean` en Spring Boot ≥ 3.4, `@MockBean` en anteriores; seguir workspace).
- **W2.** Serializar requests con `ObjectMapper` inyectado.
- **W3.** Casos mínimos por endpoint:
  - Éxito (200/201/204), incluyendo header `Location` en creación.
  - 400 por validación (un caso por restricción relevante) y verificación del formato de error.
  - 404 cuando el servicio lanza `ResourceNotFoundException`.
  - 409/422 cuando aplique.
  - 401/403 si hay seguridad (`@WithMockUser`, `spring-security-test`).
- **W4.** Aserciones con `jsonPath("$.field").value(...)`.
- **W5.** Importar `GlobalExceptionHandler` y configuración de seguridad si no se cargan automáticamente.

## 4. Integration tests

- **I1.** Repositorios: `@DataJpaTest` para consultas `@Query` y Specifications. Base de datos: **Testcontainers PostgreSQL** si el workspace lo usa (`@ServiceConnection`), con `@AutoConfigureTestDatabase(replace = NONE)`.
- **I2.** Flujo end-to-end: `@SpringBootTest(webEnvironment = RANDOM_PORT)` o `@SpringBootTest` + `@AutoConfigureMockMvc` para los casos de uso principales.
- **I3.** Aislamiento: limpiar datos entre tests (`@Transactional` en test, `@Sql`, o `deleteAll` en `@BeforeEach`).
- **I4.** Perfil `test` con `application-test.yml`; sin dependencias de servicios externos reales.

## 5. Cobertura y calidad

- **Q1.** JaCoCo: cobertura de líneas ≥ 80 % en `service` (o el umbral del workspace).
- **Q2.** Excluir de cobertura: `config`, DTOs, implementaciones generadas por MapStruct, clase `Application`.
- **Q3.** Todo bug corregido incorpora un test que lo reproduce.

## 6. Matriz mínima por funcionalidad

| Clase | Tipo de test | Obligatorio |
|-------|--------------|-------------|
| `XxxServiceImpl` | Unit (Mockito) | Sí |
| `XxxController` | `@WebMvcTest` | Sí |
| `XxxRepository` (con JPQL/Spec) | `@DataJpaTest` | Sí |
| `XxxMapper` | Unit | Si tiene mappings no triviales |
| Flujo CU principal | `@SpringBootTest` | Recomendado |
| Validadores personalizados | Unit | Sí |

## 7. Checklist

- [ ] Estándar del workspace aplicado
- [ ] Given/When/Then y nombres descriptivos
- [ ] Caminos felices + errores + reglas de negocio
- [ ] MockMvc cubre validaciones y códigos HTTP
- [ ] Consultas personalizadas con test de repositorio
- [ ] `mvn verify` en verde y cobertura cumplida

## 8. Referencias

`references/` debe contener, extraídos del workspace: un unit test de servicio, un `@WebMvcTest`, un `@DataJpaTest` y la factoría de datos de prueba, indicando proyecto y ruta de origen.
