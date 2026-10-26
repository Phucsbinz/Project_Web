"""Enter a Gmail app password privately; store only in ignored local configuration."""
from pathlib import Path
import argparse
import getpass
import re

root = Path(__file__).resolve().parent.parent
parser = argparse.ArgumentParser(description='Configure Gmail OTP delivery on this machine without committing credentials.')
parser.add_argument('--email', default='jskosiskeekdj@gmail.com', help='Gmail sender whose App Password will be entered')
args = parser.parse_args()
email = args.email.strip()
if not re.fullmatch(r'[A-Za-z0-9._%+-]+@gmail\.com', email, re.IGNORECASE):
    raise SystemExit('Expected a Gmail address. No configuration changed.')
password = getpass.getpass('Gmail App Password (hidden): ').replace(' ', '')
if len(password) != 16 or not password.isascii() or not password.isalpha():
    raise SystemExit('Expected the 16-letter Gmail app password. No configuration changed.')
local = root / '.local'
local.mkdir(exist_ok=True)
(local / 'mail.properties').write_text(
    '# Local secret. Never commit or share this file.\n'
    'app.mail.enabled=true\n'
    f'app.mail.from={email}\n'
    f'spring.mail.username={email}\n'
    f'spring.mail.password={password}\n', encoding='utf-8')
print('Saved local Gmail configuration. Restart the web application to apply.')
