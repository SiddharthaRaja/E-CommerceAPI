# E-CommerceAPI — Interview Questions & Model Answers

> Companion to [`E-CommerceAPI.md`](./E-CommerceAPI.md). First-person, 30–90 s answers. 🟢 Easy · 🟡 Medium · 🔴 Hard
> Note: this is a backend project. For ML roles it shows engineering fundamentals: auth, transactions, API design and testing.

**If you only prep 10:** A1, B2, C1, C2, C4, D1, D3, E1, G1, plus all hostile follow-ups.

---

## (a) Project overview & motivation

**A1. 🟢 Describe the project.**
> It's a Spring Boot 3 REST API for an e-commerce backend. Users register with BCrypt-hashed passwords and log in to get a JWT. They can browse products, manage a cart, and check out. Checkout validates stock, decrements inventory, records order lines with the price at purchase time, and clears the cart. It's backed by PostgreSQL through JPA and runs in Docker Compose.

**A2. 🟢 Why build it?**
> To learn the backbone of most production systems: authentication, relational modelling, transactional business logic and containerised deployment, using the stack many companies run.

**A3. 🟡 What did you learn from it?**
> That the hard part of a shop isn't CRUD, it's correctness: atomic checkout, concurrent inventory, and not leaking data through serialisation. When I revisited it and actually ran it, I found my checkout wasn't transactional and my responses included password hashes. Both are classic production bugs.

**A4. 🟢 Which part are you most confident in?**
> The data model. Price snapshotting into `OrderItem.priceAtPurchase` means editing a product never rewrites order history, and cart and order lines are modelled as separate entities with quantities.

---

## (b) Architecture & design

**B1. 🟢 Walk me through the architecture.**
> Requests go through Spring Security's filter chain, where my JWT filter authenticates the bearer token and sets the security context. Then they hit controllers for auth, products, cart and orders, which call Spring Data repositories against PostgreSQL. A global exception handler turns runtime and validation errors into JSON responses.

**B2. 🔴 Where should checkout logic live, and why does it matter here?**
> In a service method annotated `@Transactional`. Today it's in the controller with no transaction, so each `save` commits independently. I reproduced this: when the second item was out of stock, the first item's stock had already been decremented and stayed that way. Putting it in a transactional service makes all the writes commit or roll back together, and makes it unit-testable.

**B3. 🟡 Why JWT over sessions?**
> Stateless: any instance can validate a token without a shared session store, which scales horizontally. The costs are revocation (a token is valid until it expires) and secret management. I'd add short access tokens plus refresh tokens, and load the secret from the environment.

**B4. 🟡 Why return DTOs instead of entities?**
> Entities reflect the schema, not the API contract. Returning them leaked the user's password hash, because `Cart` and `Order` reference `User`, which has a password field. It also risks lazy-loading errors and N+1 queries, and lets clients bind fields they shouldn't via `@RequestBody Product`. DTOs make the contract explicit.

**B5. 🟡 How is the cart modelled?**
> `Cart` has many `CartItem`s, each pointing at a `Product` with a quantity. Adding an existing product increments the quantity. Removing decrements it, or deletes the line at zero. The user relation is `@ManyToOne` but used as one-to-one, so a unique constraint on `user_id` would enforce one cart per user at the DB level.

---

## (c) Code-level / implementation

**C1. 🔴 Explain the password-hash leak precisely.**
> `User` has a public `getPassword()` and no `@JsonIgnore`. Jackson serialises every getter. `Cart.user` and `Order.user` are plain references, so `GET /cart`, add/remove, checkout and `GET /orders` all include `"password":"$2a$10$…"`. I confirmed it by running the API. The quick fix is `@JsonIgnore`. The real fix is response DTOs.

**C2. 🔴 How would you make checkout correct under concurrency?**
> Inside one transaction, for each line, run an atomic conditional update: `UPDATE products SET stock_qty = stock_qty - :q WHERE id = :id AND stock_qty >= :q`, and fail if zero rows were affected. Alternatively, lock the rows with `@Lock(PESSIMISTIC_WRITE)`, ordered by product id to avoid deadlocks, or use an `@Version` column with retries. Plus an idempotency key, so a retried request doesn't create two orders.

**C3. 🟡 Walk me through the JWT filter.**
> For each request, if the `Authorization` header starts with `Bearer `, parse the token to get the username, load the user details, validate the subject and expiry, and put an authenticated token in the `SecurityContext`. The flaw is that parsing throws on malformed or expired tokens and the filter doesn't catch it, so a stale token causes a 403 even on `/auth/login`. I'd catch `JwtException` and skip the filter for `/auth/**`.

**C4. 🟡 What's wrong with the login error handling?**
> Two problems. Different messages for unknown username vs wrong password let attackers enumerate accounts. And both throw `RuntimeException`, which my handler maps to 400, when it should be 401 with one generic "invalid credentials" message. I'd also add rate limiting.

**C5. 🟢 How are passwords stored?**
> BCrypt via Spring's `BCryptPasswordEncoder` at the default cost 10: salted and deliberately slow. Login uses `passwordEncoder.matches`.

**C6. 🟡 Why `@JsonManagedReference`/`@JsonBackReference`?**
> Cart ↔ CartItem and Order ↔ OrderItem are bidirectional, so naive serialisation recurses forever. Managed/back reference tells Jackson to serialise one side and skip the back pointer. DTOs remove the need for it.

**C7. 🟡 What happens if you request a product that doesn't exist?**
> `findById(...).orElse(null)`, so the controller returns null, which is a 200 with an empty body. It should throw a not-found exception mapped to 404.

**C8. 🟢 Why doesn't the repo build as committed?**
> I uploaded the Java files flat into the root, but Maven expects `src/main/java/com/sid/commerceapi/...`. Restructuring by each file's package fixes it. I verified it compiles and runs after that.

---

## (d) Underlying CS theory

**D1. 🔴 Explain ACID and which property the checkout violates.**
> Atomicity (all or nothing), Consistency (invariants hold), Isolation (concurrent transactions don't interfere) and Durability (committed data survives). My checkout violates atomicity, because partial stock decrements persist on failure. Under concurrency it also lacks isolation for the read-modify-write on stock, which allows lost updates and overselling.

**D2. 🟡 What isolation level does Postgres use by default, and does it prevent overselling?**
> READ COMMITTED. Each statement sees committed data, but a read-then-write in application code can interleave with another transaction's, so it doesn't prevent lost updates. SERIALIZABLE would detect them via serialisation failures. Or use explicit row locks or an atomic UPDATE with a guard.

**D3. 🟡 How does a JWT work, and what does HS256 sign?**
> Header and payload are base64url-encoded JSON, and the signature is HMAC-SHA256 over `header.payload` with a shared secret. The server recomputes it to verify integrity, then checks `exp`. The payload is readable, not encrypted, so no secrets go in claims.

**D4. 🟡 Why BCrypt/Argon2 instead of SHA-256 for passwords?**
> Fast hashes make offline brute force cheap. BCrypt is adaptive (cost factor) and salted. Argon2id is also memory-hard, which resists GPUs and ASICs. That's why leaking even a hash matters.

**D5. 🟢 What's the N+1 query problem?**
> Fetching N parents, then lazily loading each one's children with N more queries. Returning entity graphs from controllers invites it. Fetch joins, entity graphs or DTO projections fix it.

**D6. 🟡 Optimistic vs pessimistic locking?**
> Pessimistic takes a row lock (`SELECT … FOR UPDATE`), so others wait. It's good under high contention. Optimistic uses a version column and fails on conflict, then the caller retries. It's good when conflicts are rare. For hot inventory items, pessimistic locking or atomic conditional updates are usually simpler.

---

## (e) Evaluation & testing

**E1. 🔴 How did you test it, and how should it be tested?**
> Originally manually with Postman. Re-running it myself is how I found the bugs. Properly: `@WebMvcTest` for controllers and the security rules, `@DataJpaTest` for repositories, and Testcontainers-backed integration tests for checkout. Those would include a failure-midway test asserting no stock changed, and a concurrency test with two threads checking out the last unit and asserting exactly one succeeds. Plus CI on every push.

**E2. 🟡 What security tests would you add?**
> Assert the password never appears in any response body, that USERs get 403 on product writes, that bad credentials give a generic 401, that malformed and expired tokens give 401 (and don't break `/auth/login`), and that tampered tokens are rejected.

**E3. 🟢 How would you load-test checkout?**
> k6 or Gatling with many virtual users checking out the same scarce product. Check that there's no oversell, measure p95 latency, and watch DB lock waits.

---

## (f) Scaling, production & system design

**F1. 🟡 How would you scale it?**
> The app tier is stateless thanks to JWT, so run several instances behind a load balancer. Add DB connection pooling (HikariCP is the default), read replicas for catalogue reads, a cache for product listings (Redis), and pagination. Inventory correctness stays in the primary DB with row-level guards.

**F2. 🔴 Design checkout for a flash sale.**
> Reserve inventory atomically per item (conditional decrement, or a Redis Lua script as a pre-check with the DB as the source of truth), create the order in a PENDING state, take payment asynchronously, then confirm or release the reservation with a timeout. Use idempotency keys, a queue to smooth spikes, and the outbox pattern for events.

**F3. 🟡 What would you change for production deployment?**
> Secrets from a vault or environment, Flyway migrations instead of `ddl-auto=update`, health checks and readiness probes, a multi-stage Dockerfile on a JRE image, structured logging, metrics (Actuator + Prometheus), HTTPS termination, and rate limiting on auth.

**F4. 🟢 Why is `ddl-auto=update` risky?**
> It mutates the schema implicitly at startup, never drops anything safely, and can diverge between environments. Versioned migrations are reviewable and reproducible.

---

## (g) Failure modes & debugging

**G1. 🔴 Inventory numbers drift from reality. How do you investigate?**
> Correlate stock changes with orders. With my code, a failed checkout decrements earlier items without creating an order, so you'd see decrements with no matching order lines. I'd also check for concurrent checkouts on the same product (lost updates). Fix both with a transactional, guarded decrement, and add an audit table of stock movements.

**G2. 🟡 A client says "I get 403 when I try to log in".**
> They're sending an old or invalid `Authorization` header to `/auth/login`. My filter parses it and throws before `permitAll` takes effect. The fix is catching JWT exceptions and skipping the filter for `/auth/**`.

**G3. 🟡 Registering returns a 400 with an SQL message. Why?**
> A duplicate email hits the DB unique constraint. The `DataIntegrityViolationException` is a `RuntimeException`, so my generic handler returns its message verbatim. Check email uniqueness up front, return 409, and never echo raw exception text.

**G4. 🟢 The app starts before Postgres in Compose and crashes. Why?**
> `depends_on` only orders container start, not readiness. Add a Postgres health check and `condition: service_healthy`, or retry the connection.

---

## (h) "Why didn't you…" / alternatives

**H1. 🟡 Why not Spring Security's OAuth2 resource server?**
> Rolling my own JWT filter was a learning exercise. In production I'd use the resource-server support with a proper issuer (Keycloak or Auth0), which handles key rotation and validation edge cases.

**H2. 🟢 Why not Lombok, since it's in the pom?**
> I declared it but wrote getters and setters by hand. I'd use Java records for DTOs, and either Lombok or plain code for entities, consistently.

**H3. 🟡 Why not GraphQL?**
> For a small CRUD-plus-checkout API, REST is simpler and cache-friendly. GraphQL helps when clients need flexible, nested reads across many entities.

**H4. 🟡 Why `Double` for prices?**
> Simplicity, but it's wrong for money. Floating point can't represent 0.1 exactly, so totals drift. I'd use `BigDecimal` with explicit rounding, or integer minor units (paise).

---

## (i) Behavioural (STAR)

**I1. 🔴 Tell me about finding a serious bug in your own work.**
> **S:** I'd described this API as production-style. **T:** Before using it in interviews, verify the claims. **A:** I restructured it so it builds, ran it against an in-memory database, and scripted the flows with curl. I found that cart and order responses included password hashes, and that a failed checkout left inventory partially decremented. **R:** I documented both with reproduction steps and planned the fixes (DTOs, a transactional service with guarded stock updates, RBAC, plus tests that would have caught them). The lesson: run it end to end and assert on what the responses contain.

**I2. 🟡 Tell me about a design decision you'd reverse.**
> Returning JPA entities from controllers. It saved code at first, but it leaked data, coupled the API to the schema and needed Jackson annotations to avoid recursion. DTOs from day one would have been cheaper.

**I3. 🟢 Tell me about learning a new framework.**
> Spring Security's filter chain was the steep part: understanding that filters run before controllers, so exceptions there bypass `@RestControllerAdvice`. That's exactly the source of my login-403 bug, and why I'd now add a custom entry point and handle JWT exceptions in the filter.

---

## Hostile follow-ups

**X1. 🔴 "You called this production-style, but it leaks password hashes."**
> Yes. I verified it myself. It's a serialisation bug from returning entities. The stop-gap is `@JsonIgnore` on the password field, and the real fix is DTOs plus a test asserting the hash never appears in any response. I've stopped describing it as production-style until that's done.

**X2. 🔴 "Your checkout corrupts inventory. Did you ever test a failure case?"**
> Not originally. I only tested the happy path in Postman. When I tested a failure, the first item's stock stayed decremented. It needs `@Transactional` at a service boundary and a guarded atomic decrement. And the test that should have existed is "failed checkout leaves stock unchanged".

**X3. 🔴 "Any user can delete products. Did you think about authorisation at all?"**
> I added roles to the user model but never enforced them. That's a privilege-escalation bug. The fix is an admin role and `@PreAuthorize` or request matchers on product writes, with tests.

**X4. 🟡 "Your JWT secret is in the source code."**
> Correct. Anyone with the repo can mint tokens for any user. It should come from the environment or a secret manager, be at least 256 bits of random data, and be rotated, ideally with asymmetric keys if multiple services verify tokens.

**X5. 🟡 "The repo doesn't even build."**
> The files were uploaded flat instead of in Maven's source layout. It's a packaging mistake, and it builds and runs once restructured. I'd also add CI so a broken layout can't be pushed unnoticed.

**X6. 🟡 "Why does 'not found' return 200 with an empty body?"**
> `orElse(null)` in the service, with no exception. It should throw a domain `NotFoundException` that the handler maps to 404. I'd fix the whole status-code taxonomy at once.

**X7. 🟡 "How is this relevant to an ML role?"**
> Model-serving systems are backends too: authenticated endpoints, idempotent requests, transactional state, no data leaks in responses. The failure modes I found (atomicity, concurrency, leaking internal objects) apply directly to feature stores and inference APIs.

**X8. 🟢 "What's the very first thing you'd fix?"**
> The password leak (minutes), then transactional checkout (hours), then RBAC. In order of harm.
