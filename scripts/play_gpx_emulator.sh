#!/bin/bash
# Play GPX track on Android emulator via emulator console
# Usage: ./scripts/play_gpx_emulator.sh test_track.gpx [speed_multiplier]

GPX_FILE="${1:-test_track.gpx}"
SPEED="${2:-5}"  # 5x speed by default for testing
DELAY=$(echo "scale=3; 1.0 / $SPEED" | bc)

if [ ! -f "$GPX_FILE" ]; then
    echo "❌ Error: GPX file not found: $GPX_FILE"
    exit 1
fi

# Find emulator port
EMU_PORT=$(adb devices | grep emulator | cut -f1 | cut -d'-' -f2)
if [ -z "$EMU_PORT" ]; then
    echo "❌ Error: No emulator found. Please start the emulator first."
    exit 1
fi

echo "🚴 BikeRedlights GPX Track Player"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📍 Track: $GPX_FILE"
echo "⚡ Speed: ${SPEED}x"
echo "⏱️  Delay: ${DELAY}s per point"
echo "🔌 Emulator: localhost:$EMU_PORT"
echo ""
echo "📱 Make sure BikeRedlights app is open and ride is started!"
echo "Press Ctrl+C to stop playback"
echo ""
echo "Starting in 3 seconds..."
sleep 3

POINT_NUM=0
START_TIME=$(date +%s)

# Extract and send GPS coordinates
grep -E "<trkpt|<time>" "$GPX_FILE" | \
while IFS= read -r line; do
    if [[ $line =~ lat=\"([0-9.-]+)\".*lon=\"([0-9.-]+)\" ]]; then
        LAT="${BASH_REMATCH[1]}"
        LON="${BASH_REMATCH[2]}"
    elif [[ $line =~ \<time\>(.+)\</time\> ]]; then
        TIME="${BASH_REMATCH[1]}"
        POINT_NUM=$((POINT_NUM + 1))

        # Send to emulator console (more reliable than adb emu)
        echo "geo fix $LON $LAT" | nc -w 1 localhost "$EMU_PORT" > /dev/null 2>&1

        # Progress indicator every 50 points
        if [ $((POINT_NUM % 50)) -eq 0 ]; then
            ELAPSED=$(($(date +%s) - START_TIME))
            echo "📍 Point $POINT_NUM/1564 | Elapsed: ${ELAPSED}s | Lat: $LAT, Lon: $LON"
        fi

        # Wait before next point
        sleep "$DELAY"
    fi
done

TOTAL_TIME=$(($(date +%s) - START_TIME))
echo ""
echo "✅ GPX playback complete!"
echo "📊 Total points: $POINT_NUM"
echo "⏱️  Total time: ${TOTAL_TIME}s"
echo ""
echo "🎉 Check the app for ride statistics!"
