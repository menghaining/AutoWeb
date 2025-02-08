from apriori import * #导入编写的算法文件
import pandas as pda
#filename="F:\\test1.xls"
#dataframe=pda.read_excel(filename,usecols='A:G')
#dataframe=pda.read_excel(filename,usecols='B:F')#用户序号列无用，因此不进行导入
#数据转化，行为每个用户，列为每本图书，若用户购买了图书则为1，否则为0
#change=lambda x:pda.Series(1,index=x[pda.notnull(x)])
#mapok=map(change,dataframe.values)

#data=pda.DataFrame(list(mapok)).fillna(0)
#filename="F:\\myProject\\webframeworkmodelinfer\\ict.pag.webframework.model\\result-matrix\\Entry.xls"
#data=pda.read_excel(filename,usecols='A:EM')

# filename="F:\\myProject\\webframeworkmodelinfer\\ict.pag.webframework.model\\result-matrix\\Entry.xls"
# data=pda.read_excel(filename,usecols='A:EP')
filename="F:\\myProject\\webframeworkmodelinfer\\ict.pag.webframework.model\\result-matrix\\FieldInject.xls"
data=pda.read_excel(filename,usecols='A:Z')
print(data)
 
#临界支持度，置信度设置
spt=0
cfd=0
 
#置信度计算
[result_conf, result_lift] = find_rule(data,spt,cfd,"→")
print(result_conf)