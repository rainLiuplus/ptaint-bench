#!/bin/bash

# filter from all subjects to get a list of candidate apps

cat androzoo.csv | grep -v ',snaggamea' | awk -F, '{if ($11 ~ /fdroid/) {print} }' | awk -F, '{if ($5 <=10000000 ) {print} }' | awk -F, '{if ($8 >=2 ) {print} }' | sort -t ',' -k6,6 -k9,9r | sort -t ',' -uk6,6 > candidates.csv


