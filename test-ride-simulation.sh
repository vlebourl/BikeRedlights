#!/bin/bash

# BikeRedlights Ride Simulation Script
# This script simulates a complete bike ride with GPS movement

ADB=~/Library/Android/sdk/platform-tools/adb

echo "🚴 BikeRedlights Ride Simulation Script"
echo "========================================"
echo ""

# Step 1: Stop and restart app
echo "1. Restarting app..."
$ADB shell am force-stop com.example.bikeredlights
sleep 1
$ADB shell am start -n com.example.bikeredlights/.MainActivity
sleep 4

# Step 2: Set initial GPS location (Annemasse, France)
echo "2. Setting initial GPS location (Annemasse)..."
$ADB emu geo fix 6.2347 46.1942
sleep 2

# Step 3: Take screenshot of idle state
echo "3. Taking screenshot: Idle state..."
$ADB exec-out screencap -p > /tmp/ride_sim_01_idle.png
echo "   Saved: /tmp/ride_sim_01_idle.png"
sleep 1

# Step 4: Start ride (tap Start Ride button)
echo "4. Starting ride..."
$ADB shell input tap 640 2028
sleep 3

# Step 5: Take screenshot of recording started
echo "5. Taking screenshot: Recording started..."
$ADB exec-out screencap -p > /tmp/ride_sim_02_started.png
echo "   Saved: /tmp/ride_sim_02_started.png"
sleep 1

# Step 6: Simulate GPS movement (route through Annemasse)
echo "6. Simulating GPS movement (10 waypoints)..."

waypoints=(
    "6.2347 46.1942"  # Start
    "6.2357 46.1945"  # Move east
    "6.2367 46.1950"  # Continue northeast
    "6.2377 46.1955"  # Continue
    "6.2387 46.1960"  # Continue
    "6.2397 46.1965"  # Continue
    "6.2407 46.1970"  # Continue
    "6.2417 46.1975"  # Continue
    "6.2427 46.1980"  # Continue
    "6.2437 46.1985"  # End point
)

for i in "${!waypoints[@]}"; do
    coords=(${waypoints[$i]})
    lon=${coords[0]}
    lat=${coords[1]}

    echo "   Waypoint $((i+1))/10: lat=$lat, lon=$lon"
    $ADB emu geo fix $lon $lat
    sleep 2

    # Take screenshots at key points
    if [ $i -eq 3 ]; then
        echo "   Taking screenshot: Mid-route (waypoint 4)..."
        $ADB exec-out screencap -p > /tmp/ride_sim_03_midroute.png
        echo "   Saved: /tmp/ride_sim_03_midroute.png"
    fi

    if [ $i -eq 9 ]; then
        echo "   Taking screenshot: End of route (waypoint 10)..."
        $ADB exec-out screencap -p > /tmp/ride_sim_04_endroute.png
        echo "   Saved: /tmp/ride_sim_04_endroute.png"
    fi
done

# Step 7: Wait a bit to observe final state
echo "7. Waiting 5 seconds to observe final state..."
sleep 5

# Step 8: Take final screenshot
echo "8. Taking final screenshot..."
$ADB exec-out screencap -p > /tmp/ride_sim_05_final.png
echo "   Saved: /tmp/ride_sim_05_final.png"

echo ""
echo "✅ Ride simulation complete!"
echo ""
echo "Screenshots saved:"
echo "  1. /tmp/ride_sim_01_idle.png - Idle state with map"
echo "  2. /tmp/ride_sim_02_started.png - Recording started"
echo "  3. /tmp/ride_sim_03_midroute.png - Mid-route (waypoint 4)"
echo "  4. /tmp/ride_sim_04_endroute.png - End of route (waypoint 10)"
echo "  5. /tmp/ride_sim_05_final.png - Final state"
echo ""
echo "Ride is still recording. To stop manually:"
echo "  - Tap the screen to access controls"
echo "  - Tap 'Stop Ride' button"
