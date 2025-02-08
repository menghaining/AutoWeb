# this script aims to  copy local JackEE format jars to the docker on server
# used on server
import os


def copy2docker(src, docker_dest):
    cmd = 'podman cp ' + src + ' ' + docker_dest
    # print(cmd)
    os.system(cmd)


dir = '/home/mhn330/jackee/input-projects/'
docker_dest = '8383f53993d3:/home/pldi2020/benchmark-jars/'
for case in os.listdir(dir):
    origin_path = os.path.join(dir, case)
    if os.path.isdir(origin_path):
        if case == 'struts2-vulns' or case == 'struts2-official-samples':
            for sub_case in os.listdir(origin_path):
                sub_origin_path = os.path.join(origin_path, sub_case)
                copy2docker(sub_origin_path, docker_dest)
        else:
            copy2docker(origin_path, docker_dest)
