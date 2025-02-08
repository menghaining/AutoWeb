#!/usr/bin/env python
# encoding: utf-8

import os
prifix = "java -Xms30G -Xmx120G -jar ./apply.jar ./FrameworkSemantics.json  "
option = " -insert -addEntries -cg zeroOne "
testcases=["/home/menghaining/workspace/doop_testcases/alfresco/alfresco-app/",
"/home/menghaining/workspace/doop_testcases/bitbucket-server/bitbucket-server-app/",
"/home/menghaining/workspace/doop_testcases/dotCMS/dotCMS-app/",
"/home/menghaining/workspace/doop_testcases/opencms/opencms-app/",
"/home/menghaining/workspace/doop_testcases/pybbs/pybbs-app/",
"/home/menghaining/workspace/doop_testcases/shopizer/shopizer-app/",
"/home/menghaining/workspace/doop_testcases/SpringBlog/SpringBlog-app/",
"/home/menghaining/workspace/doop_testcases/WebGoat/WebGoat-app/"]


for file in testcases:
    cmd = prifix + file + option
    print("[begin]" + cmd)
    print("[file]"+file)
    os.system(cmd)