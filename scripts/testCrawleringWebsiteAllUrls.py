# -*- coding: utf-8 -*
import os
import re
import shutil
 
REJECT_FILETYPE = 'rar,7z,css,js,jpg,jpeg,gif,bmp,png,swf,exe'#定义爬虫过程中不下载的文件类型
 
def getinfo(webaddress):
	global REJECT_FILETYPE
 
	url = 'http://'+webaddress+'/'#通过用户输入的网址连接上网络协议，得到URL。
	print ('Getting>>>>> '+url)#打印提示信息，表示正在抓取网站
	websitefilepath = os.path.abspath('.')+'/'+webaddress#通过函数os.path.abspath得到当前程序所在的绝对路径，然后搭配用户所输入的网址得到用于存储下载网页的文件夹
	if os.path.exists(websitefilepath):#如果此文件夹已经存在就将其删除，原因是如果它存在，那么爬虫将不成功
		shutil.rmtree(websitefilepath)#shutil.rmtree函数用于删除文件夹（其中含有文件）
	outputfilepath = os.path.abspath('.')+'/'+'output.txt'#在当前文件夹下创建一个过渡性质的文件output.txt
	fobj = open(outputfilepath,'w+')
	command = 'wget -r -m -nv --reject='+REJECT_FILETYPE+' -o '+outputfilepath+' '+url#利用wget命令爬取网站
	tmp0 = os.popen(command).readlines()#函数os.popen执行命令并且将运行结果存储在变量tmp0中
	#print >> fobj,tmp0#写入output.txt中
	print(tmp0, file=fobj)
	allinfo = fobj.read()
	target_url = re.compile(r'\".*?\"',re.DOTALL).findall(allinfo)#通过正则表达式筛选出得到的网址
	target_num = len(target_url)
	fobj1 = open('result.txt','w')#在本目录下创建一个result.txt文件，里面存储最终得到的内容
	for i in range(target_num):
		print(target_url[i][1:-1], file=fobj1)
        #print >> fobj1,target_url[i][1:-1]
	fobj.close()
	fobj1.close()
	if os.path.exists(outputfilepath):#将过渡文件output.txt删除
		os.remove(outputfilepath)#os.remove用于删除文件
 
if __name__=="__main__":
	webaddress = input("Input the Website Address(without \"http:\")>")
	getinfo(webaddress)
	print ("Well Done.")#代码执行完毕之后打印此提示信息