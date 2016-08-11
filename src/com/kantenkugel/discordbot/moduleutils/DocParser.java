/*
 * Copyright 2016 Michael Ritter (Kantenkugel)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kantenkugel.discordbot.moduleutils;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.dv8tion.jda.utils.SimpleLog;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DocParser {
    private static final SimpleLog LOG = SimpleLog.getLog("DocParser");

    private static final String JENKINS_PREFIX = "http://ci.dv8tion.net/job/JDA/lastSuccessfulBuild/";
    private static final String ARTIFACT_SUFFIX = "api/json?tree=artifacts[*]";

    private static final Path LOCAL_SRC_PATH = Paths.get("src.jar");

    private static final String JDA_CODE_BASE = "net/dv8tion/jda";

    private static final Pattern DOCS_PATTERN = Pattern.compile("/\\*{2}\\s*\n(.*?)\n\\s*\\*/\\s*\n\\s*(?:@[^\n]+\n\\s*)*(.*?)\n", Pattern.DOTALL);
    private static final Pattern METHOD_PATTERN = Pattern.compile(".*?\\s([a-zA-Z][a-zA-Z0-9]*)\\(([a-zA-Z0-9\\s,]*)\\)");
    private static final Pattern METHOD_ARG_PATTERN = Pattern.compile("([a-zA-Z][a-zA-Z0-9]*)\\s+[a-zA-Z][a-zA-Z0-9]");

    private static final Map<String, List<Documentation>> docs = new HashMap<>();


    public static void init() {
        if(!docs.isEmpty())
            return;
        LOG.info("Initializing JDA-Docs");
        download();
        parse();
        LOG.info("JDA-Docs initialized");
    }

    public static String get(String name) {
        String[] split = name.toLowerCase().split("[#\\.]", 2);
        if(split.length != 2)
            return "Incorrect Method declaration";
        if(!docs.containsKey(split[0]))
            return "Class not Found!";
        List<Documentation> methods = docs.get(split[0]);
        methods = methods.parallelStream().filter(doc -> doc.matches(split[1])).sorted(Comparator.comparingInt(doc -> doc.argTypes.size())).collect(Collectors.toList());
        if(methods.size() == 0)
            return "Method not found/documented in Class!";
        if(methods.size() > 1 && methods.get(0).argTypes.size() != 0)
            return "Multiple methods found: " + methods.parallelStream().map(m -> '(' + StringUtils.join(m.argTypes, ", ") + ')').collect(Collectors.joining(", "));
        Documentation doc = methods.get(0);
        StringBuilder b = new StringBuilder();
        b.append("```\n").append(doc.functionHead).append("\n```\n").append(doc.desc);
        if(doc.args.size() > 0) {
            b.append('\n').append('\n').append("**Arguments:**");
            doc.args.entrySet().stream().map(e -> "**" + e.getKey() + "** - " + e.getValue()).forEach(a -> b.append('\n').append(a));
        }
        if(doc.returns != null)
            b.append('\n').append('\n').append("**Returns:**\n").append(doc.returns);
        if(doc.throwing.size() > 0) {
            b.append('\n').append('\n').append("**Throws:**");
            doc.throwing.entrySet().stream().map(e -> "**" + e.getKey() + "** - " + e.getValue()).forEach(a -> b.append('\n').append(a));
        }
        return b.toString();
    }


    private static void download() {
        LOG.info("Downloading source-file");
        try {
            HttpResponse<String> response = Unirest.get(JENKINS_PREFIX + ARTIFACT_SUFFIX).asString();
            if(response.getStatus() < 300 && response.getStatus() > 199) {
                JSONArray artifacts = new JSONObject(response.getBody()).getJSONArray("artifacts");
                for(int i = 0; i < artifacts.length(); i++) {
                    JSONObject artifact = artifacts.getJSONObject(i);
                    if(artifact.getString("fileName").endsWith("sources.jar")) {
                        URL artifactUrl = new URL(JENKINS_PREFIX + "artifact/" + artifact.getString("relativePath"));
                        InputStream is = artifactUrl.openStream();
                        Files.copy(is, LOCAL_SRC_PATH, StandardCopyOption.REPLACE_EXISTING);
                        is.close();
                        LOG.info("Done downloading source-file");
                    }
                }
            }
        } catch(UnirestException | IOException e) {
            LOG.log(e);
        }
    }

    private static void parse() {
        LOG.info("Parsing source-file");
        try {
            JarFile file = new JarFile(LOCAL_SRC_PATH.toFile());
            file.stream()
                    .filter(entry -> !entry.isDirectory() && entry.getName().startsWith(JDA_CODE_BASE) && entry.getName().endsWith(".java"))
                    .forEach(entry -> {
                        try {
                            parse(entry.getName(), file.getInputStream(entry));
                        } catch(IOException e) {
                            e.printStackTrace();
                        }
                    });
            LOG.info("Done parsing source-file");
        } catch(IOException e) {
            LOG.log(e);
        }
    }

    private static void parse(String name, InputStream inputStream) {
        String[] nameSplits = name.split("[/\\.]");
        String className = nameSplits[nameSplits.length - 2];
        docs.putIfAbsent(className.toLowerCase(), new ArrayList<>());
        List<Documentation> docs = DocParser.docs.get(className.toLowerCase());
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(inputStream))) {
            String content = buffer.lines().collect(Collectors.joining("\n"));
            Matcher matcher = DOCS_PATTERN.matcher(content);
            while(matcher.find()) {
                String method = matcher.group(2).trim();
                if(method.contains("class ") || method.contains("interface ")) {
                    continue;
                }
                if(method.endsWith("{"))
                    method = method.substring(0, method.length() - 1).trim();
                Matcher m2 = METHOD_PATTERN.matcher(method);
                if(!m2.find())
                    continue;
                String methodName = m2.group(1);
                List<String> argTypes = new ArrayList<>();
                m2 = METHOD_ARG_PATTERN.matcher(m2.group(2));
                while(m2.find())
                    argTypes.add(m2.group(1));
                List<String> docText = cleanupDocs(matcher.group(1));
                String returns = null;

                Map<String, String> args = new HashMap<>();
                Map<String, String> throwing = new HashMap<>();
                String desc = null;
                for(String line : docText) {
                    if(!line.isEmpty() && line.charAt(0) == '@') {
                        if(line.startsWith("@return "))
                            returns = line.substring(8);
                        else if(line.startsWith("@param ")) {
                            String[] split = line.split("\\s+", 3);
                            args.put(split[1], split.length == 3 ? split[2] : "*No Description*");
                        } else if(line.startsWith("@throws ")) {
                            String[] split = line.split("\\s+", 3);
                            throwing.put(split[1], split.length == 3 ? split[2] : "*No Description*");
                        }
                    } else {
                        desc = desc == null ? line : desc + '\n' + line;
                    }
                }
                docs.add(new Documentation(methodName, argTypes, method, desc, returns, args, throwing));
            }
        } catch(IOException ignored) {}
        try {
            inputStream.close();
        } catch(IOException e) {
            LOG.log(e);
        }
    }

    private static List<String> cleanupDocs(String docs) {
        docs = docs.replace("\n", " ");
        docs = docs.replaceAll("(?:\\s+\\*)+\\s+", " ").replaceAll("\\s{2,}", " ");
        docs = docs.replaceAll("</?b>", "**").replaceAll("</?i>", "*").replaceAll("<br/?>", "\n").replaceAll("<[^>]+>", "");
        docs = docs.replaceAll("[^{]@", "\n@");
        docs = docs.replaceAll("\\{@link[^}]*[ \\.](.*?)\\}", "***$1***");
        return Arrays.stream(docs.split("\n")).map(String::trim).collect(Collectors.toList());
    }

    private static class Documentation {
        private final String functionName;
        private final List<String> argTypes;
        private final String functionHead;
        private final String desc;
        private final String returns;
        private final Map<String, String> args;
        private final Map<String, String> throwing;

        private Documentation(String functionName, List<String> argTypes, String functionHead, String desc, String returns, Map<String, String> args, Map<String, String> throwing) {
            this.functionName = functionName;
            this.argTypes = argTypes;
            this.functionHead = functionHead;
            this.desc = desc;
            this.returns = returns;
            this.args = args;
            this.throwing = throwing;
        }

        private boolean matches(String input) {
            if(input.charAt(input.length()-1) != ')')
                input += "()";
            Matcher matcher = METHOD_PATTERN.matcher(' ' + input);
            if(!matcher.find())
                return false;
            if(!matcher.group(1).equalsIgnoreCase(functionName))
                return false;
            String args = matcher.group(2);
            if(args.isEmpty())
                return true;
            String[] split = args.split(",");
            if(split.length != argTypes.size())
                return true;
            for(int i = 0; i < split.length; i++) {
                if(!split[i].trim().equalsIgnoreCase(argTypes.get(i)))
                    return false;
            }
            return true;
        }
    }
}
