"""Check the one-layout contract and optionally compare shared blobs with a Git ref."""
from pathlib import Path
import argparse
import subprocess
import sys

root = Path(__file__).resolve().parent.parent
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--compare', help='Compare the shared UI bundle with a ref, e.g. main')
args = parser.parse_args()
errors = []
layouts = root / 'src/main/webapp/WEB-INF/views/layouts'
pages = sorted(str(path.relative_to(layouts)).replace('\\', '/') for path in layouts.rglob('*.jsp'))
if pages != ['app.jsp']:
    errors.append('Expected only layouts/app.jsp; found: ' + ', '.join(pages))
config = (root / 'src/main/java/com/utetra/config/SiteMeshConfig.java').read_text(encoding='utf-8')
if config.count('.addDecoratorPath(') != 1 or '"/*", "/WEB-INF/views/layouts/app.jsp"' not in config:
    errors.append('SiteMeshConfig must have one /* decorator pointing to app.jsp')
manifest = (root / 'docs/SHARED_UI_FILES.txt').read_text(encoding='utf-8')
files = [line.strip() for line in manifest.splitlines() if line.strip() and not line.startswith('#')]
for name in files:
    path = root / name
    if not path.is_file():
        errors.append('Missing shared file: ' + name)
        continue
    if args.compare:
        reference = subprocess.run(['git', 'rev-parse', '--verify', args.compare + ':' + name],
                                   cwd=root, capture_output=True, text=True)
        current = subprocess.run(['git', 'hash-object', '--path=' + name, name],
                                 cwd=root, capture_output=True, text=True)
        if reference.returncode or current.returncode or reference.stdout.strip() != current.stdout.strip():
            errors.append('Shared file differs from ' + args.compare + ': ' + name)
if args.compare:
    reference_layouts = subprocess.run(['git', 'ls-tree', '-r', '--name-only', args.compare,
                                       'src/main/webapp/WEB-INF/views/layouts'],
                                      cwd=root, capture_output=True, text=True)
    expected = ['src/main/webapp/WEB-INF/views/layouts/app.jsp']
    if reference_layouts.returncode or [p for p in reference_layouts.stdout.splitlines() if p.endswith('.jsp')] != expected:
        errors.append('Reference does not yet use the one-layout bundle: ' + args.compare)
if errors:
    for error in errors:
        print('ERROR:', error)
    sys.exit(1)
print('Shared UI OK: one SiteMesh layout, ' + str(len(files)) + ' shared files'
      + (' match ' + args.compare if args.compare else ''))
