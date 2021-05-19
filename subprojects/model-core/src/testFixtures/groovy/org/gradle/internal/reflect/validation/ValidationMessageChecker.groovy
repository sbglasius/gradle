/*
 * Copyright 2021 the original author or authors.
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

package org.gradle.internal.reflect.validation

import groovy.transform.CompileStatic
import org.gradle.api.internal.DocumentationRegistry
import org.gradle.integtests.fixtures.executer.GradleExecuter
import org.gradle.internal.reflect.JavaReflectionUtil
import org.gradle.internal.reflect.problems.ValidationProblemId

import static org.gradle.internal.reflect.validation.TypeValidationProblemRenderer.convertToSingleLine

@CompileStatic
trait ValidationMessageChecker {
    private final DocumentationRegistry documentationRegistry = new DocumentationRegistry()

    String messageIndent = ''

    void expectReindentedValidationMessage(String indent = '    ') {
        messageIndent = indent
    }

    String userguideLink(String id, String section) {
        documentationRegistry.getDocumentationFor(id, section)
    }

    String learnAt(String id, String section) {
        "Please refer to ${userguideLink(id, section)} for more details about this problem"
    }

    @ValidationTestFor(
        ValidationProblemId.VALUE_NOT_SET
    )
    String missingValueMessage(@DelegatesTo(value = SimpleMessage, strategy = Closure.DELEGATE_FIRST) Closure<?> spec = {}) {
        def config = display(SimpleMessage, 'value_not_set', spec)
        config.description("doesn't have a configured value")
            .reason("this property isn't marked as optional and no value has been configured")
            .solution("Assign a value to '${config.property}'")
            .solution("mark property '${config.property}' as optional")
            .render()
    }

    @ValidationTestFor(
        ValidationProblemId.IGNORED_ANNOTATIONS_ON_METHOD
    )
    String methodShouldNotBeAnnotatedMessage(@DelegatesTo(value = MethodShouldNotBeAnnotated, strategy = Closure.DELEGATE_FIRST) Closure<?> spec = {}) {
        def config = display(MethodShouldNotBeAnnotated, 'ignored_annotations_on_method', spec)
        config.description("$config.kind '$config.method()' should not be annotated with: @$config.annotation")
            .reason("Input/Output annotations are ignored if they are placed on something else than a getter")
            .solution("Remove the annotations")
            .solution("rename the method")
            .render()
    }

    @ValidationTestFor(
        ValidationProblemId.PRIVATE_GETTER_MUST_NOT_BE_ANNOTATED
    )
    String privateGetterAnnotatedMessage(@DelegatesTo(value = AnnotationContext, strategy = Closure.DELEGATE_FIRST) Closure<?> spec = {}) {
        def config = display(AnnotationContext, 'private_getter_must_not_be_annotated', spec)
        config.description("is private and annotated with @${config.annotation}")
            .reason("Annotations on private getters are ignored")
            .solution("Make the getter public")
            .solution("Annotate the public version of the getter")
            .render()
    }

    @ValidationTestFor(
        ValidationProblemId.IGNORED_PROPERTY_MUST_NOT_BE_ANNOTATED
    )
    String ignoredAnnotatedPropertyMessage(@DelegatesTo(value = IgnoredAnnotationPropertyMessage, strategy = Closure.DELEGATE_FIRST) Closure<?> spec = {}) {
        def config = display(IgnoredAnnotationPropertyMessage, 'ignored_property_must_not_be_annotated', spec)
        config.description("annotated with @${config.ignoringAnnotation} should not be also annotated with ${config.alsoAnnotatedWith.collect { "@$it" }.join(", ")}")
            .reason("A property is ignored but also has input annotations")
            .solution("Remove the input annotations")
            .solution("remove the @${config.ignoringAnnotation} annotation")
            .render()
    }

    @ValidationTestFor(
        ValidationProblemId.CONFLICTING_ANNOTATIONS
    )
    String conflictingAnnotationsMessage(@DelegatesTo(value = ConflictingAnnotation, strategy = Closure.DELEGATE_FIRST) Closure<?> spec = {}) {
        def config = display(ConflictingAnnotation, 'conflicting_annotations', spec)
        String annotations = config.inConflict.collect { "@$it" }.join(", ")
        config.description("has conflicting $config.kind: $annotations")
            .reason("The different annotations have different semantics and Gradle cannot determine which one to pick")
            .solution("Choose between one of the conflicting annotations")
            .render()
    }

    @ValidationTestFor(
        ValidationProblemId.ANNOTATION_INVALID_IN_CONTEXT
    )
    String annotationInvalidInContext(@DelegatesTo(value = AnnotationContext, strategy = Closure.DELEGATE_FIRST) Closure<?> spec = {}) {
        def config = display(AnnotationContext, 'annotation_invalid_in_context', spec)
        config.description("is annotated with invalid property type @${config.annotation}")
            .reason("The '@${config.annotation}' annotation cannot be used in this context")
            .solution("Remove the property")
            .solution("use a different annotation, e.g one of ${config.validAnnotations}")
            .render()
    }

    @ValidationTestFor(
        ValidationProblemId.MISSING_ANNOTATION
    )
    String missingAnnotationMessage(@DelegatesTo(value = MissingAnnotation, strategy = Closure.DELEGATE_FIRST) Closure<?> spec = {}) {
        def config = display(MissingAnnotation, 'missing_annotation', spec)
        config.description("is missing ${config.kind}")
            .reason("A property without annotation isn't considered during up-to-date checking")
            .solution("Add ${config.kind}")
            .solution("mark it as @Internal")
            .render()
    }

    @ValidationTestFor(
        ValidationProblemId.IGNORED_ANNOTATIONS_ON_FIELD
    )
    String ignoredAnnotationOnField(@DelegatesTo(value = IgnoredAnnotationOnField, strategy = Closure.DELEGATE_FIRST) Closure<?> spec = {}) {
        def config = display(IgnoredAnnotationOnField, 'ignored_annotations_on_field', spec)
        config.description("without corresponding getter has been annotated with @${config.annotation}")
            .reason("Annotations on fields are only used if there's a corresponding getter for the field.")
            .solution("Add a getter for field '${config.property}'")
            .solution("Remove the annotations on '${config.property}'")
            .render()
    }

    @ValidationTestFor(
        ValidationProblemId.INCOMPATIBLE_ANNOTATIONS
    )
    String incompatibleAnnotations(@DelegatesTo(value = IncompatibleAnnotations, strategy = Closure.DELEGATE_FIRST) Closure<?> spec = {}) {
        def config = display(IncompatibleAnnotations, 'incompatible_annotations', spec)
        config.description("is annotated with @${config.annotatedWith} but that is not allowed for '${config.incompatibleWith}' properties")
            .reason("This modifier is used in conjunction with a property of type '${config.incompatibleWith}' but this doesn't have semantics")
            .solution("Remove the '@${config.annotatedWith}' annotation")
            .render()
    }

    @ValidationTestFor(
        ValidationProblemId.INCORRECT_USE_OF_INPUT_ANNOTATION
    )
    String incorrectUseOfInputAnnotation(@DelegatesTo(value = IncorrectUseOfInputAnnotation, strategy = Closure.DELEGATE_FIRST) Closure<?> spec = {}) {
        def config = display(IncorrectUseOfInputAnnotation, 'incorrect_use_of_input_annotation', spec)
        config.description("has @Input annotation used on property of type '${config.propertyType}'")
            .reason("A property of type '${config.propertyType}' annotated with @Input cannot determine how to interpret the file")
            .solution("Annotate with @InputFile for regular files")
            .solution("annotate with @InputDirectory for directories")
            .solution("if you want to track the path, return File.absolutePath as a String and keep @Input")
            .render()
    }

    @ValidationTestFor(
        ValidationProblemId.MISSING_NORMALIZATION_ANNOTATION
    )
    String missingNormalizationStrategy(@DelegatesTo(value = MissingNormalization, strategy = Closure.DELEGATE_FIRST) Closure<?> spec = {}) {
        def config = display(MissingNormalization, 'missing_normalization_annotation', spec)
        config.description("is annotated with @${config.annotatedWith} but missing a normalization strategy")
            .reason("If you don't declare the normalization, outputs can't be re-used between machines or locations on the same machine, therefore caching efficiency drops significantly")
            .solution("Declare the normalization strategy by annotating the property with either @PathSensitive, @Classpath or @CompileClasspath")
            .render()
    }

    @ValidationTestFor(
        ValidationProblemId.UNRESOLVABLE_INPUT
    )
    String unresolvableInput(@DelegatesTo(value = UnresolvableInput, strategy = Closure.DELEGATE_FIRST) Closure<?> spec = {}, boolean renderSolutions = true) {
        def config = display(UnresolvableInput, 'unresolvable_input', spec)
        config.description("cannot be resolved: ${config.conversionProblem}")
            .reason("An input file collection couldn't be resolved, making it impossible to determine task inputs")
            .solution("Consider using Task.dependsOn instead")
            .render(renderSolutions)
    }

    @ValidationTestFor(
        ValidationProblemId.IMPLICIT_DEPENDENCY
    )
    String implicitDependency(@DelegatesTo(value = ImplicitDependency, strategy = Closure.DELEGATE_FIRST) Closure<?> spec = {}, boolean renderSolutions = true) {
        def config = display(ImplicitDependency, 'implicit_dependency', spec)
        config.description("Gradle detected a problem with the following location: '${config.location.absolutePath}'")
            .reason("Task '${config.consumer}' uses this output of task '${config.producer}' without declaring an explicit or implicit dependency. This can lead to incorrect results being produced, depending on what order the tasks are executed")
            .solution("Declare task '${config.producer}' as an input of '${config.consumer}'")
            .solution("declare an explicit dependency on '${config.producer}' from '${config.consumer}' using Task#dependsOn")
            .solution("declare an explicit dependency on '${config.producer}' from '${config.consumer}' using Task#mustRunAfter")
            .render(renderSolutions)
    }

    @ValidationTestFor(
        ValidationProblemId.INPUT_FILE_DOES_NOT_EXIST
    )
    String inputDoesNotExist(@DelegatesTo(value = IncorrectInputMessage, strategy = Closure.DELEGATE_FIRST) Closure<?> spec = {}) {
        def config = display(IncorrectInputMessage, 'input_file_does_not_exist', spec)
        config.description("specifies ${config.kind} '${config.file}' which doesn't exist")
            .reason("An input file was expected to be present but it doesn't exist")
            .solution("Make sure the ${config.kind} exists before the task is called")
            .solution("make sure that the task which produces the ${config.kind} is declared as an input")
            .render()
    }

    @ValidationTestFor(
        ValidationProblemId.UNEXPECTED_INPUT_FILE_TYPE
    )
    String unexpectedInputType(@DelegatesTo(value = IncorrectInputMessage, strategy = Closure.DELEGATE_FIRST) Closure<?> spec = {}) {
        def config = display(IncorrectInputMessage, 'unexpected_input_file_type', spec)
        config.description("${config.kind} '${config.file}' is not a ${config.kind}")
            .reason("Expected an input to be a ${config.kind} but it was a ${config.oppositeKind}")
            .solution("Use a ${config.kind} as an input")
            .solution("declare the input as a ${config.oppositeKind} instead")
            .render()
    }

    @ValidationTestFor(
        ValidationProblemId.CANNOT_WRITE_OUTPUT
    )
    String cannotWriteToDir(@DelegatesTo(value = CannotWriteToDir, strategy = Closure.DELEGATE_FIRST) Closure<?> spec = {}) {
        def config = display(CannotWriteToDir, 'cannot_write_output', spec)
        config.description("is not writable because '${config.dir}' ${config.reason}")
            .reason("Expected '${config.problemDir}' to be a directory but it's a file")
            .solution("Make sure that the '${config.property}' is configured to a directory")
            .render()
    }

    @ValidationTestFor(
        ValidationProblemId.CANNOT_WRITE_OUTPUT
    )
    String cannotCreateRootOfFileTree(@DelegatesTo(value = CannotWriteToDir, strategy = Closure.DELEGATE_FIRST) Closure<?> spec = {}) {
        def config = display(CannotWriteToDir, 'cannot_write_output', spec)
        config.isNotDirectory()
        config.description("is not writable because '${config.dir}' ${config.reason}")
            .reason("Expected the root of the file tree '${config.problemDir}' to be a directory but it's a file")
            .solution("Make sure that the root of the file tree '${config.property}' is configured to a directory")
            .render()
    }

    @ValidationTestFor(
        ValidationProblemId.CANNOT_WRITE_OUTPUT
    )
    String cannotWriteToFile(@DelegatesTo(value = CannotWriteToFile, strategy = Closure.DELEGATE_FIRST) Closure<?> spec = {}) {
        def config = display(CannotWriteToFile, 'cannot_write_output', spec)
        config.description("is not writable because '${config.file}' ${config.reason}")
            .reason("Cannot write a file to a location pointing at a directory")
            .solution("Configure '${config.property}' to point to a file, not a directory")
            .render()
    }

    @ValidationTestFor(
        ValidationProblemId.CANNOT_WRITE_TO_RESERVED_LOCATION
    )
    String cannotWriteToReservedLocation(@DelegatesTo(value = ForbiddenPath, strategy = Closure.DELEGATE_FIRST) Closure<?> spec = {}) {
        def config = display(ForbiddenPath, 'cannot_write_to_reserved_location', spec)
        config.description("points to '${config.location}' which is managed by Gradle")
            .reason("Trying to write an output to a read-only location which is for Gradle internal use only")
            .solution("Select a different output location")
            .render()
    }

    @ValidationTestFor(
        ValidationProblemId.UNSUPPORTED_NOTATION
    )
    String unsupportedNotation(@DelegatesTo(value = UnsupportedNotation, strategy = Closure.DELEGATE_FIRST) Closure<?> spec = {}) {
        def config = display(UnsupportedNotation, 'unsupported_notation', spec)
        config.description("has unsupported value '${config.value}'")
            .reason("Type '${config.type}' cannot be converted to a ${config.targetType}")
            .render()
    }

    @ValidationTestFor(
        ValidationProblemId.INVALID_USE_OF_CACHEABLE_ANNOTATION
    )
    String invalidUseOfCacheableAnnotation(@DelegatesTo(value = InvalidUseOfCacheable, strategy = Closure.DELEGATE_FIRST) Closure<?> spec = {}) {
        def config = display(InvalidUseOfCacheable, 'invalid_use_of_cacheable_annotation', spec)
        config.description("is incorrectly annotated with @${config.invalidAnnotation}")
            .reason("This annotation only makes sense on ${config.correctTypes.join(', ')} types")
            .solution("Remove the annotation")
            .render()
    }

    @ValidationTestFor(
        ValidationProblemId.CANNOT_USE_OPTIONAL_ON_PRIMITIVE_TYPE
    )
    String optionalOnPrimitive(@DelegatesTo(value = OptionalOnPrimitive, strategy = Closure.DELEGATE_FIRST) Closure<?> spec = {}) {
        def config = display(OptionalOnPrimitive, 'cannot_use_optional_on_primitive_types', spec)
        config.description("of type ${config.primitiveType.name} shouldn't be annotated with @Optional")
            .reason("Properties of primitive type cannot be optional")
            .solution("Remove the @Optional annotation")
            .solution("use the ${config.wrapperType.name} type instead")
            .render()
    }

    @ValidationTestFor(
        ValidationProblemId.REDUNDANT_GETTERS
    )
    String redundantGetters(@DelegatesTo(value = SimpleMessage, strategy = Closure.DELEGATE_FIRST) Closure<?> spec = {}) {
        def config = display(SimpleMessage, 'redundant_getters', spec)
        config.description("has redundant getters: 'get${config.property.capitalize()}()' and 'is${config.property.capitalize()}()'")
            .reason("Boolean property '${config.property}' has both an `is` and a `get` getter")
            .solution("Remove one of the getters")
            .solution("Annotate one of the getters with @Internal")
            .render()
    }

    @ValidationTestFor(
        ValidationProblemId.MUTABLE_TYPE_WITH_SETTER
    )
    String mutableSetter(@DelegatesTo(value = MutableTypeWithSetter, strategy = Closure.DELEGATE_FIRST) Closure<?> spec = {}) {
        def config = display(MutableTypeWithSetter, 'mutable_type_with_setter', spec)
        config.description("of mutable type '${config.propertyType}' is writable")
            .reason("Properties of type '${config.propertyType}' are already mutable")
            .solution("Remove the 'set${config.property.capitalize()}' method")
            .render()
    }

    @ValidationTestFor(
        ValidationProblemId.CACHEABLE_TRANSFORM_CANT_USE_ABSOLUTE_SENSITIVITY
    )
    String invalidUseOfAbsoluteSensitivity(@DelegatesTo(value = SimpleMessage, strategy = Closure.DELEGATE_FIRST) Closure<?> spec = {}) {
        def config = display(SimpleMessage, 'cacheable_transform_cant_use_absolute_sensitivity', spec)
        config.description("is declared to be sensitive to absolute paths")
            .reason("This is not allowed for cacheable transforms")
            .solution("Use a different normalization strategy via @PathSensitive, @Classpath or @CompileClasspath")
            .render()
    }

    @ValidationTestFor(
        ValidationProblemId.UNKNOWN_IMPLEMENTATION
    )
    String implementationUnknown(boolean renderSolutions = false, @DelegatesTo(value = UnknownImplementation, strategy = Closure.DELEGATE_FIRST) Closure<?> spec) {
        def config = display(UnknownImplementation, 'implementation_unknown', spec)
        config.description("${config.prefix} ${config.reason}")
            .reason("Gradle cannot track inputs when it doesn't know their implementation")
            .solution("Use an (anonymous) inner class instead")
            .render(renderSolutions)
    }

    @ValidationTestFor(
        ValidationProblemId.TEST_PROBLEM
    )
    String dummyValidationProblem(String onType = 'InvalidTask', String onProperty = 'dummy', String desc = 'test problem', String testReason = 'this is a test') {
        display(SimpleMessage, 'dummy') {
            type(onType).property(onProperty)
            description(desc)
            reason(testReason)
        }.render()
    }

    void expectThatExecutionOptimizationDisabledWarningIsDisplayed(GradleExecuter executer,
                                                                   String message,
                                                                   String docId = 'more_about_tasks',
                                                                   String section = 'sec:up_to_date_checks') {
        String asSingleLine = convertToSingleLine(message)
        String deprecationMessage = asSingleLine + (asSingleLine.endsWith(" ") ? '' : ' ') +
            "This behaviour has been deprecated and is scheduled to be removed in Gradle 8.0. " +
            "Execution optimizations are disabled to ensure correctness. " +
            "See https://docs.gradle.org/current/userguide/${docId}.html#${section} for more details."
        executer.expectDocumentedDeprecationWarning(deprecationMessage)
    }

    private <T extends ValidationMessageDisplayConfiguration> T display(Class<T> clazz, String docSection, @DelegatesTo(value = ValidationMessageDisplayConfiguration, strategy = Closure.DELEGATE_FIRST) Closure<?> spec) {
        def conf = clazz.newInstance(this)
        conf.section = docSection
        spec.delegate = conf
        spec()
        return (T) conf
    }

    static class UnresolvableInput extends ValidationMessageDisplayConfiguration<UnresolvableInput> {
        String conversionProblem

        UnresolvableInput(ValidationMessageChecker checker) {
            super(checker)
        }

        UnresolvableInput conversionProblem(String details) {
            this.conversionProblem = details
            this
        }
    }

    static class IgnoredAnnotationOnField extends ValidationMessageDisplayConfiguration<IgnoredAnnotationOnField> {
        String annotation

        @Override
        String getPropertyIntro() {
            'field'
        }

        IgnoredAnnotationOnField(ValidationMessageChecker checker) {
            super(checker)
        }

        IgnoredAnnotationOnField annotatedWith(String annotation) {
            this.annotation = annotation
            this
        }
    }

    static class OptionalOnPrimitive extends ValidationMessageDisplayConfiguration<OptionalOnPrimitive> {
        Class<?> primitiveType

        OptionalOnPrimitive(ValidationMessageChecker checker) {
            super(checker)
        }

        OptionalOnPrimitive primitive(Class<?> primitiveType) {
            this.primitiveType = primitiveType
            this
        }

        Class<?> getWrapperType() {
            JavaReflectionUtil.getWrapperTypeForPrimitiveType(primitiveType)
        }
    }

    static class InvalidUseOfCacheable extends ValidationMessageDisplayConfiguration<InvalidUseOfCacheable> {
        String invalidAnnotation
        List<String> correctTypes

        InvalidUseOfCacheable(ValidationMessageChecker checker) {
            super(checker)
        }

        InvalidUseOfCacheable invalidAnnotation(String type) {
            this.invalidAnnotation = type
            this
        }

        InvalidUseOfCacheable onlyMakesSenseOn(String... types) {
            this.correctTypes = types as List
            this
        }
    }

    static class UnsupportedNotation extends ValidationMessageDisplayConfiguration<UnsupportedNotation> {
        String type
        String value
        String targetType

        UnsupportedNotation(ValidationMessageChecker checker) {
            super(checker)
        }

        UnsupportedNotation value(String value, String type = 'DefaultTask') {
            this.value = value
            this.type = type
            this
        }

        UnsupportedNotation cannotBeConvertedTo(String type) {
            this.targetType = type
            this
        }

        UnsupportedNotation candidates(String... candidates) {
            candidates.each { solution("Use $it") }
            this
        }
    }

    static class ForbiddenPath extends ValidationMessageDisplayConfiguration<ForbiddenPath> {
        File location

        ForbiddenPath(ValidationMessageChecker checker) {
            super(checker)
        }

        ForbiddenPath forbiddenAt(File location) {
            this.location = location
            this
        }
    }

    static class CannotWriteToDir extends ValidationMessageDisplayConfiguration<CannotWriteToDir> {
        File dir
        File problemDir
        String reason

        CannotWriteToDir(ValidationMessageChecker checker) {
            super(checker)
        }

        CannotWriteToDir dir(File directory) {
            this.problemDir = directory
            this.dir = directory
            this
        }

        CannotWriteToDir isNotDirectory() {
            this.reason = "is not a directory"
            this
        }

        CannotWriteToDir ancestorIsNotDirectory(File ancestor) {
            this.problemDir = ancestor
            this.reason = "ancestor '$ancestor' is not a directory"
            this
        }
    }

    static class CannotWriteToFile extends ValidationMessageDisplayConfiguration<CannotWriteToFile> {
        File file
        File problemDir
        String reason

        CannotWriteToFile(ValidationMessageChecker checker) {
            super(checker)
        }

        CannotWriteToFile file(File directory) {
            this.problemDir = directory
            this.file = directory
            this
        }

        CannotWriteToFile isNotFile() {
            this.reason = "is not a file"
            this
        }

        CannotWriteToFile ancestorIsNotDirectory(File ancestor) {
            this.problemDir = ancestor
            this.reason = "ancestor '$ancestor' is not a directory"
            this
        }
    }

    static class IncorrectInputMessage extends ValidationMessageDisplayConfiguration<IncorrectInputMessage> {
        String kind
        File file

        IncorrectInputMessage(ValidationMessageChecker checker) {
            super(checker)
        }

        IncorrectInputMessage file(File target) {
            kind('file')
            file = target
            this
        }

        IncorrectInputMessage dir(File target) {
            kind('directory')
            file = target
            this
        }

        IncorrectInputMessage kind(String kind) {
            this.kind = kind.toLowerCase()
            this
        }

        IncorrectInputMessage missing(File file) {
            this.file = file
            this
        }

        IncorrectInputMessage unexpected(File file) {
            this.file = file
            this
        }

        String getOppositeKind() {
            switch (kind) {
                case 'file':
                    return 'directory'
                case 'directory':
                    return 'file'
            }
            return 'unexpected file type'
        }
    }

    static class ImplicitDependency extends ValidationMessageDisplayConfiguration<ImplicitDependency> {
        String producer
        String consumer
        File location

        ImplicitDependency(ValidationMessageChecker checker) {
            super(checker)
        }

        ImplicitDependency producer(String producer) {
            this.producer = producer
            this
        }

        ImplicitDependency consumer(String consumer) {
            this.consumer = consumer
            this
        }

        ImplicitDependency at(File location) {
            this.location = location
            this
        }
    }

    static class MissingNormalization extends ValidationMessageDisplayConfiguration<MissingNormalization> {
        String annotatedWith

        MissingNormalization(ValidationMessageChecker checker) {
            super(checker)
        }

        MissingNormalization annotatedWith(String name) {
            annotatedWith = name
            this
        }
    }

    static class IncorrectUseOfInputAnnotation extends ValidationMessageDisplayConfiguration<IncorrectUseOfInputAnnotation> {
        String propertyType

        IncorrectUseOfInputAnnotation(ValidationMessageChecker checker) {
            super(checker)
        }

        IncorrectUseOfInputAnnotation propertyType(String type) {
            propertyType = type
            this
        }
    }

    static class IncompatibleAnnotations extends ValidationMessageDisplayConfiguration<IncompatibleAnnotations> {
        String annotatedWith
        String incompatibleWith

        IncompatibleAnnotations(ValidationMessageChecker checker) {
            super(checker)
        }

        IncompatibleAnnotations annotatedWith(String name) {
            annotatedWith = name
            this
        }

        IncompatibleAnnotations incompatibleWith(String name) {
            incompatibleWith = name
            this
        }
    }

    static class MissingAnnotation extends ValidationMessageDisplayConfiguration<MissingAnnotation> {
        String kind

        MissingAnnotation(ValidationMessageChecker checker) {
            super(checker)
        }

        MissingAnnotation missingInputOrOutput() {
            kind("an input or output annotation")
        }

        MissingAnnotation missingInput() {
            kind("an input annotation")
        }

        MissingAnnotation kind(String kind) {
            this.kind = kind
            this
        }
    }

    static class SimpleMessage extends ValidationMessageDisplayConfiguration<SimpleMessage> {

        SimpleMessage(ValidationMessageChecker checker) {
            super(checker)
        }

    }

    static class MutableTypeWithSetter extends ValidationMessageDisplayConfiguration<MutableTypeWithSetter> {
        String propertyType

        MutableTypeWithSetter(ValidationMessageChecker checker) {
            super(checker)
        }

        MutableTypeWithSetter propertyType(String type) {
            this.propertyType = type
            this
        }
    }

    static class MethodShouldNotBeAnnotated extends ValidationMessageDisplayConfiguration<MethodShouldNotBeAnnotated> {
        String annotation
        String kind
        String method

        MethodShouldNotBeAnnotated(ValidationMessageChecker checker) {
            super(checker)
        }

        MethodShouldNotBeAnnotated kind(String kind) {
            this.kind = kind
            this
        }

        MethodShouldNotBeAnnotated annotation(String name) {
            annotation = name
            this
        }

        MethodShouldNotBeAnnotated method(String name) {
            method = name
            this
        }
    }

    static class IgnoredAnnotationPropertyMessage extends ValidationMessageDisplayConfiguration<IgnoredAnnotationPropertyMessage> {
        String ignoringAnnotation
        List<String> alsoAnnotatedWith = []

        IgnoredAnnotationPropertyMessage(ValidationMessageChecker checker) {
            super(checker)
        }

        IgnoredAnnotationPropertyMessage ignoring(String name) {
            ignoringAnnotation = name
            this
        }

        IgnoredAnnotationPropertyMessage alsoAnnotatedWith(String... names) {
            Collections.addAll(alsoAnnotatedWith, names)
            this
        }
    }

    static class AnnotationContext extends ValidationMessageDisplayConfiguration<AnnotationContext> {

        String annotation
        String validAnnotations = ""

        AnnotationContext(ValidationMessageChecker checker) {
            super(checker)
            forTransformParameters()
        }

        AnnotationContext annotation(String name) {
            annotation = name
            this
        }

        AnnotationContext forTransformAction() {
            validAnnotations = "@Inject, @InputArtifact or @InputArtifactDependencies"
            this
        }

        AnnotationContext forTransformParameters() {
            validAnnotations = "@Console, @Inject, @Input, @InputDirectory, @InputFile, @InputFiles, @Internal, @Nested or @ReplacedBy"
            this
        }

        AnnotationContext forTask() {
            validAnnotations = "@Console, @Destroys, @Inject, @Input, @InputDirectory, @InputFile, @InputFiles, @Internal, @LocalState, @Nested, @OptionValues, @OutputDirectories, @OutputDirectory, @OutputFile, @OutputFiles or @ReplacedBy"
            this
        }
    }

    static class ConflictingAnnotation extends ValidationMessageDisplayConfiguration<ConflictingAnnotation> {

        List<String> inConflict = []
        String kind = 'type annotations declared'

        ConflictingAnnotation(ValidationMessageChecker checker) {
            super(checker)
        }

        ConflictingAnnotation kind(String kind) {
            this.kind = kind
            this
        }

        ConflictingAnnotation inConflict(List<String> conflicting) {
            inConflict = conflicting
            this
        }

        ConflictingAnnotation inConflict(String... conflicting) {
            inConflict(Arrays.asList(conflicting))
        }
    }

    static class UnknownImplementation extends ValidationMessageDisplayConfiguration<UnknownImplementation> {

        String prefix
        String reason

        UnknownImplementation(ValidationMessageChecker checker) {
            super(checker)
        }

        UnknownImplementation nestedProperty(String propertyName) {
            prefix = "Property '${propertyName}'"
            this
        }

        UnknownImplementation additionalTaskAction(String taskPath) {
            prefix = "Additional action of task '${taskPath}'"
            this
        }

        UnknownImplementation implementedByLambda(String lambdaPrefix) {
            reason = "was implemented by the Java lambda '${lambdaPrefix}\$\$Lambda\$<non-deterministic>'. Using Java lambdas is not supported, use an (anonymous) inner class instead."
            this
        }
    }
}
