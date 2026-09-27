#!/usr/bin/env bash
# Captures README screenshots of the sample app on a running emulator.
set -euo pipefail
source "$(dirname "$0")/screenshot-lib.sh"

install_sample
for mode in light dark; do
  set_night_mode "$mode"
  fresh_launch                           # the sample cart screen
  capture "home-$mode"
  fresh_launch --es scene report         # report screen with a marked-up screenshot
  capture "report-$mode"
  fresh_launch --es scene annotate       # the markup editor
  capture "annotate-$mode"
done
