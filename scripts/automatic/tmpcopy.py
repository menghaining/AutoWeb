# use to cpoy
import os, shutil
# dest_path = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\struts2-official-samples'
# src_path = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs-5-12th\struts2-official-samples'

dest_path='F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs\struts2-vulns'
src_path='F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs-5-12th\struts2-vulns'

count = 0
for app in os.listdir(src_path):
    from_path = os.path.join(src_path, app)
    to_path = os.path.join(dest_path, app)
    if not  os.path.exists(to_path):
        print('copy to : ' + to_path)
        shutil.copytree(from_path, to_path)
        count = count + 1
print(count)


# path1 = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs'
# path2 = 'F:\myProject\webframeworkmodelinfer\ict.pag.webframework.preInstrumental\outs-5-12th'

# apps = ['OpenKM', 'logicaldoc', 'halo', 'ruoyi']

# for app in apps:
#     app_dir = os.path.join(path1, app)
#     src_dir = os.path.join(path2, app)
#     # 1. deloy
#     df_path1 = os.path.join(app_dir, 'deploy','out-preModify')
#     src_df_path1 = os.path.join(src_dir, 'deploy','out-preModify','modifiy_details.json')
#     if not os.path.exists(df_path1):
#         os.makedirs(df_path1)
#     shutil.copy(src_df_path1, df_path1)

#     dl_path1 = os.path.join(app_dir, 'deploy','testcaseLogs')
#     src_dl_path = os.path.join(src_dir, 'deploy','testcaseLogs')
#     shutil.copytree(src_dl_path, dl_path1)

#     #2. req 
#     f_path2 = os.path.join(app_dir, 'out-preModify')
#     src_f_path2 = os.path.join(src_dir, 'out-preModify','modifiy_details.json')
#     if not os.path.exists(f_path2):
#         os.makedirs(f_path2)
#     shutil.copy(src_f_path2, f_path2)

#     l_path2 = os.path.join(app_dir, 'testcaseLogs')
#     src_l_path2 = os.path.join(src_dir, 'testcaseLogs')
#     shutil.copytree(src_l_path2, l_path2)

#     print(app + ' done ')
