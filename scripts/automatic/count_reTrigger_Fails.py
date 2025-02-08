## This script aims to count the number of fail cases
import os
import pandas as pd

dir_path = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs'

def count_deploy_status(path):
    successes = []
    fails = []
    if os.path.exists(path):
        for i in os.listdir(path):
            # print(i)
            log_path = os.path.join(path, i, 'catalina.txt')
            success_deploy = False
            with open(log_path, 'r', encoding='utf-8',errors='ignore') as f:
                line = f.readline()
                # lines = f.readlines()
                # for line in lines:
                while(line):
                    if line.startswith('[ReqStart]'):
                        success_deploy = True
                        break
                    line = f.readline()
            if success_deploy:
                successes.append(i)
            else:
                fails.append(i)
    return [successes, fails]


count_apps = 0
# print('id\tapplication\tsuccess1\tfail1\tsuccess2\tfail2')
result = pd.DataFrame(index=['application','success1','fail1','success2','fail2','success%','fail%'])

for app_name in os.listdir(dir_path):
    if not (app_name.endswith('-torun') or app_name.endswith('-patch')): 
        app_dir = os.path.join(dir_path, app_name)
        if app_name=='struts2-official-samples' or app_name == 'struts2-vulns':
            for sub_app_name in os.listdir(app_dir):
                count_apps = count_apps + 1
                # print(str(count_apps) + ' : ' + sub_app_name)
                #1. deploy
                deploy = os.path.join(app_dir, sub_app_name,'deploy', 'testcaseLogs')
                [deploy_success,deploy_fail] = count_deploy_status(deploy)
                # print('[deploy]deploy success: ' + str(len(deploy_success)))
                # print('[deploy]deploy fail: ' + str(len(deploy_fail)))
                # 2. req
                req = os.path.join(app_dir,sub_app_name,'testcaseLogs')
                [success,fail] = count_deploy_status(req)
                # print('[request]deploy success: ' + str(len(success)))
                # print('[request]deploy fail: ' + str(len(fail)))
                # print(str(count_apps) + '\t' + sub_app_name + '\t' + str(len(deploy_success))+'\t' +  str(len(deploy_fail)) + '\t' + str(len(success)) + '\t' + str(len(fail)))
                if not count_apps in result.columns:
                    result[count_apps] = 0.0
                result[count_apps]['application']= sub_app_name
                result[count_apps]['success1'] = len(deploy_success)
                result[count_apps]['fail1'] = len(deploy_fail)
                result[count_apps]['success2']= len(success)
                result[count_apps]['fail2'] = len(fail)
                if((len(deploy_success)+len(success)+len(deploy_fail)+len(fail)) == 0):
                    result[count_apps]['success%'] = '-'
                    result[count_apps]['fail%'] = '-'
                else:
                    result[count_apps]['success%'] = '{:.2f}%'.format((len(deploy_success)+len(success))/(len(deploy_success)+len(success)+len(deploy_fail)+len(fail))*100)
                    result[count_apps]['fail%'] = '{:.2f}%'.format((len(deploy_fail)+len(fail))/(len(deploy_success)+len(success)+len(deploy_fail)+len(fail))*100)
        else:
            count_apps = count_apps + 1
            # print(str(count_apps) + ' : ' + app_name)
            #1. deploy
            deploy = os.path.join(app_dir, 'deploy', 'testcaseLogs')
            [deploy_success,deploy_fail] = count_deploy_status(deploy)
            # print('[deploy]deploy success: ' + str(len(deploy_success)))
            # print('[deploy]deploy fail: ' + str(len(deploy_fail)))
            # 2. req
            req = os.path.join(app_dir, 'testcaseLogs')
            [success,fail] = count_deploy_status(req)
            # print('[request]deploy success: ' + str(len(success)))
            # print('[request]deploy fail: ' + str(len(fail)))
            # print(str(count_apps) + '\t' + app_name + '\t' + str(len(deploy_success))+'\t' +  str(len(deploy_fail)) + '\t' + str(len(success)) + '\t' + str(len(fail)))
            if not count_apps in result.columns:
                result[count_apps] = 0.0
            result[count_apps]['application']= app_name
            result[count_apps]['success1'] = len(deploy_success)
            result[count_apps]['fail1'] = len(deploy_fail)
            result[count_apps]['success2']= len(success)
            result[count_apps]['fail2'] = len(fail)
            if((len(deploy_success)+len(success)+len(deploy_fail)+len(fail)) == 0):
                result[count_apps]['success%'] = '-'
                result[count_apps]['fail%'] = '-'
            else:
                result[count_apps]['success%'] = '{:.2f}%'.format((len(deploy_success)+len(success))/(len(deploy_success)+len(success)+len(deploy_fail)+len(fail))*100)
                result[count_apps]['fail%'] = '{:.2f}%'.format((len(deploy_fail)+len(fail))/(len(deploy_success)+len(success)+len(deploy_fail)+len(fail))*100)

pd.set_option('display.max_columns', None)
pd.set_option('display.max_rows', None)
pd.set_option('max_colwidth',4000)

result.to_excel('./trigger_info.xlsx', sheet_name='result', index=False)

# print(result.T)