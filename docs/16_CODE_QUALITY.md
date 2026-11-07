# Code Quality Standards & Architectural Fitness Rules: Oi

> **Document ID:** QUALITY-016-CORE  
> **Status:** Confirmed Baseline  
> **Last Updated:** 2026-11-07  
> **Project Repository:** `https://github.com/shanuthchimbli/oisocialapp`  
> **Author:** Lead Developer & Quality Architect  
> **Traceability Upstream:** [`docs/01_PRODUCT_REQUIREMENTS.md`](file:///Users/shanuthchimbli/.gemini/antigravity/scratch/oisocialapp/docs/01_PRODUCT_REQUIREMENTS.md), [`docs/11_ANDROID_ARCHITECTURE.md`](file:///Users/shanuthchimbli/.gemini/antigravity/scratch/oisocialapp/docs/11_ANDROID_ARCHITECTURE.md)  

---

## 1. Executive Summary & Code Philosophy

To ensure **Oi** remains clean, robust, and maintainable by a single primary engineer over years of production operation, the codebase enforces strict, automated code quality standards and architectural fitness assertions.

---

## 2. Architectural Fitness Enforcement (Konsist Rules)

We utilize **Konsist** unit tests to automatically enforce clean architecture boundaries in CI:

```kotlin
class ArchitectureFitnessTest {
    @Test
    fun `domain layer must not depend on android framework`() {
        Konsist.scopeFromModule("core:domain")
            .files
            .assertNot { it.hasImports("android..") }
    }

    @Test
    fun `viewmodels must expose immutable stateflow only`() {
        Konsist.scopeFromPackage("..presentation..")
            .properties()
            .withNameEndingWith("State")
            .assertTrue { it.hasType("kotlinx.coroutines.flow.StateFlow..") }
    }

    @Test
    fun `presentation must not directly access repositories`() {
        Konsist.scopeFromPackage("..presentation..")
            .classes()
            .assertNot { it.hasImports("..data.repository..") }
    }
}
```

---

## 3. Static Analysis & Linter Standards

| Tool | Focus Area | Configuration & Quality Gate |
| :--- | :--- | :--- |
| **Spotless / ktlint** | Formatting, indentation, imports | Fails build if formatting deviates from standard Kotlin style guide. |
| **Android Lint** | Platform bugs, deprecated APIs, performance traps | `abortOnError true`; zero warnings permitted in release builds. |
| **Detekt** | Cyclomatic complexity, code smells, long methods | Max method length: 40 lines; Max cyclomatic complexity: 10. |
| **SonarQube / SonarCloud**| Security vulnerabilities, duplication, technical debt | **Quality Gate:** 0 Security Vulnerabilities, 0 Bugs, Debt Ratio $< 3\%$. |
| **TruffleHog / GitGuardian**| Secret & credential scanning | Pre-commit hook and CI gate blocks commits with API keys or tokens. |

---

## 4. Test Coverage Policy with Meaningful Scope

Arbitrary global coverage targets (e.g., blanket "80%") incentivize trivial tests while neglecting critical failure modes. Oi enforces targeted coverage gates:
1. **Domain UseCases & Business Invariants:** **100% Branch Coverage** mandatory.
2. **Cryptographic Ratchets & Key Derivation:** **100% Branch Coverage** with Cavium known-answer test vectors.
3. **Database DAOs & Migrations:** **100% Integration Test Coverage**.
4. **UI Composables:** Measured via UI Macrobenchmarks and Compose screenshot regression tests rather than line coverage.
