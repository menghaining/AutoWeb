#encoding=utf-8
import os
import sys
import generateTestcases4struts

# jar = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\GenMutations-nolimit.jar'
jar = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\GenMutations.jar'

## A. WAR
# 1. Logistics_Manage_System
LMS = 'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-LogisticsManageSystem\webapps-original\Logistics_Manage_System  F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-LogisticsManageSystem\logs\original\InstrumentalLog.txt  -libs F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-LogisticsManageSystem\webapps-original\Logistics_Manage_System\WEB-INF\lib  -extraDir F:\myProject\webframeworkmodelinfer\ict.pag.webframework.model\data\libs -out F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\Logistics_Manage_System'
cmd = 'java -jar ' + jar + ' ' + LMS
print('...1......')
print(cmd)
os.system(cmd)
# 2. JpetStore
jpetstore = 'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-jpetstore\webapps-original\jpetstore  F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-jpetstore\logs\original\InstrumentalLog.txt  -libs F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-jpetstore\webapps-original\jpetstore\WEB-INF\lib  -extraDir F:\myProject\webframeworkmodelinfer\ict.pag.webframework.model\data\libs -out F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\jpetstore'
cmd = 'java -jar ' + jar + ' ' + jpetstore
print('...2......')
print(cmd)
os.system(cmd)
# 3. icloud
icloud = 'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-icloud\webapps-original\icloud  F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-icloud\logs\original\InstrumentalLog.txt  -libs F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-icloud\webapps-original\icloud\WEB-INF\lib  -extraDir F:\myProject\webframeworkmodelinfer\ict.pag.webframework.model\data\libs -out F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\icloud'
cmd = 'java -jar ' + jar + ' ' + icloud
print('...3......')
print(cmd)
os.system(cmd)
# 4. spring-mvc-showcase
springmvcshowcase = 'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-spring-mvc-showcase\webapps-original\spring-mvc-showcase  F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-spring-mvc-showcase\logs\original\InstrumentalLog.txt  -libs F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-spring-mvc-showcase\webapps-original\spring-mvc-showcase\WEB-INF\lib  -extraDir F:\myProject\webframeworkmodelinfer\ict.pag.webframework.model\data\libs -out F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\spring-mvc-showcase'
cmd = 'java -jar ' + jar + ' ' + springmvcshowcase
print('...4......')
print(cmd)
os.system(cmd)
# 5. NewsSystem
NewsSystem = r'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-NewsSystem\webapps-original\NewsSystem  F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-NewsSystem\logs\original\InstrumentalLog.txt  -libs F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-NewsSystem\webapps-original\NewsSystem\WEB-INF\lib  -extraDir F:\myProject\webframeworkmodelinfer\ict.pag.webframework.model\data\libs -out F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\\NewsSystem'
cmd = 'java -jar ' + jar + ' ' + NewsSystem
print('...5......')
print(cmd)
os.system(cmd)
# 6. Shop (mission shop)
Shop = 'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-mission-Shop\webapps-original\Shop  F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-mission-Shop\logs\original\InstrumentalLog.txt  -libs F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-mission-Shop\webapps-original\Shop\WEB-INF\lib  -extraDir F:\myProject\webframeworkmodelinfer\ict.pag.webframework.model\data\libs -out F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\Shop'
cmd = 'java -jar ' + jar + ' ' + Shop
print('...6......')
print(cmd)
os.system(cmd)
# 7. webDemo
webDemo = 'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-webDemo\webapps-original\webDemo  F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-webDemo\logs\original\InstrumentalLog.txt  -libs F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-webDemo\webapps-original\webDemo\WEB-INF\lib  -extraDir F:\myProject\webframeworkmodelinfer\ict.pag.webframework.model\data\libs -out F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\webDemo'
cmd = 'java -jar ' + jar + ' ' + webDemo
print('...7......')
print(cmd)
os.system(cmd)
# 8. OpenKM
OpenKM = 'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-OpenKM\webapps-original\OpenKM  F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-OpenKM\logs\original\InstrumentalLog.txt  -libs F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-OpenKM\webapps-original\OpenKM\WEB-INF\lib  -extraDir F:\myProject\webframeworkmodelinfer\ict.pag.webframework.model\data\libs -out F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\OpenKM'
cmd = 'java -jar ' + jar + ' ' + OpenKM
print('...8......')
print(cmd)
os.system(cmd)
# 9. logicaldoc (need further mutate in FurtherModifyAppJar.java)
logicaldoc = 'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-logicaldoc\\tomcat\webapps-original\modifyInput\logicaldoc  F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-logicaldoc\\tomcat\logs\original\InstrumentalLog.txt  -extraDir F:\myProject\webframeworkmodelinfer\ict.pag.webframework.model\data\libs -libs F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-logicaldoc\\tomcat\webapps-original\modifyInput\logicaldoc\WEB-INF\lib -out F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\logicaldoc\out-preModify1'
cmd = 'java -jar ' + jar + ' ' + logicaldoc
print('...9......')
print(cmd)
os.system(cmd)
# 10. struts2-official-samples
testcasesDir1 = 'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-struts2-official-samples\webapps-original'
logsDir1 = 'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-struts2-official-samples\logs\original'
outDir1 = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\struts2-official-samples'
print('...10......')
generateTestcases4struts.generate(testcasesDir1, logsDir1, outDir1)
# 11. struts2-vulns
testcasesDir2 = 'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-struts2-vulns\webapps-original'
logsDir2 = 'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-struts2-vulns\logs\original'
outDir2 = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\struts2-vulns'
print('...11......')
generateTestcases4struts.generate(testcasesDir2, logsDir2, outDir2)
