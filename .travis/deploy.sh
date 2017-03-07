#!/bin/bash

set -e

echo "Deploying to play store.. "

for word in $*; do echo "Branch: $word"; done

#Deploy to Play Store
./gradlew publishApkFreeRelease publishApkPaidRelease