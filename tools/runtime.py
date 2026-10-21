"""Locate JDK 21 without depending on one member's machine layout."""
from pathlib import Path
import os
import re
import shutil
import subprocess

ROOT = Path(__file__).resolve().parent.parent
HIDDEN = getattr(subprocess, 'CREATE_NO_WINDOW', 0)


def java_executable():
    name = 'java.exe' if os.name == 'nt' else 'java'
    candidates = []
    for key in ('UTETRA_JAVA_HOME', 'JAVA_HOME'):
        if os.environ.get(key):
            candidates.append(Path(os.environ[key]) / 'bin' / name)
    bundled = ROOT / '.tools' / 'jdk21'
    candidates.append(bundled / 'bin' / name)
    candidates.extend(bundled.glob('*/bin/' + name))
    system = shutil.which(name)
    if system:
        candidates.append(Path(system))
    for java in candidates:
        if not java.is_file():
            continue
        info = subprocess.run([str(java), '-XshowSettings:properties', '-version'],
                              capture_output=True, text=True, creationflags=HIDDEN)
        if info.returncode == 0 and re.search(r'java.specification.version\s*=\s*21\s', info.stderr + info.stdout):
            return java.resolve()
    raise SystemExit('JDK 21 is required. Set JAVA_HOME or UTETRA_JAVA_HOME to your JDK 21 directory, then retry.')


def maven_command(*args):
    if os.name == 'nt':
        return ['cmd.exe', '/d', '/c', 'mvnw.cmd', *args]
    return ['sh', str(ROOT / 'mvnw'), *args]
