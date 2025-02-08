import os
import runWar, runWar_deploy


def run_struts_dirs(tomcat_dir, logs_dir):
    deployTime = 30
    singleReqTime = 3
    for name in os.listdir(logs_dir):
        webapp_name = name
        testcases_dir = os.path.join(logs_dir, name, 'out-preModify')
        answer_dir = os.path.join(logs_dir, name, 'testcaseLogs')
        runWar.run(webapp_name, testcases_dir, tomcat_dir,answer_dir,deployTime,singleReqTime)
        dtestcases_dir = os.path.join(logs_dir, name, 'deploy', 'out-preModify')
        danswer_dir = os.path.join(logs_dir, name, 'deploy', 'testcaseLogs')
        runWar_deploy.run(webapp_name, dtestcases_dir, tomcat_dir, danswer_dir,deployTime)

if __name__ == '__main__':
    ## 	struts2-official-samples
    tomcat_dir1 = 'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-struts2-official-samples'
    logs_dir1 = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\struts2-official-samples'
    # logs_dir1 = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\\re-run1'

    ## struts2-vulns
    tomcat_dir2 = 'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-struts2-vulns'
    logs_dir2 = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\struts2-vulns'
    # logs_dir2 = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\\re-run2'

    run_struts_dirs(tomcat_dir1, logs_dir1)
    run_struts_dirs(tomcat_dir2, logs_dir2)
