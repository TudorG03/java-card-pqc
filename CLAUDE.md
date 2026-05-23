# Java Card PQC Signing — Project Guidelines

## What this project is

A Java Card applet that signs files using ML-DSA (FIPS 204) + SHA3-256, plus a Rust host application
that drives the applet over a TCP socket. The applet runs inside jCardSim (a JVM-based Java Card
simulator). Both components are containerized via Docker Compose.

## Pinned versions

| Component | Version |
|---|---|
| JDK (local dev) | 25.0.3 (installed) |
| JDK (Docker) | 21 LTS (`eclipse-temurin:21-jdk`) |
| jCardSim | 3.0.6.0 (`com.klinec:jcardsim` — the actively maintained fork) |
| BouncyCastle | 1.80 (`bcprov-jdk18on` — ML-DSA is in the main jar, no separate bcpqc artifact) |
| Rust | 1.95.0 |
| `sha3` crate | 0.10.8 |
| `ml-dsa` crate | 0.1.0 |

Do not upgrade any of these without explicit discussion. Pin all Maven dependencies with exact versions
in `pom.xml`. Pin all Rust crates with exact versions in `Cargo.lock` (always commit the lock file).
Use `rust-toolchain.toml` to enforce the Rust version in the repo.

## Project layout

```
java-card/
├── applet/          # Java Card applet (Maven project)
├── simulator/       # Thin Java TCP wrapper around jCardSim (Maven project)
├── host/            # Rust host application (Cargo project)
├── docker/          # Dockerfiles for each component
├── compose.yml      # Docker Compose for full-stack run
├── CLAUDE.md
└── README.md
```

## Design principles

### Single Responsibility
Every class and module does exactly one thing. The applet class handles APDU routing only — it
delegates to dedicated classes for hashing and signing. The Rust host separates file I/O, hashing,
APDU transport, and signature serialization into distinct modules.

### Dependency Inversion
Program to abstractions, not concrete implementations — especially at the cryptographic boundary.

- Java: define `Signer` and `Hasher` interfaces. The ML-DSA and SHA3-256 implementations are
  injected behind them. This makes the algorithm swap-able without touching APDU dispatch logic.
- Rust: define `Signer` and `Hasher` traits. Concrete implementations are in separate modules.

The other SOLID principles (Open/Closed, Liskov, Interface Segregation) follow naturally from the
above and do not need to be explicitly enforced at this project's scale.

## Testing — TDD

Write tests before writing implementation code. Never merge to `main` with failing tests.

### Rust host
Standard TDD with `cargo test`. Each module has a corresponding `#[cfg(test)]` block.
Tests run in CI on every PR.

### Java Card applet
Unit testing through the APDU contract using jCardSim's `CardSimulator`:
1. Write a JUnit test that installs the applet into `CardSimulator`, sends an APDU, asserts
   on the response bytes and status word.
2. Then write the applet code that makes the test pass.

Cryptographic correctness tests must use NIST known-answer test vectors for both ML-DSA and
SHA3-256. These vectors are authoritative — if the implementation disagrees with them, the
implementation is wrong.

## Git workflow

### Commit format — Conventional Commits
```
<type>(<scope>): <short description>

Types: feat | fix | test | docs | chore | refactor
Scope: applet | simulator | host | docker | ci

Examples:
  feat(applet): add SIGN instruction with ML-DSA signing
  test(applet): add NIST KAT vectors for SHA3-256 HASH instruction
  chore(docker): add two-step cargo build for layer caching
```

### Branching
- `main` — always buildable, always passing tests. Never commit directly.
- `feat/task-N-short-description` — one branch per task. Example: `feat/task-1-skeleton-applet`.
- Open a PR to `main` when the task is complete and all tests pass. Self-review the diff before
  merging.

### Rules
- Every PR must have passing tests before merge.
- Commit messages must follow the Conventional Commits format above.
- Never commit secrets, keys, or generated `.cap` files.
- Always commit `Cargo.lock`. Never commit `target/` or Maven's `target/`.

## Docker

Two services in `compose.yml`:

- **simulator** — builds and runs the Java TCP wrapper (exposes port 9025)
- **host** — builds the Rust binary, connects to simulator, runs the signing demo

Rust Dockerfiles must use the two-step copy pattern to cache compiled dependencies:
```dockerfile
# 1. compile dependencies (cached unless Cargo.toml/Cargo.lock changes)
COPY Cargo.toml Cargo.lock ./
RUN mkdir src && echo "fn main() {}" > src/main.rs && cargo build --release

# 2. compile actual source (re-runs on every code change)
COPY src ./src
RUN touch src/main.rs && cargo build --release
```

## What NOT to do

- Do not implement ML-DSA from scratch. Use BouncyCastle from inside the applet.
- Do not use SHA-2 anywhere. This project is SHA-3 / Keccak only.
- Do not add features, abstractions, or error handling beyond what the current task requires.
- Do not skip writing tests first — implement, then note that you skipped and fix it.
- Do not merge a branch with `TODO` or `FIXME` comments unless they reference a tracked task.
