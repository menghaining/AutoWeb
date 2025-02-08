import pandas as pd
import itertools

# algorithm:

def is_frequent(x, prifix, append_str):
    for i in itertools.combinations(prifix, len(prifix) -1):
        tmp = []
        for item in i:
            tmp.append(item)
        tmp.append(append_str)
        if not sorted(tmp) in x:
            # print("not exist! " + str(sorted(tmp)))
            return False
    return True

def apriori_gen(x, ms):
    x = list(map(lambda i: sorted(i.split(ms)), x))
    l = len(x[0])
    r = []
    for i in range(len(x)):
        for j in range(i, len(x)):
            if x[i][:l - 1] == x[j][:l - 1] and x[i][l - 1] != x[j][l - 1]:
                # 1. 连接
                # 2. 剪枝 prune: 对于新生成的每个c，判断所有k-1 项子集是否也是频繁的，如果不是，则舍去c
                tmp = x[i][:l - 1] + sorted([x[j][l - 1], x[i][l - 1]])
                if is_frequent(x, x[i], x[j][l - 1]):
                    r.append(tmp)
    return r

def find_rule(d, support, confidence, ms=u'--'):
    result = pd.DataFrame(index=['support', 'confidence','lift'])  # 定义输出结果

    support_series = 1.0 * d.sum() / len(d)  # 支持度序列
    support_number_series = 1.0 * d.sum()  # 支持度计数
    # candidate_set = list(support_series[support_series > 1/len(d)].index)  # 初步根据支持度筛选
    candidate_set = list(support_series[support_series > support].index)  # 初步根据支持度筛选
    #  k means k-set
    k = 0

    while len(candidate_set) > 1:
        k = k + 1
        
        print(u'\n Searching %s ...' % k)
        candidate_set = apriori_gen(candidate_set, ms)
        print(u'count：%s...' % len(candidate_set))
        # 新一批支持度的计算函数
        sf = lambda i: d[i].prod(axis=1, numeric_only=True)  

        d_2 = pd.DataFrame(list(map(sf, candidate_set)), index=[ms.join(i) for i in candidate_set]).T
        # print(d_2)
        support_series_2 = 1.0 * d_2[[ms.join(i) for i in candidate_set]].sum() / len(d)  # 计算连接后的支持度
        support_number_series2 = 1.0 * d_2[[ms.join(i) for i in candidate_set]].sum()
        # L_k
        candidate_set = list(support_series_2[support_series_2 > support].index)  # 新一轮支持度筛选
        support_series = support_series.append(support_series_2)
        support_number_series = support_number_series.append(support_number_series2)

        candidate_set2 = []

        # 产生关联规则，右部只有一项
        # 遍历可能的推理，如{A,B,C}究竟是A+B-->C还是B+C-->A还是C+A-->B
        for i in candidate_set:
            i = i.split(ms)
            for j in range(len(i)):
                candidate_set2.append(i[:j] + i[j + 1:] + i[j:j + 1])
 
        # 定义置信度序列
        cofidence_series = pd.Series(index=[ms.join(i) for i in candidate_set2]) 
        # 定义提升度序列 
        lift_series = pd.Series(index=[ms.join(i) for i in candidate_set2])  

        for i in candidate_set2:
            cofidence_series[ms.join(i)] = support_series[ms.join(sorted(i))] / support_series[ms.join(i[:len(i) - 1])]
            lift_series[ms.join(i)] = support_series[ms.join(sorted(i))] / (support_series[ms.join(i[:len(i) - 1])] * support_series[ms.join(i[(len(i) - 1):])])
            # print(str(ms.join(i)) + str(ms.join(sorted(i))) + '/(' + str(ms.join(i[:len(i) - 1])) + '*' + str(ms.join(i[(len(i) - 1):])) + ') = ' + str(lift_series[ms.join(i)]))
            # print(str(ms.join(i)) + str(support_series[ms.join(sorted(i))]) + '/(' + str(support_series[ms.join(i[:len(i) - 1])]) + '*' + str(support_series[ms.join(i[(len(i) - 1):])]) + ') = ' + str(lift_series[ms.join(i)]))

        # 置信度筛选
        for i in cofidence_series[cofidence_series > confidence].index:  
            if not i in result.columns:
                result[i] = 0.0
            result[i]['confidence'] = cofidence_series[i]
            result[i]['support'] = support_series[ms.join(sorted(i.split(ms)))]
            result[i]['lift'] = lift_series[i]


    #显示所有行列
    pd.set_option('display.max_columns', None)
    pd.set_option('display.max_rows', None)
    #设置value的显示长度为100，默认为50
    pd.set_option('max_colwidth',2000)

    # T 即为对数据 行列进行转置
    result = result.T

    # 分别按照confidence 和 lift 排序 进行结果整理
    result_conf = result.sort_values(by=['confidence'], ascending=False)
    result_lift = result.sort_values(by=['lift'], ascending=False)

    # return [result_conf,result_lift]
    return result