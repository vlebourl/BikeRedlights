# Release Workflow Template

This template provides comprehensive instructions for creating annotated git tags and GitHub releases with proper documentation.

## Comprehensive Git Tag Annotation Template

```bash
git tag -a vX.Y.Z -m "$(cat <<'EOF'
Release vX.Y.Z: <Feature Name> (<Release Type>)

<Project Name> vX.Y.Z - <One-line summary of release>

FEATURES (<Number> User Stories Delivered):

User Story 1 (<Priority>): <Story Title>
- <Feature bullet 1>
- <Feature bullet 2>
- <Feature bullet 3>

User Story 2 (<Priority>): <Story Title>
- <Feature bullet 1>
- <Feature bullet 2>

[Add more user stories as needed]

ARCHITECTURE:

<Architecture pattern description>:
- <Layer 1>: <Components>
- <Layer 2>: <Components>
- <Layer 3>: <Components>

Key Features:
- <Key feature 1>
- <Key feature 2>
- <Key feature 3>
- <Key feature 4>
- <Key feature 5>

TEST COVERAGE:

<Coverage percentage>+ coverage for safety-critical code (per Constitution requirement):
- Unit Tests: <count> tests (<test suites>)
- UI Tests: <count> tests (<test suites>)
- Total: <total> tests, all passing

TECHNICAL DETAILS:

- APK Size: <size>MB (release build, minified with R8)
- Min SDK: API <version> (Android <name>)
- Target SDK: API <version> (Android <name>)
- <Key dependency 1>: <details>
- <Key dependency 2>: <details>
- <Key setting 1>: <details>
- <Key setting 2>: <details>

CHANGES:

- <count> files changed, <count> insertions
- Production code: <count> files (<areas>)
- Test code: <count> test files (<types>)
- Spec files: <count> documentation files

LINKS:

- Pull Request: <PR URL>
- Feature Spec: <spec file path>
- Implementation Plan: <plan file path>
- Tasks Breakdown: <tasks file path> (<task count> tasks completed)

Tested On: <Device/Emulator> (<Android Version>)

<Additional context or notes about this release>
EOF
)"
```

### Tag Annotation Requirements

**Minimum Content**:
- 50+ lines of comprehensive documentation
- All user stories delivered with full descriptions
- Complete architecture breakdown
- Test coverage statistics with breakdown
- Technical details (APK size, SDK versions, dependencies)
- Links to PR and all spec documentation

**Verification**:
```bash
# View full tag annotation
git show vX.Y.Z

# View first 100 lines
git tag -l -n100 vX.Y.Z
```

---

## Comprehensive GitHub Release Template

```bash
gh release create vX.Y.Z \
  --title "vX.Y.Z - <Feature Name> (<Release Type>)" \
  --notes "$(cat <<'EOF'
## 🚴 <Release Type>

**<Project Name> vX.Y.Z** - <One-line summary>

**APK Size**: <size>MB (release build, minified with R8)
**Tested On**: <Device/Emulator> (<Android Version>)
**Pull Request**: [#<number>](<PR URL>)

---

### ✨ Features (<Number> User Stories Delivered)

**[Feature <number>: <Feature Name>](<spec URL>)**

#### User Story 1 (<Priority>): <Story Title> ✅

- <Feature bullet 1>
- <Feature bullet 2>
- <Feature bullet 3>

#### User Story 2 (<Priority>): <Story Title> ✅

- <Feature bullet 1>
- <Feature bullet 2>

[Add more user stories as needed]

---

### 🏗️ Architecture

**<Architecture Pattern>:**

- **<Layer 1>**: <Components>
- **<Layer 2>**: <Components>
- **<Layer 3>**: <Components>

**Key Features:**

- 🔐 <Security feature>
- ♿ <Accessibility feature>
- 🌙 <Theme feature>
- 🔄 <Lifecycle feature>
- 🔋 <Performance feature>

---

### ✅ Test Coverage

**<Percentage>+ coverage** for safety-critical code (per [Constitution](<constitution URL>) requirement):

- **Unit Tests**: <count> tests
  - <Test suite 1>: <What it tests>
  - <Test suite 2>: <What it tests>
- **UI Tests**: <count> tests
  - <Test suite 1> (<count> tests)
  - <Test suite 2> (<count> tests)
- **Total**: <total> tests, all passing ✅

---

### 📦 Changes

- **<count> files changed**, <count> insertions
- **Production code**: <count> files (<areas>)
- **Test code**: <count> test files (<types>)
- **Spec files**: <count> documentation files

---

### 🔧 Technical Details

- **Minimum SDK**: API <version> (Android <name>)
- **Target SDK**: API <version> (Android <name>)
- **<Key dependency 1>**: <details>
- **<Key dependency 2>**: <details>
- **<Key setting 1>**: <details>
- **<Key setting 2>**: <details>

---

### 📥 Installation

1. **Download** `app-release.apk` from assets below
2. **Enable** "Install from unknown sources" in Android settings
3. **Install** the APK
4. **Grant** <required permissions> when prompted
5. **Start using** the app! 🚴

---

### 📚 Documentation

- **[Feature Specification](<spec URL>)** - Requirements and user stories
- **[Implementation Plan](<plan URL>)** - Architecture and design decisions
- **[Tasks Breakdown](<tasks URL>)** - <count> tasks completed across <count> phases
- **[Quick Start Guide](<quickstart URL>)** - Development setup and implementation guide

---

### 🚀 What's Next

This is the **<release type>** establishing the foundation for future features including:

- <Future feature 1>
- <Future feature 2>
- <Future feature 3>

---

### 🐛 Known Issues

- **<Issue 1>** - <Description and workaround>
- **<Issue 2>** - <Description and workaround>

[If no issues, write "None currently known."]

---

**Note**: <Release-specific notes>

🤖 **Generated with [Claude Code](https://claude.com/claude-code)**
EOF
)" \
  app/build/outputs/apk/release/app-release.apk
```

### GitHub Release Requirements

**Formatting**:
- Use emoji headers for all sections (🚴, ✨, 🏗️, ✅, 📦, 🔧, 📥, 📚, 🚀, 🐛)
- Include horizontal rules (---) between major sections
- All user stories must be documented with checkmarks (✅)
- Architecture must use bold for layers and keys
- Use consistent emoji prefixes for key features

**Required Sections** (in order):
1. Header with emoji and release type
2. ✨ Features (all user stories)
3. 🏗️ Architecture
4. ✅ Test Coverage
5. 📦 Changes
6. 🔧 Technical Details
7. 📥 Installation
8. 📚 Documentation
9. 🚀 What's Next
10. 🐛 Known Issues

**Verification**:
```bash
# View release
gh release view vX.Y.Z

# Verify APK attachment
gh release view vX.Y.Z --json assets -q '.assets[] | {name: .name, size: .size}'

# Open in browser
open https://github.com/<org>/<repo>/releases/tag/vX.Y.Z
```

---

## Pre-Flight Checklist

Before creating tag and release, ensure:

- [ ] PR merged to main
- [ ] RELEASE.md updated with version section
- [ ] app/build.gradle.kts version bumped
- [ ] All tests passing
- [ ] Release APK built successfully
- [ ] All placeholder values in templates replaced
- [ ] Links to PR and spec files verified

---

## Common Mistakes to Avoid

❌ **WRONG**: Single-line tag annotation
```bash
git tag -a v0.1.0 -m "Release v0.1.0: Speed tracking"
```

✅ **CORRECT**: Comprehensive tag annotation (50+ lines)
```bash
git tag -a v0.1.0 -m "$(cat <<'EOF'
[Full template with all sections]
EOF
)"
```

---

❌ **WRONG**: Minimal GitHub release description
```bash
gh release create v0.1.0 --notes "Speed tracking feature"
```

✅ **CORRECT**: Comprehensive markdown release (10 sections)
```bash
gh release create v0.1.0 --notes "$(cat <<'EOF'
[Full template with emoji sections]
EOF
)"
```

---

## Template Variables Reference

| Variable | Example | Source |
|----------|---------|--------|
| `vX.Y.Z` | `v0.1.0` | RELEASE.md version |
| `<Feature Name>` | `Real-Time Speed Tracking` | spec.md title |
| `<Release Type>` | `First MVP` | RELEASE.md section |
| `<Number>` | `3` | Count user stories in spec.md |
| `<Priority>` | `P1`, `P2`, `P3` | spec.md user story priorities |
| `<count>` | `35`, `6,173` | `git diff --stat origin/main...HEAD` |
| `<size>` | `22` | `ls -lh app/build/outputs/apk/release/app-release.apk` |
| `<PR URL>` | `https://github.com/<org>/<repo>/pull/1` | GitHub PR |
| `<spec URL>` | `specs/###-feature/spec.md` | Spec file path |

---

## Automation Consideration

For future automation, this workflow could be scripted as:

```bash
#!/bin/bash
# .specify/scripts/create-release.sh

VERSION="$1"
FEATURE_DIR="specs/$(git branch --show-current)"

# Extract values from RELEASE.md and spec files
# Generate tag annotation from template
# Generate GitHub release notes from template
# Create tag and release
```

This template ensures consistency across all releases.
