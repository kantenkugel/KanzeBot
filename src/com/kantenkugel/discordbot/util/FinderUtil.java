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

package com.kantenkugel.discordbot.util;

import net.dv8tion.jda.entities.impl.UserImpl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class FinderUtil {
    public <T> T findById(Collection<? extends T> coll, String id) {
        for(T t : coll) {
            try {
                Method idMethod = t.getClass().getMethod("getId");
                if(idMethod.getReturnType() != String.class)
                    throw new RuntimeException("Getter method getId() of class " + t.getClass().getName() + " does not return String!");
                String objId = (String) idMethod.invoke(t);
                if(objId.equals(id))
                    return t;
            } catch(NoSuchMethodException e) {
                throw new RuntimeException("Object " + t.getClass().getName() + " has no getId() method!");
            } catch(InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public <T> List<T> findByName(Collection<? extends T> coll, String name) {
        return findByName(coll, name, false);
    }

    public <T> List<T> findByName(Collection<? extends T> coll, String name, boolean lowerCase) {
        List<T> found = new LinkedList<>();
        for(T t : coll) {
            String methodName = UserImpl.class.isAssignableFrom(t.getClass()) ? "getUsername" : "getName";
            try {
                Method nameMethod = t.getClass().getMethod(methodName);
                if(nameMethod.getReturnType() != String.class)
                    throw new RuntimeException("Getter method " + methodName + " of class " + t.getClass().getName() + " does not return String!");
                String objName = (String) nameMethod.invoke(t);
                if(lowerCase ? objName.equalsIgnoreCase(name) : objName.equals(name))
                    found.add(t);
            } catch(NoSuchMethodException e) {
                throw new RuntimeException("Object " + t.getClass().getName() + " has no " + methodName + "();");
            } catch(InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return found;
    }

    public <T> List<T> findNameContains(Collection<? extends T> coll, String part) {
        return findNameContains(coll, part, false);
    }

    public <T> List<T> findNameContains(Collection<? extends T> coll, String part, boolean lowerCase) {
        List<T> found = new LinkedList<>();
        for(T t : coll) {
            String methodName = UserImpl.class.isAssignableFrom(t.getClass()) ? "getUsername" : "getName";
            try {
                Method nameMethod = t.getClass().getMethod(methodName);
                if(nameMethod.getReturnType() != String.class)
                    throw new RuntimeException("Getter method " + methodName + " of class " + t.getClass().getName() + " does not return String!");
                String objName = (String) nameMethod.invoke(t);
                if(lowerCase ? objName.toLowerCase().contains(part) : objName.contains(part))
                    found.add(t);
            } catch(NoSuchMethodException e) {
                throw new RuntimeException("Object " + t.getClass().getName() + " has no " + methodName + "();");
            } catch(InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return found;
    }

    public <T> List<T> find(Collection<? extends T> coll, String methodName, Object compare) {
        List<T> found = new LinkedList<>();
        for(T t : coll) {
            try {
                Method method = t.getClass().getMethod(methodName);
                if(method.getReturnType() == Void.TYPE)
                    throw new RuntimeException("Getter method " + methodName + " of class " + t.getClass().getName() + " does not return anything!");
                Object returned = method.invoke(t);
                if(returned.equals(compare))
                    found.add(t);
            } catch(NoSuchMethodException e) {
                throw new RuntimeException("Object " + t.getClass().getName() + " has no " + methodName + "();");
            } catch(InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return found;
    }

    public <T> T findOne(Collection<? extends T> coll, String methodName, Object compare) {
        for(T t : coll) {
            try {
                Method method = t.getClass().getMethod(methodName);
                if(method.getReturnType() == Void.TYPE)
                    throw new RuntimeException("Getter method " + methodName + " of class " + t.getClass().getName() + " does not return anything!");
                Object returned = method.invoke(t);
                if(returned.equals(compare))
                    return t;
            } catch(NoSuchMethodException e) {
                throw new RuntimeException("Object " + t.getClass().getName() + " has no " + methodName + "();");
            } catch(InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public <T> List<T> find(Collection<? extends T> coll, Function<T, Object> func, Object compare) {
        return coll.parallelStream().filter(t -> func.apply(t).equals(compare)).collect(Collectors.toList());
    }

    public <T> T findOne(Collection<? extends T> coll, Function<T, Object> func, Object compare) {
        Optional<? extends T> any = coll.parallelStream().filter(t -> func.apply(t).equals(compare)).findAny();
        return any.isPresent() ? any.get() : null;
    }

    public <T> List<T> find(Collection<? extends T> coll, Predicate<T> test) {
        return coll.parallelStream().filter(test).collect(Collectors.toList());
    }

    public <T> T findOne(Collection<? extends T> coll, Predicate<T> test) {
        Optional<? extends T> any = coll.parallelStream().filter(test).findAny();
        return any.isPresent() ? any.get() : null;
    }
}
