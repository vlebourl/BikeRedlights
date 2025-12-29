# Quickstart Guide: Stop Clustering

**Feature**: Stop Clustering (Feature 010)
**Target Audience**: Developers implementing or testing clustering functionality
**Time to Complete**: 30-45 minutes

## Prerequisites

- ✅ Android Studio (2023.3+)
- ✅ JDK 17 (OpenJDK recommended)
- ✅ Android SDK 34+ (API 34)
- ✅ BikeRedlights project cloned and buildable
- ✅ Feature 009 (Stop Detection) merged to main
- ✅ Android emulator or physical device with GPS

## Development Setup

### 1. Checkout Feature Branch

```bash
git checkout 010-stop-clustering

# Verify database is at version 2 (stops table exists)
# Check: app/src/main/java/com/example/bikeredlights/data/local/BikeRedlightsDatabase.kt
# Should see: version = 2
```

### 2. Understand Existing Schema

The `stops` table already has `cluster_id` column (prepared in Feature 009):

```sql
CREATE TABLE stops (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    ride_id INTEGER NOT NULL,
    stop_number INTEGER NOT NULL,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    start_timestamp INTEGER NOT NULL,
    end_timestamp INTEGER,
    duration_seconds INTEGER,
    cluster_id INTEGER,  -- <-- Ready for clustering!
    FOREIGN KEY (ride_id) REFERENCES rides(id) ON DELETE CASCADE
);
```

**No database migration needed for Feature 010!**

### 3. Review Key Contracts

Read these files to understand the API contracts:

1. **specs/010-stop-clustering/contracts/DBSCANAlgorithm.kt**
   - DBSCAN clustering algorithm contract
   - Input: point count, epsilon, minPts, distance function
   - Output: cluster assignments

2. **specs/010-stop-clustering/contracts/HaversineDistance.kt**
   - Geographic distance calculation
   - Input: (lat1, lon1), (lat2, lon2)
   - Output: distance in meters

3. **specs/010-stop-clustering/contracts/ClusterStopsUseCase.kt**
   - Main clustering orchestration
   - Fetches stops → runs DBSCAN → persists assignments

4. **specs/010-stop-clustering/contracts/StopRepository.kt**
   - Data access methods
   - getAllStops(), updateClusterAssignments(), getClusterStats()

---

## Implementation Order (TDD Workflow)

Follow this order for Test-Driven Development:

### Phase 1: Pure Functions (No Android Dependencies)

#### Step 1: Haversine Distance (30 min)

**Test First** (`app/src/test/.../domain/util/HaversineDistanceTest.kt`):
```kotlin
@Test
fun `same point returns zero distance`() {
    val distance = haversineDistance(37.422, -122.084, 37.422, -122.084)
    assertThat(distance).isWithin(0.01f).of(0f)
}

@Test
fun `Google campus to Moscone Center is approximately 49km`() {
    val distance = haversineDistance(37.422, -122.084, 37.784, -122.401)
    assertThat(distance).isWithin(500f).of(49000f)  // ±500m tolerance
}

@Test
fun `two stops within 20m are neighbors`() {
    val distance = haversineDistance(37.422000, -122.084000, 37.422100, -122.084100)
    assertThat(distance).isLessThan(20f)
}
```

**Implementation** (`app/src/main/java/.../domain/util/HaversineDistance.kt`):
```kotlin
fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val R = 6371000.0
    val lat1Rad = Math.toRadians(lat1)
    val lat2Rad = Math.toRadians(lat2)
    val deltaLatRad = Math.toRadians(lat2 - lat1)
    val deltaLonRad = Math.toRadians(lon2 - lon1)

    val a = sin(deltaLatRad / 2).pow(2) +
            cos(lat1Rad) * cos(lat2Rad) * sin(deltaLonRad / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return (R * c).toFloat()
}
```

**Run Tests**: `./gradlew test --tests HaversineDistanceTest`

---

#### Step 2: DBSCAN Algorithm (60 min)

**Test First** (`app/src/test/.../domain/util/DBSCANAlgorithmTest.kt`):
```kotlin
@Test
fun `empty dataset returns empty clusters`() {
    val result = dbscan.cluster(0, epsilon = 20f, minPts = 3, distanceFn = { _, _ -> 0f })
    assertThat(result.clusters).isEmpty()
    assertThat(result.clusterCount).isEqualTo(0)
}

@Test
fun `three points within epsilon form one cluster`() {
    // Points: [0,0], [10,0], [5,5] (all within 20m)
    val distances = mapOf(
        Pair(0, 1) to 10f, Pair(1, 0) to 10f,
        Pair(0, 2) to 7f,  Pair(2, 0) to 7f,
        Pair(1, 2) to 7f,  Pair(2, 1) to 7f,
        Pair(0, 0) to 0f,  Pair(1, 1) to 0f, Pair(2, 2) to 0f
    )
    val distanceFn = { i: Int, j: Int -> distances[Pair(i, j)] ?: 999f }

    val result = dbscan.cluster(3, epsilon = 20f, minPts = 3, distanceFn)

    assertThat(result.clusterCount).isEqualTo(1)
    assertThat(result.clusters[1]).containsExactly(0, 1, 2)
}

@Test
fun `noise point gets singleton cluster`() {
    // Points: [0,0] and [100,100] (far apart, >20m)
    val distanceFn = { i: Int, j: Int ->
        if (i == j) 0f else 100f  // 100m distance
    }

    val result = dbscan.cluster(2, epsilon = 20f, minPts = 3, distanceFn)

    assertThat(result.noiseCount).isEqualTo(2)
    assertThat(result.clusters).hasSize(2)  // Two singleton clusters
}
```

**Implementation** (`app/src/main/java/.../domain/util/DBSCANAlgorithm.kt`):
```kotlin
class DBSCANAlgorithmImpl : DBSCANAlgorithm {
    override fun cluster(
        pointCount: Int,
        epsilon: Float,
        minPts: Int,
        distanceFunction: (Int, Int) -> Float
    ): ClusteringResult {
        require(pointCount >= 0) { "Point count must be non-negative" }
        require(epsilon > 0) { "Epsilon must be positive" }
        require(minPts >= 1) { "MinPts must be at least 1" }

        if (pointCount == 0) {
            return ClusteringResult(emptyMap(), 0, 0)
        }

        val visited = BooleanArray(pointCount)
        val clusterAssignments = IntArray(pointCount) { -1 }  // -1 = unassigned
        var clusterId = 0
        var noiseCount = 0

        for (pointIdx in 0 until pointCount) {
            if (visited[pointIdx]) continue
            visited[pointIdx] = true

            val neighbors = findNeighbors(pointIdx, pointCount, epsilon, distanceFunction)

            if (neighbors.size < minPts) {
                // Noise point - assign to singleton cluster
                clusterAssignments[pointIdx] = ++clusterId
                noiseCount++
            } else {
                // Core point - expand cluster
                clusterId++
                expandCluster(pointIdx, neighbors, clusterId, visited, clusterAssignments,
                             pointCount, epsilon, minPts, distanceFunction)
            }
        }

        // Build result map
        val clusters = clusterAssignments
            .withIndex()
            .groupBy({ it.value }, { it.index })
            .filterKeys { it != -1 }

        return ClusteringResult(clusters, clusterId, noiseCount)
    }

    private fun findNeighbors(
        pointIdx: Int,
        pointCount: Int,
        epsilon: Float,
        distanceFunction: (Int, Int) -> Float
    ): List<Int> {
        return (0 until pointCount).filter { other ->
            distanceFunction(pointIdx, other) <= epsilon
        }
    }

    private fun expandCluster(
        pointIdx: Int,
        neighbors: List<Int>,
        clusterId: Int,
        visited: BooleanArray,
        clusterAssignments: IntArray,
        pointCount: Int,
        epsilon: Float,
        minPts: Int,
        distanceFunction: (Int, Int) -> Float
    ) {
        clusterAssignments[pointIdx] = clusterId
        val queue = ArrayDeque(neighbors)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()

            if (!visited[current]) {
                visited[current] = true
                val currentNeighbors = findNeighbors(current, pointCount, epsilon, distanceFunction)

                if (currentNeighbors.size >= minPts) {
                    queue.addAll(currentNeighbors)
                }
            }

            if (clusterAssignments[current] == -1) {
                clusterAssignments[current] = clusterId
            }
        }
    }
}
```

**Run Tests**: `./gradlew test --tests DBSCANAlgorithmTest`

---

### Phase 2: Use Case (Domain Layer)

#### Step 3: ClusterStopsUseCase (45 min)

**Test First** (`app/src/test/.../domain/usecase/ClusterStopsUseCaseTest.kt`):
```kotlin
@Test
fun `clusterAllStops with 3 stops at same location creates 1 cluster`() = runTest {
    // Mock repository to return 3 stops at same GPS coordinates
    val stops = listOf(
        Stop(id = 1, rideId = 1, stopNumber = 1, latitude = 37.422, longitude = -122.084, ...),
        Stop(id = 2, rideId = 1, stopNumber = 2, latitude = 37.422, longitude = -122.084, ...),
        Stop(id = 3, rideId = 2, stopNumber = 1, latitude = 37.422, longitude = -122.084, ...)
    )
    coEvery { stopRepository.getAllStops() } returns stops
    coEvery { stopRepository.updateClusterAssignments(any()) } just Runs
    every { settingsRepository.getClusteringRadius() } returns flowOf(20f)

    val clusteredCount = useCase.clusterAllStops()

    assertThat(clusteredCount).isEqualTo(3)
    coVerify {
        stopRepository.updateClusterAssignments(
            match { assignments ->
                // All 3 stops should have same cluster_id
                assignments.values.flatten().size == 3 &&
                assignments.keys.size == 1  // Only 1 cluster
            }
        )
    }
}
```

**Implementation** (`app/src/main/java/.../domain/usecase/ClusterStopsUseCase.kt`):
```kotlin
class ClusterStopsUseCaseImpl @Inject constructor(
    private val stopRepository: StopRepository,
    private val settingsRepository: SettingsRepository,
    private val dbscanAlgorithm: DBSCANAlgorithm
) : ClusterStopsUseCase {

    override suspend fun clusterAllStops(): Int {
        val stops = stopRepository.getAllStops()
        if (stops.isEmpty()) return 0

        val epsilon = settingsRepository.getClusteringRadius().first()  // Get current setting
        val minPts = 3  // Standard for 2D clustering

        val distanceFn = { i: Int, j: Int ->
            haversineDistance(
                stops[i].latitude, stops[i].longitude,
                stops[j].latitude, stops[j].longitude
            )
        }

        val result = dbscanAlgorithm.cluster(stops.size, epsilon, minPts, distanceFn)

        // Convert cluster assignments to Map<clusterId, List<stopId>>
        val assignments = result.clusters.mapValues { (_, indices) ->
            indices.map { stops[it].id }
        }

        stopRepository.updateClusterAssignments(assignments)

        return stops.size
    }

    override suspend fun getClusteringConfig(): Pair<Float, Int> {
        val epsilon = settingsRepository.getClusteringRadius().first()
        return Pair(epsilon, 3)  // minPts = 3
    }
}
```

**Run Tests**: `./gradlew test --tests ClusterStopsUseCaseTest`

---

### Phase 3: Data Layer (Repository & DAO)

#### Step 4: Extend StopDao (30 min)

Add new methods to `app/src/main/java/.../data/local/dao/StopDao.kt`:

```kotlin
@Query("SELECT * FROM stops ORDER BY start_timestamp ASC")
suspend fun getAllStops(): List<StopEntity>

@Query("""
    UPDATE stops
    SET cluster_id = :clusterId
    WHERE id IN (:stopIds)
""")
suspend fun updateClusterIds(clusterId: Long, stopIds: List<Long>)

@Query("""
    SELECT
        cluster_id,
        COUNT(*) as stop_count,
        AVG(latitude) as centroid_lat,
        AVG(longitude) as centroid_lon,
        AVG(duration_seconds) as avg_duration,
        SUM(duration_seconds) as total_duration,
        MIN(start_timestamp) as earliest_stop,
        MAX(start_timestamp) as latest_stop
    FROM stops
    WHERE cluster_id = :clusterId
    GROUP BY cluster_id
""")
suspend fun getClusterStats(clusterId: Long): ClusterStatsDto?
```

**Run Build**: `./gradlew assembleDebug` (verify DAO compiles)

---

#### Step 5: Extend StopRepositoryImpl (20 min)

Implement new methods in `app/src/main/java/.../data/repository/StopRepositoryImpl.kt`:

```kotlin
override suspend fun getAllStops(): List<Stop> {
    return stopDao.getAllStops().map { it.toDomain() }
}

@Transaction
override suspend fun updateClusterAssignments(clusterAssignments: Map<Long, List<Long>>) {
    for ((clusterId, stopIds) in clusterAssignments) {
        stopDao.updateClusterIds(clusterId, stopIds)
    }
}

override suspend fun getClusterStats(clusterId: Long): StopCluster? {
    return stopDao.getClusterStats(clusterId)?.toDomain()
}
```

---

## Testing Locally

### Manual Test: Cluster Test Data

#### Step 1: Insert Test Rides with Stops

Use Database Inspector (Android Studio → View → Tool Windows → App Inspection):

```sql
-- Ride 1 with 3 stops at "Main St & 1st Ave"
INSERT INTO rides (...) VALUES (...);  -- id = 1
INSERT INTO stops (ride_id, stop_number, latitude, longitude, start_timestamp, end_timestamp, duration_seconds)
VALUES
    (1, 1, 37.422000, -122.084000, 1703001000000, 1703001030000, 30),
    (1, 2, 37.422050, -122.084050, 1703002000000, 1703002025000, 25),
    (1, 3, 37.422100, -122.084100, 1703003000000, 1703003040000, 40);

-- Ride 2 with 2 stops at same intersection
INSERT INTO rides (...) VALUES (...);  -- id = 2
INSERT INTO stops (ride_id, stop_number, latitude, longitude, start_timestamp, end_timestamp, duration_seconds)
VALUES
    (2, 1, 37.422020, -122.084020, 1703010000000, 1703010020000, 20),
    (2, 2, 37.422080, -122.084080, 1703011000000, 1703011035000, 35);

-- Ride 3 with 1 stop at different intersection
INSERT INTO stops (ride_id, stop_number, latitude, longitude, start_timestamp, end_timestamp, duration_seconds)
VALUES
    (3, 1, 37.430000, -122.090000, 1703020000000, 1703020050000, 50);  -- 1km away
```

#### Step 2: Run Clustering

Call `ClusterStopsUseCase.clusterAllStops()` via:
- **Option A**: Add temporary button in Settings screen
- **Option B**: Unit test that writes to real database
- **Option C**: Android Studio Debugger → Evaluate Expression

#### Step 3: Verify Results

```sql
-- Check cluster assignments
SELECT cluster_id, COUNT(*) as stop_count
FROM stops
GROUP BY cluster_id
ORDER BY stop_count DESC;

-- Expected:
-- cluster_id | stop_count
-- 1          | 5           (Main St & 1st Ave)
-- 2          | 1           (Different intersection)

-- Verify cluster stats
SELECT
    cluster_id,
    COUNT(*) as stops,
    ROUND(AVG(latitude), 6) as lat,
    ROUND(AVG(longitude), 6) as lon,
    AVG(duration_seconds) as avg_dur
FROM stops
WHERE cluster_id = 1
GROUP BY cluster_id;

-- Expected:
-- cluster_id=1, stops=5, lat≈37.422050, lon≈-122.084050, avg_dur≈30
```

---

## Performance Benchmarks

Run these tests to validate performance targets:

```kotlin
@Test
fun `cluster 100 stops completes in under 20ms`() = runTest {
    val stops = generateRandomStops(count = 100, radius = 50f)  // 50m radius
    val start = System.currentTimeMillis()

    clusterStopsUseCase.clusterAllStops()

    val elapsed = System.currentTimeMillis() - start
    assertThat(elapsed).isLessThan(20)
}

@Test
fun `cluster 1000 stops completes in under 200ms`() = runTest {
    val stops = generateRandomStops(count = 1000, radius = 100f)
    val start = System.currentTimeMillis()

    clusterStopsUseCase.clusterAllStops()

    val elapsed = System.currentTimeMillis() - start
    assertThat(elapsed).isLessThan(200)
}
```

---

## Troubleshooting

### Issue: Stops not clustering (all get unique cluster_id)

**Cause**: Epsilon too small or GPS coordinates too spread out
**Fix**: Increase clustering radius in settings (try 30m or 50m)

### Issue: All stops in one giant cluster

**Cause**: Epsilon too large
**Fix**: Decrease clustering radius (try 10m or 15m)

### Issue: Performance slower than expected

**Cause**: O(n²) complexity with large dataset
**Solutions**:
1. Reduce number of stops for testing (< 500)
2. Run on physical device (faster than emulator)
3. Profile with Android Studio Profiler to find bottleneck

### Issue: Database migration error

**Cause**: cluster_id column doesn't exist
**Fix**: Ensure Feature 009 is merged (database version 2)

---

## Next Steps

After clustering is working:

1. ✅ **Add WorkManager integration** (trigger on settings change)
2. ⏸️ **Create ClusterAnalyticsViewModel** (optional - defer to post-MVP)
3. ⏸️ **Build ClusterAnalyticsScreen UI** (optional - defer to post-MVP)
4. ✅ **Write integration tests** with in-memory database
5. ✅ **Test on emulator** with real ride data

---

## References

- **Feature Spec**: `specs/010-stop-clustering/spec.md`
- **Implementation Plan**: `specs/010-stop-clustering/plan.md`
- **Research**: `specs/010-stop-clustering/research.md`
- **Data Model**: `specs/010-stop-clustering/data-model.md`
- **Contracts**: `specs/010-stop-clustering/contracts/`

**Estimated Total Time**: 3-4 hours for full implementation + testing
