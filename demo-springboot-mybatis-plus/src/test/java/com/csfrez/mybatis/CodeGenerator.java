package com.csfrez.mybatis;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.baomidou.mybatisplus.generator.fill.Column;

import java.nio.file.Paths;
import java.util.Collections;

/**
 * @author
 * @date 2025/5/26 15:55
 * @email
 */
public class CodeGenerator {
    public static void main(String[] args) {
//        System.out.println(System.getProperty("user.dir"));
//        System.out.println(Paths.get(System.getProperty("user.dir")));

        String url = "jdbc:mysql://10.11.0.142:3306/demo?useUnicode=true&characterEncoding=utf-8&allowMultiQueries=true";
        FastAutoGenerator.create(url, "root", "123456")
                .globalConfig(builder -> builder
                        .author("csfrez")
//                        .enableSwagger() // 开启 swagger 模式
                        .outputDir(Paths.get(System.getProperty("user.dir")) + "/demo-springboot-mybatis-plus/src/main/java")
                        .commentDate("yyyy-MM-dd")
                        .disableOpenDir()
                )
                .packageConfig(builder -> builder
                        .parent("com.csfrez.mybatis")
//                        .moduleName("") // 设置父包模块名
                        .entity("dao.entity")
                        .mapper("dao.mapper")
                        .controller("rest")
                        .serviceImpl("service")
//                        .xml("mapper.xml")
                        .pathInfo(Collections.singletonMap(OutputFile.xml, System.getProperty("user.dir") + "/demo-springboot-mybatis-plus/src/main/resources/mapper")) // 设置mapperXml生成路径
                )
                .strategyConfig(builder -> builder
                        .addInclude("user") // 设置需要生成的表名
                        .entityBuilder().enableLombok()
                        .addTableFills(new Column("create_time", FieldFill.INSERT))
                        .addTableFills(new Column("update_time", FieldFill.INSERT_UPDATE))
                        .addTableFills(new Column("version", FieldFill.INSERT_UPDATE))
                        .enableFileOverride()
                        .serviceBuilder().formatServiceFileName("%sService")
                        .disableServiceImpl() // 不生成serviceImpl
                        .serviceTemplate("/templates/service.java") // 自定义service模板
                        .controllerBuilder().enableRestStyle()  // 开启生成@RestController 控制器
                        .mapperBuilder().enableBaseResultMap()
                        .enableFileOverride().disableMapper()  // 不生成mapper
                )
//                .templateConfig(builder ->
//                    builder.service("/templates/service.java")// 自定义service模板
////                            .disable(TemplateType.CONTROLLER) // 不生成Controller 需要时开启
//                            .disable(TemplateType.SERVICE_IMPL) //不生成ServiceImpl
//                )
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();
    }
}
