import os

# old
# jar = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\generate.jar'
# djar = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\generate_deploy.jar'
# jar = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\GenMutations-nolimit.jar'
jar = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\GenMutations.jar'

extraDir = ' -extraDir F:\myProject\webframeworkmodelinfer\ict.pag.webframework.model\data\libs'

def generate(testcasesDir,logsDir,outDir):
    for name in os.listdir(testcasesDir):
        if(os.path.isdir(os.path.join(testcasesDir,name))):
            arg1 = os.path.join(testcasesDir,name)
            arg2 = os.path.join(logsDir, name) + '.txt'
            libs = ' -libs ' + os.path.join(arg1, 'WEB-INF', 'lib')
            # out = ' -out ' + os.path.join(outDir, name , 'out-preModify')
            out = ' -out ' + os.path.join(outDir, name)
            cmd = arg1 + ' ' + arg2 + ' ' + extraDir + libs + out
            runCMD = 'java -jar ' + jar + ' ' + cmd
            print(cmd)
            os.system(runCMD)
            # when split deploy and request, use below
            # runCMD2 = 'java -jar ' + djar + ' ' + arg1 + ' ' + arg2 + ' ' + extraDir + libs + ' -out ' + os.path.join(outDir, name , 'deploy', 'out-preModify')
            # print(runCMD2)
            # os.system(runCMD2)

## struts2-official-samples
testcasesDir1 = 'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-struts2-official-samples\webapps-original'
logsDir1 = 'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-struts2-official-samples\logs\original'
outDir1 = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\struts2-official-samples'

## struts2-vulns
testcasesDir2 = 'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-struts2-vulns\webapps-original'
logsDir2 = 'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-struts2-vulns\logs\original'
outDir2 = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\struts2-vulns'

if __name__ == '__main__':
    generate(testcasesDir1,logsDir1,outDir1)
    generate(testcasesDir2,logsDir2,outDir2)