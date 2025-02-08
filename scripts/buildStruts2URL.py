#!/usr/bin/env python
# encoding: utf-8

import os
import sys

path=sys.argv[1]

for f in  os.listdir(path):
    if os.path.isdir(os.path.join(path,f)):
        print("http://localhost:8080/"+f)