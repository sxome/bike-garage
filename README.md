# bike-garage

API REST base para la gestión del taller de bicicletas. Esta versión contiene únicamente la estructura inicial del proyecto y un endpoint de salud (`GET /health`). No incluye lógica de negocio, entidades ni tablas.

## Stack

- Java 21
- Spring Boot 3.2.5 (Web, Data JPA, Validation, DevTools)
- H2 en memoria
- Lombok
- MapStruct 1.5.5.Final
- Maven

## Requisitos

- JDK 21
- Maven 3.9+

## Ejecución

```bash
# Compilar y ejecutar tests
mvn clean verify

# Arrancar con el perfil por defecto
mvn spring-boot:run

# Arrancar con un perfil concreto (desa, pre, pro)
mvn spring-boot:run -Dspring-boot.run.profiles=desa
```

Alternativamente, tras compilar:

```bash
java -jar target/bike-garage-0.0.1-SNAPSHOT.jar --spring.profiles.active=desa
```

## Verificación

```bash
curl http://localhost:8080/health
# {"status":"UP"}
```

Consola H2: http://localhost:8080/h2-console

## Estructura

```
src/main/java/es/tuempresa/bikegarage
├── config
├── controller        # GarageController (GET /health)
├── service
│   ├── api           # Interfaces de servicio
│   └── impl          # Implementaciones
├── repository        # Repositorios (vacío de momento)
├── model
│   ├── dto           # DTOs (records)
│   ├── entity        # Entidades JPA (pendiente)
│   └── mapper        # Mappers MapStruct (pendiente)
├── exception         # ErrorResponse y GlobalExceptionHandler
└── util              # Constantes (ApiConstantes, ErrorConstantes)
```

## Perfiles

| Perfil | Fichero                | Descripción              |
|--------|------------------------|--------------------------|
| (base) | `application.yml`      | Configuración común      |
| desa   | `application-desa.yml` | Entorno de desarrollo    |
| pre    | `application-pre.yml`  | Entorno de preproducción |
| pro    | `application-pro.yml`  | Entorno de producción    |

Los logs se escriben en `logs/bike-garage.log` (rotación diaria, 30 días).
