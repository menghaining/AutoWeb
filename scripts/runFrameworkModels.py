#!/usr/bin/env python
# encoding: utf-8

import os
import time

input_root="/home/menghaining/webInfer/input-apps/details"
cmd_java_option="java -Xms30G -Xmx120G -jar ./model1.jar  "
# in input-apps directory
#appKind=["jpetstore","ruoyi","imooc","openkm","lms","halo","icloud","newssystem","community","newbee","logicaldoc"]



time_start=time.time()

# 1. all inputs
for t in  os.listdir(input_root):
    if "struts2-examples" in t:
        s_root=os.path.join(input_root,t)
        for s_t in  os.listdir(s_root):
            s_app=os.path.join(s_root,s_t,"jars","app")
            s_lib=os.path.join(s_root,s_t,"jars","lib")
            # TO modify!!
            s_log=os.path.join(s_root,s_t,"logs",os.listdir(os.path.join(s_root,s_t,"logs"))[0])
            s_cmd=cmd_java_option + s_app + " -appKind " + s_t + " -libs " + s_lib + " " +s_log
            
           # print(s_cmd)
            os.system(s_cmd)
    else:
        app=os.path.join(input_root,t,"jars","app")
        lib=os.path.join(input_root,t,"jars","lib")
        # TO modify!!
        log=os.path.join(input_root,t,"logs",os.listdir(os.path.join(input_root,t,"logs"))[0])
        
        cmd=cmd_java_option + app + " -appKind " + t + " -libs " + lib + " " + log
       # print(cmd)
        os.system(cmd)

# 2. demo1/ in new version, this merged in 1-else
#cmd2="java -Xms30G -Xmx120G -jar ./modelInfer.jar -appKind demo1 /home/menghaining/webInfer/old-inputApps/webDemo/webdemo-app/ /home/menghaining/webInfer/old-inputApps/webDemo/webdemo-log/0809.txt"
#print(cmd2)
# os.system(cmd2)

time_end=time.time()
print('totally cost: ',time_end-time_start)