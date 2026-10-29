# Skill · domain-bike-garage

> **Documento vivo.** Describe el lenguaje ubicuo y el modelo de dominio de `bike-garage`. **Crecerá con cada nueva funcionalidad**: el Analista propone cambios, el Arquitecto los valida y el Desarrollador los consolida aquí tras implementar.

## 0. Instrucciones de uso

1. Todo agente debe usar **exclusivamente** los términos de este documento.
2. Antes de modelar una entidad nueva, comprobar si existe aquí o en otro proyecto del workspace (p. ej. `vida-util-piezas-api` puede contener el concepto de vida útil de piezas). Si existe, **reutilizar nombres, atributos y reglas**.
3. Cada cambio se registra en el *Historial* (§6).
4. Estado de cada elemento: `PROPUESTO` (en análisis) · `DISEÑADO` · `IMPLEMENTADO`.

## 1. Visión del dominio

`bike-garage` es un garaje digital donde un **usuario** registra sus **bicicletas**, los **componentes** montados en cada una, las **rutas** realizadas y los **mantenimientos** efectuados, con el objetivo de controlar la **vida útil** de cada componente y anticipar su sustitución.

## 2. Glosario (lenguaje ubicuo)

| Término (ES) | Término código (EN) | Definición |
|--------------|---------------------|------------|
| Usuario | `User` | Propietario de bicicletas. Unidad de aislamiento de datos. |
| Bicicleta | `Bike` | Bicicleta perteneciente a un usuario. |
| Componente | `Component` | Pieza montada en una bicicleta (cadena, cassette, pastillas, neumático...). Sinónimo prohibido: "pieza" en código. |
| Tipo de componente | `ComponentType` | Clasificación del componente (enum). |
| Ruta | `Ride` | Salida realizada con una bicicleta; aporta distancia y tiempo de uso. |
| Mantenimiento | `Maintenance` | Intervención sobre una bicicleta o componente (revisión, ajuste, sustitución, limpieza). |
| Vida útil | `Lifespan` | Límite de uso de un componente (km, horas o tiempo) y su consumo acumulado. |
| Desgaste | `wearPercentage` | Porcentaje de vida útil consumida. |

## 3. Entidades iniciales (estado: PROPUESTO)

### 3.1 Usuario (`User`)
| Atributo | Tipo | Reglas |
|----------|------|--------|
| id | Long/UUID | Generado |
| email | String | Obligatorio, único, formato email |
| displayName | String | Obligatorio, 1..100 |
| createdAt / updatedAt | Instant | Auditoría |

### 3.2 Bicicleta (`Bike`)
| Atributo | Tipo | Reglas |
|----------|------|--------|
| id | Long/UUID | Generado |
| name | String | Obligatorio, 1..100, único por usuario |
| brand | String | Opcional, ≤ 100 |
| model | String | Opcional, ≤ 100 |
| type | `BikeType` (ROAD, MTB, GRAVEL, URBAN, EBIKE...) | Obligatorio |
| purchaseDate | LocalDate | Opcional, no futura |
| totalDistanceKm | BigDecimal | ≥ 0, derivado de rutas |
| active | boolean | Por defecto `true` |
| owner | `User` | Obligatorio |

### 3.3 Componente (`Component`)
| Atributo | Tipo | Reglas |
|----------|------|--------|
| id | Long/UUID | Generado |
| type | `ComponentType` (CHAIN, CASSETTE, CHAINRING, TIRE, BRAKE_PAD, BRAKE_DISC, CABLE, BOTTOM_BRACKET, OTHER) | Obligatorio |
| brand / model | String | Opcional |
| installedAt | LocalDate | Obligatorio, no futura |
| removedAt | LocalDate | Opcional, ≥ installedAt |
| distanceAtInstallKm | BigDecimal | ≥ 0 |
| status | `ComponentStatus` (ACTIVE, REPLACED, RETIRED) | Obligatorio |
| bike | `Bike` | Obligatorio mientras ACTIVE |

### 3.4 Ruta (`Ride`)
| Atributo | Tipo | Reglas |
|----------|------|--------|
| id | Long/UUID | Generado |
| date | LocalDate | Obligatorio, no futura |
| distanceKm | BigDecimal | Obligatorio, > 0 |
| durationMinutes | Integer | Opcional, > 0 |
| elevationGainM | Integer | Opcional, ≥ 0 |
| title | String | Opcional, ≤ 150 |
| bike | `Bike` | Obligatorio |

### 3.5 Mantenimiento (`Maintenance`)
| Atributo | Tipo | Reglas |
|----------|------|--------|
| id | Long/UUID | Generado |
| date | LocalDate | Obligatorio, no futura |
| type | `MaintenanceType` (INSPECTION, ADJUSTMENT, REPLACEMENT, CLEANING, LUBRICATION) | Obligatorio |
| description | String | Opcional, ≤ 1000 |
| cost | BigDecimal | Opcional, ≥ 0 |
| bike | `Bike` | Obligatorio |
| component | `Component` | Opcional (obligatorio si type = REPLACEMENT) |

### 3.6 Vida útil (`Lifespan`)
Puede modelarse como `@Embeddable` dentro de `Component` o como entidad propia; **decisión del Arquitecto** según el patrón de `vida-util-piezas-api` u otros proyectos del workspace.

| Atributo | Tipo | Reglas |
|----------|------|--------|
| limitKm | BigDecimal | Opcional, > 0 |
| limitHours | Integer | Opcional, > 0 |
| limitMonths | Integer | Opcional, > 0 |
| warningThresholdPercent | Integer | 1..100, por defecto 80 |

Al menos uno de `limitKm`, `limitHours`, `limitMonths` es obligatorio.

## 4. Relaciones

| Origen | Cardinalidad | Destino | Notas |
|--------|--------------|---------|-------|
| User | 1:N | Bike | Una bici pertenece a un único usuario |
| Bike | 1:N | Component | Histórico: un componente reemplazado permanece asociado con status REPLACED |
| Bike | 1:N | Ride | |
| Bike | 1:N | Maintenance | |
| Component | 1:N | Maintenance | Opcional |
| Component | 1:1 | Lifespan | Embebido o entidad |

## 5. Reglas de negocio iniciales

| ID | Regla |
|----|-------|
| RN-DOM-01 | Un usuario solo puede ver y modificar sus propias bicicletas y todo lo que cuelga de ellas. |
| RN-DOM-02 | El nombre de bicicleta es único por usuario. |
| RN-DOM-03 | Registrar una ruta incrementa `totalDistanceKm` de la bici y el uso de sus componentes ACTIVE. |
| RN-DOM-04 | Uso de un componente = distancia de la bici desde `installedAt` hasta `removedAt` (o hoy). |
| RN-DOM-05 | `wearPercentage = uso / límite × 100`; si hay varios límites, se toma el mayor porcentaje. |
| RN-DOM-06 | Si `wearPercentage ≥ warningThresholdPercent` → aviso; si ≥ 100 → componente debe sustituirse. |
| RN-DOM-07 | Un mantenimiento de tipo REPLACEMENT marca el componente como REPLACED, fija `removedAt` y permite dar de alta uno nuevo del mismo tipo. |
| RN-DOM-08 | No puede haber dos componentes ACTIVE del mismo `ComponentType` en la misma bici (salvo tipos múltiples como TIRE o BRAKE_PAD; limitar por posición si se define). |
| RN-DOM-09 | No se puede eliminar una bici con rutas o mantenimientos: se desactiva (`active = false`). |
| RN-DOM-10 | Fechas de rutas y mantenimientos no pueden ser futuras. |

## 6. Historial de cambios

| Fecha | Funcionalidad / análisis | Cambio | Estado |
|-------|--------------------------|--------|--------|
| 2026-10-29 | Inicial | Definición inicial del dominio | PROPUESTO |

## 7. Referencias

`references/` contendrá diagramas (Mermaid/PlantUML), glosarios ampliados y extractos de modelos equivalentes encontrados en otros proyectos del workspace.
