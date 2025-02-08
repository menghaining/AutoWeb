from re import I
from apriori2 import *  # 导入编写的算法文件
import pandas as pda
import sys

# 1. 使用Apriori算法计算频繁项集和最右项为1项的关联规则
# 2. 筛选出 最右项是 IS 的 confidence 和 lift
# 3. 根据置信度和提升度，筛选出符合策略的关联规则，即得到结果集合

# 最终结果输出格式
def outprintAnswer2File(result, threshold):
    f_path = "./res" + str(int(threshold*100)) + ".txt"
    f = open(f_path,"w")
    for ele in result:
        f.writelines(ele + '\n')
    f.close()
        
def dealWithAnswer(mtd_set, class_set, threshold):
    res = []
    for ele in class_set:
        if len(ele) == 1:
            if ele[0].startswith("[class][anno]") or ele[0].startswith("[class][xml]"):
                res.append(ele[0])
        else:
            print("!!!entry answer only one element!!!")
    for ele in mtd_set:
        if len(ele) == 1:
            if ele[0].startswith("[method][anno]") or ele[0].startswith("[method][xml]"):
                res.append(ele[0])
        else:
            print("!!!entry answer only one element!!!")
    outprintAnswer2File(res, threshold)

# 最终结果输出格式
def printAnswer(result):
    for ele in result:
        ele.sort()
    result.sort()
    i = 0
    for ele in result:
        i = i+1
        print(str(i)+'. '+str(ele))

# freq_set=[] 所有在ignore=[[]]中的子集，并起来，返回
def cal_remove_set(freq_set, ignore):
    rm = []
    for ig1 in ignore:
        if len(ig1) <= len(freq_set):
            if set(ig1).issubset(set(freq_set)):
                rm.extend(ig1)
    return rm

# 从freq=[]中删去(属于ignore=[[]]集合的子集), 返回得到的集合中没有子集属于ignore
def remove_ignore(freq, ignore_is, ignore_not):
    rm1 = cal_remove_set(freq, ignore_is)
    rm2 = cal_remove_set(freq, ignore_not)
    rm = rm1 + rm2
    return list(set(freq) - set(rm))

# 从freq=[]中删去(属于ignore=[[]]集合的子集), 返回得到的集合中没有子集属于ignore
# def remove_ignore(freq, ignore_is):
#     rm = cal_remove_set(freq, ignore_is)
#     return list(set(freq) - set(rm))

# i = [i1, i2, ...] contains method mark
def has_method(i):
    for ele in i :
        if str(ele).startswith('[method]'):
            return True
    return False

# whether collection1 contains any subset of ele
# collection1=[[]], ele=[]
def already_contains_subset(collection1, ele):
    for item in collection1:
        if len(item) < len(ele):
            if set(item).issubset(set(ele)):
                return True
    return False

def is_same_list(list1, list2):
    sam_values = set(list1) & set(list2)
    if len(sam_values) == len(set(list1)) == len(set(list2)):
        return True
    return False

# collection = [[]], ele = []
def already_has(collection1, ele):
    for item in collection1:
        if is_same_list(item, ele):
            return True
    return False
            
# if collection1 contains subset of ele,
# remove the subset from collection1
# collection1=[[]], ele=[]
def remove_from_subset(collection1, ele):
    ret = []
    for item in collection1:
        if len(item) < len(ele):
            if not set(item).issubset(set(ele)):
                ret.append(item)
    return ret

# 筛选策略：
# 3. 按照C1每项集合的size从小到大排序，进行如下操作：
#       1) 如果size = 1 && 是method上配置，直接append到result；
#       2) 如果size = 1 && 是class上的配置 && 其它C1的子项中不包含改配置，直接append到result；
#       3) 对于 t 属于C1，如果t中没有子集在result中 && (t有method配置 || t不是C1其它项的子集)， append到result；
#       4) 重复 3)，直到所有t都遍历完毕，得到最后的result集合
def infer_Entry(candidate_is2, threshold):
    ret = []
    min_only_class = []
    
    for i in candidate_is2:
        # print(i)
        if len(i) == 1:
            if has_method(i):
                ret.append(i)
            else:
                min_only_class.append(i)
        # else:
        #     if already_contains_subset(ret, i):
        #         continue
        #     else:
        #         if has_method(i):
        #             ret.append(i)
        #             # tmp = remove_from_subset(tmp, i)
        #         else:
        #             if already_contains_subset(min_only_class, i):
        #                 continue;
        #             else:
        #                 min_only_class.append(i)

    print('------method level-------')
    printAnswer(ret)
    print('------class  level-------')
    printAnswer(min_only_class)
    dealWithAnswer(ret, min_only_class, threshold)


def infer_Ijnect(candidate_is2):
    ret = []
    for i in candidate_is2:
        if len(i) == 1:
            ret.append(i)
        # else:
        #     if already_contains_subset(ret, i):
        #         continue
        #     else:
        #         ret.append(i)
    print('------field Inject-------')
    printAnswer(ret)

def infer_indirect_call(result,ms,threshold):
    res = []
    id = 1
    for rule in result.index:
        line = str(rule)
        marksSet = sorted(rule.split(ms))
        if 'NOT' in marksSet:
            continue
        if 'IS' in marksSet:
            marksSet.remove('IS')
        if(len(marksSet) == 2 or len(marksSet) == 3):
            stmt=''
            call_conf = ''
            tar_conf = ''
            for ele in marksSet:
                if ele.startswith("[stmt]"):
                    stmt = ele
                elif ele.startswith("[caller]"):
                    call_conf = ele
                elif ele.startswith("[target]"):
                    tar_conf = ele
            if (not stmt == '') and (not tar_conf == ''):
                tmp = []
                tmp2 = []
                if not call_conf =='':
                    tmp.append(call_conf)
                tmp.append(stmt)
                tmp.append(tar_conf)
                linked = ms.join(tmp)
                tmp.append('IS')
                linked_is = ms.join(tmp)
                tmp2.append(stmt)
                tmp2.append(tar_conf)
                linked2 = ms.join(tmp2)
                # print('--------' + linked)
                if  linked in result.index:
                    # if result['confidence'][linked] >= threshold and result['confidence'][linked_is] >= threshold:
                    if result['confidence'][linked_is] >= threshold:
                        if result['confidence'][linked2] <= result['confidence'][linked]:
                            if not linked2 in res:
                                res.append(linked2)
                        else:
                            if not linked in res:
                                res.append(linked)
                        # if not linked in res:
                        #     res.append(linked)
                            # print('add:' + str(tmp))
                        # print(str(id) + '. : ' + stmt + '[target]' + str(tar_conf) + '[caller]' + str(call_conf))
                        # print(str(id) + '. : ' + str(linked))
                        # id=id+1
    res.sort()
    for i in res:
        print(str(id) + '. : ' + str(i) + ' : ' + str(result['confidence'][i]))
        id=id+1
        

    # ret = []
    # id = 1
    # for group in candidate_is2:
    #     if len(group) >= 2 :
    #         stmt='-1'
    #         call_conf = []
    #         tar_conf = []
    #         for ele in group:
    #             if ele.startswith("[stmt]"):
    #                 stmt = ele
    #             elif ele.startswith("[caller]"):
    #                 call_conf.append(ele)
    #             elif ele.startswith("[target]"):
    #                 tar_conf.append(ele)
    #         if (not stmt == '-1') and (not len(tar_conf) == 0):
    #             print(str(id) + '. : ' + stmt + '[target]' + str(tar_conf) + '[caller]' + str(call_conf))
    #             id=id+1

def infer_points2(candidate_is2):
    isInject = []
    points2 = []

    ret = []
    print('----------------------------------')
    for i in candidate_is2:
        if len(i) == 1:
            if i[0].startswith("[field]"):
                isInject.append(i[0])
            if i[0].startswith("[alias]") or i[0].startswith("[value]"):
                if not (i[0] =="[value][Proxy]"):
                    # i[0] == "[value][Primordial]" or 
                    points2.append(i[0])

    count = 0
    for i in candidate_is2:
        if len(i) == 2:
            pair = []
            if i[0].startswith("[field]") and (i[1].startswith("[alias]") or i[1].startswith("[value]")):
                if i[0] in isInject :
                    if not ( i[1] =="[value][Proxy]"):
                        # i[1] == "[value][Primordial]" or
                        if i[1] in points2:
                            pair.append(i[0])
                            pair.append(i[1])
                            count=count+1
                            # print(str(count) + str(pair))
                            ret.append(pair)
            elif i[1].startswith("[field]") and (i[0].startswith("[alias]") or i[0].startswith("[value]")):
                if i[1] in isInject:
                    if not ( i[0] =="[value][Proxy]"):
                        if i[0] in points2:
                        # i[0] == "[value][Primordial]" or
                            pair.append(i[1])
                            pair.append(i[0])
                            count=count+1
                            # print(str(count) + str(pair))
                            ret.append(pair)

        # if len(i) == 1:
        #     ret.append(i)
        #     # if i[0].startswith("[field]"):
        #     #     ret.append(i[0])
        # else:
        #     if already_contains_subset(ret, i):
        #         continue
        #     else:
        #         ret.append(i)
        #         # if i[0].startswith("[field]"):
        #         #     ret.append(i[0])
    print('------Points  to-------')
    ret.sort()
    i = 0
    for ele in ret:
        i = i+1
        print(str(i)+'. '+str(ele))

    print('------field Inject-------')
    i = 0
    isInject.sort()
    for ele in isInject:
        i = i + 1
        print(str(i)+'. '+str(ele))  
    
    # printAnswer(ret_points2)

# allow 0.1 deviation
def myround(i):
    if i > 0.99:
        return 1
    return i

def apriori_cal(spt, cfd, filename, ms):
    data = pda.read_excel(filename)
    # s = data.shape
    # apriori algorithm
    # [result_conf, result_lift] = find_rule(data, spt, cfd, "→")
    result = find_rule(data, spt, cfd, ms)
    # result = find_rule(data, spt, cfd, "→")
    return result

def infer(ms, result,  type, threshold, printed):
    print('-----confidence threshold is: ' + str(threshold))
   
    # print(result['confidence'])
    # analysis result
    # 1. 根据置信度筛选出满足 threshold 的候选集 C1, 同时记录与is负相关的集合、与not正相关的集合；
    # 2. 将C1中每项集合中 is负相关集合与not正相关集合 的元素删去；
    
    # threshold = 0.9 for without-deployment-log
    candidate_is1 = []
    ignore_is = []
    ignore_not = []
    result2 = result.T
    for rule in result.index:
        line = str(rule)
        if not printed:
            print(result2[line])
        # if len(sorted(rule.split(ms))) == 2:
        #     if not printed:
        #         print(result2[line])
        if line.endswith("→IS"): 
            # if not printed:
            #     print(result2[line])
            marksSet = sorted(rule.split(ms))
            marksSet.remove('IS')
            if result['confidence'][rule] >= threshold:
                candidate_is1.append(marksSet)
            # if myround(result['lift'][rule]) < 1:
            #     ignore_is.append(marksSet) 
        if line.endswith("→NOT"): 
            # if not printed:
            #     print(result2[line])
            marksSet = sorted(rule.split(ms))
            marksSet.remove('NOT')
            # if result['confidence'][rule] >= threshold:
            #     ignore_not.append(marksSet)
            # # if result['lift'][rule] > 1.5:
            # if result['lift'][rule] > 1:
            #     ignore_not.append(marksSet)

    print('**ignore_is********** ')
    for i in ignore_is:
        print(i)
    print('**ignore_is********** ')
    print('**ignore_not********** ')
    for i in ignore_not:
        print(i)
    print('**ignore_not********** ')
    # print(result['confidence'])
    # 1.  删去所有与 is 无关的元素
    candidate_is2 = [];
    for freq_set in candidate_is1:
        freq_set2 = remove_ignore(freq_set, ignore_is, ignore_not)
        # freq_set2 = remove_ignore(freq_set, ignore_is)
        if not len(freq_set2) == 0:
            # 
            for j in range(len(freq_set2)):
                tmp = freq_set2[:j] + freq_set2[j + 1:] + freq_set2[j:j + 1]
                tmp.append('IS')
                linked = ms.join(tmp)
                # print('--------' + linked)
                if  linked in result.index:
                    if result['confidence'][linked] >= threshold:
                        if not already_has(candidate_is2, freq_set2):
                            candidate_is2.append(freq_set2)
            # if already_has(candidate_is1,freq_set2):
            #     if not already_has(candidate_is2, freq_set2):
            #         candidate_is2.append(freq_set2)

    candidate_is2.sort(key = lambda i:len(i), reverse=False) 

    print('-------------')
    if type == 'entry':
        infer_Entry(candidate_is2, threshold)
    if type == 'field_inject':
        infer_Ijnect(candidate_is2)
    if type == 'points2':
        infer_points2(candidate_is2)
    if type == 'indirect_call':
        infer_indirect_call(result,ms,threshold)

if __name__ == '__main__':
    # input
    filename = sys.argv[1]
    type = sys.argv[2]

    # configured parameters
    spt = 0
    cfd = 0
    # connect
    ms = "→"
    result_origin = apriori_cal(spt, cfd, filename, ms)

    # threshold = 1
    # infer(ms, type, threshold)
    # infer(ms, result_origin,  type, threshold)
    printed = False
    for  i in range(0,101,5):
        print('-*-*-*-*-*-*-*-*-*-ANSWER-*-*-*-*-*-*-*-*-*-*-*-')
        print('---' + str(i) + "%---")
        threshold = i / 100
        infer(ms, result_origin,  type, threshold, printed)
        printed = True
        print('------------------------------------------------')
        print('------------------------------------------------')