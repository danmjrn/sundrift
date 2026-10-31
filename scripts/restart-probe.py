#!/usr/bin/env python3
"""Create a synthetic confirmed booking, then verify it with the same cookie after restart."""
import http.cookiejar
import json
import sys
import urllib.request
from datetime import datetime, timedelta
from zoneinfo import ZoneInfo
from pathlib import Path

base = sys.argv[2] if len(sys.argv) > 2 else 'http://localhost:3000'
state = Path('.runtime/restart-probe.json')
jar_path = Path('.runtime/restart-probe.cookies')
jar = http.cookiejar.LWPCookieJar(str(jar_path))
if jar_path.exists():
    jar.load(ignore_discard=True)
opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))

def call(path, payload=None, key=None):
    headers = {}
    if payload is not None:
        token = call('/auth/csrf')
        headers[token['headerName']] = token['token']
        headers['Content-Type'] = 'application/json'
    if key:
        headers['Idempotency-Key'] = key
    request = urllib.request.Request(base + '/api/v1' + path,
        data=json.dumps(payload).encode() if payload is not None else None, headers=headers)
    with opener.open(request) as response:
        result = json.load(response)
    jar.save(ignore_discard=True)
    jar_path.chmod(0o600)
    return result

if sys.argv[1] == 'create':
    import uuid
    call('/demo/enter', {'role': 'GUEST'})
    stay = call('/catalogue?destination=Cape%20Town')['items'][0]
    arrival = datetime.now(ZoneInfo('Africa/Johannesburg')).date() + timedelta(days=90)
    quote = call('/quotes', {'listingId': stay['id'], 'checkIn': str(arrival),
        'checkOut': str(arrival + timedelta(days=2)), 'guests': 2})
    booking = call('/bookings', {'quoteId': quote['id']}, str(uuid.uuid4()))
    booking = call('/bookings/' + booking['id'] + '/confirm',
        {'scenario': 'SUCCESS', 'policyAccepted': True}, str(uuid.uuid4()))
    assert booking['status'] == 'CONFIRMED'
    state.write_text(json.dumps({'id': booking['id'], 'reference': booking['reference'], 'total': booking['total']}))
    print('Created synthetic confirmed reservation:', booking['reference'])
else:
    before = json.loads(state.read_text())
    after = call('/bookings/' + before['id'])
    assert after['status'] == 'CONFIRMED'
    assert after['reference'] == before['reference'] and after['total'] == before['total']
    print('PASS: authenticated session, reservation, reference and agreed total survived restart:', after['reference'])
