Benchmark for taint-flow bug repair.

## Files

- candidates.csv: List of potential subjects that contain bug.
- all-subjects: Downloaded subjects from candidates.csv.
- bug-subjects: Subset of all-subjects that Doop can find bug in.
- bug-srcs: Souce codes of bug subjects.

## How is this constructed?

This dataset is part of the AndroZoo dataset, constructed with the following steps:

1. Download the entire AndroZoo bug list `androzoo.csv`, and filter it with `filter.sh`. Examples 
of the filtering commands can be found at https://androzoo.uni.lu/lists. The filtering criteria is:
    - Source is FDroid (this platform contains apk with source code, and source code would be useful
    when we do Datalog-to-Java transformation).
    - apk size is smaller than 10000000 bytes.
    - The apk is reported to have bug in at least 2 AntiVirus engines. (AndroZoo uses a few AntiVirus engines to scan these apks.)

2. Use [az](https://github.com/ArtemKushnerov/az) to download apps in candidate.csv to all-subjects.
There are 75 apks in all-subjects.

3. The apks in all-subjects are flagged to likely contain bugs, but they are not guaranteed to be 
detectable by Doop. These 75 are then passed to Doop for analysis (the configuration can be 
found in bug-subjects/README.txt). As a result, Doop reports bug on 37/75 apks. These 37 apps are 
in bug-subjects, and they are the final ones we can use in the benchmark.


## Potential issues

1. These are real open source apps and may not have been well maintained, so most likely there is 
no developer fixes for the bugs reported by Doop. Since there is no ground truth patch, we may 
have to validate the generated patches just by running Doop again.
