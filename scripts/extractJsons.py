#!/usr/bin/env python
# encoding: utf-8

import os
import shutil
import sys


def findAllJsonFiles(path,target):
    count=0
    for d in os.listdir(path):
        dd=os.path.join(path,d)
        file=os.path.join(dd,(d+".json"))
        if os.path.exists(file):
            print(file)
            count=count+1
            tar=os.path.join(target,"alls")
            if not os.path.exists(tar):
                os.mkdir(tar)
            shutil.copy(file,tar)
                
                
            
    print("total json files: " + str(count))
        
def main():
    path=sys.argv[1]
    target=sys.argv[2]
    findAllJsonFiles(path,target)

if __name__ == '__main__':
    main()		
    
    

