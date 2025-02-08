import os, time
import runJar, runJar_deploy
import runWar_deploy

instrumental = 'F:\myProject\webframeworkmodelinfer\ict.pag.m.Instrumentation\\build\libs\logIntrumental.jar'

#halo : request
# webapp_name = 'halo'
# logs_dir = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\halo'
# deployTime = 800
# simgleReqTime = 10

# testcases_dir = os.path.join(logs_dir, 'out-preModify')
# answer_dir = os.path.join(logs_dir, 'testcaseLogs')
# runJar.run(webapp_name, testcases_dir, instrumental,answer_dir, deployTime, simgleReqTime)


#logicaldoc : deploy
# tomcat_dir = 'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-logicaldoc\\tomcat'
# logs_dir = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\logicaldoc'
# webapp_name = 'logicaldoc
# deployTime = 400
# time_start2 = time.time()
# dtestcases_dir = os.path.join(logs_dir, 'deploy', 'out-preModify')
# danswer_dir = os.path.join(logs_dir, 'deploy', 'testcaseLogs')
# runWar_deploy.run(webapp_name, dtestcases_dir, tomcat_dir, danswer_dir,deployTime)
# time_end2 = time.time()
# print('configure testcases totally cost: ', time_end2-time_start2)

#ruoyi-admin: deploy
webapp_name = 'ruoyi-admin'
logs_dir = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\\ruoyi'
deployTime = 400
simgleReqTime = 5
dtestcases_dir = os.path.join(logs_dir, 'deploy', 'out-preModify')
danswer_dir = os.path.join(logs_dir, 'deploy', 'testcaseLogs')
runJar_deploy.run(webapp_name, dtestcases_dir, instrumental, danswer_dir,deployTime)