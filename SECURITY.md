# Security Policy

## Supported Versions

| Version | Supported |
|---------|-----------|
| 1.x     | ✅ Yes     |

## Reporting a Vulnerability

**Do not open a public GitHub issue for security vulnerabilities.**

Please report security issues privately by emailing the ToolkitMC organisation maintainers, or by using [GitHub's private vulnerability reporting](https://github.com/ToolkitMC/macroEngine-fabric/security/advisories/new).

Include:
- A description of the vulnerability
- Steps to reproduce
- Potential impact
- Any suggested mitigations

You will receive a response within **72 hours**. If the issue is confirmed, we will release a patch as soon as possible.

## Scope

This mod runs on the server side only and requires operator permission (`level 2`) for all commands. The primary attack surface is:
- Arbitrary function execution via `/macro-engine run`
- File system writes via `/macro-engine create` and `/macro-engine add`

Both are gated behind permission level 2 and should only be accessible to trusted operators in development environments.
