# Feature Specification: Stop Clustering

**Feature Branch**: `010-stop-clustering`
**Created**: 2025-12-29
**Status**: Draft
**Input**: User description: "Feature 010: Stop Clustering - Group nearby stops across multiple rides into clusters using geospatial proximity (DBSCAN algorithm). Consumes clustering radius from Feature 008 settings (default: 20m). Updates cluster_id field in stops table. Enables analytics like 'you stopped at this intersection 15 times this month'."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Automatic Stop Clustering (Priority: P1)

As a cyclist, when I complete a ride with stops, the system automatically groups nearby stops from all my rides into clusters representing common locations (intersections, traffic lights, etc.), so I can see patterns in my cycling routes without manual effort.

**Why this priority**: Core feature that provides the foundation for all clustering analytics. Without automatic clustering, the feature cannot deliver any value. This represents the MVP - users get immediate value from seeing their stops organized geographically.

**Independent Test**: Can be fully tested by recording 2-3 rides with stops at the same intersection (within 20m radius), then verifying that the stops table has matching cluster_id values for nearby stops. Delivers value by enabling "you stopped here 3 times" insights.

**Acceptance Scenarios**:

1. **Given** I have completed 3 rides with stops at the same intersection (GPS coordinates within 20m), **When** I view my stop history, **Then** all 3 stops are assigned the same cluster_id
2. **Given** I have stops at two different intersections (>20m apart), **When** clustering runs, **Then** each intersection gets a unique cluster_id
3. **Given** I have a single isolated stop with no nearby stops, **When** clustering runs, **Then** that stop is assigned its own unique cluster_id
4. **Given** clustering radius is changed from 20m to 30m in settings, **When** I trigger re-clustering, **Then** stops within 30m are now grouped into the same cluster
5. **Given** I have 5 stops clustered together, **When** I delete 1 ride containing 2 of those stops, **Then** the remaining 3 stops retain the same cluster_id

---

### User Story 2 - Cluster Analytics (Priority: P2)

As a cyclist reviewing my ride history, I can see aggregated statistics for each cluster (number of stops, average duration, total time spent), so I understand which intersections cause the most delays on my routes.

**Why this priority**: Builds on P1 to provide actionable insights. Users can identify problematic intersections and adjust their routes. This is the primary value proposition mentioned in the feature description ("you stopped at this intersection 15 times this month").

**Independent Test**: Can be tested by creating 3 rides with 5 stops at the same cluster, then verifying that cluster shows "5 stops, avg duration 30s, total 2.5 min". Delivers value by highlighting delay patterns.

**Acceptance Scenarios**:

1. **Given** I have a cluster with 10 stops averaging 25 seconds each, **When** I view cluster analytics, **Then** I see "10 stops, avg duration 25s, total time 4m 10s"
2. **Given** I have 5 clusters with different stop counts, **When** I view the cluster list sorted by frequency, **Then** clusters are ranked from most stops to fewest stops
3. **Given** I have a cluster with stops from the past 30 days, **When** I filter by "this month", **Then** I see only stops from the current calendar month
4. **Given** a cluster has stops with durations ranging from 10s to 120s, **When** I view cluster details, **Then** I see min/max/average duration statistics

---

### User Story 3 - Manual Cluster Management (Priority: P3)

As a power user, I can manually split a cluster into two separate clusters or merge two clusters together, so I can correct clustering errors caused by GPS inaccuracy or customize clustering to match my mental model of intersections.

**Why this priority**: Nice-to-have feature for advanced users who want finer control. Most users will be satisfied with automatic clustering (P1) and analytics (P2). This addresses edge cases where GPS drift causes incorrect clustering.

**Independent Test**: Can be tested by creating a cluster with 6 stops, manually splitting it into two 3-stop clusters, and verifying both clusters persist correctly. Delivers value by giving users control over their data.

**Acceptance Scenarios**:

1. **Given** I have a cluster with 6 stops, **When** I split it into two clusters (selecting 3 stops for each), **Then** I see two separate clusters with their own analytics
2. **Given** I have two clusters that represent the same intersection, **When** I merge them, **Then** all stops from both clusters are assigned the same new cluster_id
3. **Given** I manually split a cluster, **When** automatic clustering runs again, **Then** my manual split is preserved (not overwritten)

---

### Edge Cases

- What happens when GPS accuracy is poor (±50m drift) and stops appear to cluster incorrectly?
  - System should respect the configured clustering radius. If radius is 20m and GPS drift causes 50m separation, stops won't cluster (correct behavior to avoid false groupings).

- How does the system handle deleting a ride that contains stops in a cluster?
  - Stops are deleted via CASCADE delete (existing Feature 009 database schema). Cluster_id assignments for remaining stops are preserved. If a cluster drops to 0 stops, the cluster_id simply becomes unused (no cleanup required).

- What happens when a user has 1000+ rides with 10,000+ stops?
  - Full re-clustering may take several seconds. System should run clustering asynchronously in background (WorkManager) and show progress indicator. Incremental clustering after each ride should remain fast (<1 second).

- How does clustering behave when radius changes from 20m to 50m and back to 20m?
  - Re-clustering is triggered on radius change. When radius increases to 50m, more stops cluster together (cluster_id values decrease as clusters merge). When radius decreases back to 20m, clusters split apart (new cluster_id values assigned). Previous cluster_id values are not preserved.

- What happens when two stops are exactly at the clustering radius boundary (e.g., 20.0m apart with 20m radius)?
  - DBSCAN epsilon parameter is inclusive (≤ radius), so stops at exactly 20.0m distance will cluster together. This is consistent with user expectations (20m radius means "within 20m").

- How does clustering handle stops at ride start/end destinations vs traffic stops?
  - Clustering is location-based only, agnostic to stop type. Destination stops (deleted by Feature 009 when ride ends) won't be in database, so only traffic stops are clustered. User can manually split clusters if home/work destinations coincide with traffic intersections.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST implement DBSCAN (Density-Based Spatial Clustering of Applications with Noise) algorithm for geospatial clustering of stops
- **FR-002**: System MUST use clustering radius from Feature 008 settings (Stop Detection Settings), with default value of 20 meters
- **FR-003**: System MUST support clustering radius range of 10-50 meters (matching Feature 008 validation)
- **FR-004**: System MUST update cluster_id field in existing stops table (Feature 009 schema) when clustering runs
- **FR-005**: System MUST run incremental clustering automatically after each ride completes (adds new stops to existing clusters or creates new clusters)
- **FR-006**: System MUST provide manual re-clustering capability that processes all stops across all rides
- **FR-007**: System MUST trigger full re-clustering when clustering radius setting changes
- **FR-008**: System MUST calculate cluster analytics including: total stop count, average duration, total time spent
- **FR-009**: System MUST rank clusters by frequency (number of stops) for analytics display
- **FR-010**: System MUST allow filtering cluster analytics by date range (this week, this month, all time)
- **FR-011**: System MUST persist cluster_id assignments across app restarts and updates
- **FR-012**: System MUST handle cluster_id assignment for single isolated stops (assign unique cluster_id to each)
- **FR-013**: System MUST preserve cluster_id values when stops are deleted (remaining stops keep their cluster assignments)
- **FR-014**: System MUST run clustering operations asynchronously in background thread to avoid blocking UI
- **FR-015**: System MUST use Haversine formula for calculating geographic distance between stop coordinates
- **FR-016**: System MUST assign minimum 3 stops per cluster to qualify as a "cluster" vs isolated stops (standard DBSCAN minPts parameter)
- **FR-017**: System MUST allow manual splitting of a cluster into two or more separate clusters
- **FR-018**: System MUST allow manual merging of two or more clusters into a single cluster
- **FR-019**: System MUST prevent automatic re-clustering from overwriting manual cluster split/merge operations
- **FR-020**: System MUST display cluster location as the geographic centroid (average latitude/longitude) of all stops in the cluster

### Key Entities

- **Cluster**: Represents a geographic grouping of stops from multiple rides. Conceptually identified by cluster_id (integer), with computed attributes: centroid coordinates (latitude/longitude average), stop count, average duration, total time spent. Not a separate database table - cluster_id exists as foreign key in stops table.

- **Stop**: Existing entity from Feature 009. Enhanced with cluster_id field (nullable integer). Attributes: stop location (latitude/longitude), start/end timestamps, duration, ride_id (foreign key), stop_number, cluster_id (new, nullable foreign key to identify cluster membership).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Riders can identify their most frequently stopped intersections within 3 taps from ride history screen
- **SC-002**: Clustering algorithm processes 100 stops in under 2 seconds on mid-range Android device (2020+ hardware)
- **SC-003**: Clustering accuracy is within 95% for stops at the same physical intersection (GPS accuracy permitting)
- **SC-004**: Full re-clustering of 1000 stops completes in under 10 seconds and does not block UI
- **SC-005**: Cluster analytics show actionable insights: "You stopped at this intersection 15 times this month, average delay 30 seconds"
- **SC-006**: Changing clustering radius from 20m to 30m and re-clustering completes in under 5 seconds for 500 stops
- **SC-007**: Manual cluster split/merge operations persist correctly and are not overwritten by automatic clustering
- **SC-008**: Riders can filter cluster analytics by time period and see accurate stop counts for "this week", "this month", and "all time"

## Assumptions *(optional)*

- GPS accuracy is typically ±5-10m under good conditions, which is sufficient for 20m clustering radius to correctly group stops at the same intersection
- Riders typically have 10-50 stops per ride, with 1-10 rides per week, resulting in hundreds to thousands of stops over several months of usage
- Most common use case is identifying frequent intersections on commute routes (home-work-home)
- DBSCAN is preferred over k-means or hierarchical clustering because:
  - Does not require pre-specifying number of clusters (k-means limitation)
  - Handles noise (isolated stops) naturally by marking them as outliers
  - Works well with geospatial data and arbitrary cluster shapes (e.g., stops along a corridor)
- Incremental clustering after each ride is preferred over batch clustering for better UX (immediate feedback)
- Manual cluster management (P3) is low priority because automatic clustering should handle 90%+ of use cases correctly

## Dependencies *(optional)*

- **Feature 009 (Stop Detection & Recording)**: Provides stops table with latitude/longitude coordinates and ride_id foreign key. Cluster_id field will be added to existing schema.
- **Feature 008 (Stop Detection Settings)**: Provides clustering radius setting (10-50m, default 20m) consumed by clustering algorithm.
- **Room Database**: Existing persistence layer will be extended with clustering queries and cluster_id updates.

## Out of Scope *(optional)*

- **Route optimization suggestions** (e.g., "take this alternate route to avoid 3 clustered stops") - deferred to future feature
- **Real-time cluster detection during active ride** - clustering runs after ride completes, not live
- **Cluster visualization on map** - deferred to future UI enhancement (current feature focuses on analytics)
- **Exporting cluster data to external formats** (CSV, JSON) - deferred to future data export feature
- **Cluster naming/labeling** (e.g., "Main St & 5th Ave") - deferred to future UX enhancement; clusters identified by centroid coordinates only
- **Social features** (sharing clusters with other riders) - out of scope for MVP
- **Predictive analytics** (e.g., "you'll likely stop here based on time of day") - requires ML, out of scope
