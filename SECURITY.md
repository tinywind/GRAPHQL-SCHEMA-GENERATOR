# Security Policy

## Supported Versions

Security fixes are released for the current minor release line. Older releases do not receive security backports.

| Version | Supported |
| ------- | --------- |
| 0.1.x   | Yes       |
| < 0.1.0 | No        |

## Reporting a Vulnerability

Please do **not** report undisclosed security vulnerabilities through public GitHub issues, pull requests, or discussions.

Use GitHub's [private vulnerability reporting](https://github.com/tinywind/GRAPHQL-SCHEMA-GENERATOR/security/advisories/new) for this repository to submit a report confidentially. If you cannot use GitHub Security Advisories, contact the maintainer directly at `tinywind0@gmail.com`.

When reporting an issue, please include as much of the following as possible:

- Affected version of `org.tinywind.graphql-schema-generator`
- JDK, Gradle, JDBC driver, and database dialect/version used
- A minimal reproduction or proof of concept (without real database credentials or private schemas)
- Description of the potential impact and any suggested mitigation

## Response Process

- We aim to acknowledge receipt of a vulnerability report within 7 days.
- Validated issues are tracked privately through a GitHub Security Advisory while a fix is prepared and verified.
- Once a patched release is available, we will publish the advisory and credit reporters who wish to be acknowledged.

## Security Model Notes

`org.tinywind.graphql-schema-generator` runs at build time as a Gradle plugin (`generateGraphqlSchema` and `verifyGraphqlSchema`). It loads a JDBC driver from the `graphqlSchema` configuration, reads schema metadata through `jooq-meta`, and renders a GraphQL SDL file. Database passwords are excluded from error output, JDBC URL query strings are stripped from messages, and table/column selection is fail-closed so unmatched rules fail generation rather than silently exposing objects. Reports involving credential leakage in logs or errors, unsafe GraphQL SDL escaping or injection from database metadata, bypasses of table or column exclusion rules, unsafe driver loading or file path handling, or vulnerable transitive dependencies are in scope.
