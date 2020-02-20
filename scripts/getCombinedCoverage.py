#!/usr/bin/python3

import sys
import subprocess
import xml.etree.ElementTree as ET
import os
from os import listdir
from os.path import isfile, join
from replaceEmptyCoverageFiles import replaceEmptyCoverageFiles
from coverageReport import genCoverageReport


package = sys.argv[1]
chromosomes = sys.argv[2]
src_dir = package + ".src"
coverage_path = package + ".coverage"
xml_report_path = coverage_path + "/jacocoTestReport.xml"

replaceEmptyCoverageFiles(package)

listOfFiles = list()

if chromosomes == "all":
    for (dirpath, dirnames, filenames) in os.walk(coverage_path):
        listOfFiles += [os.path.join(dirpath, file) for file in filenames]
else:
    for ch in chromosomes.split("+"):
        cp = coverage_path + "/" + ch
        listOfFiles += [cp + "/" + f for f in listdir(cp) if isfile(join(cp, f))]

genCoverageReport(listOfFiles, src_dir, xml_report_path)

tree = ET.parse(xml_report_path)
instruction_element = tree.getroot().findall("./counter[@type='LINE']")[0]
missed_instructions = float(instruction_element.get('missed'))
covered_instructions = float(instruction_element.get('covered'))
print("Missed instructions: " + str(missed_instructions))
print("Covered instructions: " + str(covered_instructions))
total = missed_instructions + covered_instructions
coverage_result = covered_instructions / total
print("Total coverage:")
print(coverage_result)
