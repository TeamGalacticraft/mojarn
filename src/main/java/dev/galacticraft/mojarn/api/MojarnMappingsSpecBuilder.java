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

package dev.galacticraft.mojarn.api;

import net.fabricmc.loom.api.mappings.layered.spec.FileMappingsSpecBuilder;

/**
 * A builder for a Mojarn mappings spec.
 */
public interface MojarnMappingsSpecBuilder {
    /**
     * Whether to name synthetic methods.
     * @param nameSyntheticMethods whether to name synthetic methods
     * @return this builder
     * @see net.fabricmc.loom.api.mappings.layered.spec.MojangMappingsSpecBuilder#setNameSyntheticMembers(boolean)
     */
    MojarnMappingsSpecBuilder nameSyntheticMethods(boolean nameSyntheticMethods);

    /**
     * Whether to remap arguments with class types.
     * @param remapArguments whether to map arguments with class types
     * @return this builder
     */
    MojarnMappingsSpecBuilder remapArguments(boolean remapArguments);

    /**
     * Whether to remap partial argument matches with class types.
     * @param partialMatch whether to remap partial argument matches with class types
     * @return this builder
     */
    MojarnMappingsSpecBuilder partialMatch(boolean partialMatch);

    /**
     * Disable mapping when the class name is different.
     * @param skipDifferent whether to skip mapping when the class name is different
     * @return this builder
     */
    MojarnMappingsSpecBuilder skipDifferent(boolean skipDifferent);

    /**
     * Whether to map variables.
     * @param mapVariables whether to map variables
     * @return this builder
     */
    MojarnMappingsSpecBuilder mapVariables(boolean mapVariables);

    /**
     * Whether to disable mojarn when in a CI environment.
     * @param skipCI whether to disable mojarn when in a CI environment
     * @return this builder
     */
    MojarnMappingsSpecBuilder skipCI(boolean skipCI);

    /**
     * Whether the mapping file is an Enigma mappings file (forwarded to file mapping builder).
     * @return this builder
     * @see FileMappingsSpecBuilder#enigmaMappings()
     */
    MojarnMappingsSpecBuilder fileIsEnigma();
}
