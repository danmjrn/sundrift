#!/usr/bin/env python3
"""App-scoped operator deployment; immutable source, private config and bounded edge changes."""
from pathlib import Path
import fcntl,hashlib,json,os,re,secrets,subprocess,sys,tarfile,time,urllib.request
BASE=Path('/opt/sundrift');SHARED=BASE/'shared';DOMAIN='sundrift.shapeit.solutions'
action=sys.argv[1];sha=sys.argv[2] if len(sys.argv)>2 else (BASE/'current').resolve().name
assert os.geteuid()==0 and re.fullmatch('[0-9a-f]{40}',sha)
BASE.mkdir(mode=0o750,exist_ok=True);SHARED.mkdir(mode=0o700,exist_ok=True)
lock=open(SHARED/'deploy.lock','a');fcntl.flock(lock,fcntl.LOCK_EX)
release=BASE/'releases'/sha

def run(args,**kwargs):
    return subprocess.run(args,check=True,**kwargs)
def compose(at,*args):
    env=dict(os.environ,SOURCE_REVISION=at.name,COMPOSE_PARALLEL_LIMIT='1')
    return run(['docker','compose','--project-name','sundrift','--env-file',str(SHARED/'runtime.env'),'-f',str(at/'compose.yml'),'-f',str(at/'infrastructure/production.yml'),*args],cwd=at,env=env)
def backup():
    current=(BASE/'current').resolve();stamp=time.strftime('%Y%m%dT%H%M%SZ',time.gmtime());out=SHARED/'backups'/stamp;out.mkdir(parents=True,mode=0o700)
    recipients=json.loads((SHARED/'recipients.json').read_text());age=['age',*[v for r in recipients for v in ['-r',r]]]
    env=dict(os.environ,SOURCE_REVISION=current.name)
    pg=subprocess.Popen(['docker','compose','-p','sundrift','--env-file',str(SHARED/'runtime.env'),'-f',str(current/'compose.yml'),'-f',str(current/'infrastructure/production.yml'),'exec','-T','database','pg_dump','-U','sundrift','-Fc','sundrift'],stdout=subprocess.PIPE,env=env)
    run([*age,'-o',str(out/'database.dump.age')],stdin=pg.stdout);pg.stdout.close();assert pg.wait()==0
    media=Path(subprocess.check_output(['docker','volume','inspect','sundrift_media','--format','{{.Mountpoint}}'],text=True).strip())
    tar=subprocess.Popen(['tar','-czf','-','-C',str(media),'.'],stdout=subprocess.PIPE)
    run([*age,'-o',str(out/'media.tar.gz.age')],stdin=tar.stdout);tar.stdout.close();assert tar.wait()==0
    receipt={'source':current.name,'files':{p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in out.iterdir()}}
    (out/'receipt.json').write_text(json.dumps(receipt,indent=2)+'\n');print('Encrypted app backup:',stamp)

if action=='prepare':
    recovery=sys.argv[3];assert re.fullmatch('age1[0-9a-z]+',recovery)
    assert not release.exists(),'Release already staged'
    release.mkdir(parents=True,mode=0o750)
    with tarfile.open(fileobj=sys.stdin.buffer,mode='r|gz') as archive:archive.extractall(release,filter='data')
    key=SHARED/'age-key.txt'
    if not key.exists():run(['age-keygen','-o',str(key)],stdout=subprocess.PIPE,stderr=subprocess.PIPE);key.chmod(0o600)
    host=subprocess.check_output(['age-keygen','-y',str(key)],text=True).strip()
    recipients=[host,recovery];(SHARED/'recipients.json').write_text(json.dumps(recipients))
    runtime=SHARED/'runtime.env'
    if not runtime.exists():
        runtime.write_text('DATABASE_PASSWORD='+secrets.token_urlsafe(36)+'\nSPRING_PROFILES_ACTIVE=demo\nCOOKIE_SECURE=true\nPUBLIC_BIND=127.0.0.1\nPUBLIC_PORT=3110\nPUBLIC_ORIGIN=https://'+DOMAIN+'\n');runtime.chmod(0o600)
        run(['age','-r',host,'-r',recovery,'-o',str(SHARED/'runtime.env.age'),str(runtime)])
    (release/'provenance.json').write_text(json.dumps({'source':sha,'runtime_configuration_sha256':hashlib.sha256(runtime.read_bytes()).hexdigest()},indent=2)+'\n')
    print('Staged exact Sundrift source:',sha)
elif action=='build':
    available=int(next(x.split()[1] for x in Path('/proc/meminfo').read_text().splitlines() if x.startswith('MemAvailable:')))
    assert available>4*1024*1024,'Insufficient memory for isolated sequential builds'
    compose(release,'build','backend');compose(release,'build','frontend')
elif action=='activate':
    previous=(BASE/'current').resolve() if (BASE/'current').exists() else None
    if previous:backup()
    try:compose(release,'up','-d','--wait','--wait-timeout','240')
    except Exception:
        if previous:compose(previous,'up','-d','--no-build','--wait')
        raise
    pending=BASE/'current.pending';pending.symlink_to(release);pending.replace(BASE/'current')
    if previous:(SHARED/'previous-release').write_text(str(previous))
    print('Healthy Sundrift source activated:',sha)
elif action=='edge':
    assert (BASE/'current').resolve()==release
    locks=[]
    for path in ['/run/lock/shapeit-host-deploy.lock','/run/lock/shapeit-host.lock']:
        f=open(path,'a');fcntl.flock(f,fcntl.LOCK_EX);locks.append(f)
    target=Path('/etc/nginx/sites-available')/(DOMAIN+'.conf');enabled=Path('/etc/nginx/sites-enabled')/target.name
    peers={str(p.resolve()):hashlib.sha256(p.read_bytes()).hexdigest() for p in enabled.parent.iterdir() if p.name!=target.name}
    previous=target.read_bytes() if target.exists() else None
    http=f'server {{ listen 80; server_name {DOMAIN}; location ^~ /.well-known/acme-challenge/ {{ root /var/www/certbot; }} location / {{ return 308 https://$host$request_uri; }} }}\n'
    try:
        target.write_text(http)
        if not enabled.exists():enabled.symlink_to(target)
        run(['nginx','-t']);run(['systemctl','reload','nginx'])
        run(['certbot','certonly','--webroot','-w','/var/www/certbot','--cert-name',DOMAIN,'-d',DOMAIN,'--non-interactive','--keep-until-expiring'],stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
        tls=f'''server {{
listen 443 ssl; server_name {DOMAIN};
ssl_certificate /etc/letsencrypt/live/{DOMAIN}/fullchain.pem;
ssl_certificate_key /etc/letsencrypt/live/{DOMAIN}/privkey.pem;
client_max_body_size 6m;
add_header Strict-Transport-Security "max-age=31536000" always;
add_header X-Content-Type-Options nosniff always;
location / {{ proxy_pass http://127.0.0.1:3110; proxy_set_header Host $host; proxy_set_header X-Forwarded-Host $host; proxy_set_header X-Forwarded-Proto https; proxy_set_header X-Forwarded-For $remote_addr; proxy_read_timeout 60s; }}
}}\n'''
        target.write_text(http+tls);run(['nginx','-t']);run(['systemctl','reload','nginx'])
        assert all(hashlib.sha256(Path(p).read_bytes()).hexdigest()==v for p,v in peers.items()),'Peer edge changed'
        (SHARED/'edge-receipt.json').write_text(json.dumps({'source':sha,'domain':DOMAIN,'unchanged_peer_configs':len(peers),'tls_config_sha256':hashlib.sha256(target.read_bytes()).hexdigest()},indent=2)+'\n')
    except Exception:
        if previous is None:
            enabled.unlink(missing_ok=True);target.unlink(missing_ok=True)
        else:target.write_bytes(previous)
        run(['nginx','-t']);run(['systemctl','reload','nginx']);raise
    print('TLS route active; peer configurations unchanged:',len(peers))
elif action=='backup':backup()
elif action=='restart':compose(release,'restart','backend');compose(release,'up','-d','--wait','backend')
elif action=='rollback':
    previous=Path((SHARED/'previous-release').read_text().strip());assert previous.parent==BASE/'releases'
    backup();compose(previous,'up','-d','--no-build','--wait')
    pending=BASE/'current.pending';pending.symlink_to(previous);pending.replace(BASE/'current')
    print('Previous code/images restored; database migrations are not reversed:',previous.name)
else:raise SystemExit('Unsupported app operation')
