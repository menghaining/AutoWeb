# run jackee runnable benchmarks
import runWarAll
import os
import runJar, runJar_deploy

instrumental = 'F:\myProject\webframeworkmodelinfer\ict.pag.m.Instrumentation\\build\libs\logIntrumental-JackEE.jar'

def runJarSingle(webapp_name,logs_dir,deployTime,simgleReqTime):
    testcases_dir = os.path.join(logs_dir, 'out-preModify')
    answer_dir = os.path.join(logs_dir, 'testcaseLogs')
    runJar.run(webapp_name, testcases_dir, instrumental,answer_dir, deployTime, simgleReqTime)

    dtestcases_dir = os.path.join(logs_dir, 'deploy', 'out-preModify')
    danswer_dir = os.path.join(logs_dir, 'deploy', 'testcaseLogs')
    runJar_deploy.run(webapp_name, dtestcases_dir, instrumental, danswer_dir,deployTime)

# 1. shopizer. war
# running on server:120,124,220

# 2. SpringBlog. jar
deployTime = 150
singleReqTime = 10
name = 'SpringBlog'
mutation_dir = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\SpringBlog'
runJarSingle(name, mutation_dir, deployTime, singleReqTime)

# 3. WebGoat. war (done)
# deployTime = 50
# singleReqTime = 10
# name = 'WebGoat'
# tomcat_dir = 'F:\Framework\InputRunnableTestcases\JackEE_testcase_runnable\\tomcat-WebGoat'
# mutation_dir = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\WebGoat'
# runWarAll.runSingle(tomcat_dir, mutation_dir, name, deployTime, singleReqTime)

# 4. pybbs. jar (done)
# deployTime = 680
# singleReqTime = 10
# name = 'pybbs'
# mutation_dir = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\pybbs'
# runJarSingle(name, mutation_dir, deployTime, singleReqTime)
