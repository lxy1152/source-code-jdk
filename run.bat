@echo off

:: 执行前的路径
set currentPath=%cd%

:: rt.jar所在文件
set rtPath=%JAVA_HOME%\jre\lib

:: 进入编译后class文件所在目录
cd /d target\classes

echo copy rt.jar to rt folder
copy %rtPath%\rt.jar .\
echo rt.jar copied into rt folder

:: 替换jar包中class文件
echo replace class files
jar -uf0 rt.jar com
jar -uf0 rt.jar java
jar -uf0 rt.jar javax
jar -uf0 rt.jar org
echo class files replaced

:: 一个新的jar包生成完毕
copy rt.jar ..\
echo new rt.jar output to: .\source\target\rt.jar

:: 替换原有的rt.jar
copy rt.jar %rtPath%
del rt.jar

:: 执行完成返回原来的目录
echo complete
cd /d %currentPath%