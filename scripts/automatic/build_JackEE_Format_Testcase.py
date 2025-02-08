# This script aims to transformat input wars/jars to jackEE format 
# like xxx-app and xxx-jar 
import os
import shutil

def copydir(source_path, target_path):
    if not os.path.exists(target_path):
        os.makedirs(target_path)
    if os.path.exists(source_path):
        shutil.rmtree(target_path)
    shutil.copytree(source_path, target_path)

def build_app_libs_war(origin_path, dest_path, name):
    app_path = os.path.join(dest_path, name, name+"-app")
    libs_path = os.path.join(dest_path, name, name+"-libs")
    if not os.path.exists(app_path):
        os.makedirs(app_path)
    if not os.path.exists(libs_path):
        os.makedirs(libs_path)
    # 1. copy war
    tmp_app_path = os.path.join(dest_path, name, name+"-tmp")
    # tmp_app_path = os.path.join(dest_path, name, name+"-tmp", "wrapper")
    if not os.path.exists(tmp_app_path):
        os.makedirs(tmp_app_path)
    copydir(origin_path, tmp_app_path)
    # 2. split xx-app and xx-libs
    origin_libs_path = os.path.join(tmp_app_path, "WEB-INF", "lib")
    copydir(origin_libs_path, libs_path)
    # 2.1 remove libs in xx-app
    shutil.rmtree(origin_libs_path)
    # 2.2 add extra javax into libs
    for f in os.listdir(javax_libs):
        shutil.copy(os.path.join(javax_libs, f), libs_path)
    # 3. build jar for jar app
    # 3.1 split wrapper and classes
    wrapper_path = os.path.join(tmp_app_path, "wrapper")
    os.mkdir(wrapper_path)
    for f in os.listdir(tmp_app_path):
        if not (f == "WEB-INF" or f == "META-INF"):
            shutil.move(os.path.join(tmp_app_path,f),wrapper_path)
    for f in os.listdir(os.path.join(tmp_app_path, "WEB-INF")):
        shutil.move(os.path.join(tmp_app_path,"WEB-INF", f),tmp_app_path)
    shutil.rmtree(os.path.join(tmp_app_path,"WEB-INF"))
    for f in os.listdir(os.path.join(tmp_app_path, "classes")):
        if not os.path.exists(os.path.join(tmp_app_path, f)):
            shutil.move(os.path.join(tmp_app_path,"classes", f), tmp_app_path)
    if len(os.listdir(os.path.join(tmp_app_path, "classes"))) == 0:
        shutil.rmtree(os.path.join(tmp_app_path, "classes"))
    # 3.2 build jar
    cmd = "jar -cf0 " + \
        os.path.join(app_path, name + "-app.jar ") + "./"
    os.chdir(tmp_app_path)
    # added_files = ""
    # for i in os.listdir(tmp_app_path):
    #     if not i == "WEB-INF":
    #         added_files =  added_files + " -C " + tmp_app_path + " "  + i
    # cmd = "jar -cf0 " + \
    #     os.path.join(app_path, name + "-app.jar ") +   added_files  + " -C " + os.path.join(tmp_app_path,'WEB-INF') + " " + "."
    os.system(cmd)
    os.chdir(dest_path)
    # 3.1 remove tmp
    shutil.rmtree(tmp_app_path)


def normal_wars(target_path):
    names = [
        'icloud', 'jpetstore', 'Logistics_Manage_System', 'NewsSystem', 'Shop', 'spring-mvc-showcase',
        'OpenKM', 'logicaldoc'
    ]
    tomcats = [
        'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-icloud',
        'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-jpetstore',
        'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-LogisticsManageSystem',
        'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-NewsSystem',
        'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-mission-Shop',
        'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-spring-mvc-showcase',
        'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-OpenKM',
        'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-logicaldoc\\tomcat'
    ]
    for i in range(len(names)):
        webapp_name = names[i]
        tomcat_dir = tomcats[i]
        originalWAR = os.path.join(tomcat_dir, 'webapps-original', webapp_name)
        build_app_libs_war(originalWAR, target_path, webapp_name)


def subset_wars(target_path):
    # struts2-official-samples
    testcasesDir1 = 'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-struts2-official-samples\webapps-original'
    for name in os.listdir(testcasesDir1):
        if(os.path.isdir(os.path.join(testcasesDir1, name))):
            build_app_libs_war(os.path.join(testcasesDir1, name), os.path.join(
                target_path, 'struts2-official-samples'), name)
    # struts2-vulns
    testcasesDir2 = 'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-struts2-vulns\webapps-original'
    for name in os.listdir(testcasesDir2):
        if(os.path.isdir(os.path.join(testcasesDir2, name))):
            build_app_libs_war(os.path.join(testcasesDir2, name), os.path.join(
                target_path, 'struts2-vulns'), name)

def build_app_libs_jar(jar_dir, dest_path, name):
    app_path = os.path.join(dest_path, name, name+"-app")
    libs_path = os.path.join(dest_path, name, name+"-libs")
    if not os.path.exists(app_path):
        os.makedirs(app_path)
    if not os.path.exists(libs_path):
        os.makedirs(libs_path)
    dest_path2 = os.path.join(dest_path, name)
    # 1. copy jar to target
    jar_path = os.path.join(jar_dir, name+'.jar')
    shutil.copy(jar_path, dest_path2)
    # 2. unzip
    tmp_app_path = os.path.join(dest_path, name, name+"-tmp")
    if not os.path.exists(tmp_app_path):
        os.makedirs(tmp_app_path)
    os.chdir(tmp_app_path)
    cmd1 = 'jar -xf ' + os.path.join(dest_path2, name+".jar")
    os.system(cmd1)
    # 3. split xx-app and xx-libs
    origin_libs_path = os.path.join(tmp_app_path, 'BOOT-INF', "lib")
    copydir(origin_libs_path, libs_path)
    # 3.1 remove libs in xx-app
    shutil.rmtree(origin_libs_path)
    # 3.2 build jar
    origin_classes_dir = os.path.join(tmp_app_path, 'BOOT-INF', "classes")
    cmd = "jar -cf0 " + \
        os.path.join(app_path, name + "-app.jar ") + "./"
    os.chdir(origin_classes_dir)
    os.system(cmd)
    os.chdir(dest_path)
    # 3.3 remove tmp
    shutil.rmtree(tmp_app_path)
    os.remove(os.path.join(dest_path2, name+".jar"))

def normal_jars(target_path):
    names = ['community', 'newbeeMall',
             'springPetclinic', 'sell', 'halo', 'ruoyi-admin']
    dir_root = 'F:\Framework\InputRunnableTestcases\\runnableJars'
    dirs_sub = ['community', 'newbeeMall', 'springPetclinic',
                'wechatResturant', 'halo', 'ruoyi']
    for i in range(len(names)):
        webapp_name = names[i]
        app_dir = os.path.join(dir_root, dirs_sub[i])
        print(app_dir)
        build_app_libs_jar(app_dir, target_path, webapp_name)

javax_libs = "F:\myProject\webframeworkmodelinfer\ict.pag.webframework.infer\data\libs"
target_path = 'F:\Framework\InputRunnableTestcases\JackEE_Format'
normal_wars(target_path)
normal_jars(target_path)
subset_wars(target_path)
