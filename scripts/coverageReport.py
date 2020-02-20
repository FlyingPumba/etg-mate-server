import os
import subprocess
import tempfile

def chunks(l, n):
    """Yield successive n-sized chunks from l."""
    for i in range(0, len(l), n):
        yield l[i:i + n]

def crunsh(listOfFiles):
    lof = list()
    for files in chunks(listOfFiles, 100):
        (fd, crunsh_file) = tempfile.mkstemp(dir=".")
        crunsh_cmd = "java -jar bin/jacococli.jar merge " + " ".join(files) + " --destfile " + crunsh_file
        lof.append(crunsh_file)

        subprocess.run(["bash", "-c", crunsh_cmd], stdout=subprocess.PIPE)
    return lof

def genCoverageReport(listOfFiles, src_dir, xml_report_path):
    while len(listOfFiles) > 100:
        listOfFiles = crunsh(listOfFiles)

    coverage_files = " ".join(listOfFiles)

    html_report_path = os.path.dirname(xml_report_path) + "/html_report"

    generate_coverage_command = "java -jar bin/jacococli.jar report " + coverage_files + " --classfiles " + src_dir + "/classes --sourcefiles " + src_dir + "/java --xml " + xml_report_path + " --html " + html_report_path
    print("HOLA")
    print(generate_coverage_command)

    subprocess.run(["bash", "-c", generate_coverage_command])
