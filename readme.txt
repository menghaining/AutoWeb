1. 输入运行环境：F:\Framework\InputRunnableTestcases
2. 输入测试original：F:\Framework\InputRunnableTestcases 各个文件夹里
3. 原始的插桩log：F:\Framework\InputRunnableTestcases 各个文件夹 original文件夹
4. mutated 结果：F:\myProject\webframeworkinfer_testcases\modified-preInstrumental\outs 内为最新
5. run mutation脚本：F:\myProject\webframeworkmodelinfer\scripts\automatic ：runWarAll.py和runJarAll.py 按需修改是运行全部的；trigger4StrutsTestcases.py是给struts两个集合写的 trigger脚本
6. 最终推断的结果+app包含配置-可达配置+推断配置_diff：F:\myProject\webframeworkmodelinfer\ict.pag.webframework.infer\outs\runnable-0907 （最新）
	更新：最新在F:\myProject\webframeworkmodelinfer\ict.pag.webframework.infer\outs\runnable-1102中
7. 对比脚本在：F:\myProject\webframeworkmodelinfer\scripts 中

所有整理出的结果在：F:\myProject\webframeworkinfer_resultRecords\Results\0821

主要文件夹：
1. F:\myProject\webframeworkmodelinfer 项目代码所在地
2. F:\myProject\webframeworkinfer_resultRecords 分析与汇总结果，包括间接结果
3. F:\Framework\InputRunnableTestcases 输入环境

使用步骤：
1. ict.pag.m.Instrumentation ：用于构建运行时插桩的jar包
2. ict.pag.webframework.preInstrumental ：根据运行时log变异配置，为每个输入项目得到一组变异用例
3. ict.pag.webframework.infer ：根据变异用例的运行结果，推断出最终结果