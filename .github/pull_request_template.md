# Pull Request: [Feature Name]

## 📋 Feature Summary

[Provide a brief description of what this PR implements]

## 🔗 Related Specification

Link to specification: `.specify/specs/###-feature-name/spec.md`

## 🏗️ Implementation Details

### Key Changes
- [Describe major implementation decisions]
- [List significant architectural changes]
- [Note any new dependencies added]

### Files Modified/Added
- [List key files changed or created]

## ✅ Testing

### Test Coverage
- [ ] All unit tests passing (`./gradlew test`)
- [ ] All integration tests passing (if applicable)
- [ ] UI tests passing (if applicable)
- [ ] Test coverage meets requirements (80%+ for ViewModels/UseCases/Repositories)

### Emulator Testing Completed
- [ ] App installs successfully on emulator
- [ ] Feature UI renders correctly
- [ ] Feature functionality works as expected
- [ ] No runtime crashes or ANR events
- [ ] Location permissions work correctly (if applicable)
- [ ] Dark mode displays correctly (if UI changes)
- [ ] Rotation handling works (if applicable)
- [ ] Back navigation behaves correctly

**Emulator Testing Notes**:
[Describe manual testing performed - scenarios tested, edge cases verified, etc.]

**Screenshots** (if UI changes):
[Add screenshots or screen recordings if this PR includes visual changes]

## 💥 Breaking Changes

- [ ] No breaking changes
- [ ] This PR includes breaking changes (details below)

**If breaking changes exist, describe them**:
[Explain what breaks and provide migration guidance]

## 📝 Constitution Compliance Checklist

Before submitting this PR, verify ALL of the following:

### Code Quality
- [ ] Follows Kotlin coding conventions per CLAUDE.md
- [ ] Uses Jetpack Compose (no new XML layouts)
- [ ] Implements MVVM architecture correctly
- [ ] ViewModels don't hold Context references
- [ ] State is hoisted appropriately in composables
- [ ] Composables are stateless where possible
- [ ] Material 3 theming used consistently
- [ ] Dark mode works correctly
- [ ] Accessibility features implemented (content descriptions, touch targets, contrast)

### Testing & Quality
- [ ] No memory leaks (verified with Android Profiler)
- [ ] Tests are written and passing (if feature requires tests per spec)
- [ ] All lint warnings addressed
- [ ] No new dependencies without justification
- [ ] Debug build tested on emulator (MANDATORY)

### Documentation
- [ ] **Commits are small and frequent** (reviewed git history)
- [ ] **TODO.md updated** with current feature status (MANDATORY)
- [ ] **RELEASE.md updated** with feature entry in Unreleased section (MANDATORY)

### Architecture Compliance
- [ ] Clean Architecture layers respected (UI → ViewModel → Domain → Data)
- [ ] Dependencies point inward (no domain/data depending on UI)
- [ ] Unidirectional data flow maintained
- [ ] Offline-first design for critical features

### BikeRedlights-Specific (if applicable)
- [ ] Battery-efficient location tracking
- [ ] Handles GPS signal loss gracefully
- [ ] Works offline (critical features don't depend on network)
- [ ] Privacy: minimal location data collection, local processing
- [ ] Tested at various speeds (if speed-related feature)

## 🔍 Reviewer Checklist

For reviewers to verify:

- [ ] Constitution compliance verified
- [ ] Code follows established patterns
- [ ] Tests are meaningful and comprehensive
- [ ] Documentation is clear and complete
- [ ] No obvious security issues
- [ ] Performance considerations addressed

## 📦 Release Preparation (if ending specify session)

- [ ] RELEASE.md moved from "Unreleased" to version section
- [ ] app/build.gradle.kts version codes updated
- [ ] Git tag prepared with release notes
- [ ] Signed APK build ready

## 🤔 Questions/Concerns

[List any questions or concerns for reviewers, or areas where you'd like specific feedback]

## 📸 Additional Context

[Add any other context, diagrams, or information that would help reviewers understand this PR]

---

**Constitution Reference**: This PR template ensures compliance with Constitution v1.3.0 requirements (Development Workflow section).
