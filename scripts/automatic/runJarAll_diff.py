# this is to run Jar All different muatations
# if the mutation is same as before, just copy; else run
import os
import shutil
import sys
import time
import threading
import json
import runJar
import runJar_deploy
import deploy
import sendRequests


def deploy_one(id, webapp_name, testcases_dir, instrumental, answer_dir, deployTime):
    if(os.path.exists(os.path.join(testcases_dir, str(id)))):
        print('\n...deal with deploy...' + os.path.join(testcases_dir, str(id)))
        # preparation
        runnableJar = os.path.join(
            testcases_dir, str(id), webapp_name+'-updated.jar')
        dstpath = os.path.join(answer_dir, str(id))
        if not os.path.exists(dstpath):
            os.makedirs(dstpath)
        runtime_log = os.path.join(dstpath, 'catalina.txt')
        if os.path.exists(runtime_log):
            os.remove(runtime_log)
        # 1. deploy
        print('\n...step 1. deploy remaked testcase')
        t = threading.Thread(target=deploy.deploy_jar, args=(
            runnableJar, instrumental, runtime_log))
        t.start()
        time.sleep(deployTime)
        # 1.1 validate deploy success or not
        requestSequenceFile = os.path.join(
            testcases_dir, str(id), 'requestsTrigger.txt')
        if not os.path.exists(runtime_log):
            f = open(runtime_log, "x")
        d1 = sendRequests.extract_all_reqs(requestSequenceFile)
        if(len(d1) > 2):
            url0 = d1[2]  # 0 and 1 is deploy string
            req_info = sendRequests.parse_request(url0)
            req_url_dict = {'url': req_info.get('url'), 'method': req_info.get(
                'method'), 'param': req_info.get('param'), 'cookie_type': 'none', 'queryString': req_info.get('queryString')}
            cmd = sendRequests.build_request(req_url_dict, 'none')
            print('valid : ' + cmd)
            os.system(cmd)
            time.sleep(5)
        # 2. shutdown
        print('\n...step 2. shut down')
        deploy.shutdown_jar()
        time.sleep(5)
        # id = id + 1


def deploy_process(case_ids, webapp_name, testcases_dir, instrumental, answer_dir, deployTime):
    time_start = time.time()
    # id = 1
    for id in case_ids:
        print('dealing with deploy process ' +
              str(id) + ' in ' + testcases_dir)
        deploy_one(id, webapp_name, testcases_dir,
                   instrumental, answer_dir, deployTime)
    time_end = time.time()
    print('totally cost: ', time_end-time_start)
    print('finish at ' + time.strftime('%Y-%m-%d %H:%M:%S',
          time.localtime(time.time())))


def req_one(id, webapp_name, testcases_dir, instrumental, answer_dir, deployTime, singleReqTime):
    if(os.path.exists(os.path.join(testcases_dir, str(id)))):
        print('\n...deal with request...' +
              os.path.join(testcases_dir, str(id)))
        # preparation
        runnableJar = os.path.join(
            testcases_dir, str(id), webapp_name+'-updated.jar')
        dstpath = os.path.join(answer_dir, str(id))
        if not os.path.exists(dstpath):
            os.makedirs(dstpath)
        runtime_log = os.path.join(dstpath, 'catalina.txt')
        if os.path.exists(runtime_log):
            os.remove(runtime_log)
        # 1. deploy
        print('\n...step 1. deploy remaked testcase')
        t = threading.Thread(target=deploy.deploy_jar, args=(
            runnableJar, instrumental, runtime_log))
        t.start()
        time.sleep(deployTime)
        # 2. send requests
        print('\n...step 2. send requests')
        requestSequenceFile = os.path.join(
            testcases_dir, str(id), 'requestsTrigger.txt')
        if not os.path.exists(runtime_log):
            f = open(runtime_log, "x")
        sendRequests.runSingleTestcase(
            requestSequenceFile, runtime_log, singleReqTime)
        time.sleep(10)
        # 3. shutdown
        print('\n...step 3. shut down')
        deploy.shutdown_jar()
        time.sleep(5)


def req_process(case_ids, webapp_name, testcases_dir, instrumental, answer_dir, deployTime, singleReqTime):
    time_start = time.time()
    for id in case_ids:
        print('dealing with req process ' +
              str(id) + ' in ' + testcases_dir)
        req_one(id, webapp_name, testcases_dir, instrumental,
                answer_dir, deployTime, singleReqTime)
    time_end = time.time()
    print('totally cost: ', time_end-time_start)
    print('finish at ' + time.strftime('%Y-%m-%d %H:%M:%S',
          time.localtime(time.time())))


def load_json(path):
    with open(path, 'r') as f:
        data = json.load(f)
    cases = []
    for group in data:
        url = group.get('url')
        for case in group.get('testcases'):
            case['url'] = url
            cases.append(case)
    return cases


def isSame(data, before_data):
    if not (data['url'] == before_data['url']
            and data['appear'] == before_data['appear']
            and data['statement'] == before_data['statement']
            and data['position'] == before_data['position']
            and data['trigger'] == before_data['trigger']
            and data['type'] == before_data['type']
            and data['way'] == before_data['way']
            and len(data['configurationContent']) == len(before_data['configurationContent'])
            and len(data['checkBody']) == len(before_data['checkBody'])):
        return False

    d1 = data['configurationContent']
    d2 = before_data['configurationContent']
    for i in range(len(d1)):
        if not (d1[i] == d2[i]):
            return False
    dd1 = data['checkBody']
    dd2 = before_data['checkBody']
    for i in range(len(dd1)):
        if not (dd1[i] == dd2[i]):
            return False
    return True

# copy_dic is curr:before


def extarct_cases(curr_json_file, before_json_file):
    curr_content = load_json(curr_json_file)
    before_content = load_json(before_json_file)
    print('current cases count: ' + str(len(curr_content)))
    print('before cases count: ' + str(len(before_content)))

    before_index = 0
    copy_dic = {}
    re_run = []
    counted = []  # counted in before
    for i in range(0, len(curr_content)):
        data = curr_content[i]
        if before_index < len(before_content):
            # op1: not in order
            flag = False
            for j in range(before_index, len(before_content)):
                if j in counted:
                    continue
                before_data = before_content[j]
                if isSame(data, before_data):
                    flag = True
                    copy_dic[str(data.get('testcase'))] = str(
                        before_data.get('testcase'))
                    counted.append(j)
                    if j == before_index:
                        before_index = before_index + 1
                    break
            if not flag:
                re_run.append(data.get('testcase'))
            # op2: suppose all in order
            # before_data = before_content[before_index]
            # if isSame(data, before_data):
            #     copy_dic[str(data.get('testcase'))] = str(before_data.get('testcase'))
            #     before_index = before_index + 1
            # else:
            #     re_run.append(data.get('testcase'))
        else:
            re_run.append(data.get('testcase'))
    print('re-run: ' + str(re_run))
    # print(len(re_run))
    print('copy map: ' + str(copy_dic))
    # print(len(copy_dic))
    return [re_run, copy_dic]

# copy_dic is curr:before


def copy2answer(copy_dic, curr, before):
    for key, value in copy_dic.items():
        curr_path = os.path.join(curr, key)
        if not os.path.exists(curr_path):
            os.makedirs(curr_path)
        else:
            print('already exist: ' + curr_path)
            sys.exit()
        shutil.copy(os.path.join(before, value, 'catalina.txt'), curr_path)


def handle_case(name, path, before_path, instrumental, deployTime, singleReqTime):
    webapp_name = name
    testcases_dir = os.path.join(path, 'out-preModify')
    answer_dir = os.path.join(path, 'testcaseLogs')
    dtestcases_dir = os.path.join(path, 'deploy', 'out-preModify')
    danswer_dir = os.path.join(path, 'deploy', 'testcaseLogs')
    # 1. calculate the cases need to be re-run
    # 1.1 deploy
    # print('deploy...')
    delpoy_json = os.path.join(dtestcases_dir, 'modifiy_details.json')
    before_deploy_json = os.path.join(
        before_path, 'deploy', 'out-preModify', 'modifiy_details.json')
    deploy_res = extarct_cases(delpoy_json, before_deploy_json)
    deploy_re_run = deploy_res[0]
    deploy_copy_dic = deploy_res[1]
    # 1.2 sending request
    # print('req...')
    req_json = os.path.join(testcases_dir, 'modifiy_details.json')
    before_req_json = os.path.join(
        before_path,  'out-preModify', 'modifiy_details.json')
    req_res = extarct_cases(req_json, before_req_json)
    req_re_run = req_res[0]
    req_copy_dic = req_res[1]
    rerun_count = len(deploy_re_run) + len(req_re_run)
    # 2. copy the old logs
    # 2.1 deploy
    if not os.path.exists(danswer_dir):
        os.makedirs(danswer_dir)
    copy2answer(deploy_copy_dic, danswer_dir, os.path.join(
        before_path, 'deploy', 'testcaseLogs'))
    # 2.2 sending requests
    if not os.path.exists(answer_dir):
        os.makedirs(answer_dir)
    copy2answer(req_copy_dic, answer_dir,
                os.path.join(before_path, 'testcaseLogs'))
    # 3. run these new cases
    # deploy_process(deploy_re_run, webapp_name, dtestcases_dir, instrumental, danswer_dir, deployTime)
    # req_process(req_re_run, webapp_name, testcases_dir, instrumental,answer_dir, deployTime, singleReqTime)

    print('done case: ' + name)
    print('re-run count : ' + str(rerun_count))
    print('-------')


if __name__ == '__main__':
    path = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs'
    before_path = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs-5-12th'
    instrumental = 'F:\myProject\webframeworkmodelinfer\ict.pag.m.Instrumentation\\build\libs\logIntrumental.jar'

    deployTime = 60
    simgleReqTime = 5
    names = ['community', 'newbeeMall', 'springPetclinic']
    for name in names:
        handle_case(name, os.path.join(path, name), os.path.join(
            before_path, name), instrumental, deployTime, simgleReqTime)

    webapp_name = 'sell'
    sell_path = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\wechatResturant'
    before_sell_path = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs-5-12th\wechatResturant'
    handle_case(webapp_name, sell_path, before_sell_path,
                instrumental, deployTime, simgleReqTime)
