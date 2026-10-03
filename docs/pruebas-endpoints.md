# Evidencia de Pruebas de Endpoints — API Multas Biblioteca

Este documento contiene la evidencia técnica y visual de la ejecución de pruebas sobre todos los endpoints REST expuestos por `multas-biblioteca-api`, demostrando el cumplimiento de los flujos de negocio y los códigos de estado HTTP requeridos.

---

## 1. Matriz de Cobertura de Endpoints y Códigos HTTP

| Caso | Método HTTP | Endpoint | Descripción | Código HTTP | Resultado |
| :---: | :---: | :--- | :--- | :---: | :---: |
| **01** | `GET` | `/api/multas` | Listar todas las multas (inicialmente vacío) | **200 OK** | Exitoso |
| **02** | `POST` | `/api/multas` | Generar multa con cálculo ordinario ($500 × 5 = $2.500) | **201 Created** | Exitoso |
| **03** | `POST` | `/api/multas` | Generar multa con tope máximo ($500 × 35 = $15.000) | **201 Created** | Exitoso |
| **04** | `POST` | `/api/multas` | Generar 3ra multa pendiente para el estudiante | **201 Created** | Exitoso |
| **05** | `POST` | `/api/multas` | Intentar generar 4ta multa pendiente (bloqueo por límite) | **409 Conflict** | Exitoso |
| **06** | `POST` | `/api/multas` | Enviar payload inválido (`diasAtraso: 0`, campos vacíos) | **400 Bad Request** | Exitoso |
| **07** | `GET` | `/api/multas/{id}` | Consultar multa existente por ID (`id: 1`) | **200 OK** | Exitoso |
| **08** | `GET` | `/api/multas/{id}` | Consultar multa inexistente (`id: 99`) | **404 Not Found** | Exitoso |
| **09** | `GET` | `/api/multas/estudiante/{id}` | Listar multas del estudiante `EST-2024-001` | **200 OK** | Exitoso |
| **10** | `PATCH` | `/api/multas/{id}/pagar` | Pagar multa en ventanilla física | **200 OK** | Exitoso |
| **11** | `PATCH` | `/api/multas/{id}/pagar` | Intentar pagar nuevamente una multa ya pagada | **409 Conflict** | Exitoso |
| **12** | `POST` | `/api/multas/{id}/pagar-en-linea` | Pagar en línea con rechazo/falla de pasarela | **402 Payment Required** | Exitoso |

---

## 2. Detalle de Ejecución y Respuestas HTTP

### Caso 01: `GET /api/multas` (Estado Inicial)
* **Comando cURL:**
```bash
curl -X GET http://localhost:8080/api/multas -H "Accept: application/json"
```
* **Respuesta HTTP `200 OK`:**
```json
[]
```

---

### Caso 02: `POST /api/multas` (Cálculo Ordinario de Monto — 201 Created)
* **Comando cURL:**
```bash
curl -X POST http://localhost:8080/api/multas \
  -H "Content-Type: application/json" \
  -d '{
    "estudianteId": "EST-2024-001",
    "concepto": "Devolucion tardia: Clean Code",
    "diasAtraso": 5
  }'
```
* **Respuesta HTTP `201 Created`:**
```json
{
  "id": 1,
  "estudianteId": "EST-2024-001",
  "concepto": "Devolucion tardia: Clean Code",
  "diasAtraso": 5,
  "monto": 2500.00,
  "estado": "PENDIENTE",
  "fechaGeneracion": "2026-10-03",
  "fechaPago": null,
  "metodoPago": null
}
```

---

### Caso 03: `POST /api/multas` (Aplicación de Tope Máximo $15.000 — 201 Created)
* **Comando cURL:**
```bash
curl -X POST http://localhost:8080/api/multas \
  -H "Content-Type: application/json" \
  -d '{
    "estudianteId": "EST-2024-001",
    "concepto": "Devolucion tardia: Design Patterns",
    "diasAtraso": 35
  }'
```
* **Respuesta HTTP `201 Created`:**
```json
{
  "id": 2,
  "estudianteId": "EST-2024-001",
  "concepto": "Devolucion tardia: Design Patterns",
  "diasAtraso": 35,
  "monto": 15000.00,
  "estado": "PENDIENTE",
  "fechaGeneracion": "2026-10-03",
  "fechaPago": null,
  "metodoPago": null
}
```

---

### Caso 04: `POST /api/multas` (Generación de Tercera Multa — 201 Created)
* **Comando cURL:**
```bash
curl -X POST http://localhost:8080/api/multas \
  -H "Content-Type: application/json" \
  -d '{
    "estudianteId": "EST-2024-001",
    "concepto": "Devolucion tardia: Domain-Driven Design",
    "diasAtraso": 2
  }'
```
* **Respuesta HTTP `201 Created`:**
```json
{
  "id": 3,
  "estudianteId": "EST-2024-001",
  "concepto": "Devolucion tardia: Domain-Driven Design",
  "diasAtraso": 2,
  "monto": 1000.00,
  "estado": "PENDIENTE",
  "fechaGeneracion": "2026-10-03",
  "fechaPago": null,
  "metodoPago": null
}
```

---

### Caso 05: `POST /api/multas` (Bloqueo por Límite de 3 Multas Pendientes — 409 Conflict)
* **Comando cURL:**
```bash
curl -X POST http://localhost:8080/api/multas \
  -H "Content-Type: application/json" \
  -d '{
    "estudianteId": "EST-2024-001",
    "concepto": "Devolucion tardia: Refactoring",
    "diasAtraso": 1
  }'
```
* **Respuesta HTTP `409 Conflict`:**
```json
{
  "timestamp": "2026-10-03T08:14:22.333524400",
  "status": 409,
  "error": "Conflict",
  "message": "El estudiante con ID 'EST-2024-001' ha alcanzado el límite máximo de 3 multas pendientes.",
  "path": "/api/multas"
}
```

---

### Caso 06: `POST /api/multas` (Validación de Payload Inválido — 400 Bad Request)
* **Comando cURL:**
```bash
curl -X POST http://localhost:8080/api/multas \
  -H "Content-Type: application/json" \
  -d '{
    "estudianteId": "",
    "concepto": "",
    "diasAtraso": 0
  }'
```
* **Respuesta HTTP `400 Bad Request`:**
```json
{
  "timestamp": "2026-10-03T08:14:17.675464900",
  "status": 400,
  "error": "Bad Request",
  "message": "Error de validación: El ID del estudiante no puede estar vacío, El concepto no puede estar vacío, Los días de atraso deben ser al menos 1",
  "path": "/api/multas"
}
```

---

### Caso 07: `GET /api/multas/1` (Buscar Multa por ID — 200 OK)
* **Comando cURL:**
```bash
curl -X GET http://localhost:8080/api/multas/1 -H "Accept: application/json"
```
* **Respuesta HTTP `200 OK`:**
```json
{
  "id": 1,
  "estudianteId": "EST-2024-001",
  "concepto": "Devolucion tardia: Clean Code",
  "diasAtraso": 5,
  "monto": 2500.00,
  "estado": "PENDIENTE",
  "fechaGeneracion": "2026-10-03",
  "fechaPago": null,
  "metodoPago": null
}
```

---

### Caso 08: `GET /api/multas/99` (ID Inexistente — 404 Not Found)
* **Comando cURL:**
```bash
curl -X GET http://localhost:8080/api/multas/99 -H "Accept: application/json"
```
* **Respuesta HTTP `404 Not Found`:**
```json
{
  "timestamp": "2026-10-03T08:14:17.719921700",
  "status": 404,
  "error": "Not Found",
  "message": "Multa no encontrada con ID: 99",
  "path": "/api/multas/99"
}
```

---

### Caso 09: `GET /api/multas/estudiante/EST-2024-001` (Listar Multas por Estudiante — 200 OK)
* **Comando cURL:**
```bash
curl -X GET http://localhost:8080/api/multas/estudiante/EST-2024-001 -H "Accept: application/json"
```
* **Respuesta HTTP `200 OK`:**
```json
[
  {
    "id": 1,
    "estudianteId": "EST-2024-001",
    "concepto": "Devolucion tardia: Clean Code",
    "diasAtraso": 5,
    "monto": 2500.00,
    "estado": "PENDIENTE",
    "fechaGeneracion": "2026-10-03",
    "fechaPago": null,
    "metodoPago": null
  },
  {
    "id": 2,
    "estudianteId": "EST-2024-001",
    "concepto": "Devolucion tardia: Design Patterns",
    "diasAtraso": 35,
    "monto": 15000.00,
    "estado": "PENDIENTE",
    "fechaGeneracion": "2026-10-03",
    "fechaPago": null,
    "metodoPago": null
  },
  {
    "id": 3,
    "estudianteId": "EST-2024-001",
    "concepto": "Devolucion tardia: Domain-Driven Design",
    "diasAtraso": 2,
    "monto": 1000.00,
    "estado": "PENDIENTE",
    "fechaGeneracion": "2026-10-03",
    "fechaPago": null,
    "metodoPago": null
  }
]
```

---

### Caso 10: `PATCH /api/multas/1/pagar` (Pago en Ventanilla Física — 200 OK)
* **Comando cURL:**
```bash
curl -X PATCH http://localhost:8080/api/multas/1/pagar -H "Accept: application/json"
```
* **Respuesta HTTP `200 OK`:**
```json
{
  "id": 1,
  "estudianteId": "EST-2024-001",
  "concepto": "Devolucion tardia: Clean Code",
  "diasAtraso": 5,
  "monto": 2500.00,
  "estado": "PAGADA",
  "fechaGeneracion": "2026-10-03",
  "fechaPago": "2026-10-03",
  "metodoPago": "VENTANILLA"
}
```

---

### Caso 11: `PATCH /api/multas/1/pagar` (Reintento de Pago sobre Multa Ya Pagada — 409 Conflict)
* **Comando cURL:**
```bash
curl -X PATCH http://localhost:8080/api/multas/1/pagar -H "Accept: application/json"
```
* **Respuesta HTTP `409 Conflict`:**
```json
{
  "timestamp": "2026-10-03T08:14:17.769060400",
  "status": 409,
  "error": "Conflict",
  "message": "La multa con ID 1 ya se encuentra pagada.",
  "path": "/api/multas/1/pagar"
}
```

---

### Caso 12: `POST /api/multas/2/pagar-en-linea` (Falla de Pasarela / Rechazo — 402 Payment Required)
* **Comando cURL:**
```bash
curl -X POST http://localhost:8080/api/multas/2/pagar-en-linea -H "Accept: application/json"
```
* **Respuesta HTTP `402 Payment Required`:**
```json
{
  "timestamp": "2026-10-03T08:14:17.778293600",
  "status": 402,
  "error": "Payment Required",
  "message": "Pago rechazado por pasarela [PAGOSUDES]: Fallo de comunicación con PagosUDES: I/O error on POST request for \"http://localhost:9001/pagosudes/transacciones\": Connection refused: connect",
  "path": "/api/multas/2/pagar-en-linea"
}
```
