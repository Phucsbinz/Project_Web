"""Verify using the committed Maven Wrapper and a configured JDK 21."""
from pathlib import Path
import os
import subprocess
import sys
from runtime import java_executable, maven_command

root = Path(__file__).resolve().parent.parent
java = java_executable()
env = os.environ.copy()
env['JAVA_HOME'] = str(java.parent.parent)
env['PATH'] = str(java.parent) + os.pathsep + env['PATH']
local = root / '.local'
local.mkdir(exist_ok=True)
logfile = local / 'verify.log'
with logfile.open('wb') as log:
    result = subprocess.run(maven_command('-B', '-ntp', *(sys.argv[1:] or ['clean', 'verify'])),
                            cwd=root, env=env, stdout=log, stderr=subprocess.STDOUT)
print('Maven exit:', result.returncode, 'Log: .local/verify.log')
sys.exit(result.returncode)
