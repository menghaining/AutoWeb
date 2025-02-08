import sys

answer_file_path = sys.argv[1]
answer_file = open(answer_file_path)
answer_list = []
for line in answer_file:
    line = line.strip('\n')
    answer_list.append(line)
    # print(line)

file_path = sys.argv[2]
file = open(file_path)
list = []
for line in file:
    line = line.strip('\n')
    list.append(line)
    # print(line)

# compare:
# in file1 but not in file2
diff_set1 = set(answer_list).difference(set(list))
count = 0
print('------answer more:')
more1=[]
for i in diff_set1:
    count = count + 1
    # print(str(count) + "." + i)
    # print(i)
    more1.append(i)
more1.sort()
for i in more1:
    print(i)
    
# in file2 but not in file1
diff_set2 = set(list).difference(set(answer_list))
count = 0
print('------report more:')
more2=[]
for i in diff_set2:
    count = count + 1
    # print(str(count) + "." + i)
    # print(i)
    more2.append(i)
more2.sort()
for i in more2:
    print(i)
    

print("[all report] " + str(len(list)))
print("[all answer] " + str(len(answer_list)))
print("[TP] " + str(len(list) - len(diff_set2)))
print("[FP] " + str(len(diff_set2)))
print("[FN] " + str(len(diff_set1)))