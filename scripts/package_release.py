#!/usr/bin/env python3
"""Package tracked release files, runnable JAR, all Git refs, and SHA-256 checksums."""
import hashlib
from pathlib import Path
import subprocess
import tempfile
import zipfile


def main():
    root = Path(__file__).resolve().parents[1]

    def git(*args):
        return subprocess.check_output(["git", "-C", str(root), *args])

    if git("status", "--porcelain").strip():
        raise SystemExit("Commit the release files before packaging.")
    if git("branch", "--show-current").strip() != b"main":
        raise SystemExit("Switch to main before packaging.")
    if git("rev-parse", "v1.0^{commit}") != git("rev-parse", "HEAD"):
        raise SystemExit("Tag v1.0 must identify the current release commit.")
    jar = root / "target/daa-assignment3-graphs-1.0.jar"
    if not jar.exists():
        raise SystemExit("Run mvn verify to build the runnable JAR first.")
    content = {path: (root / path).read_bytes() for path in
               git("ls-files", "-z").decode().split("\0") if path}
    content["bin/daa-assignment3-graphs-1.0.jar"] = jar.read_bytes()
    with tempfile.TemporaryDirectory(prefix="daa-release-") as temporary:
        bundle = Path(temporary) / "repository.bundle"
        subprocess.run(["git", "-C", str(root), "bundle", "create", str(bundle), "--all"], check=True)
        content["repository.bundle"] = bundle.read_bytes()
    content["SUBMISSION.txt"] = (
        "Beibarys Balykbayev | SE-2516 | DAA Assignment 3\n"
        "Repository: https://github.com/lackoftalentt/assignment3daa\n"
        "Branch: main; tag: v1.0\n"
        "Build and test: mvn clean verify\n"
        "Prebuilt demo: java -jar bin/daa-assignment3-graphs-1.0.jar demo data/dag.txt\n"
        "Report: REPORT.pdf (5 pages); preparation: DEFENSE.md (Russian)\n"
        "Git history and every required branch/tag: repository.bundle\n"
        "Remote publication could not be verified: GitHub DNS unavailable in execution environment.\n"
        "See README.md for push and bundle restoration commands.\n"
    ).encode()
    content["SHA256SUMS"] = "".join(f"{hashlib.sha256(data).hexdigest()}  {path}\n"
                                      for path, data in sorted(content.items())).encode()
    archive = root.parent / "DAA_Assignment3_Beibarys_Balykbayev_SE-2516.zip"
    with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as output:
        for path, data in sorted(content.items()):
            output.writestr(f"{root.name}/{path}", data)
    with zipfile.ZipFile(archive) as check:
        assert check.testzip() is None
    print(f"{archive} ({archive.stat().st_size:,} bytes, {len(content)} files)")


if __name__ == "__main__":
    main()
