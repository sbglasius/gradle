/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.nativebinaries.test.internal;

import org.gradle.nativebinaries.ProjectNativeBinary;
import org.gradle.nativebinaries.ProjectNativeComponent;
import org.gradle.nativebinaries.internal.AbstractProjectNativeBinary;
import org.gradle.nativebinaries.internal.resolve.NativeDependencyResolver;
import org.gradle.nativebinaries.toolchain.internal.ToolChainInternal;
import org.gradle.runtime.base.internal.BinaryNamingScheme;

import java.io.File;

public class DefaultProjectNativeTestSuiteBinary extends AbstractProjectNativeBinary implements ProjectNativeTestSuiteBinaryInternal {
    private final ProjectNativeBinary testedBinary;
    private File executableFile;

    public DefaultProjectNativeTestSuiteBinary(ProjectNativeComponent owner, ProjectNativeBinary testedBinary, BinaryNamingScheme namingScheme, NativeDependencyResolver resolver) {
        super(owner, testedBinary.getFlavor(), (ToolChainInternal) testedBinary.getToolChain(), testedBinary.getTargetPlatform(), testedBinary.getBuildType(), namingScheme, resolver);
        this.testedBinary = testedBinary;
    }

    public ProjectNativeBinary getTestedBinary() {
        return testedBinary;
    }

    public File getExecutableFile() {
        return executableFile;
    }

    public void setExecutableFile(File executableFile) {
        this.executableFile = executableFile;
    }

    public File getPrimaryOutput() {
        return getExecutableFile();
    }
}
