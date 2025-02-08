import os
# 488 + 43
logs_dir = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\struts2-official-samples'

# 60 + 16
logs_dir2 = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\struts2-vulns'

count1 = 0
count2 = 0
for name in os.listdir(logs_dir):
    print(name)
    testcases_dir = os.path.join(logs_dir, name, 'out-preModify')
    dtestcases_dir = os.path.join(logs_dir, name, 'deploy', 'out-preModify')
    for name in os.listdir(testcases_dir):
        print(name)
        count1 = count1 + 1
    for name in os.listdir(dtestcases_dir):
        print(name)
        count2 = count2 + 1
print(count1)
print(count2)