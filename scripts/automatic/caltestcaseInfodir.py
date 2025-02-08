import os

# path = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.infer\outs\\testcaseInfo\struts2-offical'
# path = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.infer\outs\\testcaseInfo\struts2-vulns'


def printTestcaseInfo():
    path = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.infer\outs\\testcaseInfo'
    app = 0
    all = 0
    reachable = 0
    
    clMarks_all = []
    clMarks_reachable = []
    mtdMarks_all = []
    mtdMarks_reachable = []
    fMarks_all = []
    fMarks_reachable = []

    names=[]
    rechs = []
    for name in os.listdir(path):
        file = os.path.join(path, name)
        with open(file, 'r', encoding='utf-8', errors='ignore') as fp:
                totalMarks_class = []
                reachableMarks_class = []
                totalMarks_method = []
                reachableMarks_method = []
                totalMarks_field = []
                reachableMarks_field = []
                lines = fp.readlines()
                for j in range(lines.index('[all class marks]\n')+1, lines.index('[reachable class marks]\n')):
                    l = lines[j].strip()
                    if not l in clMarks_all:
                        clMarks_all.append(l)
                    if not l in totalMarks_class:
                        totalMarks_class.append(l)
                for j in range(lines.index('[reachable class marks]\n')+1, lines.index('[all method marks]\n')):
                    l = lines[j].strip()
                    if not l in clMarks_reachable:
                        clMarks_reachable.append(l)
                    if not l in reachableMarks_class:
                        reachableMarks_class.append(l)
                for j in range(lines.index('[all method marks]\n')+1, lines.index('[reachable method marks]\n')):
                    l = lines[j].strip()
                    if not l in mtdMarks_all:
                        mtdMarks_all.append(l)
                    if not l in totalMarks_method:
                        totalMarks_method.append(l)
                for j in range(lines.index('[reachable method marks]\n')+1, lines.index('[all field marks]\n')):
                    l = lines[j].strip()
                    if not l in mtdMarks_reachable:
                        mtdMarks_reachable.append(l)
                    if not l in reachableMarks_method:
                        reachableMarks_method.append(l)
                for j in range(lines.index('[all field marks]\n')+1, lines.index('[reachable field marks]\n')):
                    l = lines[j].strip()
                    if not l in fMarks_all:
                        fMarks_all.append(l)
                    if not l in totalMarks_field:
                        totalMarks_field.append(l)
                for j in range(lines.index('[reachable field marks]\n')+1, len(lines)):
                    l = lines[j].strip()
                    if not l in fMarks_reachable:
                        fMarks_reachable.append(l)
                    if not l in reachableMarks_field:
                        reachableMarks_field.append(l)

                line1 = lines[0].strip()
                line2 = lines[1].strip()
                line3 = lines[2].strip()
                app_classes = int(line1[14:])
                all_classes = int(line2[14:])
                reachable_classes = int(line3[24:])
                app = app + app_classes
                all = all + all_classes
                reachable = reachable + reachable_classes
                names.append(name)
                print(name + ':' + str(all_classes) + ':' + str(app_classes) + ':' + str(reachable_classes) + ":" + str(len(totalMarks_class) + len(totalMarks_method) +len(totalMarks_field)) + ':' + str(len(reachableMarks_class)+len(reachableMarks_method)+len(reachableMarks_field)))
                
#                 # print(name + '\t' + line1[14:] + '\t' + line2[14:] + '\t' + line3[24:])
#     # for i in names:
#     #     print(i)
#     # for i in rechs:
#     #     print(i)
#     print(app)
#     print(all)
#     print(reachable)
#     print(clMarks_all)
#     print(clMarks_reachable)
#     print(mtdMarks_all)
#     print(mtdMarks_reachable)
#     print(fMarks_all)
#     print(fMarks_reachable)
#     # print(len(clMarks_all))
#     # print(len(clMarks_reachable))
#     # print(len(mtdMarks_all))
#     # print(len(mtdMarks_reachable))
#     # print(len(fMarks_all))
#     # print(len(fMarks_reachable))

# printTestcaseInfo()
# path = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.infer\outs\\runnable\struts2-vulns'
# path='F:\myProject\webframeworkmodelinfer\ict.pag.webframework.infer\outs\\runnable\struts2-offical'


def printRunnableInfo():
    # path='F:\myProject\webframeworkmodelinfer\ict.pag.webframework.infer\outs\\runnable-23-0202-without-jackEE'
    path = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.infer\outs\\runnable-23-0202-no-jackee-no-not_entryClass\struts2-samples'
    reachable_cl = 0
    reachable_mtd = 0
    reachable_f=0
    actual_cl = 0
    actual_mtd = 0
    actual_f=0
    for name in os.listdir(path):
        # if(not name.endswith('_diff.txt')):
        #     print(name)
        #     file = os.path.join(path, name)
        #     with open(file, 'r', encoding='utf-8', errors='ignore') as fp:
        #         lines = fp.readlines()
        #         for j in range(lines.index('[entry]\n')+1, lines.index('[inject on field]\n')):
        #             l = lines[j].strip()
        #             print(l)
        #         for j in range(lines.index('[inject on field]\n')+1, lines.index('[inject on method]\n')):
        #             l = lines[j].strip()
        #             print(l)
        #         for j in range(lines.index('[inject on method]\n')+1, lines.index('[field to target]\n')):
        #             l = lines[j].strip()
        #             print(l)
        #         for j in range(lines.index('[field to target]\n')+1, lines.index('[framework call to target]\n')):
        #             l = lines[j].strip()
        #             print(l)
        #         for j in range(lines.index('[framework call to target]\n')+1, len(lines)):
        #             l = lines[j].strip()
        #             print(l)
        if name.endswith('_diff.txt'):
            print(name)
            file = os.path.join(path, name)
            with open(file, 'r', encoding='utf-8', errors='ignore') as fp:
                lines = fp.readlines()
                for line in lines:
                    if '[Reachable Class Marks]' in line:
                        print(line)
                        print(line[24:])
                        reachable_cl = reachable_cl + int(line[24:])
                    if '[Reachable Method Marks]' in line:
                        print(line)
                        print(line[25:])
                        reachable_mtd = reachable_mtd + int(line[25:])
                    if '[Reachable Field Marks]' in line:
                        print(line)
                        print(line[24:])
                        reachable_f = reachable_f + int(line[24:])
                    if '[Actual Class Marks]' in line:
                        print(line)
                        print(line[21:])
                        actual_cl = actual_cl + int(line[21:])
                    if '[Actual Method Marks]' in line:
                        print(line)
                        print(line[22:])
                        actual_mtd = actual_mtd + int(line[22:])
                    if '[Actual Field Marks]' in line:
                        print(line)
                        print(line[21:])
                        actual_f = actual_f + int(line[21:])
                print('--Reachable class more:')
                for j in range(lines.index('[Reachable class marks more]\n')+1, lines.index('[Actual class marks more]\n')):
                    l = lines[j].strip()
                    print(l)
                print('--Reachable method more:')
                for j in range(lines.index('[Reachable method marks more]\n')+1, lines.index('[Actual method marks more]\n')):
                    l = lines[j].strip()
                    print(l)
                print('--Reachable field more:')
                for j in range(lines.index('[Reachable field marks more]\n')+1, lines.index('[Actual field marks more]\n')):
                    l = lines[j].strip()
                    print(l)
                extra_mtds = len(range(lines.index('[Actual method marks more]\n')+1, lines.index('[Reachable field marks more]\n')))
                print('--Extra methods more:')
                print(extra_mtds)
                actual_mtd = actual_mtd - extra_mtds
    print('======================')
    print(reachable_cl)
    print(reachable_mtd)
    print(reachable_f)
    print(actual_cl)
    print(actual_mtd)
    print(actual_f)

printRunnableInfo()
# printTestcaseInfo()