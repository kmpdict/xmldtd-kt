package com.boswelja.xmldtd.codegen

import com.boswelja.xmldtd.deserialize.AttributeDefinition
import com.boswelja.xmldtd.deserialize.ChildElementDefinition
import com.boswelja.xmldtd.deserialize.DocumentTypeDefinition
import com.boswelja.xmldtd.deserialize.ElementDefinition
import com.boswelja.xmldtd.deserialize.Entity
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import java.nio.file.Path

/**
 * Creates a class that can take a [DocumentTypeDefinition] and write it to [targetDir].
 *
 * @param packageName The package name for the generated sources.
 * @param targetDir The path at which the generated sources will be written.
 */
public class DataClassGenerator(
    private val packageName: String,
    private val targetDir: Path
) {
    internal val PcDataClassName = ClassName(packageName, "ParsedCharacterData")

    /**
     * Writes the provided [DocumentTypeDefinition] to the specified [targetDir].
     */
    public fun writeDtdToTarget(dtd: DocumentTypeDefinition) {
        buildFileSpec(dtd)
            .writeTo(targetDir)
    }

    internal fun buildFileSpec(dtd: DocumentTypeDefinition): FileSpec {
        val generatedTypes = generateTypeSpecForElement(dtd.rootElement)
        return FileSpec.builder(generatedTypes.rootClassName)
            .addTypes(generatedTypes.topLevelTypes)
            .addTypes(generateXmlDtdTypes(dtd.entities))
            .build()
    }

    internal fun generateXmlDtdTypes(entities: List<Entity>): List<TypeSpec> {
        // PCData
        val entities = generatePropertiesForEntities(entities)
        val parsedCharacterDataBuilder = TypeSpec.classBuilder(PcDataClassName)
            .addModifiers(KModifier.VALUE)
            .addAnnotation(Serializable::class)
            .addAnnotation(JvmInline::class)
        if (entities.isNotEmpty()) {
            parsedCharacterDataBuilder
                .addProperty(
                    PropertySpec.builder("rawValue", String::class)
                        .addModifiers(KModifier.INTERNAL)
                        .initializer("rawValue")
                        .build()
                )
                .primaryConstructor(
                    FunSpec.constructorBuilder()
                        .addParameter("rawValue", String::class)
                        .build()
                )
                .addFunction(
                    FunSpec.builder("parse")
                        .addParameter(
                            ParameterSpec.builder("entities", Map::class.parameterizedBy(String::class, String::class))
                                .defaultValue("InternalEntities")
                                .build()
                        )
                        .returns(String::class)
                        .addCode(
                            CodeBlock.builder()
                                .addStatement("val regex = Regex(\"&([a-zA-Z0-9]+);\")")
                                .beginControlFlow("return regex.replace(rawValue) { matchResult ->")
                                .addStatement("val key = matchResult.groupValues[1]")
                                .addStatement("entities[key] ?: key")
                                .endControlFlow()
                                .build()
                        )
                        .build()
                )
                .addType(
                    TypeSpec.companionObjectBuilder()
                        .addProperties(entities)
                        .build()
                )
        } else {
            parsedCharacterDataBuilder
                .addProperty(
                    PropertySpec.builder("content", String::class)
                        .initializer("content")
                        .build()
                )
                .primaryConstructor(
                    FunSpec.constructorBuilder()
                        .addParameter("content", String::class)
                        .build()
                )
        }
        return listOf(parsedCharacterDataBuilder.build())
    }

    internal fun generatePropertiesForEntities(entities: List<Entity>): List<PropertySpec> {
        val internalEntities = mutableMapOf<String, String>()
        val externalEntities = mutableMapOf<String, String>()

        entities.forEach { entity ->
            when (entity) {
                is Entity.External -> externalEntities[entity.name] = entity.url
                is Entity.Internal -> internalEntities[entity.name] = entity.value
            }
        }

        val properties = mutableListOf<PropertySpec>()

        if (internalEntities.isNotEmpty()) {
            val builder = CodeBlock.builder()
                .addStatement("mapOf(")
                .indent()
            internalEntities.forEach { (key, value) ->
                builder.addStatement("%S to %S,", key, value)
            }
            builder
                .unindent()
                .addStatement(")")
            properties.add(
                PropertySpec.builder(
                    "InternalEntities",
                    Map::class.parameterizedBy(String::class, String::class)
                )
                    .initializer(builder.build())
                    .build()
            )
        }
        if (externalEntities.isNotEmpty()) {
            val builder = CodeBlock.builder()
                .addStatement("mapOf(")
                .indent()
            externalEntities.forEach { (key, value) ->
                builder.addStatement("%S to %S,", key, value)
            }
            builder
                .unindent()
                .addStatement(")")
            properties.add(
                PropertySpec.builder(
                    "ExternalEntities",
                    Map::class.parameterizedBy(String::class, String::class)
                )
                    .initializer(builder.build())
                    .build()
            )
        }

        return properties
    }

    internal fun generateTypeSpecForElement(element: ElementDefinition): GeneratedTypes {
        return when (element) {
            is ElementDefinition.Empty -> element.toTypeSpec()
            is ElementDefinition.Mixed -> element.toTypeSpec()
            is ElementDefinition.WithChildren -> element.toTypeSpec()
            is ElementDefinition.Any -> element.toTypeSpec()
            is ElementDefinition.ParsedCharacterData -> element.toTypeSpec()
            is ElementDefinition.Either -> element.toTypeSpec()
        }
    }

    internal fun ElementDefinition.Either.toTypeSpec(): GeneratedTypes {
        val types = mutableListOf<TypeSpec>()
        val nestedTypes = mutableListOf<TypeSpec>()
        val parameters = mutableListOf<ParameterSpec>()
        val properties = mutableListOf<PropertySpec>()

        val className = ClassName(packageName, elementName.toPascalCase())

        generatePropsForAttrs(attributes).also { generatedProperties ->
            parameters.addAll(generatedProperties.parameters)
            properties.addAll(generatedProperties.properties)
            nestedTypes.addAll(generatedProperties.types)
        }
        val rootTypeBuilder = TypeSpec.interfaceBuilder(className)
            .addModifiers(KModifier.SEALED)
        options
            .map { generateTypesForChild(it) }
            .forEach { generatedTypes ->
                val generatedSealedSubtypes = generatedTypes.topLevelTypes.map {
                    if (it.name == generatedTypes.rootClassName.simpleName) {
                        it.toBuilder()
                            .addSuperinterface(className)
                            .primaryConstructor(
                                it.primaryConstructor?.toBuilder()
                                    ?.addParameters(parameters)
                                    ?.build()
                            )
                            .build()
                    } else {
                        it
                    }
                }
                nestedTypes.addAll(generatedSealedSubtypes)
            }
        val rootType = rootTypeBuilder
            .apply {
                comment?.let {
                    addKdoc(it)
                }
            }
            .addProperties(properties)
            .addTypes(nestedTypes)
            .addAnnotation(Serializable::class)
            .addAnnotation(AnnotationSpec.builder(XmlElement::class).addMember("value = %L", true).build())
            .addAnnotation(AnnotationSpec.builder(SerialName::class)
                .addMember("value = %S", elementName)
                .build())
            .build()

        types.add(rootType)
        return GeneratedTypes(
            rootClassName = className,
            topLevelTypes = types
        )
    }

    internal fun ElementDefinition.ParsedCharacterData.toTypeSpec(): GeneratedTypes {
        val types = mutableListOf<TypeSpec>()
        val nestedTypes = mutableListOf<TypeSpec>()
        val parameters = mutableListOf<ParameterSpec>()
        val properties = mutableListOf<PropertySpec>()

        val className = ClassName(packageName, elementName.toPascalCase())

        generatePropsForAttrs(attributes).also { generatedProperties ->
            parameters.addAll(generatedProperties.parameters)
            properties.addAll(generatedProperties.properties)
            nestedTypes.addAll(generatedProperties.types)
        }
        val propertyName = "content"
        parameters.add(
            ParameterSpec.builder(propertyName, PcDataClassName)
                .build()
        )
        properties.add(
            PropertySpec.builder(propertyName, PcDataClassName)
                .addModifiers(KModifier.PUBLIC)
                .addAnnotation(XmlValue::class)
                .initializer(propertyName)
                .build()
        )
        val rootTypeBuilder = if (parameters.isEmpty()) {
            TypeSpec.objectBuilder(className.simpleName)
        } else {
            val constructorBuilder = FunSpec.constructorBuilder()
                .addParameters(parameters)
                .build()
            TypeSpec.classBuilder(className.simpleName)
                .primaryConstructor(constructorBuilder)
        }

        val rootType = rootTypeBuilder
            .apply {
                comment?.let {
                    addKdoc(it)
                }
            }
            .addProperties(properties)
            .addModifiers(KModifier.DATA)
            .addTypes(nestedTypes)
            .addAnnotation(Serializable::class)
            .addAnnotation(AnnotationSpec.builder(XmlElement::class).addMember("value = %L", true).build())
            .addAnnotation(AnnotationSpec.builder(SerialName::class)
                .addMember("value = %S", elementName)
                .build())
            .build()

        types.add(rootType)
        return GeneratedTypes(
            rootClassName = className,
            topLevelTypes = types
        )
    }

    internal fun ElementDefinition.Any.toTypeSpec(): GeneratedTypes {
        val types = mutableListOf<TypeSpec>()
        val nestedTypes = mutableListOf<TypeSpec>()
        val parameters = mutableListOf<ParameterSpec>()
        val properties = mutableListOf<PropertySpec>()

        val className = ClassName(packageName, elementName.toPascalCase())

        generatePropsForAttrs(attributes).also { generatedProperties ->
            parameters.addAll(generatedProperties.parameters)
            properties.addAll(generatedProperties.properties)
            nestedTypes.addAll(generatedProperties.types)
        }
        val propertyName = "content"
        parameters.add(
            ParameterSpec.builder(propertyName, PcDataClassName)
                .build()
        )
        properties.add(
            PropertySpec.builder(propertyName, PcDataClassName)
                .addModifiers(KModifier.PUBLIC)
                .addAnnotation(XmlValue::class)
                .initializer(propertyName)
                .build()
        )
        val rootTypeBuilder = if (parameters.isEmpty()) {
            TypeSpec.objectBuilder(className.simpleName)
        } else {
            val constructorBuilder = FunSpec.constructorBuilder()
                .addParameters(parameters)
                .build()
            TypeSpec.classBuilder(className.simpleName)
                .primaryConstructor(constructorBuilder)
        }

        val rootType = rootTypeBuilder
            .apply {
                comment?.let {
                    addKdoc(it)
                }
            }
            .addProperties(properties)
            .addModifiers(KModifier.DATA)
            .addTypes(nestedTypes)
            .addAnnotation(Serializable::class)
            .addAnnotation(AnnotationSpec.builder(XmlElement::class).addMember("value = %L", true).build())
            .addAnnotation(AnnotationSpec.builder(SerialName::class)
                .addMember("value = %S", elementName)
                .build())
            .build()

        types.add(rootType)
        return GeneratedTypes(
            rootClassName = className,
            topLevelTypes = types
        )
    }

    internal fun ElementDefinition.WithChildren.toTypeSpec(): GeneratedTypes {
        val types = mutableListOf<TypeSpec>()
        val nestedTypes = mutableListOf<TypeSpec>()
        val parameters = mutableListOf<ParameterSpec>()
        val properties = mutableListOf<PropertySpec>()

        val className = ClassName(packageName, elementName.toPascalCase())

        generatePropsForAttrs(attributes).also { generatedProperties ->
            parameters.addAll(generatedProperties.parameters)
            properties.addAll(generatedProperties.properties)
            nestedTypes.addAll(generatedProperties.types)
        }

        generatePropertiesForChildren(children).also { generatedProperties ->
            parameters.addAll(generatedProperties.parameters)
            properties.addAll(generatedProperties.properties)
            types.addAll(generatedProperties.types)
        }

        val rootTypeBuilder = if (parameters.isEmpty()) {
            TypeSpec.objectBuilder(className.simpleName)
        } else {
            val constructorBuilder = FunSpec.constructorBuilder()
                .addParameters(parameters)
                .build()
            TypeSpec.classBuilder(className.simpleName)
                .primaryConstructor(constructorBuilder)
        }

        val rootType = rootTypeBuilder
            .apply {
                comment?.let {
                    addKdoc(it)
                }
            }
            .addProperties(properties)
            .addModifiers(KModifier.DATA)
            .addTypes(nestedTypes)
            .addAnnotation(Serializable::class)
            .addAnnotation(AnnotationSpec.builder(XmlElement::class).addMember("value = %L", true).build())
            .addAnnotation(AnnotationSpec.builder(SerialName::class)
                .addMember("value = %S", elementName)
                .build())
            .build()

        types.add(rootType)
        return GeneratedTypes(
            rootClassName = className,
            topLevelTypes = types
        )
    }

    internal fun ElementDefinition.Empty.toTypeSpec(): GeneratedTypes {
        val types = mutableListOf<TypeSpec>()
        val nestedTypes = mutableListOf<TypeSpec>()
        val parameters = mutableListOf<ParameterSpec>()
        val properties = mutableListOf<PropertySpec>()

        val className = ClassName(packageName, elementName.toPascalCase())

        generatePropsForAttrs(attributes).also { generatedProperties ->
            parameters.addAll(generatedProperties.parameters)
            properties.addAll(generatedProperties.properties)
            nestedTypes.addAll(generatedProperties.types)
        }

        val rootTypeBuilder = if (parameters.isEmpty()) {
            TypeSpec.objectBuilder(className.simpleName)
        } else {
            val constructorBuilder = FunSpec.constructorBuilder()
                .addParameters(parameters)
                .build()
            TypeSpec.classBuilder(className.simpleName)
                .primaryConstructor(constructorBuilder)
        }

        val rootType = rootTypeBuilder
            .apply {
                comment?.let {
                    addKdoc(it)
                }
            }
            .addProperties(properties)
            .addModifiers(KModifier.DATA)
            .addTypes(nestedTypes)
            .addAnnotation(Serializable::class)
            .addAnnotation(AnnotationSpec.builder(XmlElement::class).addMember("value = %L", true).build())
            .addAnnotation(AnnotationSpec.builder(SerialName::class)
                .addMember("value = %S", elementName)
                .build())
            .build()

        types.add(rootType)
        return GeneratedTypes(
            rootClassName = className,
            topLevelTypes = types
        )
    }

    internal fun ElementDefinition.Mixed.toTypeSpec(): GeneratedTypes {
        val types = mutableListOf<TypeSpec>()
        val nestedTypes = mutableListOf<TypeSpec>()
        val parameters = mutableListOf<ParameterSpec>()
        val properties = mutableListOf<PropertySpec>()

        val className = ClassName(packageName, elementName.toPascalCase())

        generatePropsForAttrs(attributes).also { generatedProperties ->
            parameters.addAll(generatedProperties.parameters)
            properties.addAll(generatedProperties.properties)
            nestedTypes.addAll(generatedProperties.types)
        }

        if (containsPcData && children.isEmpty()) {
            val propertyName = "content"
            parameters.add(
                ParameterSpec.builder(propertyName, List::class.asClassName().parameterizedBy(PcDataClassName))
                    .build()
            )
            properties.add(
                PropertySpec.builder(propertyName, List::class.asClassName().parameterizedBy(PcDataClassName))
                    .addModifiers(KModifier.PUBLIC)
                    .addAnnotation(XmlValue::class)
                    .initializer(propertyName)
                    .build()
            )
        } else {
            val sealedType = TypeSpec.interfaceBuilder("Content")
                .addModifiers(KModifier.SEALED)
                .addAnnotation(Serializable::class)
                .build()
            val sealedSubtypes = children
                .map { elementDefinition ->
                    val typeSpec = generateTypeSpecForElement(elementDefinition)
                    val rootTypeSpec = typeSpec.topLevelTypes
                        .first { it.name == typeSpec.rootClassName.simpleName }
                    typeSpec.topLevelTypes
                        .toMutableList()
                        .apply {
                            remove(rootTypeSpec)
                            add(rootTypeSpec.toBuilder()
                                .addSuperinterface(
                                    ClassName(packageName, className.simpleName, sealedType.name!!)
                                )
                                .build())
                        }
                }
                .flatten()
                .toMutableList()
            if (containsPcData) {
                sealedSubtypes += TypeSpec.classBuilder("PcData")
                    .addModifiers(KModifier.VALUE)
                    .addAnnotation(Serializable::class)
                    .addAnnotation(JvmInline::class)
                    .addProperty(PropertySpec.builder("content", PcDataClassName)
                        .addModifiers(KModifier.PUBLIC)
                        .initializer("content")
                        .build())
                    .primaryConstructor(FunSpec.constructorBuilder()
                        .addParameter("content", PcDataClassName)
                        .build())
                    .addSuperinterface(ClassName(packageName, className.simpleName, sealedType.name!!))
                    .build()
            }
            nestedTypes.add(sealedType)
            nestedTypes.addAll(sealedSubtypes)
            val propertyType = List::class.asClassName()
                .parameterizedBy(className.nestedClass(sealedType.name!!))
            parameters.add(
                ParameterSpec.builder("content", propertyType)
                    .build()
            )
            properties.add(
                PropertySpec.builder("content", propertyType)
                    .addModifiers(KModifier.PUBLIC)
                    .addAnnotation(XmlValue::class)
                    .initializer("content")
                    .build()
            )
        }

        val rootTypeBuilder = if (parameters.isEmpty()) {
            TypeSpec.objectBuilder(className.simpleName)
        } else {
            val constructorBuilder = FunSpec.constructorBuilder()
                .addParameters(parameters)
                .build()
            TypeSpec.classBuilder(className.simpleName)
                .primaryConstructor(constructorBuilder)
        }

        val rootType = rootTypeBuilder
            .apply {
                comment?.let {
                    addKdoc(it)
                }
            }
            .addProperties(properties)
            .addModifiers(KModifier.DATA)
            .addTypes(nestedTypes)
            .addAnnotation(Serializable::class)
            .addAnnotation(AnnotationSpec.builder(XmlElement::class).addMember("value = %L", true).build())
            .addAnnotation(AnnotationSpec.builder(SerialName::class)
                .addMember("value = %S", elementName)
                .build())
            .build()

        types.add(rootType)
        return GeneratedTypes(
            rootClassName = className,
            topLevelTypes = types
        )
    }

    internal fun generatePropertiesForChildren(children: List<ChildElementDefinition>): GeneratedProperties {
        val parameters = mutableListOf<ParameterSpec>()
        val properties = mutableListOf<PropertySpec>()
        val types = mutableListOf<TypeSpec>()

        children.forEach { childElementDefinition ->
            val childTypes = generateTypesForChild(childElementDefinition)
            val type: TypeName = when (childElementDefinition.occurs) {
                ChildElementDefinition.Occurs.Once -> childTypes.rootClassName
                ChildElementDefinition.Occurs.AtMostOnce -> childTypes.rootClassName.copy(nullable = true)
                ChildElementDefinition.Occurs.AtLeastOnce,
                ChildElementDefinition.Occurs.ZeroOrMore ->
                    List::class.asClassName().parameterizedBy(childTypes.rootClassName)
            }
            // If the type is a List, pluralize the name
            val propertyName = if (childElementDefinition.occurs == ChildElementDefinition.Occurs.ZeroOrMore ||
                childElementDefinition.occurs == ChildElementDefinition.Occurs.AtLeastOnce) {
                childTypes.rootClassName.simpleName.toCamelCase().toPlural()
            } else {
                childTypes.rootClassName.simpleName.toCamelCase()
            }
            parameters.add(
                ParameterSpec.builder(propertyName, type)
                    .build()
            )
            properties.add(
                PropertySpec.builder(propertyName, type)
                    .addModifiers(KModifier.PUBLIC)
                    .addAnnotation(AnnotationSpec.builder(XmlElement::class).addMember("value = %L", true).build())
                    .apply {
                        if (childElementDefinition is ChildElementDefinition.Single) {
                            addAnnotation(AnnotationSpec.builder(SerialName::class)
                                .addMember("value = %S", childElementDefinition.elementDefinition.elementName)
                                .build())
                            childElementDefinition.elementDefinition.comment?.let { addKdoc(it) }
                        }
                    }
                    .initializer(propertyName)
                    .build()
            )
            types.addAll(childTypes.topLevelTypes)
        }

        return GeneratedProperties(
            properties = properties,
            parameters = parameters,
            types = types
        )
    }

    internal fun generateTypesForChild(childElementDefinition: ChildElementDefinition): GeneratedTypes {
        return when (childElementDefinition) {
            is ChildElementDefinition.Either -> {
                val childTypes = childElementDefinition.options.map { generateTypesForChild(it) }
                val typeName = childTypes.joinToString(separator = "Or") { it.rootClassName.simpleName }
                val sealedSpec = TypeSpec.interfaceBuilder(typeName)
                    .addModifiers(KModifier.SEALED)
                    .addAnnotation(Serializable::class)
                    .build()
                val topLevelTypes = childTypes
                    .map { childType ->
                        val type = childType.topLevelTypes.first { it.name == childType.rootClassName.simpleName }
                        childType.topLevelTypes.toMutableList().apply {
                            remove(type)
                            add(type.toBuilder().addSuperinterface(ClassName(packageName, typeName)).build())
                        }
                    }
                    .flatten()
                GeneratedTypes(
                    rootClassName = ClassName(packageName, typeName),
                    topLevelTypes = topLevelTypes + sealedSpec
                )
            }
            is ChildElementDefinition.Single -> generateTypeSpecForElement(childElementDefinition.elementDefinition)
        }
    }

    internal fun generateEnumForAttribute(name: String, enumValues: AttributeDefinition.Type.Enum): TypeSpec {
        val enumBuilder = TypeSpec.enumBuilder(name.toPascalCase())
            .addAnnotation(Serializable::class)
        enumValues.options.forEach { enumName ->
            enumBuilder.addEnumConstant(
                enumName.toPascalCase(),
                TypeSpec.anonymousClassBuilder()
                    .addAnnotation(AnnotationSpec.builder(SerialName::class).addMember("value = %S", enumName).build())
                    .build()
            )
        }
        return enumBuilder.build()
    }

    internal fun generatePropsForAttrs(attributes: List<AttributeDefinition>): GeneratedProperties {
        val types = mutableListOf<TypeSpec>()
        val properties = mutableListOf<PropertySpec>()
        val parameters = mutableListOf<ParameterSpec>()
        attributes.forEach { attribute ->
            val propertyName = attribute.attributeName.stripPrefix().toCamelCase()
            var type: TypeName = when (attribute.attributeType) {
                AttributeDefinition.Type.Entity,
                AttributeDefinition.Type.IdRef,
                AttributeDefinition.Type.NmToken,
                AttributeDefinition.Type.Id,
                AttributeDefinition.Type.Notation,
                AttributeDefinition.Type.CharacterData -> String::class.asTypeName()
                AttributeDefinition.Type.Entities,
                AttributeDefinition.Type.IdRefs,
                AttributeDefinition.Type.NmTokens,
                AttributeDefinition.Type.Xml -> List::class.parameterizedBy(String::class)
                is AttributeDefinition.Type.Enum -> {
                    val enumType = generateEnumForAttribute(attribute.attributeName.stripPrefix(),
                        attribute.attributeType as AttributeDefinition.Type.Enum
                    )
                    types.add(enumType)
                    ClassName(packageName, enumType.name!!)
                }
            }
            if (attribute.value is AttributeDefinition.Value.Implied) {
                type = type.copy(nullable = true)
            }
            if (attribute.value is AttributeDefinition.Value.Fixed) {
                properties.add(
                    PropertySpec.builder(propertyName, type)
                        .addModifiers(KModifier.PUBLIC)
                        .addAnnotation(AnnotationSpec.builder(XmlElement::class).addMember("value = %L", false).build())
                        .apply {
                            attribute.comment?.let { addKdoc(it) }
                        }
                        .apply {
                            if (attribute.attributeName.contains(":")) {
                                val (prefix, name) = attribute.attributeName.split(":")
                                addAnnotation(AnnotationSpec.builder(XmlSerialName::class)
                                    .addMember("prefix = %S", prefix)
                                    .addMember("value = %S", name)
                                    .build())
                                addAnnotation(AnnotationSpec.builder(SerialName::class)
                                    .addMember("value = %S", name)
                                    .build())
                            } else {
                                addAnnotation(AnnotationSpec.builder(SerialName::class)
                                    .addMember("value = %S", attribute.attributeName)
                                    .build())
                            }
                        }
                        .initializer((attribute.value as AttributeDefinition.Value.Fixed).value)
                        .build()
                )
            } else {
                if (attribute.value is AttributeDefinition.Value.Default) {
                    parameters.add(
                        ParameterSpec.builder(propertyName, type)
                            .defaultValue("\"${(attribute.value as AttributeDefinition.Value.Default).value}\"")
                            .build()
                    )
                } else {
                    parameters.add(
                        ParameterSpec.builder(propertyName, type)
                            .build()
                    )
                }

                properties.add(
                    PropertySpec.builder(propertyName, type)
                        .addModifiers(KModifier.PUBLIC)
                        .addAnnotation(AnnotationSpec.builder(XmlElement::class).addMember("value = %L", false).build())
                        .apply {
                            attribute.comment?.let { addKdoc(it) }
                        }
                        .apply {
                            if (attribute.attributeName.contains(":")) {
                                val (prefix, name) = attribute.attributeName.split(":")
                                addAnnotation(AnnotationSpec.builder(XmlSerialName::class)
                                    .addMember("prefix = %S", prefix)
                                    .addMember("value = %S", name)
                                    .build())
                                addAnnotation(AnnotationSpec.builder(SerialName::class)
                                    .addMember("value = %S", name)
                                    .build())
                            } else {
                                addAnnotation(AnnotationSpec.builder(SerialName::class)
                                    .addMember("value = %S", attribute.attributeName)
                                    .build())
                            }
                        }
                        .initializer(propertyName)
                        .build()
                )
            }
        }
        return GeneratedProperties(
            properties = properties,
            parameters = parameters,
            types = types
        )
    }

    internal data class GeneratedTypes(
        val rootClassName: ClassName,
        val topLevelTypes: List<TypeSpec>
    )

    internal data class GeneratedProperties(
        val properties: List<PropertySpec>,
        val parameters: List<ParameterSpec>,
        val types: List<TypeSpec>
    )
}
