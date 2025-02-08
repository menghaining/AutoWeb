import os, sys, shutil, time
import deploy
import sendRequests
import threading


def run(webapp_name, testcases_dir, instrumental,answer_dir, deployTime, singleReqTime):
    time_start = time.time()
    id = 1
    while(os.path.exists(os.path.join(testcases_dir,str(id)))):
        print('\n...deal with...' + os.path.join(testcases_dir,str(id)))
    
        # preparation
        runnableJar = os.path.join(testcases_dir,str(id), webapp_name+'-updated.jar')

        dstpath = os.path.join(answer_dir, str(id))
        if not os.path.exists(dstpath):
            os.makedirs(dstpath)
        runtime_log = os.path.join(dstpath, 'catalina.txt')
        if os.path.exists(runtime_log):
            os.remove(runtime_log)

        # 1. deploy
        print('\n...step 1. deploy remaked testcase')
        t=threading.Thread(target=deploy.deploy_jar, args=(runnableJar, instrumental, runtime_log))
        t.start()

        # time.sleep(50)
        time.sleep(deployTime)

        # 2. send requests
        print('\n...step 2. send requests')
        requestSequenceFile = os.path.join(testcases_dir, str(id), 'requestsTrigger.txt')
        if not os.path.exists(runtime_log):
            f = open(runtime_log, "x")
        sendRequests.runSingleTestcase(requestSequenceFile, runtime_log, singleReqTime)

        time.sleep(10)

        # 3. shutdown
        print('\n...step 3. shut down')
        deploy.shutdown_jar()

        time.sleep(5)

        id = id + 1

    time_end = time.time()

    print('totally cost: ', time_end-time_start)
    print('finish at ' + time.strftime('%Y-%m-%d %H:%M:%S',time.localtime(time.time()))) 

if __name__ == '__main__':
    webapp_name = sys.argv[1]
    testcases_dir = sys.argv[2]
    instrumental = sys.argv[3]
    answer_dir = sys.argv[4]
    run(webapp_name, testcases_dir, instrumental,answer_dir, 50, 3)