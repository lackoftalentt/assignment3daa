#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mvn clean verify
java -Xmx1g -jar target/daa-assignment3-graphs-1.0.jar benchmark results | tee results/benchmark.log
python3 scripts/analyze.py
