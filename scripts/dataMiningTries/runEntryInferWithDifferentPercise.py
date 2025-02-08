import sys
import os

filename = sys.argv[1]
type = sys.argv[2]

for  i in range(0,101,5):
    threshold = i/100
    
    cmd = "python ./runInfer.py " + filename + " " + type + " " + str(threshold) + " > ./0306-entry-fulllog-" + str(i) + ".txt"
    print(cmd)
    os.system(cmd)
