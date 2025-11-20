#!/bin/bash
# Fast GPX playback using adb emu geo fix (simpler, no auth needed)
# Usage: ./scripts/play_gpx_fast.sh test_track.gpx [speed_multiplier]

GPX_FILE="${1:-test_track.gpx}"
SPEED="${2:-5}"
DELAY=$(echo "scale=3; 1.0 / $SPEED" | bc)

if [ ! -f "$GPX_FILE" ]; then
    echo "❌ Error: GPX file not found: $GPX_FILE"
    exit 1
fi

echo "🚴 BikeRedlights GPX Track Player"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📍 Track: $GPX_FILE"
echo "⚡ Speed: ${SPEED}x (${DELAY}s per point)"
echo ""
echo "📱 IMPORTANT: Start a ride in BikeRedlights NOW!"
echo "   1. Open BikeRedlights app"
echo "   2. Tap 'Start Ride' button"
echo "   3. Grant location permission if prompted"
echo ""
echo "Starting GPX playback in 5 seconds..."
sleep 5

POINT_NUM=0
TOTAL_POINTS=$(grep -c "<trkpt" "$GPX_FILE")
START_TIME=$(date +%s)

echo ""
echo "🏁 Playback started!"
echo ""

# Extract and send GPS coordinates
grep -E "<trkpt|<time>" "$GPX_FILE" | \
while IFS= read -r line; do
    if [[ $line =~ lat=\"([0-9.-]+)\".*lon=\"([0-9.-]+)\" ]]; then
        LAT="${BASH_REMATCH[1]}"
        LON="${BASH_REMATCH[2]}"
    elif [[ $line =~ \<time\>(.+)\</time\> ]]; then
        POINT_NUM=$((POINT_NUM + 1))

        # Send to emulator (target specific device)
        adb -s emulator-5554 emu geo fix "$LON" "$LAT" 2>/dev/null

        # Progress every 100 points
        if [ $((POINT_NUM % 100)) -eq 0 ]; then
            ELAPSED=$(($(date +%s) - START_TIME))
            PERCENT=$((POINT_NUM * 100 / TOTAL_POINTS))
            echo "📍 $POINT_NUM/$TOTAL_POINTS ($PERCENT%) | ${ELAPSED}s | Lat: $LAT, Lon: $LON"
        fi

        sleep "$DELAY"
    fi
done

TOTAL_TIME=$(($(date +%s) - START_TIME))
echo ""
echo "✅ GPX playback complete!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 Points sent: $POINT_NUM"
echo "⏱️  Duration: ${TOTAL_TIME}s"
echo "⚡ Avg speed: $(echo "scale=1; $POINT_NUM / $TOTAL_TIME" | bc) points/sec"
echo ""
echo "🎉 Check BikeRedlights for ride stats!"
