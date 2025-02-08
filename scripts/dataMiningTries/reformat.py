import sys


file_path = sys.argv[1]
file = open(file_path)
list = []
for line in file:
    tmp = []
    line = line.strip('\n')
    if "[anno]" in line:
        str2 = "@" + line[line.rindex(".")+1:]
        print(str2)
# list.sort()
# for tmp in list:
#     if len(tmp) == 1:
#         print(tmp[0])