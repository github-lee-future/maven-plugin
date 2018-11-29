package cn.net.cobot.plugins;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Goal which touches a timestamp file.
 *
 * @goal touch
 * 
 * @phase process-sources
 */
// 标记mojo，mojo名称和执行时机
@Mojo(name = "updateFile", defaultPhase = LifecyclePhase.PACKAGE)
public class UpdateMojo
    extends AbstractMojo
{
    // 入参需要添加@Parameter注解
    @Parameter
    private String rootPath;
    // 集合下一级为对应单数
    @Parameter
    private List<String> files;
    /**
     * war or jar
     */
    @Parameter
    private String packag;

    // 主要业务方法
    @Override
    public void execute()
        throws MojoExecutionException
    {
        Log log = getLog();
        log.info("--- excute updateFile plugin ---");
        log.info("rootpath: " + rootPath);
        if (files != null) {
            try {
                for (String fileName:
                        files) {
                    if("cobot-platform.properties".equals(fileName)) {
                        updatePlatformProp(fileName);
                    } else if ("Findsecbugs-config.properties".equals(fileName)) {
                        updateFindsecbugsProp(fileName);
                    } else if ("web.xml".equals(fileName)) {
                        updateWebXml(fileName);
                    }
                }
                log.info("excute updateFile plugin success");
            } catch (Exception e) {
                log.info("excute updateFile plugin failed");
            }
        }
    }

    /**
     * 修改web.xml, 修改运行环境
     * @param fileName
     */
    private void updateWebXml(String fileName) {
        File file = new File("src/main/webapp/WEB-INF", fileName);
        if (file.exists()) {
            try {
                boolean isReplace = false;
                String encode = "UTF-8";
                // 读取xml
                SAXReader saxReader = new SAXReader();
                saxReader.read(file);
                Document document = saxReader.read(file);
                // 获取根节点
                Element rootElement = document.getRootElement();
                // 获取根节点下指定节点
                List params = rootElement.elements("context-param");
                boolean update = false;
                for (Iterator paramIt = params.iterator(); paramIt.hasNext();) {
                    Element paramElem = (Element) paramIt.next();
                    for (Iterator it = paramElem.elementIterator(); it.hasNext();) {
                        Element element = (Element) it.next();
                        // 修改运行环境
                        if ("param-name".equals(element.getName()) && "spring.profiles.active".equals(element.getText())) {
                            if (it.hasNext()) {
                                Element nextElement = (Element) it.next();
                                if ("param-value".equals(nextElement.getName())) {
                                    // 修改节点
                                    nextElement.setText("product");
                                    getLog().info(nextElement.getName() + "===" + nextElement.getText());
                                    update = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (update) {
                        break;
                    }
                }
                // 写入文件
                if (update) {
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    OutputFormat outputFormat = OutputFormat.createPrettyPrint();
                    outputFormat.setEncoding("UTF-8");
                    XMLWriter xmlWriter = new XMLWriter(fileOutputStream, outputFormat);
                    xmlWriter.write(document);
                    xmlWriter.close();
                    getLog().info("修改web.xml成功");
                }
            } catch (Exception e) {
                getLog().debug("修改web.xml异常", e);
            }
        }
    }

    /**
     * 修改cobot中的Findsecbugs-config.properties，切换开发生产环境
     * @param fileName
     */
    private void updateFindsecbugsProp(String fileName) {
        File file = new File(rootPath, fileName);
        if (file.exists()) {
            try {
                boolean isReplace = false;
                String encode = "UTF-8";
                List<String> lines = FileUtils.readLines(file, encode);
                for(int i = 0;i < lines.size();i++)
                {
                    // 修改运行环境
                    if(lines.get(i).startsWith("environment=")) {
                        // cobot.war使用prod
                        if ("war".equals(packag)) {
                            lines.set(i, "environment=prod");
                        } else if ("jar".equals(packag)) {
                            // cobot.jat(检测队列)使用非dev/prod
                            lines.set(i, "environment=jar");
                        }
                        isReplace = true;
                        break;
                    }
                }
                if (isReplace) {
                    FileUtils.writeLines(file, encode, lines);
                    getLog().info("修改Findsecbugs-config.properties成功");
                }
            } catch (IOException e) {
                getLog().debug("修改Findsecbugs-config.properties异常", e);
            }
        }
    }

    /**
     * 修改cobot中的cobot-platform.properties，修改屏蔽开发者模式
     * @param fileName
     */
    private void updatePlatformProp(String fileName) {
        File file = new File(rootPath, fileName);
        if (file.exists()) {
            try {
                boolean isReplace = false;
                String encode = "UTF-8";
                List<String> lines = FileUtils.readLines(file, encode);
                for(int i = 0;i < lines.size();i++)
                {
                    // 屏蔽开发者模式
                    if(lines.get(i).startsWith("Mode=")) {
                        lines.set(i, "Mode=common");
                        isReplace = true;
                        break;
                    }
                }
                if (isReplace) {
                    FileUtils.writeLines(file, encode, lines);
                    getLog().info("修改cobot-platform.properties成功");
                }
            } catch (IOException e) {
                getLog().debug("修改platform异常", e);
            }
        }
    }
}
