"""Explicit additive UTETra demo data; run without --apply to see the plan."""
from pathlib import Path
import os
import subprocess
import sys
from runtime import java_executable

root = Path(__file__).resolve().parent.parent
if '--apply' not in sys.argv:
    print('Plan: add 12 Vietnamese demo accounts, 3 stores, 30 drinks/store, 3 sizes/drink,')
    print('90 completed COD orders with reviews/comments, 9 office orders, 7 state/payment scenarios, 2 vouchers.')
    print('Existing accounts and data are preserved. No email or real payment requests.')
    print('Run python tools/seed_demo.py --apply to write to configured utetra_db.')
    raise SystemExit(0)
java = java_executable()
classpath_file = root / '.local/identity-classpath.txt'
if not classpath_file.is_file():
    raise SystemExit('Build the project and create .local/identity-classpath.txt using Maven dependency:build-classpath first.')
classes = root / '.local/demo-classes'
classes.mkdir(exist_ok=True)
classpath = str(root / 'target/classes') + os.pathsep + classpath_file.read_text(encoding='utf-8').strip()
subprocess.run([str(java.with_name('javac.exe' if os.name == 'nt' else 'javac')),
                '-encoding', 'UTF-8', '-cp', classpath, '-d', str(classes),
                str(root / 'tools/demo/FeedbackCopy.java'), str(root / 'tools/demo/SeedDemo.java'), str(root / 'tools/demo/ShowcaseOrders.java'), str(root / 'tools/demo/AssignCatalogKeys.java')], cwd=root, check=True)
logfile = root / '.local/demo-seed.log'
with logfile.open('wb') as log:
    for entry in ['SeedDemo', 'AssignCatalogKeys', 'ShowcaseOrders']:
        result = subprocess.run([str(java), '-Xmx256m', '-XX:+UseSerialGC',
                                 '-XX:ActiveProcessorCount=2', '-XX:TieredStopAtLevel=1',
                                 '-cp', str(classes) + os.pathsep + classpath,
                                 entry, '--apply-utetra-demo'], cwd=root, stdout=log,
                                stderr=subprocess.STDOUT)
        if result.returncode:
            break
print('Seed exit:', result.returncode, 'Log: .local/demo-seed.log')
raise SystemExit(result.returncode)
