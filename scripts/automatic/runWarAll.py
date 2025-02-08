## This script used to run all testcases which deployed through tomcat
import os
import time
from unicodedata import name
import runWar
import runWar_deploy
import trigger4StrutsTestcases


def run_normals():
    deployTime = 50
    singleReqTime = 5
    names = [
        'icloud', 'jpetstore', 'Logistics_Manage_System', 'NewsSystem', 'Shop', 'spring-mvc-showcase'
        # , 'webDemo'
    ]
    tomcats = [
        'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-icloud',
        'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-jpetstore',
        'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-LogisticsManageSystem',
        'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-NewsSystem',
        'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-mission-Shop',
        'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-spring-mvc-showcase'
        # , 'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-webDemo'
    ]
    logs_all = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs'

    for i in range(len(names)):
        webapp_name = names[i]
        tomcat_dir = tomcats[i]
        logs_dir = os.path.join(logs_all, webapp_name)
        print(webapp_name)
        print(tomcat_dir)
        print(logs_dir)

        time_start1 = time.time()
        testcases_dir = os.path.join(logs_dir, 'out-preModify')
        answer_dir = os.path.join(logs_dir, 'testcaseLogs')
        runWar.run(webapp_name, testcases_dir, tomcat_dir,
                   answer_dir, deployTime, singleReqTime)
        time_end1 = time.time()
        print('request testcases totally cost: ', time_end1-time_start1)

        time_start2 = time.time()
        dtestcases_dir = os.path.join(logs_dir, 'deploy', 'out-preModify')
        danswer_dir = os.path.join(logs_dir, 'deploy', 'testcaseLogs')
        runWar_deploy.run(webapp_name, dtestcases_dir,
                          tomcat_dir, danswer_dir, deployTime)
        time_end2 = time.time()
        print('configure testcases totally cost: ', time_end2-time_start2)

# OpenKM: use extra instrumenter.jar need to run single
def run_OpenKM():
    tomcat_dir = 'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-OpenKM'
    logs_dir = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\OpenKM'
    webapp_name = 'OpenKM'
    deployTime = 1800
    singleReqTime = 10
    runSingle(tomcat_dir, logs_dir, webapp_name, deployTime, singleReqTime)

# logicaldoc: deploy time long, run single
def run_logicaldoc():
    tomcat_dir = 'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-logicaldoc\\tomcat'
    logs_dir = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\logicaldoc'
    webapp_name = 'logicaldoc'
    deployTime = 400
    singleReqTime = 10
    runSingle(tomcat_dir, logs_dir, webapp_name, deployTime, singleReqTime)

# run single code
def runSingle(tomcat_dir, logs_dir, webapp_name, deployTime, singleReqTime):
    time_start1 = time.time()
    testcases_dir = os.path.join(logs_dir, 'out-preModify')
    answer_dir = os.path.join(logs_dir, 'testcaseLogs')
    runWar.run(webapp_name, testcases_dir, tomcat_dir,
               answer_dir, deployTime, singleReqTime)
    time_end1 = time.time()
    print('request testcases totally cost: ', time_end1-time_start1)

    time_start2 = time.time()
    dtestcases_dir = os.path.join(logs_dir, 'deploy', 'out-preModify')
    danswer_dir = os.path.join(logs_dir, 'deploy', 'testcaseLogs')
    runWar_deploy.run(webapp_name, dtestcases_dir,
                      tomcat_dir, danswer_dir, deployTime)
    time_end2 = time.time()
    print('configure testcases totally cost: ', time_end2-time_start2)


def run_struts2():
    # struts2-official-samples
    tomcat_dir1 = 'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-struts2-official-samples'
    logs_dir1 = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\struts2-official-samples'
    # struts2-vulns
    tomcat_dir2 = 'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-struts2-vulns'
    logs_dir2 = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\struts2-vulns'
    trigger4StrutsTestcases.run_struts_dirs(tomcat_dir1, logs_dir1)
    trigger4StrutsTestcases.run_struts_dirs(tomcat_dir2, logs_dir2)


if __name__ == '__main__':
    run_normals()
    run_OpenKM()
    run_logicaldoc()
    run_struts2()

# debug : for single testcase
# webDemo
# tomcat_dir = 'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-webDemo'
# logs_dir = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\webDemo'
# webapp_name = 'webDemo'
# for Single testcase run deploy and responsing
# deployTime = 20
# simgleReqTime = 3
# webapp_name = 'rest-angular'
# logs_dir = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\struts2-official-samples\\rest-angular'
# tomcat_dir = 'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-struts2-official-samples'
