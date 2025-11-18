# BikeRedlights Neo4j Knowledge Graph

> **Purpose**: Comprehensive knowledge graph for future developers to quickly understand the BikeRedlights Android codebase architecture, patterns, and development workflows.

## 📊 Overview

This Neo4j knowledge graph contains **100+ nodes** representing the complete BikeRedlights project structure, including:

- **Project Structure**: Screens, Components, ViewModels, Repositories, Services
- **Architecture Layers**: UI, Domain, Data, Service, DI
- **Features**: All 7 released features (v0.1.0 - v0.7.0)
- **Technology Stack**: 10 core technologies with versions
- **Design Decisions**: 7 key architectural and UX decisions
- **Common Issues**: 4 documented bugs and their solutions
- **Quick References**: 5 development workflow guides
- **Helpful Queries**: 10 pre-built Cypher queries

## 🚀 Quick Start

### Prerequisites
- Neo4j instance running (local or cloud)
- Neo4j MCP server configured in Claude Code

### Accessing the Knowledge Graph

The knowledge graph is already populated and ready to use! Simply run any of the example queries below.

## 📋 Available Node Types

| Node Type | Count | Description |
|-----------|-------|-------------|
| **Technology** | 10 | Tech stack (Kotlin, Compose, Room, Hilt, etc.) |
| **UseCase** | 7 | Domain layer use cases |
| **Feature** | 7 | Released features (v0.1.0 - v0.7.0) |
| **DesignDecision** | 7 | Key architectural decisions |
| **Screen** | 6 | Jetpack Compose screens |
| **Component** | 6 | Reusable UI components |
| **DomainModel** | 6 | Domain layer models |
| **Layer** | 5 | Architecture layers |
| **ViewModel** | 5 | MVVM ViewModels |
| **Repository** | 5 | Data repositories |
| **Pattern** | 5 | Architectural patterns (MVVM, Clean Architecture) |
| **QuickReference** | 5 | Development workflow guides |
| **Documentation** | 4 | Project documentation files |
| **CommonIssue** | 4 | Known bugs and solutions |
| **TestRequirement** | 3 | Testing requirements |
| **NavigationDestination** | 3 | Bottom navigation tabs |
| **DatabaseEntity** | 2 | Room entities |
| **DAO** | 2 | Room DAOs |
| **QueryExample** | 10 | Pre-built helpful queries |
| **Project** | 1 | Root project node |
| **Database** | 1 | Room database |
| **Service** | 1 | Background service |
| **TestingStrategy** | 1 | Testing approach |
| **Navigation** | 1 | Navigation structure |

## 🔍 Essential Queries

### 1. View All Screens and Their ViewModels
```cypher
MATCH (s:Screen {project: 'BikeRedlights'})-[:USES_VIEWMODEL]->(vm:ViewModel)
RETURN s.name as Screen, vm.name as ViewModel, s.description as ScreenDescription
ORDER BY s.name
```

**Use Case**: Understand which ViewModel powers which screen.

---

### 2. Trace Data Flow (Screen → ViewModel → UseCase → Repository)
```cypher
MATCH path = (s:Screen)-[:USES_VIEWMODEL]->(vm:ViewModel)-[:CALLS_USECASE]->(uc:UseCase)-[:USES_REPOSITORY]->(r:Repository)
WHERE s.project = 'BikeRedlights'
RETURN path
```

**Use Case**: Visualize complete data flow for a feature.

---

### 3. View Architecture Layers and Dependencies
```cypher
MATCH (p:Project {name: 'BikeRedlights'})-[:HAS_LAYER]->(l:Layer)
OPTIONAL MATCH (l)-[:DEPENDS_ON]->(dep:Layer)
RETURN l.name as Layer, l.technology as Technology, collect(dep.name) as DependsOn
ORDER BY l.name
```

**Use Case**: Understand Clean Architecture layer structure.

---

### 4. Find All Components Used in LiveRideScreen
```cypher
MATCH (s:Screen {name: 'LiveRideScreen', project: 'BikeRedlights'})-[:RENDERS_COMPONENT*1..2]->(c:Component)
RETURN s.name, c.name, c.description
```

**Use Case**: See which components compose a specific screen.

---

### 5. List All Features by Version
```cypher
MATCH (f:Feature {project: 'BikeRedlights'})
RETURN f.version as Version, f.name as Feature, f.description as Description, f.releaseDate as Released
ORDER BY f.version
```

**Use Case**: Get project feature history timeline.

---

### 6. Get Technology Stack by Category
```cypher
MATCH (t:Technology {project: 'BikeRedlights'})
RETURN t.category as Category, collect({name: t.name, version: t.version}) as Technologies
ORDER BY t.category
```

**Use Case**: View complete tech stack with versions.

---

### 7. Find Common Issues and Their Solutions
```cypher
MATCH (i:CommonIssue {project: 'BikeRedlights'})
RETURN i.name as Issue, i.severity as Severity, i.description as Description,
       i.solution as Solution, i.fixedIn as FixedIn
ORDER BY i.severity
```

**Use Case**: Learn from past bugs and their fixes.

---

### 8. Get Quick Reference for Development Tasks
```cypher
MATCH (r:QuickReference {project: 'BikeRedlights'})
WHERE r.category = 'Development'
RETURN r.name as Task, r.steps as Steps
```

**Use Case**: Get step-by-step guides for common tasks.

---

### 9. View Design Decisions by Category
```cypher
MATCH (d:DesignDecision {project: 'BikeRedlights'})
RETURN d.category as Category, d.name as Decision, d.rationale as Rationale, d.impact as Impact
ORDER BY d.category, d.name
```

**Use Case**: Understand "why" behind architectural choices.

---

### 10. Find Service Dependencies
```cypher
MATCH (s:Service {name: 'RideRecordingService', project: 'BikeRedlights'})-[:USES_REPOSITORY]->(r:Repository)
RETURN s.name as Service, collect(r.name) as Repositories, s.responsibilities as Responsibilities
```

**Use Case**: See which repositories the background service uses.

---

## 🎯 Common Developer Questions

### "How do I add a new screen?"

```cypher
MATCH (r:QuickReference {name: 'How to add a new screen', project: 'BikeRedlights'})
RETURN r.steps as Steps
```

**Returns**:
1. Create Screen composable in ui/screens/
2. Create ViewModel in ui/viewmodel/
3. Add navigation route in AppNavigation.kt
4. Wire ViewModel to Screen with hiltViewModel()
5. Add to bottom nav if top-level destination

---

### "What's the data flow for ride recording?"

```cypher
MATCH path = (s:Screen {name: 'LiveRideScreen'})-[:USES_VIEWMODEL]->(vm:ViewModel)
      -[:CALLS_USECASE]->(uc:UseCase)-[:USES_REPOSITORY]->(r:Repository)
WHERE s.project = 'BikeRedlights'
RETURN path
```

**Visualizes**: LiveRideScreen → RideRecordingViewModel → StartRideUseCase → RideRepository

---

### "Which technologies are used for UI?"

```cypher
MATCH (t:Technology {project: 'BikeRedlights'})
WHERE t.category IN ['UI Framework', 'Design System']
RETURN t.name, t.version, t.description
```

**Returns**: Jetpack Compose (BOM 2024.11.00), Material 3

---

### "What are the testing requirements?"

```cypher
MATCH (tr:TestRequirement {project: 'BikeRedlights'})
RETURN tr.name as Requirement, tr.type as Type, tr.priority as Priority,
       tr.description as Description, tr.tools as Tools
ORDER BY tr.priority
```

**Returns**: Emulator Testing (MANDATORY), Unit Testing (Required), UI Testing (Recommended)

---

### "Why is speed the hero metric?"

```cypher
MATCH (d:DesignDecision {name: 'Speed as Hero Metric', project: 'BikeRedlights'})
RETURN d.rationale as Rationale, d.impact as Impact
```

**Returns**: "Safety app - speed awareness is primary goal, not fitness tracking" → Speed displayed at 57sp

---

## 🏗️ Architecture Exploration

### View Complete Layer Structure
```cypher
MATCH (p:Project {name: 'BikeRedlights'})-[:HAS_LAYER]->(l:Layer)
RETURN l.name, l.description, l.technology
ORDER BY l.name
```

### Find All Patterns Used
```cypher
MATCH (p:Project {name: 'BikeRedlights'})-[:IMPLEMENTS_PATTERN]->(pattern:Pattern)
RETURN pattern.name, pattern.description, pattern.benefits
```

### Get Navigation Structure
```cypher
MATCH (nav:Navigation {project: 'BikeRedlights'})-[:HAS_DESTINATION]->(dest:NavigationDestination)
RETURN nav.name, collect({tab: dest.name, route: dest.route, icon: dest.icon}) as Tabs
```

---

## 🐛 Troubleshooting

### Find Critical Bugs Fixed
```cypher
MATCH (i:CommonIssue {project: 'BikeRedlights'})
WHERE i.severity STARTS WITH 'P0'
RETURN i.name, i.description, i.solution, i.fixedIn
```

### Get All Testing Strategies
```cypher
MATCH (ts:TestingStrategy {project: 'BikeRedlights'})-[:INCLUDES_REQUIREMENT]->(tr:TestRequirement)
RETURN ts.name, ts.unitTestCoverage, collect({req: tr.name, priority: tr.priority}) as Requirements
```

---

## 📚 Documentation Links

### Get All Documentation Files
```cypher
MATCH (d:Documentation {project: 'BikeRedlights'})
RETURN d.name, d.type, d.path, d.description
ORDER BY d.type
```

**Returns**:
- CLAUDE.md (Development Standards)
- Constitution (Project Governance)
- TODO.md (Progress Tracking)
- RELEASE.md (Version History)

---

## 🔄 Feature Evolution

### See Feature Timeline
```cypher
MATCH (f:Feature {project: 'BikeRedlights'})
RETURN f.version, f.name, f.releaseDate, f.status
ORDER BY f.releaseDate
```

### Find Latest Feature
```cypher
MATCH (f:Feature {project: 'BikeRedlights'})
RETURN f.version, f.name, f.description
ORDER BY f.releaseDate DESC
LIMIT 1
```

---

## 🛠️ Development Workflows

### Get All Quick References
```cypher
MATCH (r:QuickReference {project: 'BikeRedlights'})
RETURN r.name, r.category, r.steps
ORDER BY r.category, r.name
```

**Categories**: Development, Testing, Release

---

## 🎨 UI Component Tree

### Find All Reusable Components
```cypher
MATCH (c:Component {project: 'BikeRedlights'})
RETURN c.name, c.type, c.description, c.path
ORDER BY c.type, c.name
```

### Map Component Dependencies
```cypher
MATCH (parent:Component)-[:RENDERS_COMPONENT]->(child:Component)
WHERE parent.project = 'BikeRedlights'
RETURN parent.name as Parent, collect(child.name) as ChildComponents
```

---

## 💾 Database Schema

### Get Room Database Structure
```cypher
MATCH (db:Database {project: 'BikeRedlights'})-[:CONTAINS_TABLE]->(entity:DatabaseEntity)
RETURN db.name, db.version, collect({table: entity.tableName, columns: entity.columns}) as Tables
```

### Find All DAOs and Their Queries
```cypher
MATCH (dao:DAO {project: 'BikeRedlights'})-[:QUERIES]->(entity:DatabaseEntity)
RETURN dao.name, dao.queries, entity.tableName
```

---

## 🔐 Repository Pattern

### List All Repositories and Their Operations
```cypher
MATCH (r:Repository {project: 'BikeRedlights'})
RETURN r.name, r.description, r.operations
ORDER BY r.name
```

---

## 📊 Project Statistics

### Count Nodes by Type
```cypher
MATCH (n {project: 'BikeRedlights'})
RETURN labels(n)[0] as NodeType, count(*) as Count
ORDER BY Count DESC
```

### Get Project Overview
```cypher
MATCH (p:Project {name: 'BikeRedlights'})
RETURN p.description, p.platform, p.language, p.architecture,
       p.currentVersion, p.kotlinVersion, p.repository
```

---

## 🎓 Learning Path for New Developers

### Step 1: Understand Architecture
```cypher
MATCH (p:Project {name: 'BikeRedlights'})-[:IMPLEMENTS_PATTERN]->(pattern:Pattern)
RETURN pattern.name, pattern.description, pattern.benefits
```

### Step 2: Explore Layers
```cypher
MATCH (l:Layer {project: 'BikeRedlights'})
OPTIONAL MATCH (l)-[:DEPENDS_ON]->(dep:Layer)
RETURN l.name, l.description, collect(dep.name) as Dependencies
```

### Step 3: Study Feature History
```cypher
MATCH (f:Feature {project: 'BikeRedlights'})
RETURN f.version, f.name, f.description, f.releaseDate
ORDER BY f.releaseDate
```

### Step 4: Review Design Decisions
```cypher
MATCH (d:DesignDecision {project: 'BikeRedlights'})
RETURN d.name, d.category, d.rationale, d.impact
ORDER BY d.category
```

### Step 5: Learn from Past Issues
```cypher
MATCH (i:CommonIssue {project: 'BikeRedlights'})
RETURN i.name, i.severity, i.description, i.solution, i.fixedIn
ORDER BY i.severity
```

---

## 🚦 Safety-Critical Considerations

### Find Safety-Related Design Decisions
```cypher
MATCH (d:DesignDecision {project: 'BikeRedlights'})
WHERE d.name CONTAINS 'Safety' OR d.rationale CONTAINS 'safety'
RETURN d.name, d.rationale, d.impact
```

### Get Testing Requirements for Safety
```cypher
MATCH (tr:TestRequirement {project: 'BikeRedlights'})
WHERE tr.priority = 'MANDATORY'
RETURN tr.name, tr.description, tr.requirements
```

---

## 🔄 Data Flow Visualization

### Complete Flow: User Action → Database
```cypher
MATCH path = (s:Screen)-[:USES_VIEWMODEL]->(vm:ViewModel)
             -[:CALLS_USECASE]->(uc:UseCase)
             -[:USES_REPOSITORY]->(r:Repository)
WHERE s.name = 'LiveRideScreen' AND s.project = 'BikeRedlights'
RETURN path
```

### Service to Repository Flow
```cypher
MATCH path = (svc:Service {name: 'RideRecordingService'})
             -[:USES_REPOSITORY]->(r:Repository)
WHERE svc.project = 'BikeRedlights'
RETURN path
```

---

## 📖 Best Practices

### Get All Pre-Built Query Examples
```cypher
MATCH (q:QueryExample {project: 'BikeRedlights'})
RETURN q.name, q.category, q.query
ORDER BY q.category, q.name
```

**Returns**: 10 helpful query examples for common development tasks.

---

## 🤝 Contributing to the Knowledge Graph

If you discover new architectural patterns, add new features, or fix bugs, consider updating the knowledge graph:

### Add a New Feature
```cypher
MATCH (project:Project {name: 'BikeRedlights'})
CREATE (f:Feature {
  name: 'Feature 008: Your Feature Name',
  project: 'BikeRedlights',
  version: 'v0.8.0',
  status: 'In Progress',
  description: 'Your feature description',
  releaseDate: '2025-XX-XX'
})
CREATE (project)-[:HAS_FEATURE]->(f)
```

### Add a New Design Decision
```cypher
MATCH (project:Project {name: 'BikeRedlights'})
CREATE (d:DesignDecision {
  name: 'Your Decision Name',
  project: 'BikeRedlights',
  category: 'Architecture/Performance/UX',
  rationale: 'Why you made this decision',
  impact: 'What impact it has on the codebase'
})
CREATE (project)-[:HAS_DESIGN_DECISION]->(d)
```

### Document a Common Issue
```cypher
MATCH (project:Project {name: 'BikeRedlights'})
CREATE (i:CommonIssue {
  name: 'Issue Name',
  project: 'BikeRedlights',
  category: 'Bug',
  severity: 'P0/P1/P2',
  description: 'Issue description',
  solution: 'How it was fixed',
  fixedIn: 'vX.Y.Z'
})
CREATE (project)-[:HAS_KNOWN_ISSUE]->(i)
```

---

## 🔧 Maintenance

The knowledge graph should be updated whenever:
- New features are added (update Feature nodes)
- Architecture changes (update Layer/Pattern nodes)
- Bugs are fixed (add to CommonIssue nodes)
- New technologies are adopted (add Technology nodes)
- Design decisions are made (add DesignDecision nodes)

---

## 📝 Schema Summary

### Key Relationships
- `HAS_LAYER`: Project → Layer
- `HAS_FEATURE`: Project → Feature
- `USES_TECHNOLOGY`: Project → Technology
- `IMPLEMENTS_PATTERN`: Project → Pattern
- `USES_VIEWMODEL`: Screen → ViewModel
- `CALLS_USECASE`: ViewModel → UseCase
- `USES_REPOSITORY`: UseCase → Repository
- `RENDERS_COMPONENT`: Screen/Component → Component
- `DEPENDS_ON`: Layer → Layer
- `COMMUNICATES_WITH`: ViewModel → Service

---

## 🎯 Key Insights from the Knowledge Graph

### 1. **Clean Architecture is Strictly Enforced**
- UI Layer depends on Domain Layer
- Domain Layer depends on Data Layer
- No reverse dependencies (Dependency Inversion)

### 2. **MVVM Pattern Throughout**
- Every Screen has a ViewModel
- ViewModels call UseCases (not Repositories directly)
- Unidirectional Data Flow (StateFlow down, events up)

### 3. **Safety-First Design Philosophy**
- Offline-first architecture (no cloud dependencies)
- Battery efficiency considerations (configurable GPS accuracy)
- Speed as hero metric (safety awareness over fitness tracking)

### 4. **Comprehensive Testing Required**
- Emulator testing is MANDATORY for every feature
- 80%+ unit test coverage target
- Physical device testing for GPS-dependent features

### 5. **Incremental Development Workflow**
- Small, frequent commits (~200 LOC max)
- Conventional commit messages
- PR workflow with code review
- Semantic versioning for releases

---

## 🌟 Conclusion

This Neo4j knowledge graph provides a **comprehensive, queryable representation** of the BikeRedlights Android codebase. Use it to:

- **Onboard new developers** quickly
- **Understand data flows** and architecture
- **Learn from past bugs** and their solutions
- **Reference development workflows** and best practices
- **Explore feature history** and evolution

The graph is living documentation - keep it updated as the project evolves!

---

**Last Updated**: 2025-11-18
**Graph Version**: 1.0
**Total Nodes**: 100+
**Total Relationships**: 100+
**Coverage**: Complete (all layers, features v0.1.0-v0.7.0)
