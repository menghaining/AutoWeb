import os
import time
import psutil
import threading

jar_apps = ['ruoyi', 'resturant', 'halo',
            'community', 'newbee-mall', 'petclinic']
war_apps = ['jpetstore', 'openkm', 'struts2-examples', 'LMS', 'icloud', 'Newssystem',
            'logical-dms', 'spring-showcase', 'struts2-vuln', 'mission008', 'demo1']


def deploy_jar(path, agent, consoles_log):
    cmd = ""
    # option1: agent
    if agent == "":
        print('deploy without javaagent...')
        cmd = 'java -jar ' + path
    else:
        print('deploy with javaagent...')
        cmd = 'java -javaagent:'+agent+' -jar ' + path
    # option2: log file
    if not consoles_log == "":
        cmd = cmd+" > "+consoles_log+" -encoding utf8"

    print("[runnning]" + cmd)
    os.system(cmd)

def shutdown_jar():
    pids = psutil.pids()
    for pid in pids:
        try:
            p = psutil.Process(pid)
            if p.name() == 'java.exe':
                print("...kill java.exe process")
                p.kill()
        except Exception as e:
            print("...java.exe process already killed")
            # cmd = 'taskkill /F /IM java.exe'
            # os.system(cmd)

# javaagent already configured in this bin path
# output file also already configured in this bin path
def deploy_war(tomcat_root_path):
    path = os.path.join(tomcat_root_path, "bin", "startup.sh")
    # print(path)
    os.system(path)


def shutdown_war(tomcat_root_path):
    path = os.path.join(tomcat_root_path, "bin", "shutdown.sh")
    # print(path)
    os.system(path)


if __name__ == '__main__':
    deploy_jar("F:\Framework\InputRunnableTestcases\\runnableJars\springPetclinic\springPetclinic.jar",
               "F:\myProject\webframeworkmodelinfer\ict.pag.m.Instrumentation\\build\libs\logIntrumental.jar", "F:\Framework\InputRunnableTestcases\\runnableJars\springPetclinic\\test.txt")
    # deploy_war("F:\Framework\\apache-tomcat-8.5.61-new\\apache-tomcat-8.5.61")
