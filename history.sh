#!/bin/bash

# Extract changelog from HISTORY.md between two versions
# Usage: ./history.sh [current_version] [previous_version]
# If no arguments, extracts the first version section

CURRENT_VERSION="${1:-}"
PREVIOUS_VERSION="${2:-}"

if [ -z "$CURRENT_VERSION" ]; then
    # No version specified, extract first section
    in_section=false
    while IFS= read -r line; do
        [[ -z "$line" ]] && continue
        if [[ "$line" == "### "* ]] && ! $in_section; then
            in_section=true
            continue
        fi
        if $in_section && [[ "$line" == "### "* ]]; then
            break
        fi
        $in_section && echo "$line"
    done < HISTORY.md
    exit 0
fi

# Extract between current and previous versions
in_section=false
found_current=false

while IFS= read -r line; do
    [[ -z "$line" ]] && continue

    if [[ "$line" == "### "* ]]; then
        version_str="${line### }"

        # Check if we've reached the current version
        if [[ "$version_str" == *"$CURRENT_VERSION"* ]]; then
            found_current=true
            echo "$line"
            continue
        fi

        # Check if we've reached the previous version (stop)
        if [[ -n "$PREVIOUS_VERSION" ]] && [[ "$version_str" == *"$PREVIOUS_VERSION"* ]]; then
            break
        fi

        # Print version header if we're in range
        $found_current && echo "$line"
        continue
    fi

    # Print content lines
    $found_current && echo "$line"
done < HISTORY.md
