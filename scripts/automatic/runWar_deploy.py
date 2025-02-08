import os, sys, shutil, time
import deploy
import remakeWAR, sendRequests

def run(webapp_name, testcases_dir, tomcat_dir,answer_dir, deployTime):
    time_start = time.time()

    id = 1
    while(os.path.exists(os.path.join(testcases_dir,str(id)))):
        print('\n...deal with...' + os.path.join(testcases_dir,str(id)))
        # 1. remake war
        print('\n...step 1. remake testcase')
        originalWAR = os.path.join(tomcat_dir, 'webapps-original', webapp_name)
        targetPath = os.path.join(tomcat_dir, 'webapps')
        patchPath = os.path.join(testcases_dir, str(id))
        remakeWAR.make(originalWAR, targetPath, patchPath)

        time.sleep(15)

        # 2. deploy
        print('\n...step 2. deploy remaked testcase')
        runtime_log = os.path.join(tomcat_dir, 'logs', 'catalina.txt')
        if os.path.exists(runtime_log):
            os.remove(runtime_log)
        deploy.deploy_war(tomcat_dir)

        # time.sleep(50)
        time.sleep(deployTime)

        # 2.1 validate deploy success or not
        requestSequenceFile = os.path.join(testcases_dir, str(id), 'requestsTrigger.txt')
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

        # 3. shutdown
        print('\n...step 4. shut down')
        deploy.shutdown_war(tomcat_dir)

        time.sleep(30)

        # 4. save testcase log
        print('\n...step 5. move log')
        fpath,fname = os.path.split(runtime_log)             
        dstpath = os.path.join(answer_dir, str(id))
        if not os.path.exists(dstpath):
            os.makedirs(dstpath)                       
        shutil.move(runtime_log, os.path.join(dstpath, fname))         
        print ("move %s -> %s"%(runtime_log, dstpath + fname))

        id = id + 1

    time_end = time.time()

    print('totally cost: ', time_end-time_start)
    print('finish at ' + time.strftime('%Y-%m-%d %H:%M:%S',time.localtime(time.time()))) 

if __name__ == '__main__':
    webapp_name = sys.argv[1]
    testcases_dir = sys.argv[2]
    tomcat_dir = sys.argv[3]
    answer_dir = sys.argv[4]

    run(webapp_name, testcases_dir, tomcat_dir, answer_dir,50)