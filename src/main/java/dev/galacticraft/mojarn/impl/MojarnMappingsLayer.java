/*
 * Copyright (c) 2024 Team Galacticraft
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package dev.galacticraft.mojarn.impl;

import net.fabricmc.loom.api.mappings.layered.MappingLayer;
import net.fabricmc.loom.api.mappings.layered.MappingsNamespace;
import net.fabricmc.loom.configuration.providers.mappings.intermediary.IntermediaryMappingLayer;
import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class MojarnMappingsLayer implements MappingLayer {
    // https://stackoverflow.com/a/7594052
    private static final Pattern PATTERN = Pattern.compile("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");

    private final @NotNull MappingLayer intermediary;
    private final @NotNull MappingLayer mojang;
    private final @NotNull MappingLayer yarn;
    private final boolean remapArguments;
    private final boolean partialMatch;
    private final boolean skipDifferent;
    private final boolean mapVariables;

    public MojarnMappingsLayer(@NotNull MappingLayer intermediary, @NotNull MappingLayer mojang, @NotNull MappingLayer yarn, boolean remapArguments, boolean partialMatch, boolean skipDifferent, boolean mapVariables) {
        this.intermediary = intermediary;
        this.mojang = mojang;
        this.yarn = yarn;
        this.remapArguments = remapArguments;
        this.partialMatch = partialMatch;
        this.skipDifferent = skipDifferent;
        this.mapVariables = mapVariables;
    }

    @Override
    public void visit(MappingVisitor mappingVisitor) throws IOException {
        this.mojang.visit(mappingVisitor);

        // generate a tree of official mappings
        MemoryMappingTree officialTree = new MemoryMappingTree();
        this.intermediary.visit(officialTree);
        this.mojang.visit(officialTree);

        // official mapping destination namespaces (intermediary is not the source namespace)
        int intermediary = officialTree.getNamespaceId(MappingsNamespace.INTERMEDIARY.toString());
        int official = officialTree.getNamespaceId(MappingsNamespace.NAMED.toString());

        // generate a tree of yarn mappings
        MemoryMappingTree yarnTree = new MemoryMappingTree();
        this.yarn.visit(yarnTree);

        // yarn mappings namespace
        int named = yarnTree.getNamespaceId(MappingsNamespace.NAMED.toString());

        // map yarn class names to official class names
        Map<String, String> yarn2official = new HashMap<>(yarnTree.getClasses().size());
        for (MappingTree.ClassMapping clazz : officialTree.getClasses()) {
            MappingTree.ClassMapping yarn = yarnTree.getClass(clazz.getDstName(intermediary));
            if (yarn != null) {
                String yarnName = getClassName(yarn.getDstName(named));
                String officialName = getClassName(clazz.getDstName(official));
                // ignore classes that have the same name in both mappings
                if (yarnName != null && !yarnName.equals(officialName)) {
                    yarn2official.put(yarnName, officialName);
                }
            }
        }

        // set up the mapping visitor
        mappingVisitor.visitNamespaces(MappingsNamespace.OFFICIAL.toString(), List.of(MappingsNamespace.NAMED.toString()));

        // cached arraylist for method descriptor parsing
        List<String> descriptor = new ArrayList<>(16);

        // visit all official classes
        for (MappingTree.ClassMapping clazz : officialTree.getClasses()) {
            MappingTree.ClassMapping yarnClass = yarnTree.getClass(clazz.getDstName(intermediary));
            // check if yarn has mapped the class
            if (yarnClass != null) {
                mappingVisitor.visitClass(clazz.getSrcName());
                // visit all official methods
                for (MappingTree.MethodMapping method : clazz.getMethods()) {
                    String intermediaryMethod = method.getDstName(intermediary);
                    // ensure that intermediary has the method
                    if (intermediaryMethod != null) {
                        MappingTree.MethodMapping yarnMethod = yarnClass.getMethod(intermediaryMethod, method.getDstDesc(intermediary));
                        // check if yarn has mapped the method
                        if (yarnMethod != null) {
                            String dstDesc = yarnMethod.getDstDesc(named);
                            if (dstDesc != null) {
                                mappingVisitor.visitMethod(method.getSrcName(), method.getSrcDesc());
                                // parse the method descriptor into a list of arguments
                                char[] desc = dstDesc.toCharArray();
                                descriptor.clear();
                                for (int i = 1; i < desc.length; i++) {
                                    if (desc[i] == 'L') {
                                        // parse class types
                                        StringBuilder sb = new StringBuilder();
                                        while (desc[i] != ';') {
                                            sb.append(desc[i]);
                                            i++;
                                        }
                                        descriptor.add(sb.toString());
                                    } else if (desc[i] == ')') {
                                        // end of method descriptor
                                        break;
                                    } else {
                                        // primitive types have no mappings
                                        descriptor.add(null);
                                    }
                                }

                                HashMap<String, Integer> names = new HashMap<>();

                                // visit all method arguments
                                int i = 0;
                                for (MappingTree.MethodArgMapping arg : yarnMethod.getArgs()) {
                                    String argName = arg.getDstName(named);
                                    if (argName != null) {
                                        // check if the argument is a class
                                        if (descriptor.get(i) != null) {
                                            // get the class mapping of the argument type
                                            MappingTree.ClassMapping typeClass = yarnTree.getClass(descriptor.get(i));
                                            // if there is a mapping for this type, try to remap it.
                                            if (typeClass != null) {
                                                // skip if class remapping is disabled
                                                if (!this.remapArguments) {
                                                    continue;
                                                }

                                                String typeName = typeClass.getDstName(named);
                                                if (yarn2official.containsKey(typeName)) {
                                                    // check if the argument name is the same as the type name
                                                    if (argName.equalsIgnoreCase(typeName)) {
                                                        String offDst = yarn2official.get(typeName);
                                                        argName = Character.toLowerCase(offDst.charAt(0)) + offDst.substring(1);
                                                    } else if (this.partialMatch) {
                                                        // check if the argument name contains part of the type name
                                                        boolean success = false;
                                                        // split the name ("CamelCase" -> ["Camel", "Case"])
                                                        for (String s : PATTERN.split(typeName)) {
                                                            if (s.equalsIgnoreCase(argName)) {
                                                                success = true;
                                                                String offDst = yarn2official.get(typeName);
                                                                argName = Character.toLowerCase(offDst.charAt(0)) + offDst.substring(1);
                                                                break;
                                                            }
                                                        }
                                                        if (!success && this.skipDifferent) continue;
                                                    } else if (this.skipDifferent) {
                                                        continue;
                                                    }
                                                }
                                            }
                                        }

                                        // avoid duplicate names
                                        if (!names.containsKey(argName)) {
                                            names.put(argName, 1);
                                        } else {
                                            argName += names.get(argName);
                                            names.put(argName, names.get(argName) + 1);
                                        }

                                        // apply the mapping
                                        mappingVisitor.visitMethodArg(arg.getArgPosition(), arg.getLvIndex(), null);
                                        mappingVisitor.visitDstName(MappedElementKind.METHOD_ARG, 0, argName);
                                    }
                                    i++;
                                }

                                // visit all method variables (if enabled)
                                // no type data, so it is just copied verbatim
                                if (this.mapVariables) {
                                    for (MappingTree.MethodVarMapping var : yarnMethod.getVars()) {
                                        String paramName = var.getDstName(named);

                                        if (names.containsKey(paramName)) {
                                            paramName += names.get(paramName);
                                        }

                                        mappingVisitor.visitMethodVar(var.getLvtRowIndex(), var.getLvIndex(), var.getStartOpIdx(), var.getEndOpIdx(), null);
                                        mappingVisitor.visitDstName(MappedElementKind.METHOD_VAR, 0, paramName);
                                    }
                                }
                            }
                        }
                    }
                }
                mappingVisitor.visitEnd();
            }
        }
    }

    @Nullable
    private static String getClassName(@Nullable String fullName) {
        return fullName == null ? null : fullName.substring(Math.max(fullName.lastIndexOf('/') + 1, fullName.lastIndexOf('$') + 1));
    }

    @Override
    public List<Class<? extends MappingLayer>> dependsOn() {
        return Collections.singletonList(IntermediaryMappingLayer.class);
    }

    @Override
    public MappingsNamespace getSourceNamespace() {
        return MappingsNamespace.OFFICIAL;
    }
}
