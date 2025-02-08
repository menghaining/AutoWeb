#!/usr/bin/env python
# encoding: utf-8

import os

cmd0="java -Xms30G -Xmx120G -jar ./modelInfer.jar "

cmd1s=["-appKind shopizer /home/menghaining/webInfer/old-inputApps/shopizer/shopizer-app/ /home/menghaining/webInfer/old-inputApps/shopizer/shopizer-log/shopizer_0729.txt",
"-appKind springblog /home/menghaining/webInfer/old-inputApps/Springblog/springblog-app/ /home/menghaining/webInfer/old-inputApps/Springblog/springblog-log/springblog_0729.txt",
"-appKind webgoat /home/menghaining/webInfer/old-inputApps/webgoat7/webgoat-app/ /home/menghaining/webInfer/old-inputApps/webgoat7/webgoat-log/webgoat7_0729.txt",
"-appKind pybbs /home/menghaining/webInfer/old-inputApps/pybbs/pybbs-app/ /home/menghaining/webInfer/old-inputApps/pybbs/pybbs-log/log_pybbs_0726.txt",
"-appKind opencms /home/menghaining/webInfer/old-inputApps/opencms/opencms-app/ /home/menghaining/webInfer/old-inputApps/opencms/opencms-log/opemcms_0729.txt"]

for cmd1 in cmd1s:
    cmd=cmd0+cmd1
    os.system(cmd)