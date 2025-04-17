/*
 * Copyright (c) 2024-2025 Team Galacticraft
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

import dev.galacticraft.mojarn.api.MojarnMappingsSpecBuilder;
import net.fabricmc.loom.api.mappings.layered.spec.MappingsSpec;
import net.fabricmc.loom.configuration.providers.mappings.intermediary.IntermediaryMappingsSpec;
import net.fabricmc.loom.configuration.providers.mappings.mojmap.MojangMappingsSpecBuilderImpl;

public class MojarnMappingsSpecBuilderImpl implements MojarnMappingsSpecBuilder {
    boolean nameSyntheticMethods = false;
    boolean remapArguments = true;
    boolean partialMatch = true;
    boolean skipDifferent = false;
    boolean mapVariables = true;
    boolean copyComments = true;
    boolean skipCI = true;
    boolean fileIsEnigma = false;

    @Override
    public MojarnMappingsSpecBuilder nameSyntheticMethods(boolean nameSyntheticMethods) {
        this.nameSyntheticMethods = nameSyntheticMethods;
        return this;
    }

    @Override
    public MojarnMappingsSpecBuilder remapArguments(boolean remapArguments) {
        this.remapArguments = remapArguments;
        return this;
    }

    @Override
    public MojarnMappingsSpecBuilder partialMatch(boolean partialMatch) {
        this.partialMatch = partialMatch;
        return this;
    }

    @Override
    public MojarnMappingsSpecBuilder skipDifferent(boolean skipDifferent) {
        this.skipDifferent = skipDifferent;
        return this;
    }

    @Override
    public MojarnMappingsSpecBuilder mapVariables(boolean mapVariables) {
        this.mapVariables = mapVariables;
        return this;
    }

    @Override
    public MojarnMappingsSpecBuilder copyComments(boolean copyComments) {
        this.copyComments = copyComments;
        return this;
    }

    @Override
    public MojarnMappingsSpecBuilder skipCI(boolean skipCI) {
        this.skipCI = skipCI;
        return this;
    }

    @Override
    public MojarnMappingsSpecBuilder fileIsEnigma() {
        this.fileIsEnigma = true;
        return this;
    }

    public MojarnMappingsSpec build(MappingsSpec<?> intermediary, MappingsSpec<?> mojang, MappingsSpec<?> file) {
        return new MojarnMappingsSpec(intermediary, mojang, file, this.remapArguments, this.partialMatch, this.skipDifferent, this.mapVariables, this.copyComments, this.skipCI);
    }

    public MojarnMappingsSpec build(MappingsSpec<?> intermediary, MappingsSpec<?> file) {
        MojangMappingsSpecBuilderImpl builder = MojangMappingsSpecBuilderImpl.builder();
        builder.setNameSyntheticMembers(this.nameSyntheticMethods);

        return this.build(intermediary, builder.build(), file);
    }

    public MojarnMappingsSpec build(MappingsSpec<?> file) {
        return this.build(new IntermediaryMappingsSpec(), file);
    }
}
