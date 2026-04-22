# Smart Campus API

A RESTful API for managing campus rooms and sensors, built with JAX-RS (Jersey) and Grizzly embedded HTTP server. No Spring Boot. No database — in-memory storage only.

---

## API Overview

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/` | Discovery — lists all resource links |
| GET | `/api/v1/rooms` | List all rooms |
| POST | `/api/v1/rooms` | Create a room |
| GET | `/api/v1/rooms/{roomId}` | Get a specific room |
| DELETE | `/api/v1/rooms/{roomId}` | Delete a room (fails if sensors exist) |
| GET | `/api/v1/sensors` | List all sensors (optional `?type=` filter) |
| POST | `/api/v1/sensors` | Register a sensor (roomId must exist) |
| GET | `/api/v1/sensors/{sensorId}` | Get a specific sensor |
| GET | `/api/v1/sensors/{sensorId}/readings` | Get reading history |
| POST | `/api/v1/sensors/{sensorId}/readings` | Add a reading |

---

## How to Build and Run

### Prerequisites
- Java 17 (JDK)
- Maven 3.x

### Steps

```bash
# 1. Clone the repo
git clone https://github.com/acey2024/smart-campus-api.git
cd smart-campus-api

# 2. Build
mvn clean package

# 3. Run
mvn exec:java
```

The server starts at `http://localhost:8080/api/v1/`

---

## Sample curl Commands

```bash
# 1. Discovery
curl http://localhost:8080/api/v1/

# 2. Create a room
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"LIB-301","name":"Library Quiet Study","capacity":50}'

# 3. Register a sensor (roomId must exist)
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"CO2-001","type":"CO2","status":"ACTIVE","roomId":"LIB-301"}'

# 4. Filter sensors by type
curl "http://localhost:8080/api/v1/sensors?type=CO2"

# 5. Add a sensor reading
curl -X POST http://localhost:8080/api/v1/sensors/CO2-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":412.5}'

# 6. Try deleting a room that has sensors (expect 409 Conflict)
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301

# 7. Register a sensor with a fake roomId (expect 422)
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-002","type":"Temperature","roomId":"FAKE-999"}'
```

---

## Report: Answers to Coursework Questions

### Part 1.1 — JAX-RS Resource Lifecycle & Thread Safety

By default, JAX-RS creates a **new instance of each resource class for every incoming HTTP request** (per-request scope). This means instance variables inside a resource class are not shared between requests and are discarded after each request completes.

This design decision has a critical implication for in-memory data storage: if you store data as instance fields inside the resource class (e.g., `private Map<String, Room> rooms = new HashMap<>()`), that data will be lost at the end of every request because the object is destroyed.

To prevent this, a shared **singleton data store** (`DataStore.java`) is used with `static` fields. This ensures all resource instances — regardless of which request created them — read from and write to the same maps.

Furthermore, because multiple requests can arrive concurrently (multiple threads), a regular `HashMap` is not thread-safe and can cause race conditions (corrupted data, lost updates, or `ConcurrentModificationException`). The solution is to use `ConcurrentHashMap`, which is designed for concurrent access and provides thread-safe reads and writes without blocking the entire map for every operation.

---

### Part 1.2 — HATEOAS and Why It Matters

**HATEOAS** (Hypermedia as the Engine of Application State) is a REST constraint where API responses include hyperlinks that guide the client to the next available actions, rather than the client having to know all URLs in advance.

For example, rather than just returning `{"id": "LIB-301"}`, a HATEOAS-compliant response might include:
```json
{
  "id": "LIB-301",
  "links": {
    "self": "/api/v1/rooms/LIB-301",
    "sensors": "/api/v1/sensors?roomId=LIB-301"
  }
}
```

**Benefits over static documentation:**
- Clients are **self-discoverable** — they can navigate the API by following links, just like a web browser follows hyperlinks, without needing to read external docs.
- APIs become **loosely coupled** — if the server changes a URL, clients that follow links rather than hardcoding paths are unaffected.
- Reduces **integration errors** — developers can explore what actions are available at runtime rather than memorising URL patterns.
- The discovery endpoint (`GET /api/v1/`) in this project is an example of this principle: it tells clients where rooms and sensors live.

---

### Part 2.1 — Returning IDs vs Full Objects in List Responses

**Returning only IDs:**
- Pros: Very small payload, low bandwidth usage, fast response.
- Cons: The client must make N additional GET requests to retrieve details for each room — known as the "N+1 problem". This increases latency and server load.

**Returning full room objects:**
- Pros: All data is available in a single response. Clients can display lists immediately without extra requests.
- Cons: Larger payload, especially with thousands of rooms. Each object includes all fields even if the client only needs a name.

**In this implementation**, full room objects are returned in list responses. This is appropriate for a campus management system where facilities managers need room name, capacity, and sensor list at a glance. For very large datasets, a pagination strategy (e.g., `?page=1&size=20`) would be added in a production system.

---

### Part 2.2 — Is DELETE Idempotent?

**Yes**, DELETE is idempotent in this implementation.

Idempotency means that sending the same request multiple times produces the same server state as sending it once.

- **First DELETE `/api/v1/rooms/LIB-301`**: Room exists and has no sensors → room is removed → returns `204 No Content`.
- **Second DELETE `/api/v1/rooms/LIB-301`**: Room no longer exists → returns `404 Not Found`.

The **server state is identical** after both calls: the room does not exist. A 404 is not an error indicating something went wrong — it is simply the server reporting that the resource is already gone. HTTP idempotency refers to the effect on the server state, not the response code. This aligns with the HTTP/1.1 specification (RFC 7231), which explicitly defines DELETE as idempotent.

---

### Part 3.1 — What Happens with Wrong Content-Type?

When a client sends a POST request with `Content-Type: text/plain` or `Content-Type: application/xml` to an endpoint annotated with `@Consumes(MediaType.APPLICATION_JSON)`, JAX-RS automatically rejects the request **before your method code is ever called**.

The framework returns an **HTTP 415 Unsupported Media Type** response. This is handled entirely by the JAX-RS runtime's content negotiation layer — no custom code is needed for this case.

This protects the API from malformed input being passed to the Jackson deserialiser, which would otherwise throw a parse error. The `@Consumes` annotation acts as a contract filter: only requests matching the specified media type are dispatched to that method.

---

### Part 3.2 — @QueryParam vs Path Segment for Filtering

**Query parameter approach** (implemented): `GET /api/v1/sensors?type=CO2`

**Path segment alternative**: `GET /api/v1/sensors/type/CO2`

The query parameter approach is superior for filtering because:

1. **REST semantics**: A URL path segment should identify a **specific resource**. `/api/v1/sensors/CO2` implies that `CO2` is a sensor ID, not a filter criterion. This is misleading and breaks the resource model.
2. **Optional filters**: Query parameters are naturally optional. A client can call `GET /api/v1/sensors` without any filter and get all sensors — no path restructuring needed.
3. **Multiple filters**: Query parameters compose cleanly: `?type=CO2&status=ACTIVE`. Encoding this in a path becomes awkward and breaks REST conventions.
4. **Industry standard**: All major APIs (GitHub, Twitter, Google) use query parameters for filtering, searching, and pagination — not path segments.

The rule of thumb: **path segments identify resources; query parameters modify/filter the representation**.

---

### Part 4.1 — Benefits of the Sub-Resource Locator Pattern

The sub-resource locator pattern, where a method with only `@Path` (no HTTP verb annotation) returns an instance of another resource class, provides several architectural benefits:

1. **Separation of concerns**: `SensorResource` handles sensor CRUD. `SensorReadingResource` handles the reading lifecycle. Each class has a single, well-defined responsibility.
2. **Reduced complexity**: Without this pattern, every nested route (`/sensors/{id}/readings`, `/sensors/{id}/readings/{rid}`) would be crammed into one massive `SensorResource` class. This class would grow unmanageably with more nesting levels.
3. **Easier testing**: `SensorReadingResource` can be unit-tested in isolation by constructing it with a known `sensorId`, without needing to bootstrap the full JAX-RS container.
4. **Reusability**: The same sub-resource class could theoretically be reused from multiple parent locators if the data model required it.
5. **Mirrors the domain model**: The physical hierarchy (a Room contains Sensors; a Sensor has Readings) is naturally expressed as a URL hierarchy (`/rooms/{id}` → `/sensors/{id}` → `/readings`), making the API intuitive.

---

### Part 5.2 — Why 422 Over 404 for a Missing roomId Reference?

When a client POSTs a new sensor with `"roomId": "FAKE-999"` and that room does not exist:

- **404 Not Found** would imply that the URL the client requested (`/api/v1/sensors`) does not exist — but it does. The endpoint is perfectly valid.
- **422 Unprocessable Entity** correctly communicates: "The request URL is valid, the JSON is syntactically correct, but the data inside the payload refers to a resource (`FAKE-999`) that does not exist in the system — so the request cannot be fulfilled."

The distinction is important: 404 is about the **URL**; 422 is about the **content** of a valid request being semantically invalid. Using 422 gives the client developer a precise signal: "your request body contained a broken reference", which is far more actionable than a generic 404 that might make them think they called the wrong endpoint.

---

### Part 5.4 — Security Risk of Exposing Stack Traces

Exposing Java stack traces to external API consumers is a serious security vulnerability. A stack trace reveals:

1. **Class and package names**: Attackers learn the internal architecture — e.g., `com.smartcampus.DataStore` tells them a `DataStore` class exists and what package it's in.
2. **Method names and line numbers**: Reveals the internal logic flow. Line numbers help attackers pinpoint exactly where an exception occurred, correlating it with publicly known vulnerabilities in those code paths.
3. **Third-party library versions**: E.g., `at org.glassfish.jersey.server.ServerRuntime$2.run(ServerRuntime.java:317)` reveals Jersey is in use and hints at the version — attackers can look up CVEs for that exact version.
4. **File system paths**: On some servers, full paths like `/home/ubuntu/app/src/...` are included, revealing OS, username, and directory layout.

This information gives attackers everything they need to craft targeted exploits. The `GenericExceptionMapper<Throwable>` in this API catches all unexpected errors and returns a safe, generic `500 Internal Server Error` JSON response — keeping all internal details server-side in logs only.

---

### Part 5.5 — Why Use Filters for Logging Instead of Inline Logger Calls?

Using a JAX-RS filter (`LoggingFilter`) for logging is superior to adding `Logger.info()` inside every resource method for several reasons:

1. **DRY principle**: One filter handles all logging for every request/response. With inline logging, every new endpoint added requires a developer to remember to add logging — and they will forget.
2. **Cross-cutting concerns**: Logging, authentication, CORS headers, and rate limiting are concerns that apply to all requests, not to any specific business logic. Filters are the correct architectural place for these.
3. **Single point of change**: If the log format needs to change (e.g., adding a request ID), you change one file — the filter — rather than editing dozens of resource methods.
4. **No pollution of business logic**: Resource methods stay focused on their domain task. Mixing logging into business code makes it harder to read, test, and maintain.
5. **Consistent coverage**: A filter guaranteed to run on every request/response means no endpoint is accidentally left unlogged, which is critical for auditing and debugging in production.
