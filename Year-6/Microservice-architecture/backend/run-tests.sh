#!/bin/bash

set -e

echo "========================================="
echo "Running tests with coverage..."
echo "========================================="

./gradlew clean

MODULES="admin discovery"

for module in $MODULES; do
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "Testing $module..."
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    ./gradlew :$module:test --no-daemon --continue || true
    ./gradlew :$module:jacocoTestReport --no-daemon || true

    # Parse coverage from CSV (more reliable than XML on macOS)
    CSV_PATH="$module/build/reports/jacoco/test/jacocoTestReport.csv"
    if [ -f "$CSV_PATH" ]; then
        # Get last line (total) and extract instruction coverage
        LAST_LINE=$(tail -1 "$CSV_PATH")
        MISSED=$(echo "$LAST_LINE" | cut -d',' -f4)
        COVERED=$(echo "$LAST_LINE" | cut -d',' -f5)

        if [ -n "$MISSED" ] && [ -n "$COVERED" ]; then
            TOTAL=$((MISSED + COVERED))
            if [ $TOTAL -gt 0 ]; then
                PERCENT=$(awk "BEGIN {printf \"%.2f\", ($COVERED * 100.0 / $TOTAL)}")
                echo "[$module] Coverage: ${PERCENT}%"
            fi
        fi
    fi
done

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Generating summary report..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
./gradlew printCoverageReport --no-daemon

echo ""
echo "Test reports:"
for module in $MODULES; do
    REPORT_HTML="$(pwd)/$module/build/reports/tests/test/index.html"
    if [ -f "$REPORT_HTML" ]; then
        echo "  $module tests: file://$REPORT_HTML"
    fi

    COVERAGE_HTML="$(pwd)/$module/build/reports/jacoco/test/html/index.html"
    if [ -f "$COVERAGE_HTML" ]; then
        echo "  $module coverage: file://$COVERAGE_HTML"
    fi
done
