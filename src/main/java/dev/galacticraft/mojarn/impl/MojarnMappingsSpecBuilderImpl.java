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

import dev.galacticraft.mojarn.api.MojarnMappingsSpecBuilder;

public class MojarnMappingsSpecBuilderImpl implements MojarnMappingsSpecBuilder {
    boolean remapArguments = true;
    boolean partialMatch = false;
    boolean skipDifferent = false;
    boolean mapVariables = true;
    boolean fileIsEnigma = false;

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
    public MojarnMappingsSpecBuilder fileIsEnigma() {
        this.fileIsEnigma = true;
        return this;
    }
}
