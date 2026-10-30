# Selected toolchain

Version selection was checked against official documentation and published package registries
on 30–31 October 2026. Dependencies are stable releases; the pnpm lockfile is committed.

| Component | Selected version | Source / reason |
| --- | --- | --- |
| Java | 21 LTS | Supported LTS, compatible with Spring Boot 4.1 |
| Spring Boot | 4.1.2 | [Official system requirements](https://docs.spring.io/spring-boot/system-requirements.html) |
| Gradle | 8.14.3 | [Java compatibility](https://docs.gradle.org/current/userguide/compatibility.html); committed executable wrapper |
| PostgreSQL | 18.6 | [Supported versions](https://www.postgresql.org/support/versioning/); explicit Docker image tag |
| Springdoc | 3.2.0 | [Springdoc compatibility](https://springdoc.org/); Spring Boot 4.1-compatible line |
| Testcontainers | 2.0.5 | Boot-compatible BOM; real PostgreSQL and Docker Desktop verified |
| Next.js | 16.4.3 | [Next releases](https://nextjs.org/blog); registry-verified stable patch |
| React | 19.3.1 | Next-compatible stable React release |
| TypeScript | 6.0.3 | Strict checks; compatible with selected lint/tooling |
| Tailwind | 4.3.3 | PostCSS integration, shared CSS design tokens |
| pnpm | 11.12.0 | Explicit packageManager and committed lockfile |
| Node | 24.13.1 Docker; 22/24 supported locally | Next.js-compatible LTS lines |
| ESLint | 10.13.0 | Maintained core, compatible Next/TypeScript/hooks rules |
| Vitest | 4.1.12 | Focused formatting/date contracts |
| Playwright | 1.64.1 | Real desktop/phone Chromium journeys |

Spring's dependency-management BOM pins Spring Security, JPA/Hibernate, Flyway and JDBC
transitives. The inventory SQL intentionally depends on PostgreSQL rather than an H2
approximation. AWS, MySQL, Thymeleaf, WebFlux, Lombok and JWT dependencies were removed after
checking their inherited usages; local operation needs no cloud account.

The production build uses `next build --webpack`, a supported Next option. Turbopack's CSS
worker failed to bind a local port under this workstation's execution restrictions. This
selection is explicit in both development and build scripts and the Docker image.

A final support audit corrected the initial 3.5 selection: its final OSS release was
[announced on 25 June 2026](https://spring.io/blog/2026/06/25/spring-boot-3-5-16-available-now/).
The delivered backend uses supported Spring Boot 4.1.2 with Springdoc 3.2.0, Jackson 3 and
Testcontainers 2.0.5. No commercial support subscription is required.
