import os
##struts2-official-samples
# logs_dir = 'F:/myProject/webframeworkmodelinfer/ict.pag.webframework.preInstrumental/outs/struts2-official-samples'
# apps = 'F:/Framework/InputRunnableTestcases/apache-tomcat-8-with-wars/tomcat-struts2-official-samples/webapps-original'

##struts2-vulns
logs_dir = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\struts2-vulns'
apps = 'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-struts2-vulns\webapps-original'

# for name in os.listdir(logs_dir):
#     path1 = os.path.join(logs_dir, name)
#     path2 = os.path.join(apps, name)
#     if(os.path.isdir(path1)):
#         jobj = ",{ \"logDir\": \"" + path1 + "\"," + "\"webappDir\":\"" + path2 + "\"}"
#         print(jobj)

# tomcat
# dir = 'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars'
# for name in os.listdir(dir):
#     subName = name[7:]
#     p1 = os.path.join(dir,name,"webapps-original",subName)
#     p2 = os.path.join(dir,name,"logs","original","InstrumentalLog.txt")
#     print("apps.add(\"" + p1 + "\");")
#     print("logs.add(\"" + p2 + "\");")
# collection
# dir = 'F:\Framework\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-struts2-vulns'
# p10 = os.path.join(dir,"webapps-original")
# p20 = os.path.join(dir,"logs","original")
# for name in os.listdir(p10):
#     p1 = os.path.join(p10, name)
#     if(os.path.isdir(p1)):
#         p2 = os.path.join(p20, name+".txt")
#         print("apps.add(\"" + p1 + "\");")
#         print("logs.add(\"" + p2 + "\");")
# jars
dir = 'F:\Framework\InputRunnableTestcases\\runnableJars'
for name in os.listdir(dir):
    p1 = os.path.join(dir,name,name)
    p2 = os.path.join(dir,name,"original","InstrumentalLog.txt")
    print("apps.add(\"" + p1 + "\");")
    print("logs.add(\"" + p2 + "\");")