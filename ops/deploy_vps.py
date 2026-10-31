#!/usr/bin/env python3
"""Deploy only exact pushed Sundrift source; keep the existing operator identity local."""
from pathlib import Path
import argparse,subprocess,shlex
root=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser();p.add_argument('action',choices=['prepare','build','activate','edge','backup','restart','rollback']);p.add_argument('--identity',type=Path,required=True);p.add_argument('--recovery-recipient');args=p.parse_args()
def git(*a):return subprocess.check_output(['git','-C',str(root),*a],text=True).strip()
sha=git('rev-parse','HEAD');assert not git('status','--porcelain'),'Commit the reviewed release before deploying'
assert git('remote','get-url','origin')=='git@github.com:danmjrn/sundrift.git'
branch=git('branch','--show-current');assert git('ls-remote','origin','refs/heads/'+branch).split()[0]==sha,'Push exact source before deployment'
command=['sudo','-n','python3','-c',(root/'ops/vps_release.py').read_text(),args.action,sha]
if args.action=='prepare':assert args.recovery_recipient;command.append(args.recovery_recipient)
ssh=['ssh','-T','-i',str(args.identity),'-o','BatchMode=yes','-o','IdentitiesOnly=yes','-o','StrictHostKeyChecking=yes','-o','UserKnownHostsFile='+str(root/'ops/known_hosts'),'wethrive-deploy@84.247.128.207',shlex.join(command)]
if args.action=='prepare':
    archive=subprocess.Popen(['git','-C',str(root),'archive','--format=tar.gz',sha],stdout=subprocess.PIPE)
    subprocess.run(ssh,stdin=archive.stdout,check=True);archive.stdout.close();assert archive.wait()==0
else:subprocess.run(ssh,check=True)
