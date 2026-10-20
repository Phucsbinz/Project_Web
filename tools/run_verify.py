"""Build using local JDK 21 with bounded memory; no application data is modified."""
from pathlib import Path
import os,subprocess,shutil,sys
root=Path(__file__).resolve().parent.parent
os.chdir(root)
java=next((root/'.tools/jdk21').glob('*/bin/java.exe'))
env=os.environ.copy();env['JAVA_HOME']=str(java.parent.parent);env['PATH']=str(java.parent)+';'+env['PATH']
env['MAVEN_OPTS']='-Xms24m -Xmx192m -Xss384k -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=24m -XX:HeapBaseMinAddress=8g -XX:MaxDirectMemorySize=24m -Dserver.tomcat.threads.max=12 -Dserver.tomcat.threads.min-spare=1'
# In-process Surefire cannot reliably spawn the Mockito self-attachment helper
# on Windows under memory pressure. Load its official premain agent at JVM start.
classpath=root/'.local/identity-classpath.txt'
if classpath.exists():
 agents=[Path(p) for p in classpath.read_text(encoding='utf-8').strip().split(';') if Path(p).name.startswith('mockito-core-') and Path(p).is_file()]
 if agents:env['MAVEN_OPTS']+=' -javaagent:"'+str(agents[0])+'"'
with (root/'tmp-integration-verify.log').open('wb') as log:
 result=subprocess.run([shutil.which('mvn.cmd'),'-B','-ntp','-DforkCount=0',*(sys.argv[1:] or ['verify'])],env=env,stdout=log,stderr=subprocess.STDOUT)
print('Maven exit:',result.returncode,'Log: tmp-integration-verify.log')
sys.exit(result.returncode)
