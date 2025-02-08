#encoding=utf-8
import os

# jar = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\GenMutations-nolimit.jar'
jar = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\GenMutations.jar'

## B. JAR
# 12. community
community = 'F:\Framework\InputRunnableTestcases\\runnableJars\community\community  F:\Framework\InputRunnableTestcases\\runnableJars\community\original\InstrumentalLog.txt  -extraDir F:\myProject\webframeworkmodelinfer\ict.pag.webframework.model\data\libs -libs F:\Framework\InputRunnableTestcases\\runnableJars\community\community\BOOT-INF\lib  -isJar F:\Framework\InputRunnableTestcases\\runnableJars\community\community.jar -out F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\community'
cmd = 'java -jar ' + jar + ' ' + community
print('...12......')
print(cmd)
os.system(cmd)
# 13. newbeeMall
newbeeMall = 'F:\Framework\InputRunnableTestcases\\runnableJars\\newbeeMall\\newbeeMall F:\Framework\InputRunnableTestcases\\runnableJars\\newbeeMall\original\InstrumentalLog.txt   -extraDir F:\myProject\webframeworkmodelinfer\ict.pag.webframework.model\data\libs -libs F:\Framework\InputRunnableTestcases\\runnableJars\\newbeeMall\\newbeeMall\WEB-INF\lib -isJar F:\Framework\InputRunnableTestcases\\runnableJars\\newbeeMall\\newbeeMall.jar -out F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\\newbeeMall'
cmd = 'java -jar ' + jar + ' ' + newbeeMall
print('...13......')
print(cmd)
os.system(cmd)
# 14. springPetclinic
springPetclinic = 'F:\Framework\InputRunnableTestcases\\runnableJars\springPetclinic\springPetclinic F:\Framework\InputRunnableTestcases\\runnableJars\springPetclinic\original\InstrumentalLog.txt   -extraDir F:\myProject\webframeworkmodelinfer\ict.pag.webframework.model\data\libs -libs F:\Framework\InputRunnableTestcases\\runnableJars\springPetclinic\springPetclinic\BOOT-INF\lib -isJar F:\Framework\InputRunnableTestcases\\runnableJars\springPetclinic\springPetclinic.jar -out F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\springPetclinic'
cmd = 'java -jar ' + jar + ' ' + springPetclinic
print('...14......')
print(cmd)
os.system(cmd)
# 15. sell wechatRestaurant
sell = 'F:\Framework\InputRunnableTestcases\\runnableJars\wechatResturant\sell  F:\Framework\InputRunnableTestcases\\runnableJars\wechatResturant\original\InstrumentalLog.txt   -extraDir F:\myProject\webframeworkmodelinfer\ict.pag.webframework.model\data\libs -libs F:\Framework\InputRunnableTestcases\\runnableJars\wechatResturant\sell\BOOT-INF\lib  -isJar F:\Framework\InputRunnableTestcases\\runnableJars\wechatResturant\sell.jar -out F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\wechatResturant'
cmd = 'java -jar ' + jar + ' ' + sell
print('...15......')
print(cmd)
os.system(cmd)
# 16. halo
halo = 'F:\Framework\InputRunnableTestcases\\runnableJars\halo\halo F:\Framework\InputRunnableTestcases\\runnableJars\halo\original\InstrumentalLog.txt   -extraDir F:\myProject\webframeworkmodelinfer\ict.pag.webframework.model\data\libs -libs F:\Framework\InputRunnableTestcases\\runnableJars\halo\halo\BOOT-INF\lib -isJar F:\Framework\InputRunnableTestcases\\runnableJars\halo\halo.jar -out F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\halo'
cmd = 'java -jar ' + jar + ' ' + halo
print('...16......')
print(cmd)
os.system(cmd)
# 17. ruoyi (need further mutate in FutherModifyJar.java and writeAll2Jar.java)
ruoyi = 'F:\Framework\InputRunnableTestcases\\runnableJars\\ruoyi\modifyInput\\ruoyi-admin   F:\Framework\InputRunnableTestcases\\runnableJars\\ruoyi\original\InstrumentalLog.txt  -extraDir F:\myProject\webframeworkmodelinfer\ict.pag.webframework.model\data\libs -libs F:\Framework\InputRunnableTestcases\\runnableJars\\ruoyi\modifyInput\\ruoyi-admin\BOOT-INF\lib -out F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\\ruoyi\out-preModify1'
cmd = 'java -jar ' + jar + ' ' + ruoyi
print('...17......')
print(cmd)
os.system(cmd)
