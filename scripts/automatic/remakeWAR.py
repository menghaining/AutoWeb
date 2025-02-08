import sys, shutil, os, time

def copydir(source_path, target_path): 
    if not os.path.exists(target_path):
        os.makedirs(target_path)
    if os.path.exists(source_path):
        shutil.rmtree(target_path)
    shutil.copytree(source_path, target_path)
    print('copy dir finished!')

def replacePath(target_path, patchPath):
    for f in os.listdir(patchPath):
        path = os.path.join(patchPath,f)
        if os.path.isfile(path):
            if not f == 'requestsTrigger.txt':
                shutil.copy(path, target_path)
                print ("copy %s -> %s"%(path, target_path))
        elif os.path.isdir(path):
            target_subPath = os.path.join(target_path, f)
            replacePath(target_subPath, path)

def make(originalWARPath, webappPath, patchPath):
    name = os.path.basename(originalWARPath)
    target_path = os.path.join(webappPath,name)
    # 1. copy the originalWAR directory to targetPath
    copydir(originalWARPath, target_path)
    # 2. copy the patch files to
    replacePath(target_path, patchPath)
    

if __name__ == '__main__':
    # originalWAR = 'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-spring-mvc-showcase\webapps-original\spring-mvc-showcase'
    # targetPath = 'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-spring-mvc-showcase\webapps'
    # patchPath = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\spring-mvc-showcase\out-preModify\\100'
    # make(originalWAR, targetPath, patchPath)
    originalWAR = 'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-OpenKM\webapps-original\OpenKM'
    targetPath = 'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-OpenKM\webapps'
    time_start = time.time()
    copydir(originalWAR, targetPath)
    time_end = time.time()
    print('totally cost: ', time_end-time_start)