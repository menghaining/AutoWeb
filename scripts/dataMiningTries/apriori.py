import pandas as pd
import itertools

#https://blog.csdn.net/Smart3S/article/details/88298312
# 该算法的基本思想是：
# 1. 首先找出所有的频集，这些项集出现的频繁性至少和预定义的最小支持度一样。
# 2. 然后由频集产生强关联规则，这些规则必须满足最小支持度和最小可信度。
# 3. 然后使用第1步找到的频集产生期望的规则，产生只包含集合的项的所有规则，其中每一条规则的右部只有一项.
# 一旦这些规则被生成，那么只有那些大于用户给定的最小可信度的规则才被留下来。
# 为了生成所有频集，使用了递归的方法。

# 自定义连接函数，用于实现L_{k-1}到C_k的连接
# 计算数据库的自然连接
def connect_string(x, ms):
    # 把x作为lambda函数的输入，遍历i进行sorted()内的操作
    # map() 会根据提供的函数对指定序列做映射
    x = list(map(lambda i: sorted(i.split(ms)), x))
    l = len(x[0])
    r = []
    # print("***")
    # print(x)
    for i in range(len(x)):
        for j in range(i, len(x)):
            # print('start')
            # print(x[i][:l - 1])
            # print(x[j][:l - 1])
            # print(x[i][l - 1])
            # print(x[j][l - 1])
            # 只进行了 自然连接 操作，没有剪枝！！！
            if x[i][:l - 1] == x[j][:l - 1] and x[i][l - 1] != x[j][l - 1]:
                # tmp = x[i][:l - 1] + sorted([x[j][l - 1], x[i][l - 1]])
                r.append(x[i][:l - 1] + sorted([x[j][l - 1], x[i][l - 1]]))
    return r

def is_frequent(x, prifix, append_str):
    for i in itertools.combinations(prifix, len(prifix) -1):
        #  debug
        tmp = []
        for item in i:
            tmp.append(item)
        tmp.append(append_str)
        if not sorted(tmp) in x:
            print("not exist! " + str(sorted(tmp)))
            return False
        # print(i)
    return True


def apriori_gen(x, ms):
    x = list(map(lambda i: sorted(i.split(ms)), x))
    l = len(x[0])
    r = []

    for i in range(len(x)):
        for j in range(i, len(x)):
            if x[i][:l - 1] == x[j][:l - 1] and x[i][l - 1] != x[j][l - 1]:
                # 1. 连接
                # 2. 剪枝 prune
                # 对于新生成的每个c，判断所有k-1 项子集是否也是频繁的，如果不是，则舍去c
                tmp = x[i][:l - 1] + sorted([x[j][l - 1], x[i][l - 1]])
                if is_frequent(x, x[i], x[j][l - 1]):
                    r.append(tmp)
    return r

    

 
# 寻找关联规则的函数，输入数据、最小支持度、置信度以及用于表达多个物品联系的连接符
def find_rule(d, support, confidence, ms=u'--'):
    result = pd.DataFrame(index=['support', 'confidence','lift'])  # 定义输出结果
 
    # print(len(d))
    # print(d.sum())
    support_series = 1.0 * d.sum() / len(d)  # 支持度序列
    # print(support_series)
    column = list(support_series[support_series > support].index)  # 初步根据支持度筛选
    # print(column)
    k = 0

    # 当连接(计算)不出新的 C集合时 终止
    while len(column) > 1:
        k = k + 1
        
        print(u'\n正在进行第%s次搜索...' % k)
        # 1. 连接
        # 2. 剪枝 prune
        column = apriori_gen(column, ms)
        # column = connect_string(column, ms)
        
        print(u'数目：%s...' % len(column))
        # print(column)
        sf = lambda i: d[i].prod(axis=1, numeric_only=True)  # 新一批支持度的计算函数
        # print('---')
        # print(d)
        # print(list(map(sf, column)))
        # 创建连接数据，这一步耗时、耗内存最严重。当数据集较大时，可以考虑并行运算优化。
        # 将 通过k-1数据算出的k数据 转为可计算的格式
        d_2 = pd.DataFrame(list(map(sf, column)), index=[ms.join(i) for i in column]).T
        # print(d_2)
        support_series_2 = 1.0 * d_2[[ms.join(i) for i in column]].sum() / len(d)  # 计算连接后的支持度
        column = list(support_series_2[support_series_2 > support].index)  # 新一轮支持度筛选
        # print('&&&&&&&&&')
        # print(column)
        support_series = support_series.append(support_series_2)
        column2 = []
 
        # 关联规则计算，某种情况下 推出 最后一项的置信度
        for i in column:  # 遍历可能的推理，如{A,B,C}究竟是A+B-->C还是B+C-->A还是C+A-->B？
            i = i.split(ms)
            for j in range(len(i)):
                column2.append(i[:j] + i[j + 1:] + i[j:j + 1])
 
        cofidence_series = pd.Series(index=[ms.join(i) for i in column2])  # 定义置信度序列
        lift_series = pd.Series(index=[ms.join(i) for i in column2])  # 定义提升度序列

        for i in column2:  # 计算置信度序列
            # 因为标签的名称都是sorted存储的，所以要取出集合中的值时要把集合按照标签顺序sorted一下
            # 因为column2每条数据在计算的时候，前i-1项是sorted，但集合并不是sorted，所有要sorted(i)
            cofidence_series[ms.join(i)] = support_series[ms.join(sorted(i))] / support_series[ms.join(i[:len(i) - 1])]
            lift_series[ms.join(i)] = support_series[ms.join(sorted(i))] / (support_series[ms.join(i[:len(i) - 1])] * support_series[ms.join(i[(len(i) - 1):])])
        
        for i in cofidence_series[cofidence_series > confidence].index:  # 置信度筛选
            if not i in result.columns:
                result[i] = 0.0
            result[i]['confidence'] = cofidence_series[i]
            result[i]['support'] = support_series[ms.join(sorted(i.split(ms)))]
            result[i]['lift'] = lift_series[i]
        # lift 和 confidence series的标签是一样的，所以放在一起写即可
        # for i in lift_series.index:
        #     if not i in result.columns:
        #         result[i] = 0.0
        #     result[i]['lift'] = lift_series[i]

    #显示所有列
    pd.set_option('display.max_columns', None)
    #显示所有行
    pd.set_option('display.max_rows', None)
    #设置value的显示长度为100，默认为50
    pd.set_option('max_colwidth',2000)

    # T 即为对数据 行列进行转置
    result = result.T.sort_values(by=['confidence', 'support'], ascending=False)  # 结果整理，输出
    result2 = result.sort_values(by=['lift'], ascending=False)

    # 分别按照confidence 和 lift 排序 进行结果整理
    result_conf = result.sort_values(by=['confidence'], ascending=False)  # 结果整理，输出
    result_lift = result.sort_values(by=['lift'], ascending=False)

    

  
  # print answers
    # print('[Answer1]--------------------------------------')
    # for i in result.index:
    #     s=str(i)
    #     marksSet = sorted(i.split(ms))
    #     if s.endswith("→NOT"):
    #         marksSet.remove('NOT')
    #         print('[NOT]\n\t'+str(marksSet)+'\n\t'+'[confidence]: '+ str(result['confidence'][i]))
    #     if s.endswith("→IS"):
    #         marksSet.remove('IS')
    #         print('[IS]\n\t'+str(marksSet)+'\n\t'+'[confidence]: '+ str(result['confidence'][i]))

    # print('[Answer2]--------------------------------------')
    # for i in result2['lift'].index:
    #     s=str(i)
    #     marksSet = sorted(i.split(ms))
    #     if s.endswith("→NOT"):
    #         marksSet.remove('NOT')
    #         print('[NOT]\n\t'+str(marksSet)+'\n\t'+'[lift]: '+ str(result2['lift'][i]))
    #     if s.endswith("→IS"):
    #         marksSet.remove('IS')
    #         print('[IS]\n\t'+str(marksSet)+'\n\t'+'[lift]: '+ str(result2['lift'][i]))

    # print('--------------------------------------')
    # for i in result.index:
    #     s=str(i)
    #     if s.endswith("→NOT") or s.endswith("→IS"):
    #         print(str(i))
    #         print('confidence: '+str(result['confidence'][i]))
    # print('--------------------------------------')
    # for i in result2['lift'].index:
    #     s=str(i)
    #     if s.endswith("→NOT") or s.endswith("→IS"):
    #         print(str(i))
    #         print('lift: '+str(result2['lift'][i]))

    # print(result)
    return [result_conf,result_lift]

    
