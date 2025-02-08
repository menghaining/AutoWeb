#  aims to produce JackE facts in bulk
#  used in docker

import os
jackee_names = ['alfresco','bitbucket-server', 'dotCMS', 'opencms', 'shopizer', 'SpringBlog', 'pybbs', 'WebGoat']
extends_names = [
    'community',
    'halo',
    'icloud',
    'jpetstore',
    'logicaldoc',
    'Logistics_Manage_System',
    'newbeeMall',
    'NewsSystem',
    'OpenKM',
    'ruoyi-admin',
    'sell',
    'Shop',
    'spring-mvc-showcase',
    'springPetclinic',
    'action-chaining',
    'annotations',
    'basic-struts',
    'bean-validation',
    'blank',
    'coding-actions',
    'control-tags',
    'crud',
    'debugging-struts',
    'exception-handling',
    'exclude-parameters',
    'file-upload',
    'form-processing',
    'form-tags',
    'form-xml-validation',
    'form_validation',
    'hello-world',
    'http-session',
    'interceptors',
    'json',
    'json-customize',
    'mailreader2',
    'message-resource',
    'message-store',
    'portlet',
    'preparable-interface',
    'rest-angular',
    'reststyleactionmapper',
    'shiro-basic',
    'spring-struts',
    'text-provider',
    'themes',
    'themes-override',
    'tiles',
    'type-conversion',
    'unit-testing',
    'unknown-handler',
    'using-tags',
    'validation-messages',
    'wildcard-method-selection',
    'wildcard-regex',
    's2-059_war',
    's2_001_war_exploded',
    's2_003_war_exploded',
    's2_005_war_exploded',
    's2_007_war_exploded',
    's2_008_war_exploded',
    's2_009_war_exploded',
    's2_012_war_exploded',
    's2_013_war_exploded',
    's2_015_war_exploded',
    's2_016_war_exploded',
    's2_032_war_exploded',
    's2_045_war_exploded',
    's2_046_war_exploded',
    's2_053_war_exploded',
    's2_061_war_exploded'
]

# 1. run jackee benchmarks with mhn0202.dl
for name in jackee_names:
    cmd = '/home/pldi2020/runSingleCIBench-mhn0202.sh ' + name
    print(cmd)
    os.system(cmd)

# 2. run extends benchmarks with mhn0202.dl
for name in extends_names:
    cmd = '/home/pldi2020/runSingleCIBench-mhn0202.sh ' + name
    print(cmd)
    os.system(cmd)