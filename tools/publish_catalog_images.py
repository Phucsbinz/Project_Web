"""Publish existing database-referenced local images to configured Cloudinary."""
from pathlib import Path
import os,sys,subprocess
from runtime import java_executable
root=Path(__file__).resolve().parent.parent
os.chdir(root)
if '--apply' not in sys.argv:raise SystemExit('Use --apply to upload referenced local images and replace their URLs in the configured database.')
cp=str(root/'target/classes')+os.pathsep+(root/'.local/identity-classpath.txt').read_text().strip()
folder=root/'.local/publish-image-classes';folder.mkdir(exist_ok=True)
java=java_executable()
subprocess.run([str(java.with_name('javac.exe')),'-encoding','UTF-8','-cp',cp,'-d',str(folder),str(root/'tools/demo/PublishCatalogImages.java')],check=True)
subprocess.run([str(java),'-cp',str(folder)+os.pathsep+cp,'PublishCatalogImages','--apply'],check=True)
