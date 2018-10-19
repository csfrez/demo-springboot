# demo-springboot
springboot demo

## Mybatis 自动生成:
- 需要配置文件:generator/generatorConfig.xml  
- 在工程根目录下 命令提示符 运行命令
- 1)解决中文乱码问题 set MAVEN_OPTS="-Dfile.encoding=UTF-8"
- 2)开始生成 mvn mybatis-generator:generate 或者 maven projects里面运行plugins-generator