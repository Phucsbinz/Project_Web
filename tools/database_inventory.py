from pathlib import Path
from runtime import java_executable
import subprocess
root=Path(__file__).resolve().parent.parent
java=java_executable()
cp=(root/'.local/identity-classpath.txt').read_text(encoding='utf-8').strip()
raise SystemExit(subprocess.call([str(java),'-Xmx96m','-XX:+UseSerialGC','-cp',cp,'tools/DatabaseInventory.java'],cwd=root))
