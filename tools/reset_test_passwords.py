"""Explicit reset of ALL existing accounts in the configured utetra_db test database."""
from pathlib import Path
import os
import subprocess
import sys
from runtime import java_executable

root = Path(__file__).resolve().parent.parent
if sys.argv[1:] != ['--apply-all-accounts']:
    print('Plan: reset ALL existing accounts, including original team accounts; revoke sessions and OTP grants.')
    print('Requires the latest application database migrations. Does not activate or verify accounts.')
    print('Run python tools/reset_test_passwords.py --apply-all-accounts only against the intended test database.')
    raise SystemExit(0)
java = java_executable()
classpath_file = root / '.local/identity-classpath.txt'
if not classpath_file.is_file():
    raise SystemExit('Create .local/identity-classpath.txt using Maven dependency:build-classpath first.')
classpath = classpath_file.read_text(encoding='utf-8').strip()
subprocess.run([str(java), '-cp', classpath, str(root / 'tools/demo/ResetTestPasswords.java'),
                '--apply-all-accounts'], cwd=root, check=True)
