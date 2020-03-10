#!/usr/bin/python3

import sys
import subprocess
import pathlib
import tempfile

device = sys.argv[1]
package = sys.argv[2]
chromosome = sys.argv[3]
emmaCoverageReceiverClass = sys.argv[4]

coverage_dir = package + ".coverage/" + chromosome
pathlib.Path(coverage_dir).mkdir(parents=True, exist_ok=True)

(fd, coverage_file) = tempfile.mkstemp(dir=coverage_dir)

print("Created new coverage file: "  + coverage_file)

subprocess.run(["bash", "-c", "adb root"])
subprocess.run(["bash", "-c", "adb -s " + device + " rm /data/user/0/" + package + "/files/coverage.ec"])

# press HOME to trigger the "onPause" event (https://stackoverflow.com/questions/7789826/adb-shell-input-events)
subprocess.run(["bash", "-c", "adb -s " + device + " shell input keyevent 3"])

# collect coverage
subprocess.run(["bash", "-c", "adb -s " + device + " shell am broadcast -a evolutiz.emma.COLLECT_COVERAGE -n " +
                emmaCoverageReceiverClass])
subprocess.run(["bash", "-c", "adb -s " + device + " pull /data/user/0/" + package + "/files/coverage.ec " +
                coverage_file])

# open app again
subprocess.run(["bash", "-c", "adb -s " + device + " shell monkey -p " + package + " 1"])
