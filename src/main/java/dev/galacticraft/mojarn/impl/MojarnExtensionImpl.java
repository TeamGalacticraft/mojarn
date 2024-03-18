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

import dev.galacticraft.mojarn.api.MojarnExtension;
import dev.galacticraft.mojarn.api.MojarnMappingsSpecBuilder;
import net.fabricmc.loom.api.LoomGradleExtensionAPI;
import net.fabricmc.loom.api.mappings.layered.spec.FileSpec;
import net.fabricmc.loom.configuration.providers.mappings.file.FileMappingsSpec;
import net.fabricmc.loom.configuration.providers.mappings.file.FileMappingsSpecBuilderImpl;
import net.fabricmc.loom.configuration.providers.mappings.intermediary.IntermediaryMappingsSpec;
import net.fabricmc.loom.configuration.providers.mappings.mojmap.MojangMappingsSpec;
import net.fabricmc.loom.configuration.providers.mappings.mojmap.MojangMappingsSpecBuilderImpl;
import org.gradle.api.Action;
import org.gradle.api.artifacts.Dependency;

public class MojarnExtensionImpl implements MojarnExtension {
    private final LoomGradleExtensionAPI loom;

    public MojarnExtensionImpl(LoomGradleExtensionAPI loom) {
        this.loom = loom;
    }

    @Override
    public Dependency mappings(Object file) {
        return mappings(file, action -> {});
    }

    @Override
    public Dependency mappings(Object file, Action<? super MojarnMappingsSpecBuilder> action) {
        MojarnMappingsSpecBuilderImpl builder = new MojarnMappingsSpecBuilderImpl();
        action.execute(builder);
        FileMappingsSpecBuilderImpl fileBuilder = FileMappingsSpecBuilderImpl.builder(FileSpec.create(file));
        if (builder.fileIsEnigma) fileBuilder.enigmaMappings();

        FileMappingsSpec fileSpec = fileBuilder.build();
        MojangMappingsSpec mojangSpec = MojangMappingsSpecBuilderImpl.builder().build();

        return loom.layered(b -> b.addLayer(new MojarnMappingsSpec(new IntermediaryMappingsSpec(), mojangSpec, fileSpec, builder.remapArguments, builder.partialMatch, builder.skipDifferent, builder.mapVariables)));
    }
}
