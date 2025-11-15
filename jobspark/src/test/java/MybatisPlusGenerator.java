

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.exceptions.MybatisPlusException;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.generator.AutoGenerator;
import com.baomidou.mybatisplus.generator.config.GlobalConfig;
import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.config.PackageConfig;
import com.baomidou.mybatisplus.generator.config.StrategyConfig;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.InjectionConfig;
import com.baomidou.mybatisplus.generator.config.FileOutConfig;
import com.baomidou.mybatisplus.generator.config.po.TableInfo;
import com.baomidou.mybatisplus.generator.config.TemplateConfig;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Mybatis-Plus代码生成器 快速生成 Entity、Mapper、Mapper XML、Service、Controller 等各个模块的代码
 */
@Slf4j
public class MybatisPlusGenerator {

    /**
     * RUN THIS 参考配置
     */
    public static void main(String[] args) {
        // 代码生成器
        AutoGenerator mpg = new AutoGenerator();

        // 全局配置
        String projectPath = System.getProperty("user.dir") + "/jobspark";
        mpg.setGlobalConfig(new GlobalConfig()
                .setOutputDir(projectPath + "/src/main/java")
                .setOpen(false)
                .setAuthor("Teng-Yii")
                .setSwagger2(false)
                .setBaseResultMap(false)
                .setIdType(IdType.AUTO)
                .setServiceName("%sRepository")
                .setServiceImplName("%sRepositoryImpl")
                .setEntityName("%sPO")
        );

        // 数据源配置
        mpg.setDataSource(new DataSourceConfig()
                .setUrl("jdbc:mysql://tidb-9nvom6ww0m-tidb.tidb-9nvom6ww0m-hb.jvessel2.jdcloud.com:4000/sop_test")
                .setUrl("jdbc:mysql://localhost:3306/jobspark?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false")
                .setDriverName("com.mysql.cj.jdbc.Driver")
                .setUsername("root")
                .setPassword("****")
        );

        // 包配置
        mpg.setPackageInfo(new PackageConfig()
                .setParent("com.tengYii.jobspark")
                .setMapper("infrastructure.mapper")
                .setEntity("model.po")
                .setService("infrastructure.repo")
                .setServiceImpl("infrastructure.repo.impl")
        );

        // 策略配置
        mpg.setStrategy(new StrategyConfig()
                        .setNaming(NamingStrategy.underline_to_camel)
                        .setColumnNaming(NamingStrategy.underline_to_camel)
                        .setEntityLombokModel(true)
                        .setInclude(scanner("表名"))
                        .setControllerMappingHyphenStyle(true)

        );

        // 自定义配置
        InjectionConfig cfg = new InjectionConfig() {

            @Override
            public void initMap() {
                // to do nothing
            }
        };
        List<FileOutConfig> focList = new ArrayList<>();

        focList.add(new FileOutConfig("/templates/mapper.xml.ftl") {

            @Override
            public String outputFile(TableInfo tableInfo) {
                return projectPath + "/src/main/resources/mybatis/mapper/mysql/" + tableInfo.getMapperName()
                        + StringPool.DOT_XML;
            }
        });
        cfg.setFileOutConfigList(focList);
        mpg.setCfg(cfg);
        TemplateConfig templateConfig = new TemplateConfig();
        templateConfig.setController(null);
        templateConfig.setXml(null);
        mpg.setTemplate(templateConfig);
        mpg.setTemplateEngine(new FreemarkerTemplateEngine());


        mpg.execute();
    }

    private static String scanner(String tip) {
        Scanner scanner = new Scanner(System.in);
        log.info("请输入" + tip + ": ");
        if (scanner.hasNext()) {
            String ipt = scanner.next();
            if (ipt != null && !ipt.isEmpty()) {
                return ipt;
            }
        }
        throw new MybatisPlusException("请输入正确的" + tip + "!");
    }

}
