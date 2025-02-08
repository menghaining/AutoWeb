# only record lines with [ReqStart] and [ReqEnd]
from operator import mod
import os
import sys
import time


def extract_all_reqs(file_path):
    reqs_list = []
    with open(file_path, 'r', encoding='utf-8') as file:
        for line in file:
            line = line.strip('\n')
            reqs_list.append(line)
    return reqs_list


def parse_request(data):
    # print("\n" + data)
    rmh = data[len("[ReqStart]"):]
    line = rmh[rmh.index("]")+1:]

    url = line[:line.index("[hashcode]")]
    line = line[len(url)+len("[hashcode]"):]
    hashcode = line[:line.index("[method]")]
    line = line[len(str(hashcode))+len("[method]"):]
    mtd = line[:line.index("[queryString]")]
    line = line[len(mtd)+len("[queryString]"):]
    query_string = line[:line.index("[param]")]
    line = line[len(query_string)+len("[param]"):]
    param = line[:line.index("[pathInfo]")]
    line = line[len(param)+len("[pathInfo]"):]
    pathInfo = line[:line.index("[user]")]
    line = line[len(pathInfo)+len("[user]"):]
    user = line[:line.index("[sessionId]")]
    line = line[len(user)+len("[sessionId]"):]
    sessionId = line[:line.index("[cookie]")]
    cookie = line[len(sessionId)+len("[cookie]"):]

    request_dict = {'url': url, 'hashcode': hashcode, 'method': mtd, 'queryString': query_string,
                    'param': param, 'pathInfo': pathInfo, 'user': user, 'sessionId': sessionId, 'cookie': cookie}
    # print(request_dict)
    return request_dict


def parse_response(data):
    # print("\n" + data)
    rmh = data[len("[ReqEnd]"):]
    line = rmh[rmh.index("]")+1:]

    url = line[:line.index("[hashcode]")]
    line = line[len(url)+len("[hashcode]"):]
    hashcode = line[:line.index("[headers-cookie]")]
    line = line[len(str(hashcode))+len("[headers-cookie]"):]
    setcookie = line[:line.index("[status-code]")]
    status_code = line[len(str(setcookie))+len("[status-code]"):]

    response_dict = {'url': url, 'hashcode': hashcode,
                     'setcookie': setcookie, 'status_code': status_code}
    # print(response_dict)
    return response_dict


def extract_curls(data):
    urlSeq_list = []
    for index, line in enumerate(data):
        if mod(index, 2) == 0:
            req = data[index]
            req_info = parse_request(req)

            # cookie_type: is same as before? none:new, same:same as before
            cookie_type = 'none'
            if index/2 == 0:
                # do not care about previous cookie
                print('first curl')
            else:
                pre_req = data[index-2]
                pre_res = data[index-1]
                pre_req_info = parse_request(pre_req)
                if (pre_req_info.get('sessionId') == req_info.get('sessionId')) and (req_info.get('sessionId') != 'null'):
                    # same as the cookie as previous req
                    cookie_type = 'same'
                elif pre_res != 'none':
                    curr_sessionid = req_info.get('sessionId')
                    pre_res_info = parse_response(pre_res)
                    pre_res_cookie = pre_res_info.get('setcookie')
                    if pre_res_cookie != '[]':
                        tmp = pre_res_cookie[pre_res_cookie.index(
                            'SESSIONID=')+len('SESSIONID='):]
                        if not ";" in tmp:
                            pre_sessionid = tmp[:len(tmp)-1]
                        else:
                            pre_sessionid = tmp[:tmp.index(";")]
                        if pre_sessionid == curr_sessionid:
                            cookie_type = 'same'
                        # print(pre_sessionid)
            req_url_dict = {'url': req_info.get('url'), 'method': req_info.get(
                'method'), 'param': req_info.get('param'), 'cookie_type': cookie_type, 'queryString': req_info.get('queryString')}
            # print(req_url_dict)
            urlSeq_list.append(req_url_dict)
    return urlSeq_list


def build_request(data, cookie):
    # use "" insteadof ''
    cmd = 'curl -X ' + \
        data.get('method') + ' ' + data.get('url')
    if len(data.get('queryString')) != 0:
        if(data.get('queryString') != "null"):
            queryString = data.get('queryString')
            cmd = cmd + '?' + queryString
    if cookie != 'none':
        cmd = cmd+' --cookie ' + '\"'+cookie+'\"'
    if len(data.get('param')) != 0:
        params = data.get('param')
        cmd = cmd+' -d ' + '\"'+params[:len(params)-1]+'\"'
    return cmd
    # os.system(cmd)


def get_runtime_cookie(is_same, runtime_log, pre_cookie):
    if is_same == 'none':
        return 'none'
    else:
        if pre_cookie != 'none':
            return pre_cookie
        else:
            cookie = 'none'
            with open(runtime_log, 'r', encoding='utf-8', errors='ignore') as fp:
                lines = fp.readlines()
                i = -1
                count = 0
                while(abs(i) <= len(lines)):
                    line = lines[i]
                    if line.startswith('[ReqStart]') and count == 0:
                        # may exception occurs
                        break
                    elif line.startswith('[ReqEnd]'):
                        count = count+1
                        res_info = parse_response(line)
                        tmp = res_info.get('setcookie')
                        if tmp != '[]':
                            cookie = tmp[1:len(tmp)-1]
                        break
                    i = i-1
            return cookie


def runSingleTestcase(requestSequenceFile, runtime_log, singleReqTime):
    # step1. extract
    data = extract_all_reqs(requestSequenceFile)
    print('total lines: ' + str(len(data)))

    # step2.
    if mod(len(data), 2) != 0:
        print('ERROR: file lines is odd!')
    else:
        print('calculating...')
        url_list = extract_curls(data)
        pre_cookie = 'none'
        for i in range(len(url_list)):
            line = url_list[i]
            cookie = get_runtime_cookie(
                line.get('cookie_type'), runtime_log, pre_cookie)
            # update current session cookie value
            pre_cookie = cookie
            cmd = build_request(line, cookie)
            print(str(i)+" : "+cmd + " [cookie-type] "+line.get('cookie_type'))
            os.system(cmd)
            # time.sleep(3)
            time.sleep(singleReqTime)


if __name__ == '__main__':
    # requestSequenceFile = extract_all_reqs(
    #     'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\spring-mvc-showcase\out-preModify\\1\\requestsTrigger.txt')
    # runtime_log = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\spring-mvc-showcase\\testcaseLogs\\1\catalina.txt'

    # input arguments:
    # arg[0] :
    # arg[1] :
    requestSequenceFile = sys.argv[1]
    runtime_log = sys.argv[2]
    runSingleTestcase(requestSequenceFile, runtime_log, 3)
