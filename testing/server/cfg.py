# usage: cfg.py <config.yml> storage=mysql|yaml
import re, sys
path = sys.argv[1]
s = open(path).read()
for arg in sys.argv[2:]:
    k, v = arg.split('=')
    if k == 'storage':
        s = re.sub(r'(?m)^storage-type: .*$', f'storage-type: "{v}"', s)
    else:
        sys.exit(f'unknown setting: {k}')
open(path, 'w').write(s)
