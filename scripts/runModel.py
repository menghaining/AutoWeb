#!/usr/bin/env python
# encoding: utf-8

import os
cmds=["java -Xms30G -Xmx120G -jar ./webInfer.jar -appKind demo1 ./inputApps/webDemo/webdemo-app/ ./inputApps/webDemo/webdemo-log/0809.txt",
"java -Xms30G -Xmx120G -jar ./webInfer.jar -appKind shopizer ./inputApps/shopizer/shopizer-app/ ./inputApps/shopizer/shopizer-log/shopizer_0729.txt",
"java -Xms30G -Xmx120G -jar ./webInfer.jar -appKind webgoat /home/menghaining/webInfer/inputApps/webgoat7/webgoat-app/ /home/menghaining/webInfer/inputApps/webgoat7/webgoat-log/webgoat7_0729.txt",
"java -Xms30G -Xmx120G -jar ./webInfer.jar -appKind springblog /home/menghaining/webInfer/inputApps/Springblog/springblog-app/ /home/menghaining/webInfer/inputApps/Springblog/springblog-log/springblog_0729.txt",
"java -Xms30G -Xmx120G -jar ./webInfer.jar -appKind pybbs /home/menghaining/webInfer/inputApps/pybbs/pybbs-app/ /home/menghaining/webInfer/inputApps/pybbs/pybbs-log/log_pybbs_0726.txt",
"java -Xms30G -Xmx120G -jar ./webInfer.jar -appKind jpetstore /home/menghaining/webInfer/inputApps/jpetstore/jpetstore-app/ /home/menghaining/webInfer/inputApps/jpetstore/jpetstore-log/0816.txt",
"java -Xms30G -Xmx120G -jar ./webInfer.jar -appKind ruoyi /home/menghaining/webInfer/inputApps/ruoyi/ruoyi-app/ /home/menghaining/webInfer/inputApps/ruoyi/ruoyi-log/ruoyi0816.txt",
"java -Xms30G -Xmx120G -jar ./webInfer.jar -appKind imooc /home/menghaining/webInfer/inputApps/imooc/imooc-app/ /home/menghaining/webInfer/inputApps/imooc/imooc-log/0816.txt",
"java -Xms30G -Xmx120G -jar ./webInfer.jar -appKind openkm ./inputApps/openkm/openkm-app/ ./inputApps/openkm/openkm-log/0824.txt",
"java -Xms30G -Xmx120G -jar ./webInfer.jar -appKind icloud ./inputApps/icloud/icloud-app/ ./inputApps/icloud/icloud-log/icloud_0729.txt",
"java -Xms30G -Xmx120G -jar ./webInfer.jar -appKind opencms ./inputApps/opencms/opencms-app/ ./inputApps/opencms/opencms-log/opemcms_0729.txt"]

for cmd in cmds:
    print(cmd)
    os.system(cmd)

