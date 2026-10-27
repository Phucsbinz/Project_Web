"""Repair only known generated review/comment boilerplate; --apply opts in to writing."""
from pathlib import Path
import os,subprocess,sys
from runtime import java_executable
root=Path(__file__).resolve().parent.parent
java=java_executable();cpfile=root/'.local/identity-classpath.txt'
if not cpfile.is_file():raise SystemExit('Create Maven dependency classpath in .local/identity-classpath.txt first.')
cp=cpfile.read_text(encoding='utf-8').strip();classes=root/'.local/feedback-classes';classes.mkdir(exist_ok=True)
subprocess.run([str(java.with_name('javac.exe' if os.name=='nt' else 'javac')),'-encoding','UTF-8','-cp',cp,'-d',str(classes),str(root/'tools/demo/FeedbackCopy.java'),str(root/'tools/demo/RepairDemoFeedback.java')],cwd=root,check=True)
subprocess.run([str(java),'-cp',str(classes)+os.pathsep+cp,'RepairDemoFeedback',*(['--apply'] if '--apply' in sys.argv else [])],cwd=root,check=True)
