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
- [x] `java -version` shows 21 and `.\mvnw.cmd verify` passes.
- [x] `hotshelf-core` has no `<dependency>` without `<scope>test</scope>`.
- [x] No Spring classes remain in the repo.
- [x] README tech stack and roadmap still accurate.

## Pitfalls
- `JAVA_HOME` still pointing at 17 while `java` on PATH is 21, or the other way round. Maven uses `JAVA_HOME`.
- Leaving `HELP.md` around. It's Initializr boilerplate and `.gitignore` already ignores it, so just delete it.

## Learning log
**What changed**
- Root `pom.xml` is now `hotshelf-parent` (`<packaging>pom</packaging>`). It holds metadata (MIT license, SCM,
  developer), the Java level (`maven.compiler.release=21`), and pinned versions of JUnit, AssertJ and the
  compiler / surefire / jar plugins.
- `hotshelf-core/` is the only module. Its `pom.xml` declares exactly two dependencies, both `test`-scoped.
- `HotshelfApplication`, its `@SpringBootTest`, `application.properties` and `HELP.md` are gone.
- `BuildSanityTest` moved into core and now asserts Java **21**, not 17.
- `package-info.java` gives the package a home (git doesn't track empty folders) and a one-line description.

**Patterns and principles**
- *Separation of concerns at build level.* The parent knows *how to build*. Core knows *what the library is*.
  A future starter module gets Spring on its own classpath and core still cannot see it, so ADR-001 is
  enforced by the compiler, not by discipline.
- *Single source of truth (DRY) for versions.* `<dependencyManagement>` and `<pluginManagement>` only declare
  versions. A module still has to ask for a dependency to get it. That is the difference from putting
  `<dependencies>` in the parent, which would push JUnit into every module whether it wants it or not.
- *BOM import.* `junit-bom` keeps `junit-jupiter-api`, `-engine` and `junit-platform-*` on matching versions.
  We pin one number, not five.

**Rejected alternatives**
- *Keep `spring-boot-starter-parent` as parent.* It's built for apps: it manages hundreds of versions we don't
  use and configures repackaging into an executable jar. A library should not inherit any of that.
- *Single module, add modules later.* Cheaper today, but the "no Spring in core" rule would only be a convention
  until then, and moving `src/` later touches every file.
- *`maven.compiler.source/target` instead of `release`.* `release` also checks that you only call JDK 21 APIs,
  so building on a newer JDK can't sneak in a newer API by accident.

**Gotchas hit**
- The system PATH still resolves `java` to JDK 17. Maven uses `JAVA_HOME` (JDK 21), so the build is fine; the
  first `BuildSanityTest` assertion would fail loudly if that ever changed.
