import deploy
import threading
import time
import psutil

if __name__ == '__main__':
    runnableJar = "F:\Framework\InputRunnableTestcases\\runnableJars\springPetclinic\springPetclinic.jar"
    instrumental = "F:\myProject\webframeworkmodelinfer\ict.pag.m.Instrumentation\\build\libs\logIntrumental.jar"
    out = "F:\Framework\InputRunnableTestcases\\runnableJars\springPetclinic\\test.txt"

    # deploy.deploy_jar(runnableJar,instrumental,out)
    t=threading.Thread(target=deploy.deploy_jar, args=(runnableJar, instrumental, out))
    t.start()
    print("....delpoy start")
    time.sleep(40)
    print("....sleep done")
    pids = psutil.pids()
    for pid in pids:
        p = psutil.Process(pid)
        if p.name() == 'java.exe':
            print(pid)
            print(p.exe())
            print(p.cmdline())
            cmd = 'taskkill /F /IM java.exe'
            print(cmd)
            # p.kill()
            # os.system(cmd)
    print("....list process done")