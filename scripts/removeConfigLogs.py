#!/usr/bin/env python

import os
import sys

def diffFile(configFile, fullFile, out):
    file1 = open(configFile,'r+', errors='ignore') 
    file2 = open(fullFile,'r+', errors='ignore') 
    fc=open(out,'w+')

    lines1 = file1.readlines()
    lines2 = file2.readlines()
    print("lines1: " + str(len(lines1)))
    print("lines2: " + str(len(lines2)))

    i = 0
    while(i < len(lines1)):
        line1 = lines1[i]
        line2 = lines2[i]
        if(line1 == line2):
            i = i + 1
        else:
            if (i == (len(lines1))) and (line1 == "\n") :
                print('WARNING-1')
                break
            else :
                print('WARNING-2')
                i = i+1
                # line1.encode('gbk')
                # line2.encode('gbk')
                # if line1 == line2:
                #     i = i+1
                #     print("encode!")
                # else:
                #     print("ERROR!! " + str(i+1))
                #     break

    while(i < len(lines2)):
        line2 = lines2[i]
        fc.write(line2)
        i = i + 1

    file1.close()
    file2.close()
    fc.close()

if __name__ == '__main__':
    input_root = sys.argv[1]
    for t in os.listdir(input_root):
        logdir = os.path.join(input_root, t, "logs","0120")
        
        i = 1
        while(i < 4):
            fullFile = os.path.join(logdir, t+"_log"+str(i)+".txt")
            configFile = os.path.join(logdir, t+"_log_deploy"+str(i)+".txt")
            out = os.path.join(logdir, t+"_log_without_deploy"+str(i)+".txt")
            
            if os.path.exists(fullFile) and os.path.exists(configFile):
                print("...in ...[" + t + "] " + str(i))
                diffFile(configFile, fullFile, out)
            else:
                print('ERROR: NOT EXIST ')
            
            i = i + 1

    # configFile = sys.argv[1]
    # fullFile = sys.argv[2]
    # out = sys.argv[3]
    # diffFile(configFile, fullFile, out)