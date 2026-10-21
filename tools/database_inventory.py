from pathlib import Path
import subprocess
root=Path(__file__).resolve().parent.parent
java=next((root/'.tools/jdk21').glob('*/bin/java.exe'))
cp=(root/'.local/identity-classpath.txt').read_text(encoding='utf-8').strip()
raise SystemExit(subprocess.call([str(java),'-Xmx96m','-XX:+UseSerialGC','-cp',cp,'tools/DatabaseInventory.java'],cwd=root))
