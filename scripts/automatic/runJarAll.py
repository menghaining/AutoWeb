import os
import runJar, runJar_deploy



instrumental = 'F:\myProject\webframeworkmodelinfer\ict.pag.m.Instrumentation\\build\libs\logIntrumental.jar'

def runAll(webapp_name,logs_dir,deployTime,simgleReqTime):
    testcases_dir = os.path.join(logs_dir, 'out-preModify')
    answer_dir = os.path.join(logs_dir, 'testcaseLogs')
    runJar.run(webapp_name, testcases_dir, instrumental,answer_dir, deployTime, simgleReqTime)

    dtestcases_dir = os.path.join(logs_dir, 'deploy', 'out-preModify')
    danswer_dir = os.path.join(logs_dir, 'deploy', 'testcaseLogs')
    runJar_deploy.run(webapp_name, dtestcases_dir, instrumental, danswer_dir,deployTime)

if __name__ == '__main__':
    deployTime = 60
    simgleReqTime = 5
    names = ['community','newbeeMall','springPetclinic']
    # names = ['community','springPetclinic']
    logs = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs'
    for name in names:
        webapp_name = name
        logs_dir = os.path.join(logs, name)
        runAll(webapp_name,logs_dir,deployTime,simgleReqTime)

    #wechatResturant: name is different
    webapp_name = 'sell'
    logs_dir = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\wechatResturant'
    runAll(webapp_name,logs_dir,deployTime,simgleReqTime)

    #halo: deploy long time
    webapp_name = 'halo'
    logs_dir = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\halo'
    deployTime = 800
    simgleReqTime = 10
    runAll(webapp_name,logs_dir,deployTime,simgleReqTime)

    #ruoyi-admin: deploy long time
    webapp_name = 'ruoyi-admin'
    logs_dir = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\\ruoyi'
    deployTime = 350
    simgleReqTime = 5
    runAll(webapp_name,logs_dir,deployTime,simgleReqTime)

## single testcase run
#halo
# webapp_name = 'halo'
# logs_dir = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\halo'
# instrumental = 'F:\myProject\webframeworkmodelinfer\ict.pag.m.Instrumentation\\build\libs\logIntrumental.jar'
# deployTime = 700
# simgleReqTime = 10
#ruoyi-admin
# webapp_name = 'ruoyi-admin'
# logs_dir = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\\ruoyi'
# instrumental = 'F:\myProject\webframeworkmodelinfer\ict.pag.m.Instrumentation\\build\libs\logIntrumental.jar'
# deployTime = 350
# simgleReqTime = 5
