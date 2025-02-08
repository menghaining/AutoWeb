import sys

# 最终结果输出格式
def printAnswer(result):
    for ele in result:
        ele.sort()
    result.sort()
    i = 0
    for ele in result:
        i = i+1
        print(str(i)+'. '+str(ele))

# whether collection1 contains any subset of ele
# collection1=[[]], ele=[]
def already_contains_subset(collection1, ele):
    for item in collection1:
        if len(item) < len(ele):
            if set(item).issubset(set(ele)):
                return True
    return False

def generate(candidate_is2):
    ret = []
    for i in candidate_is2:
        if len(i) == 1:
            ret.append(i)
        else:
            if already_contains_subset(ret, i):
                continue
            else:
                ret.append(i)
    print('------field Inject-------')
    printAnswer(ret)


file_path = sys.argv[1]
file = open(file_path)
list = []
for line in file:
    tmp = []
    line = line.strip('\n')
    # print()
    strlist = line.split(',')
    for i in strlist:
        if not i==',':
            # print(i)
            tmp.append(i)
    if not len(tmp) == 0:
        list.append(tmp)
list.sort()
for tmp in list:
    if len(tmp) == 1:
        print(tmp[0])
    # print('******')
# generate(list)
