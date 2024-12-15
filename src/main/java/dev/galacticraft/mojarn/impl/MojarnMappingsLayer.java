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
import net.fabricmc.mappingio.tree.MappingTreeView;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
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
    private final boolean skipCI;
    private int skipped = 0;

    public MojarnMappingsLayer(@NotNull MappingLayer intermediary, @NotNull MappingLayer mojang, @NotNull MappingLayer yarn, boolean remapArguments, boolean partialMatch, boolean skipDifferent, boolean mapVariables, boolean skipCI) {
        this.intermediary = intermediary;
        this.mojang = mojang;
        this.yarn = yarn;
        this.remapArguments = remapArguments;
        this.partialMatch = partialMatch;
        this.skipDifferent = skipDifferent;
        this.mapVariables = mapVariables;
        this.skipCI = skipCI;
    }

    @Override
    public void visit(MappingVisitor mappingVisitor) throws IOException {
        this.skipped = 0;

        long start = System.currentTimeMillis();

        this.mojang.visit(mappingVisitor);

        if (MojarnPlugin.isCI && this.skipCI) {
            MojarnPlugin.LOGGER.info("Skipping mapping layer generation for CI build.");
            return;
        }

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
        HashMap<String, String> yarn2official = new HashMap<>(yarnTree.getClasses().size());
        for (MappingTree.ClassMapping clazz : officialTree.getClasses()) {
            MappingTree.ClassMapping yarn = yarnTree.getClass(clazz.getDstName(intermediary));
            if (yarn != null) {
                String yarnName = getClassName(yarn.getDstName(named));
                String officialName = getClassName(clazz.getDstName(official));
                // ignore classes that have the same name in both mappings
                if (yarnName != null && officialName != null && !yarnName.equals(officialName)) {
                    yarn2official.put(yarnName, lowerCamelCase(officialName));
                }
            }
        }

        // set up the mapping visitor
        mappingVisitor.visitNamespaces(MappingsNamespace.OFFICIAL.toString(), List.of(MappingsNamespace.NAMED.toString()));

        // cached arraylist for method descriptor parsing
        List<String> descriptor = new ArrayList<>(16);
        HashMap<String, Integer> names = new HashMap<>(16);

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

                                names.clear(); // reset used names
                                parseMethodDescriptor(dstDesc.toCharArray(), descriptor);

                                // visit all method arguments
                                mapArguments(mappingVisitor, yarnMethod, named, descriptor, yarnTree, yarn2official, names);

                                // visit all method variables (if enabled)
                                // no type data, so it is just copied verbatim
                                if (this.mapVariables) {
                                    mapVariables(mappingVisitor, yarnMethod, named, names);
                                }
                            }
                        }
                    }
                }
                mappingVisitor.visitEnd();
            }
        }

        long time = System.currentTimeMillis() - start;
        MojarnPlugin.LOGGER.info("Failed to map {} method arguments due to LVT mismatch.", this.skipped);
        MojarnPlugin.LOGGER.info("Mapping layer generation took {}ms", time);
    }

    /**
     * Parses the given method descriptor into a list of arguments.
     * @param desc the method descriptor to parse
     * @param descriptor the output list (will be cleared)
     */
    private static void parseMethodDescriptor(char[] desc, List<String> descriptor) {
        descriptor.clear();
        for (int i = 1; i < desc.length; i++) {
            if (desc[i] == 'L') {
                // parse class types
                StringBuilder sb = new StringBuilder();
                while (desc[++i] != ';') {
                    sb.append(desc[i]);
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
    }

    /**
     * Maps the arguments of the given method, remapping as necessary.
     * @param output the output mapping visitor
     * @param method the method whose arguments are being mapped
     * @param named the integer id of the named namespace in the {@code yarnTree}
     * @param descriptor the method descriptor ({@code null} represents a primitive)
     * @param yarnTree the tree of yarn/file mapping names
     * @param yarn2official map of yarn to official class mapping names
     * @param names map of already visited names (to avoid duplication)
     * @throws IOException if the mapping visitor fails to accept the name(s)
     */
    private void mapArguments(MappingVisitor output, MappingTree.MethodMapping method, int named, List<@Nullable String> descriptor, MemoryMappingTree yarnTree, Map<String, String> yarn2official, HashMap<String, Integer> names) throws IOException {
        int offset = 0;
        MappingTree.MethodArgMapping[] args = method.getArgs().toArray(new MappingTree.MethodArgMapping[0]);
        Arrays.sort(args, Comparator.comparingInt(MappingTreeView.MethodArgMappingView::getLvIndex));
        if (args.length == descriptor.size()) {
            offset = -1;
        } else if (args.length != 0 && args[args.length - 1].getLvIndex() >= descriptor.size()) {
            offset = args[args.length - 1].getLvIndex() - descriptor.size() + 1;
        }

        for (MappingTree.MethodArgMapping arg : args) {
            String argName = arg.getDstName(named);
            if (argName != null) {
                if (offset >= 0 && (arg.getLvIndex() - offset < 0 || arg.getLvIndex() - offset >= descriptor.size())) {
                    MojarnPlugin.LOGGER.debug("Skipping arguments of method '{}' (LVT offset mismatch)", method.getName(named));
                    this.skipped++;
                    break;
                }
                // check if the argument is a class
                String desc = descriptor.get(offset < 0 ? -(offset-- + 1) : arg.getLvIndex() - offset);
                if (this.remapArguments && desc != null) {
                    // get the class mapping of the argument type
                    MappingTree.ClassMapping typeClass = yarnTree.getClass(desc, named);
                    // if there is a mapping for this type, try to remap it.
                    if (typeClass != null) {
                        // skip if class remapping is disabled
                        String typeName = getClassName(desc);

                        String remapped = yarn2official.get(typeName);
                        if (remapped != null) {
                            argName = tryRemap(typeName, argName, remapped);
                        }
                    }
                }

                if (argName != null) {
                    // avoid duplicate names
                    int dup = names.merge(argName, 0, (s, k) -> s + 1);

                    // apply the mapping
                    output.visitMethodArg(arg.getArgPosition(), arg.getLvIndex(), null);
                    output.visitDstName(MappedElementKind.METHOD_ARG, 0, dup == 0 ? argName : argName + dup);
                }
            }
        }
    }

    /**
     * Remaps the argument name based on its type
     *
     * @param typeName the (yarn) type name of the argument
     * @param argName the argument name to remap
     * @param remapped the remapped (target) argument name
     * @return the remapped name, or {@code null} if the name should be dropped.
     */
    private @Nullable String tryRemap(@NotNull String typeName, String argName, String remapped) {
        // check if class ends in numeric suffix
        if (!Character.isDigit(typeName.charAt(typeName.length() - 1))) {
            // strip numeric suffix on argument (if it exists)
            while (Character.isDigit(argName.charAt(argName.length() - 1))) {
                argName = argName.substring(0, argName.length() - 1);
            }
        }

        // check if the argument name is the same as the type name
        if (argName.equalsIgnoreCase(typeName)) {
            return remapped;
        } else if (this.partialMatch) {
            // check if the argument name contains part of the type name
            // split the name ("CamelCase" -> ["Camel", "Case"])
            String[] split = PATTERN.split(typeName);
            String[] split1 = PATTERN.split(remapped);
            if (split.length == split1.length) {
                for (int i = 0; i < split.length; i++) {
                    if (split[i].equalsIgnoreCase(argName)) {
                        return lowerCamelCase(split1[i]);
                    }
                }
            }
        }
        return this.skipDifferent ? null : argName;
    }

    /**
     * Maps the variables of the given method. Copied verbatim as there is no type information provided.
     * @param output the output mapping visitor
     * @param method the method whose variables are being mapped
     * @param named the integer id of the named namespace in the {@code yarnTree}
     * @param names map of already visited names (to avoid duplication)
     * @throws IOException if the mapping visitor fails to accept the name(s)
     */
    private static void mapVariables(MappingVisitor output, MappingTree.MethodMapping method, int named, HashMap<String, Integer> names) throws IOException {
        for (MappingTree.MethodVarMapping var : method.getVars()) {
            String varName = var.getDstName(named);
            if (varName != null) {
                // strip numeric suffix on variable (if it exists)
                while (Character.isDigit(varName.charAt(varName.length() - 1))) {
                    varName = varName.substring(0, varName.length() - 1);
                }
                int dup = names.merge(varName, 0, (s, k) -> s + 1);

                output.visitMethodVar(var.getLvtRowIndex(), var.getLvIndex(), var.getStartOpIdx(), var.getEndOpIdx(), null);
                output.visitDstName(MappedElementKind.METHOD_VAR, 0, dup == 0 ? varName : varName + dup);
            }
        }
    }

    /**
     * Converts the string from {@code PascalCase} to {@code lowerCamelCase}.
     * The string must not be empty.
     * @param string the string to convert
     * @return the converted string
     */
    private static @NotNull String lowerCamelCase(@NotNull String string) {
        return Character.toLowerCase(string.charAt(0)) + string.substring(1);
    }

    /**
     * Extracts the class name (e.g. {@code Def}) from the full class descriptor (e.g. {@code a/b/c/Def}).
     * Supports subclasses too.
     * @param fullName the full class descriptor
     * @return the extracted class name
     */
    private static @Nullable String getClassName(@Nullable String fullName) {
        return fullName == null ? null : fullName.substring(Math.max(fullName.lastIndexOf('/'), fullName.lastIndexOf('$')) + 1);
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
