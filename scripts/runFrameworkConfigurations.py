
#!/usr/bin/env python
# encoding: utf-8

from genericpath import exists
import os
import time

def run_all_logs(app, lib, kind, out_name):
    times = 3
    runTimes = 0

    i = 1
    while(i < times + 1):
        logFile = os.path.join(logdir, kind + "_log" + str(i) + ".txt")
        cmd = cmd_java_option + app + " -appKind " + kind + " -output "  + out_name + str(i) + " -libs " + lib + " " + logFile
        print(cmd)
        i = i + 1
        if os.path.exists(logFile):
            # print("exist!")
            runTimes = runTimes + 1
        os.system(cmd)
    return runTimes

input_root = "/home/menghaining/webInfer/input-apps"
cmd_java_option = "java -Xms30G -Xmx120G -jar ./handleMain-0305.jar  "

logSubDir = "0120"


time_start = time.time()

runTimes = 0
for t in os.listdir(input_root):
    app = os.path.join(input_root, t, "jars", "app")
    lib = os.path.join(input_root, t, "jars", "lib")

    logdir = os.path.join(input_root, t, "logs",logSubDir)

    if (t == 'apache-struts2-examples') or  (t == 'apache-struts2-vulns'):
        for sub in os.listdir(app):
            print(sub)
            sub_app = os.path.join(app, sub)
            runTimes = runTimes + run_all_logs(sub_app, lib, t, ('apache-struts2-examples-'+sub))
    else:
        runTimes = runTimes + run_all_logs(app, lib, t, t)
    

time_end = time.time()
print('totally cost: ', time_end-time_start)
print('totally run times: ' + str(runTimes))
