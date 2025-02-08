#!/usr/bin/env python
# encoding: utf-8
import sys

result_file = sys.argv[1]
with open(result_file,'r',encoding='UTF-8') as f:
    lines = f.readlines()
    flag = False
    for line in lines:
        if line.startswith("[entry]"):
            flag=True
            continue
        if line.startswith("[inject"):
            flag=False
            continue
        if flag:
            line = line.strip()
            strs = line.split("\t")
            clazz = strs[0]
            method = strs[1]
            if not "("  in method:
                # not inheritance
                operation = "\"operation\": \"entry_point\""
                if "/" in method:
                    type = "\"type\":\"xml\""
                else:
                    type = "\"type\":\"anno\""
                config1 = "\"config1\": \"" + clazz + "\""
                config2 = "\"config2\": \"" + method + "\""
                jsonstr = "{" + operation + "," + type + "," + config1 + "," + config2  + "},"
                print(jsonstr)