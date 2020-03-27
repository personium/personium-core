#!/usr/bin/env python3

description = []

with open('CHANGELOG.md') as f:
  count = 0
  for line in f:
    if line.startswith('#'):
      count += 1
      if count == 1:
        version = line.replace('## ', '')
      elif count >= 2:
        break
    else:
      description.append(line)

print(version)
print(''.join(description))

import urllib.request
import base64
import json
import os

url = 'https://api.github.com/repos/personium/%s/releases' % os.environ['COMPONENT']
user = os.environ['GITHUB_USER']
password = os.environ['GITHUB_TOKEN']
basic_user_and_pasword = base64.b64encode('{}:{}'.format(user, password).encode('utf-8'))
headers = {
  'Content-Type': 'application/json',
  'Authorization': 'Basic %s' % basic_user_and_pasword.decode('utf-8'),
}
data = json.dumps({
  'tag_name': 'v%s' % version,
  'target_commitish': 'master',
  'name': 'Release v%s' % version,
  'body': ''.join(description),
  'draft': True,
  'prerelease': False,
}).encode('utf-8')

req = urllib.request.Request(url, data, headers)
with urllib.request.urlopen(req) as res:
    body = json.loads(res.read().decode('utf-8'))
print(body)
print('Succeeded!')
