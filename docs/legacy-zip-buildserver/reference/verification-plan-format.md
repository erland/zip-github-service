# Verification Plan Format

Verification plan files use a deliberately small YAML-like format. The parser is intentionally explicit rather than a general YAML parser so uploaded repositories cannot influence deserialization behavior and so unsupported syntax fails clearly.

## Supported root keys

- `id`
- `name`
- `technology`
- `enabled`
- `networkMode`
- `selectionReason`
- `indicators`
- `commands`

Unknown root keys are rejected with a line-numbered parse error.

## Supported list sections

`indicators` accepts indented list entries:

```yaml
indicators:
  - package.json
```

`commands` accepts indented command entries. Each command must start with `- label:` and may then include:

- `workingDirectory`
- `commandDisplay`
- `timeoutSeconds`
- `optional`

Unknown command keys are rejected with a line-numbered parse error.

## Values and comments

Single-quoted and double-quoted scalar values are supported. Inline comments begin with `#` only when the character appears outside quotes, so values such as `"npm test # not a comment"` are preserved.

## Defaults

- `networkMode`: `DEPENDENCY`
- `enabled`: `true`
- command `workingDirectory`: `${project.path}`
- command `timeoutSeconds`: `600`
- command `optional`: `false`

## Validation

After parsing, plans are validated by `VerificationPlanValidator`. A valid plan must include `id`, `name`, `technology`, and at least one command. Each command must include `label` and `commandDisplay`.
