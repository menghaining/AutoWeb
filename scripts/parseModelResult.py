#!/usr/bin/env python
# encoding: utf-8

import os
import sys
import json

result_file = sys.argv[1]

spring = []
struts = []
javaee = []
gwt = []
other = []
with open(result_file,'r',encoding='UTF-8') as load_f:
    records = json.load(load_f)
    for record in records:
        kind = record['kind']
        if kind == 'Entry':
            methodMarks = record['methodMarks']
            if len(methodMarks):
                for m in methodMarks:
                    if m.startswith('[anno]Lorg/springframework/') or m.startswith('[inheritance]org.springframework.'):
                        spring.append(record)
                        break
                    elif m.startswith('[anno]Ljavax/') or m.startswith('[inheritance]javax.'):
                        javaee.append(record)
                        break
                    elif m.startswith('[inheritance]org.apache.struts2.') or m.startswith('[inheritance]com.opensymphony.xwork2.') or m.startswith('[anno]Lorg/apache/struts2') or m.startswith('[anno]Lcom/opensymphony/xwork2/'):
                        struts.append(record)
                        break
                    else:
                        other.append(record)
            else:
                other.append(record)

def printDetails(s):
    print('classMarks:')
    print(s['classMarks'])
    print('methodMarks:')
    print(s['methodMarks'])
    print()
    
print("========spring========")
for s in spring:
    printDetails(s)
print("========spring========" + str(len(spring)))
print("========struts2========")
for s in struts:
    printDetails(s)
print("========struts2========" + str(len(struts)))
print("========javaee========")
for s in javaee:
    printDetails(s)
print("========javaee========" + str(len(javaee)))
print("========other========")
for s in other:
    printDetails(s)
print("========other========" + str(len(other)))


    
    