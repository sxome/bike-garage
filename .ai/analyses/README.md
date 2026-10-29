# Análisis técnicos · bike-garage

Cada funcionalidad se describe en un fichero `NNN-nombre-funcionalidad.md` y recorre el pipeline de agentes:

| Paso | Agente | Entrada | Salida |
|------|--------|---------|--------|
| 1 | `agents/01_analista_tecnico.md` | `NNN-x.md` | `NNN-x.backlog.md` |
| 2 | `agents/02_backend_architect.md` | `NNN-x.backlog.md` | `NNN-x.design.md` |
| 3 | `agents/03_backend_developer.md` | `NNN-x.design.md` | Código + `NNN-x.implementation.md` |
| 4 | `agents/04_qa_reviewer.md` | Todo lo anterior | `NNN-x.qa-report.md` |

## Uso en Copilot Chat (IntelliJ IDEA)

```
Actúa siguiendo #file:.ai/agents/01_analista_tecnico.md y aplica sus skills.
Analiza #file:.ai/analyses/001-gestion-bicicletas.md.
Revisa los demás proyectos abiertos del workspace para reutilizar patrones.
```

## Plantilla de análisis

```markdown
# NNN · <Funcionalidad>

## 1. Contexto y objetivo
## 2. Requisitos funcionales
## 3. Requisitos no funcionales
## 4. Modelo de datos (entidades, atributos, relaciones)
## 5. Reglas de negocio
## 6. Operaciones / endpoints esperados
## 7. Seguridad y permisos
## 8. Criterios de aceptación
## 9. Fuera de alcance
```
