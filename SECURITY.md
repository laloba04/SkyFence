# Security Policy

## Secret management

- Never commit real credentials to git.
- Use `.env` for local secrets (already gitignored).
- Keep only placeholders in `.env.example`.
- Prefer CI/CD secret managers for deployed environments.

## If a secret is exposed

1. Revoke and rotate the credential immediately.
2. Rewrite git history to remove compromised commits.
3. Force-push the sanitized branch and re-open/re-scan PR.
4. Document the incident and remediation.

## Automated security scanning (CI/CD)

Every push and pull request to `main` triggers:

| Tool | Workflow | What it checks |
|------|----------|----------------|
| **Gitleaks** | `secret-scan.yml` | Secrets and credentials accidentally committed |
| **CodeQL** | `codeql.yml` | SAST — Java vulnerabilities and code injection risks |
| **SonarCloud** | `sonar.yml` | Code quality, security hotspots and coverage |
| **npm audit** | `ci.yml` | Frontend dependency vulnerabilities (SCA) |

## Local secret scanning

This repository includes pre-commit hooks for `gitleaks` and `detect-secrets`.

```bash
pip install pre-commit
pre-commit install
pre-commit run --all-files
```
