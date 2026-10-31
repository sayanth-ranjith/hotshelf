# Iteration 00: Project foundation

## Goal
Turn the Spring Initializr scaffold into a clean **library** project: a multi-module Maven build with a dependency-free
`hotshelf-core` module, building and testing on Java 21.

## Concepts you'll learn
- How a **library** differs from an **application**. A library has no `main`, depends on as little as possible, and has a small public API.
- Maven multi-module builds: parent POM, `<modules>`, `<dependencyManagement>`.
- Why dependency hygiene matters: every dependency you ship becomes your users' dependency too.

## Scope
**In**
- Install JDK 21 (e.g. Eclipse Temurin 21) and point `JAVA_HOME` at it. The machine currently only has JDK 17.
- Root `pom.xml` becomes a parent POM (`<packaging>pom</packaging>`) that is *not* Spring Boot's parent.
- New module `hotshelf-core/` with `src/main/java` and `src/test/java` under `io.github.sayanth_ranjith.hotshelf`.
- Test dependencies only: JUnit 5 (via the `junit-bom`) and AssertJ.
- Remove `HotshelfApplication`, its test, `application.properties` and the Spring dependencies.
- Fill in the empty `<name>`, `<description>` and `<licenses>` metadata. Pick a license, e.g. MIT or Apache-2.0.
- One trivial passing test proves the build works: `.\mvnw.cmd verify`.

**Out**
- Any cache code (iteration 01).
- The Spring starter module (v3). Its directory is not created yet.

## Design

```
hotshelf/                      (parent POM: versions, plugins, Java 21)
└── hotshelf-core/             (the library: zero runtime deps)
    └── src/main/java/io/github/sayanth_ranjith/hotshelf/
```

Later: `hotshelf-spring-boot-starter/`, `hotshelf-benchmarks/` (JMH, iteration 07).

**Why multi-module from day one?** It costs about 20 lines of XML now, and it saves moving every file later. More importantly,
it *enforces* ADR-001: core can't accidentally import Spring, because Spring isn't on its classpath. This is
**separation of concerns** at the build level.

**Why not keep `spring-boot-starter-parent`?** It is designed for applications. It pins dozens of dependency versions
and configures repackaging that a plain library doesn't want. A note on versions: the scaffold picked Spring Boot **4.1.2**,
while the README says **3.x**. We'll settle that in v3 and update the README to match.

## Definition of done
- [ ] `java -version` shows 21 and `.\mvnw.cmd verify` passes.
- [ ] `hotshelf-core` has no `<dependency>` without `<scope>test</scope>`.
- [ ] No Spring classes remain in the repo.
- [ ] README tech stack and roadmap still accurate.

## Pitfalls
- `JAVA_HOME` still pointing at 17 while `java` on PATH is 21, or the other way round. Maven uses `JAVA_HOME`.
- Leaving `HELP.md` around. It's Initializr boilerplate and `.gitignore` already ignores it, so just delete it.

## Learning log
*(fill in after the iteration)*
