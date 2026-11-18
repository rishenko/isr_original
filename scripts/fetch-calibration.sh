#!/usr/bin/env bash
# Download the Cassini ISS calibration files the converter's calibrators read (about 33 MB),
# instead of the full 2.7 GB coiss_0011_v3 archive.
#
# Usage: scripts/fetch-calibration.sh [target-dir]
#   target-dir defaults to ../isr-calib/coiss_0011_v3 (outside the repo).
# Then run tests with: mvn verify -Disr.calib.dir=<target-dir>
# or point cassini.calibration.dir in RunConverter.properties at it.
#
# Slope (flat-field) files: only those matching the test images are fetched. Converting other
# images may need more; DivideByFlatsCalibrator picks them from calib/slope/slope_db_2.tab.
set -euo pipefail

# PDS Ring-Moon Systems Node mirror. The JPL imaging node URL used before
# (planetarydata.jpl.nasa.gov/img/data/cassini/...) returned 502 errors in November 2026.
# Version 3 of COISS_0011 is the edition the golden tests were recorded with; the current
# edition (v4, 2019) has re-derived bitweight tables, so switching would change calibrated output.
BASE="https://pds-rings.seti.org/holdings/volumes/COISS_0xxx_v3/COISS_0011/calib"
TARGET="${1:-$(cd "$(dirname "$0")/.." && pwd)/../isr-calib/coiss_0011_v3}"

fetch() {
  local path="$1"
  local dest="$TARGET/calib/$path"
  if [[ -s "$dest" ]]; then
    return
  fi
  mkdir -p "$(dirname "$dest")"
  curl --fail --silent --show-error --location --retry 3 -o "$dest.part" "$BASE/$path"
  mv "$dest.part" "$dest"
}

for camera in nac wac; do
  for gain in 1 2 3; do
    for temp in m10 p5 p25; do
      fetch "bitweight/${camera}g${gain}${temp}_bwt.tab"
    done
  done
done

fetch dustring/nac_dustring_1308947228.img
fetch dustring/nac_dustring_1308947228.lbl
fetch dustring/nac_mottle_1444733393.tif
fetch dustring/nac_mottle_1444733393.lbl

for db in 1 2 3; do
  fetch "slope/slope_db_${db}.tab"
done
for slope in 108 114 209 218 250 253; do
  fetch "slope/slope${slope}.img"
  fetch "slope/slope${slope}.lbl"
done

echo "Calibration files are in $TARGET"
