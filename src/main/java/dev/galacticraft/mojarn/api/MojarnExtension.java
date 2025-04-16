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

import org.gradle.api.Action;
import org.gradle.api.artifacts.Dependency;

/**
 * The Mojarn extension API.
 */
public interface MojarnExtension {
    /**
     * Create a new mixed mapping layer using the specified file mappings.
     * @param file The dependency spec or file to use for mappings
     * @return Mixed official/file mappings
     */
    Dependency mappings(Object file);

    /**
     * Create a new mixed mapping layer using the specified file mappings.
     * @param file The dependency spec or file to use for mappings
     * @param action The action to configure the mapping spec
     * @return Mixed official/file mappings
     */
    Dependency mappings(Object file, Action<? super MojarnMappingsSpecBuilder> action);
}
