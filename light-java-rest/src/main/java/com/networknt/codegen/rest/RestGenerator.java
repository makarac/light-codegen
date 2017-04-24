package com.networknt.codegen.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fizzed.rocker.runtime.ArrayOfByteArraysOutput;
import com.fizzed.rocker.runtime.DefaultRockerModel;
import com.networknt.codegen.Generator;
import com.networknt.codegen.Utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.io.File.separator;

/**
 * The input for rest generator include config with json format
 * and swagger specification in json format.
 *
 * The module is swagger spec in json format. And it will be used
 * as Map like config. It is not necessary to parse it to POJO with
 * swagger-parser for code generator.
 *
 * Created by stevehu on 2017-04-23.
 */
public class RestGenerator implements Generator {
    static ObjectMapper mapper = new ObjectMapper();

    @Override
    public void generate(String targetPath, Object model, Map<String, Object> config) throws IOException {
        // whoever is calling this needs to make sure that model is converted to Map<String, Object>
        String rootPackage = (String)config.get("rootPackage");
        String modelPackage = (String)config.get("modelPackage");
        String handlerPackage = (String)config.get("handlerPackage");

        transfer(targetPath, "", "pom.xml", templates.pom.template(config));
        transfer(targetPath, "", "Dockerfile", templates.dockerfile.template(config));
        transfer(targetPath, "", ".gitignore", templates.gitignore.template());
        transfer(targetPath, "", "README.md", templates.README.template());
        transfer(targetPath, "", "LICENSE", templates.LICENSE.template());
        transfer(targetPath, "", ".classpath", templates.classpath.template());
        transfer(targetPath, "", ".project", templates.project.template());

        // config
        transfer(targetPath, ("src.main.resources.config").replace(".", separator), "server.yml", templates.server.template(config.get("groupId") + "." + config.get("artifactId") + "-" + config.get("version")));
        transfer(targetPath, ("src.main.resources.config").replace(".", separator), "secret.yml", templates.secret.template());
        transfer(targetPath, ("src.main.resources.config").replace(".", separator), "security.yml", templates.security.template());

        transfer(targetPath, ("src.main.resources.config.tls").replace(".", separator), "server.keystore", templates.serverkeystore.template());
        transfer(targetPath, ("src.main.resources.config.tls").replace(".", separator), "server.truststore", templates.servertruststore.template());

        transfer(targetPath, ("src.main.resources.config.oauth").replace(".", separator), "primary.crt", templates.primaryCrt.template());
        transfer(targetPath, ("src.main.resources.config.oauth").replace(".", separator), "secondary.crt", templates.secondaryCrt.template());

        transfer(targetPath, ("src.main.resources.META-INF.services").replace(".", separator), "com.networknt.server.HandlerProvider", templates.routingService.template(rootPackage));
        transfer(targetPath, ("src.main.resources.META-INF.services").replace(".", separator), "com.networknt.server.MiddlewareHandler", templates.middlewareService.template());
        transfer(targetPath, ("src.main.resources.META-INF.services").replace(".", separator), "com.networknt.server.StartupHookProvider", templates.startupHookProvider.template());
        transfer(targetPath, ("src.main.resources.META-INF.services").replace(".", separator), "com.networknt.server.ShutdownHookProvider", templates.shutdownHookProvider.template());

        // logging
        transfer(targetPath, ("src.main.resources").replace(".", separator), "logback.xml", templates.logback.template());
        transfer(targetPath, ("src.test.resources").replace(".", separator), "logback-test.xml", templates.logback.template());

        // routing
        transfer(targetPath, ("src.main.java." + rootPackage).replace(".", separator), "PathHandlerProvider.java", templates.handlerProvider.template(rootPackage, handlerPackage, getOperationList(model)));


        // model


        // handler


        // handler test cases
        transfer(targetPath, ("src.test.java." + handlerPackage + ".").replace(".", separator),  "TestServer.java", templates.testServer.template(handlerPackage));


        // last step to write swagger.json as the directory must be there already.
        // TODO add server info before write it.
        writeSwagger(FileSystems.getDefault().getPath(targetPath, ("src.main.resources.config").replace(".", separator), "swagger.json"), model);

    }

    private void writeSwagger(Path path, Object model) throws IOException {

        mapper.writerWithDefaultPrettyPrinter().writeValue(new FileOutputStream(path.toFile()), model);
    }

    public List<Map<String, Object>> getOperationList(Object model) {
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> map = (Map<String, Object>)model;
        String basePath = (String)map.get("basePath");
        Map<String, Object> paths = (Map<String, Object>)map.get("paths");

        for(Map.Entry<String, Object> entryPath: paths.entrySet()) {
            String path = entryPath.getKey();
            Map<String, Object> pathValues = (Map<String, Object>)entryPath.getValue();
            for(Map.Entry<String, Object> entryOps: pathValues.entrySet()) {
                Map<String, Object> flattened = new HashMap<>();
                flattened.put("method", entryOps.getKey().toUpperCase());
                flattened.put("path", basePath + path);
                String normalizedPath = path.replace("{", "").replace("}", "");
                flattened.put("handlerName", Utils.camelize(normalizedPath) + Utils.camelize(entryOps.getKey()) + "Handler");
                result.add(flattened);
            }
        }


        return result;
    }
}
