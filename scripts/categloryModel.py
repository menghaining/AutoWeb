#!/usr/bin/env python
# encoding: utf-8

import os
import sys
import json

result_file = sys.argv[1]

def printMarksSet(s):
    for m in s:
        #if (("]Lorg/springframework" in m) or ("]org.springframework." in m) or ("[xml]" in m)):
        #if("struts2" in m or "opensymphony" in m or ("[xml]" in m)):
        #if (("]Ljavax/" in m) or ("]javax." in m) or ("[xml]" in m)):
        if (("gwt" in m) or ("google" in m) or ("[xml]" in m)):
            print(m)
            

with open(result_file,'r',encoding='UTF-8') as load_f:
    records = json.load(load_f)
    for record in records:
        kind = record['kind']
        if kind == 'Entry':
            methodMarks = record['methodMarks']
            printMarksSet(methodMarks)
            classMarks = record['classMarks']
            printMarksSet(classMarks)
        elif kind == 'Generate-Class-Alias':
            classMarks = record['classMarks']
            printMarksSet(classMarks)
        elif kind == 'Generate-Class':
            classMarks = record['classMarks']
            printMarksSet(classMarks)
        elif kind == 'Inject-Field':
            fieldMarks = record['fieldMarks']
            printMarksSet(fieldMarks)
        elif kind == 'Inject-Field-Points2':
            fieldMarks = record['fieldMarks']
            printMarksSet(fieldMarks)
            mark = record['objectFromAttribute']
            printMarksSet(mark)
        elif kind == 'IndirectCall':
            methodMarks = record['methodMarks']
            printMarksSet(methodMarks)




    
    