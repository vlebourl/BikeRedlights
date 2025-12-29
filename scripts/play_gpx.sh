#!/bin/bash
# Play GPX track on Android emulator
# Usage: ./scripts/play_gpx.sh test_track.gpx [speed_multiplier]

GPX_FILE="${1:-test_track.gpx}"
SPEED="${2:-1}"  # 1x speed by default
DELAY=$(echo "scale=2; 1 / $SPEED" | bc)

if [ ! -f "$GPX_FILE" ]; then
    echo "Error: GPX file not found: $GPX_FILE"
    exit 1
fi

echo "Playing GPX track: $GPX_FILE"
echo "Speed: ${SPEED}x (delay: ${DELAY}s between points)"
echo "Press Ctrl+C to stop"
echo ""

# Extract lat/lon/time from GPX
grep -E "<trkpt|<time>" "$GPX_FILE" | \
while IFS= read -r line; do
    if [[ $line =~ lat=\"([0-9.-]+)\".*lon=\"([0-9.-]+)\" ]]; then
        LAT="${BASH_REMATCH[1]}"
        LON="${BASH_REMATCH[2]}"
    elif [[ $line =~ \<time\>(.+)\</time\> ]]; then
        TIME="${BASH_REMATCH[1]}"

        # Send location to emulator
        adb emu geo fix "$LON" "$LAT"
        echo "$(date '+%H:%M:%S') - Sent: lat=$LAT, lon=$LON (time=$TIME)"

        # Wait before next point
        sleep "$DELAY"
    fi
done

echo ""
echo "GPX playback complete!"
