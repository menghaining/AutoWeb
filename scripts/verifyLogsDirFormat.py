#!/usr/bin/env python
# encoding: utf-8

import os
import sys

path=sys.argv[1]

for f in os.listdir(path):
    jarPath=os.path.join(path,f,"jars")
    logPath=os.path.join(path,f,"logs")

    if not os.path.exists(jarPath):
        print("error jar path: " + jarPath)
    if not os.path.exists(os.path.join(jarPath,"app")):
        print("error jar app path: " + jarPath)
    if not os.path.exists(os.path.join(jarPath,"lib")):
        print("error jar lib path: " + jarPath)

    if not os.path.exists(logPath):
        print("error log path: " + logPath)
    if not os.path.exists(os.path.join(logPath,(f+"_log.txt"))):
        print("error log file format: " + os.path.join(logPath,(f+"_log.txt")))

    
    # for logf in os.listdir(logPath):
    #     if not logf.endswith("_log.txt"):
    #         print ("remove file " + logf)
    #         os.system("rm " + os.path.join(logPath,logf))
    