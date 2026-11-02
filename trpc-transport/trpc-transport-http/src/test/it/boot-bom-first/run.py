#!/usr/bin/env python3
"""Run the HTTP transport contract against the supported Boot BOM range."""

import json
from pathlib import Path
import re
import subprocess
import sys
import time
import urllib.request
import xml.etree.ElementTree as ET


METADATA = (
    "https://repo.maven.apache.org/maven2/org/springframework/boot/"
    "spring-boot-dependencies/maven-metadata.xml"
)


def select_versions(metadata, baseline):
    versions = sorted({
        tuple(map(int, item.text.split(".")))
        for item in metadata.findall("versioning/versions/version")
        if item.text and re.fullmatch(r"\d+\.\d+\.\d+", item.text)
    })
    if baseline not in versions:
        raise ValueError("The framework's Boot baseline is absent from Maven Central")
    previous_lines = sorted({
        version[:2] for version in versions
        if version[0] == baseline[0] and version[:2] < baseline[:2]
    })[-2:]
    previous = [max(v for v in versions if v[:2] == line) for line in previous_lines]
    supported = previous + [v for v in versions if v[:2] >= baseline[:2]]
    return [".".join(map(str, v)) for v in supported]


def main():
    fixture = Path(__file__).resolve().parent
    repository = fixture.parents[5]
    ns = {"m": "http://maven.apache.org/POM/4.0.0"}
    bom = ET.parse(repository / "trpc-dependencies/trpc-dependencies-bom/pom.xml")
    baseline_text = bom.findtext("m:properties/m:springboot.version", namespaces=ns)
    baseline = tuple(map(int, baseline_text.split(".")))
    with urllib.request.urlopen(METADATA, timeout=30) as response:
        metadata = ET.fromstring(response.read())
    versions = select_versions(metadata, baseline)
    output = repository / "target/jetty-boot-compatibility"
    output.mkdir(parents=True, exist_ok=True)
    (output / "summary.json").write_text("[]\n")
    print("HTTP transport Boot BOM versions: " + ", ".join(versions), flush=True)
    results = []
    for version in versions:
        log = output / (version + ".log")
        command = [
            "mvn", "-B", "-ntp", "-f", str(fixture / "pom.xml"), *sys.argv[1:],
            "-Dspring-boot.version=" + version,
            "org.apache.maven.plugins:maven-dependency-plugin:3.8.1:tree",
            "-Dincludes=org.eclipse.jetty*:*,jakarta.servlet:*", "clean", "test",
        ]
        started = time.time()
        with log.open("w") as stream:
            process = subprocess.run(command, stdout=stream, stderr=subprocess.STDOUT)
        counts = dict.fromkeys(("tests", "failures", "errors", "skipped"), 0)
        for report in (fixture / "target/surefire-reports").glob("TEST-*.xml"):
            if report.stat().st_mtime < started:
                continue
            suite = ET.parse(report).getroot()
            for key in counts:
                counts[key] += int(suite.get(key, "0"))
        passed = process.returncode == 0 and counts["tests"] > 0 and not any(
            counts[key] for key in ("failures", "errors", "skipped")
        )
        text = log.read_text()
        jetty = re.search(r"org.eclipse.jetty:jetty-server:jar:([^:]+):", text)
        result = {"boot": version, "passed": passed, "exit_code": process.returncode,
                  "jetty": jetty.group(1) if jetty else None, **counts, "log": str(log)}
        results.append(result)
        (output / "summary.json").write_text(json.dumps(results, indent=2) + "\n")
        print(json.dumps(result), flush=True)
        if not passed:
            errors = [line for line in text.splitlines() if "[ERROR]" in line or "Caused by:" in line]
            print("\n".join(errors[:15]), flush=True)
    return 0 if all(result["passed"] for result in results) else 1


if __name__ == "__main__":
    sys.exit(main())
