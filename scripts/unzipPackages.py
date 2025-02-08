#!/usr/bin/env python
# encoding: utf-8

import os

curr_path = os.getcwd()

for root, dirs, files in os.walk(curr_path):
    for filename in files:
        src = os.path.join(root,filename)
        if(src.endswith(".jar")):
            cmd = "unzip " + src + " -d ./" + filename[0:-4]
            print(cmd)
            os.system(cmd)