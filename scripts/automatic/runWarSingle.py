## for single war test
import os,time
from unicodedata import name
import runWar, runWar_deploy

##logicaldoc: deploy time long, run single
tomcat_dir = 'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-logicaldoc\\tomcat'
logs_dir = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\logicaldoc'
webapp_name = 'logicaldoc'
deployTime = 500
simgleReqTime = 10

## run single code
# time_start1 = time.time()
# testcases_dir = os.path.join(logs_dir, 'out-preModify')
# answer_dir = os.path.join(logs_dir, 'testcaseLogs')
# runWar.run(webapp_name, testcases_dir, tomcat_dir,answer_dir, deployTime, simgleReqTime)
# time_end1 = time.time()
# print('request testcases totally cost: ', time_end1-time_start1)
time_start2 = time.time()
dtestcases_dir = os.path.join(logs_dir, 'deploy', 'out-preModify')
danswer_dir = os.path.join(logs_dir, 'deploy', 'testcaseLogs')
runWar_deploy.run(webapp_name, dtestcases_dir, tomcat_dir, danswer_dir,deployTime)
time_end2 = time.time()
print('configure testcases totally cost: ', time_end2-time_start2)

