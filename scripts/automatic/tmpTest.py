## this file used to calculate the diff between limit and no limit cases 
import os, runJarAll_diff

path1 = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\struts2-official-samples'
path2 = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs-5-12th\struts2-official-samples'

# path1='F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\struts2-vulns'
# path2='F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs-5-12th\struts2-vulns'

same = []
notsame = []
needRun = 0
re_runs = 0
for case in os.listdir(path1):
    deploy1 = os.path.join(path1, case, 'deploy', 'out-preModify')
    reqdir1 = os.path.join(path1, case, 'out-preModify')
    deploy2 = os.path.join(path2, case, 'deploy', 'out-preModify')
    reqdir2 = os.path.join(path2, case, 'out-preModify')

    print(case + ':')
    print('deploy : ' + str(len(os.listdir(deploy1))) +
          ' : ' + str(len(os.listdir(deploy2))))
    print('request : ' + str(len(os.listdir(reqdir1))) +
          ' : ' + str(len(os.listdir(reqdir2))))
    if len(os.listdir(deploy1)) == len(os.listdir(deploy2)) and len(os.listdir(reqdir1)) == len(os.listdir(reqdir2)):
        same.append(case)
    else:
        notsame.append(case)
        needRun = needRun + len(os.listdir(deploy1)) + len(os.listdir(reqdir1)) - 2
    
    # deploy_json1 = os.path.join(deploy1,'modifiy_details.json')
    # deploy_json2 = os.path.join(deploy2, 'modifiy_details.json')
    # deploy_res = runJarAll_diff.extarct_cases(deploy_json1, deploy_json2)
    # deploy_re_run = deploy_res[0]
    # req_json1 = os.path.join(reqdir1,'modifiy_details.json')
    # req_json2 = os.path.join(reqdir2, 'modifiy_details.json')
    # req_res = runJarAll_diff.extarct_cases(req_json1, req_json2)
    # req_re_run = req_res[0]
    # print('this case need to re-run : [deploy]' + str(len(deploy_re_run)) + ' [req]' + str(len(req_re_run)))
    # re_runs = re_runs + len(req_re_run) + len(deploy_re_run)

print('same: ' + str(len(same)))
print(same)
print('not same : ' + str(len(notsame)))
print(notsame)
print('need to re-run cases count: ' + str(needRun))
# print('need to re-run cases count2: ' + str(re_runs))