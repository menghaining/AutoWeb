#coding:utf-8
import os
#root_path = "F:/Framework/Tesecases/openmrs/openmrs-core/api/target"
#root_path = "F:/Framework/Tesecases/openmrs/openmrs-core/api/target"
#root_path = "F:/Framework/Tesecases/pybbs-new/pybbs"
root_path = "F:\\myProject\\webFrameworkInfer\\Testcases_Records\\shopizer\\2_1_classes"

def extractAllXMLS(path):
    list_all = []
    for root, dirs, files in os.walk(path):
        
        for file in files:
            if isConcerned(file):
                list_all.append(os.path.join(root,file))
                print(os.path.join(root,file))
                #print(file)
   # print(list_all)
    return list_all
                
                

def isConcerned(file):
    
    if not file.endswith('.xml'):
        return False

    if file == 'pom.xml' or file == 'web.xml':
        return False
    # database
    if file=='hibernate.cfg.xml' or file.endswith('.hbm.xml'):
        return False
    # database tracker
    if 'liquibase' in file:
        return False
    # cache
    if 'ehcache' in file:
        return False
    # JDBC
    if 'c3p0' in file:
        return False
    # Maven
    if ('Maven' in file) or ('maven' in file):
        return False
    # log
    if 'log4j' in file:
        return False

    if 'javadoc' in file or 'properties.xml' in file:
        return False
    
 
    return True
    
extractAllXMLS(root_path)

# TODO: dismiss Logger