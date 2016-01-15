package com.kantenkugel.discordbot.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassEnumerator {

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException("Unexpected ClassNotFoundException loading class '" + className + "'");
        }
    }

    private static List<Class<?>> processDirectory(File directory, String pkgname) {

        ArrayList<Class<?>> classes = new ArrayList<>();

        // Get the list of the files contained in the package
        String[] files = directory.list();
        for (int i = 0; i < files.length; i++) {
            String fileName = files[i];
            String className = null;

            // we are only interested in .class files
            if (fileName.endsWith(".class")) {
                // removes the .class extension
                className = pkgname + '.' + fileName.substring(0, fileName.length() - 6);
            }

            if (className != null) {
                classes.add(loadClass(className));
            }

            //If the file is a directory recursively class this method.
            File subdir = new File(directory, fileName);
            if (subdir.isDirectory()) {
                classes.addAll(processDirectory(subdir, pkgname + '.' + fileName));
            }
        }
        return classes;
    }

    private static List<Class<?>> processJarfile(URL resource, String pkgname) {
        List<Class<?>> classes = new ArrayList<>();

        //Turn package name to relative path to jar file
        String relPath = pkgname.replace('.', '/');
        String resPath = resource.getPath();
        String jarPath = resPath.replaceFirst("[.]jar[!].*", ".jar").replaceFirst("file:", "");
        JarFile jarFile;

        try {
            jarFile = new JarFile(jarPath);
        }
        catch (IOException e) {
            throw new RuntimeException("Unexpected IOException reading JAR File '" + jarPath + "'", e);
        }

        //get contents of jar file and iterate through them
        Enumeration<JarEntry> entries = jarFile.entries();
        while(entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();

            //Get content name from jar file
            String entryName = entry.getName();
            String className = null;

            //If content is a class save class name.
            if(entryName.endsWith(".class") && entryName.startsWith(relPath)
                    && entryName.length() > (relPath.length() + "/".length())) {
                className = entryName.replace('/', '.').replace('\\', '.').replace(".class", "");
            }

            //If content is a class add class to List
            if (className != null) {
                classes.add(loadClass(className));
            }
        }
        return classes;
    }
    /**
     * Give a package this method returns all classes contained in that package
     * @param pkg the package to search in
     */
    public static List<Class<?>> getClassesForPackage(Package pkg) {
        ArrayList<Class<?>> classes = new ArrayList<>();

        //Get name of package and turn it to a relative path
        String pkgname = pkg.getName();
        String relPath = pkgname.replace('.', '/');

        // Get a File object for the package
        URL resource = ClassLoader.getSystemClassLoader().getResource(relPath);

        //If we can't find the resource we throw an exception
        if (resource == null) {
            throw new RuntimeException("Unexpected problem: No resource for " + relPath);
        }

        //If the resource is a jar get all classes from jar
        if(resource.toString().startsWith("jar:")) {
            classes.addAll(processJarfile(resource, pkgname));
        }
        else {
            classes.addAll(processDirectory(new File(resource.getPath()), pkgname));
        }

        return classes;
    }
}
