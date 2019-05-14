package com.zhanyage.bindview.compile.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import com.sun.tools.javac.tree.JCTree;
import com.zhanyage.bindview.annotation.Bind;
import com.zhanyage.bindview.compile.entry.Id;
import com.zhanyage.bindview.compile.utils.Constants;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import javax.annotation.Nullable;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.*;

import static com.google.auto.common.MoreElements.getPackage;
import static com.zhanyage.bindview.compile.utils.Constants.*;
import static javax.lang.model.element.ElementKind.CLASS;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

/**
 * Created by andya on 2019/4/12
 * Describe: Bind for Processor
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes({ANNOTATION_TYPE_BIND})
public class BindProcessor extends BaseProcessor {

    //bind collection for class
    private Map<TypeElement, List<Element>> parentAndChild = new HashMap<>();   // Contain field need autowired and his super class.


    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        for (Class<? extends Annotation> annotation : getSupportedAnnotation()) {
            types.add(annotation.getCanonicalName());
        }
        return super.getSupportedAnnotationTypes();
    }

    private Set<Class<? extends Annotation>> getSupportedAnnotation() {
        Set<Class<? extends Annotation>> annotations = new LinkedHashSet<>();
        annotations.add(Bind.class);
        return annotations;
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (CollectionUtils.isNotEmpty(set)) {
            try {
                logger.info(">>> Found bind field, start... <<<");
                findAndParseTargets(roundEnvironment);
            } catch (Exception e) {
                logger.error(e);
            }
        }

        return false;
    }

    private void findAndParseTargets(RoundEnvironment env) {
        try {
            categories(env.getElementsAnnotatedWith(Bind.class));
            parseBindView();
        } catch (Exception e) {
            logger.error(e);
        }
    }

    /**
     * generate a file for use BindView class
     */
    private void parseBindView() {
        TypeMirror viewTm = elementUtils.getTypeElement(Constants.VIEW_TYPE).asType();
        ClassName iBindTemplateClass = ClassName.get(Constants.BIND_TEMPLATE_PACKAGE + Constants.TEMPLATE_PACKAGE, IBINDTEMPLATE);
        ParameterSpec objectParameter = ParameterSpec.builder(TypeName.OBJECT, PARAMETER_TARGET).build();
        ParameterSpec viewObjectParameter = ParameterSpec.builder(TypeName.OBJECT, PARAMETER_VIEW_TARGET).build();

        if (MapUtils.isNotEmpty(parentAndChild)) {
            //real start process bindView annotation for a class
            for (Map.Entry<TypeElement, List<Element>> entry : parentAndChild.entrySet()) {
                //Build Method : bindTargetView method
                MethodSpec.Builder bindTargetViewMethodSpec = MethodSpec.methodBuilder(METHOD_BINDTARGETVIEW)
                        .addAnnotation(Override.class)
                        .addModifiers(PUBLIC)
                        .addParameter(objectParameter)
                        .addParameter(viewObjectParameter);

                //get parent Element
                TypeElement parent = entry.getKey();
                //get this class Bind for field
                List<Element> childs = entry.getValue();

                //this get package will not geterate SimpleAdapter package
                String packageName = getPackage(parent).getQualifiedName().toString();
                String className = parent.getQualifiedName().toString().substring(
                        packageName.length() + 1).replace('.', '$');
                ClassName fileName = ClassName.get(packageName, className + NAME_OF_BINDVIEW);

                logger.info(">>> Start process " + childs.size() + " field in " + parent.getSimpleName() + " ... <<<");
                logger.info(">>> fileName:" + IBINDTEMPLATE);
                TypeSpec.Builder bindViewClassTypeSpec = TypeSpec.classBuilder(fileName)
                        .addJavadoc(WARNING_TIPS)
                        .addSuperinterface(iBindTemplateClass)
                        .addModifiers(PUBLIC);

                bindTargetViewMethodSpec.addStatement("$T substitute = ($T)target", ClassName.get(parent),
                        ClassName.get(parent));
                bindTargetViewMethodSpec.addStatement("$T substituteView = ($T)viewTarget", ClassName.get(viewTm),
                        ClassName.get(viewTm));

                for (Element element : childs) {
                    boolean isError = isInaccessibleViaGeneratedCode(Bind.class, "fields", element);
                    //judge this element is  view , if not view then toast error
                    if (!isSubtypeOfType(element.asType(),Constants.VIEW_TYPE)) {
                        logger.error("type is not same");
                        isError = true;
                    }
                    if (isError) {
                        logger.error("isError = true");
                        return;
                    }
                    Bind fieldConfig = element.getAnnotation(Bind.class);
                    //get type name
                    String fieldName = element.getSimpleName().toString();
                    String originalValue = "substitute." + fieldName;
                    String statement = originalValue + "=" + buildCastCode(element) + "(substituteView.findViewById($L))";
                    Id resourceId = elementToId(element, Bind.class, fieldConfig.value());
                    bindTargetViewMethodSpec.addStatement(statement, resourceId.code);
                }
                bindViewClassTypeSpec.addMethod(bindTargetViewMethodSpec.build());
                try {
                    JavaFile.builder(packageName, bindViewClassTypeSpec.build()).build().writeTo(mFiler);
                } catch (IOException e) {
                    logger.error(">>> writeTo file fail" + mFiler.toString());
                }
                logger.info(">>> " + parent.getSimpleName() + " has been processed, " + fileName + " has been generated. <<<");
            }
            logger.info(">>> Bind processor stop. <<<");
        }
    }

    private String buildCastCode(Element element) {
        return CodeBlock.builder().add("($T) ", ClassName.get(element.asType())).build().toString();
    }

    /**
     * judge bindView use is reasonable
     */
    private boolean isInaccessibleViaGeneratedCode(Class<? extends Annotation> annotationClass,
                                                   String targetThing, Element element) {
        boolean hasError = false;
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

        // Verify field or method modifiers.
        Set<Modifier> modifiers = element.getModifiers();
        logger.info(element.getModifiers().contains(PUBLIC)?"contains(PUBLIC):true":"false");
        if (modifiers.contains(PRIVATE) || modifiers.contains(STATIC)) {
            logger.error(String.format("@%s %s must not be private or static. (%s.%s)",
                    annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
                    element.getSimpleName()));
            hasError = true;
        }

        // Verify containing type.
        if (enclosingElement.getKind() != CLASS) {
            logger.error(String.format("@%s %s may only be contained in classes. (%s.%s)",
                    annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
                    element.getSimpleName()));
            hasError = true;
        }

        // Verify containing class visibility is not private.
        if (enclosingElement.getModifiers().contains(PRIVATE)) {
            logger.error(String.format("@%s %s may not be contained in private classes. (%s.%s)",
                    annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
                    element.getSimpleName()));
            hasError = true;
        }

        return hasError;
    }

    /**
     * judge typeMirror is otherType or child
     */
    static boolean isSubtypeOfType(TypeMirror typeMirror, String otherType) {
        if (isTypeEqual(typeMirror, otherType)) {
            return true;
        }
        //if not interface or class , return
        if (typeMirror.getKind() != TypeKind.DECLARED) {
            return false;
        }
        DeclaredType declaredType = (DeclaredType) typeMirror;
        //return this type real field type
        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        if (typeArguments.size() > 0) {
            StringBuilder typeString = new StringBuilder(declaredType.asElement().toString());
            typeString.append('<');
            for (int i = 0; i < typeArguments.size(); i++) {
                if (i > 0) {
                    typeString.append(',');
                }
                typeString.append('?');
            }
            typeString.append('>');
            if (typeString.toString().equals(otherType)) {
                return true;
            }
        }
        Element element = declaredType.asElement();
        if (!(element instanceof TypeElement)) {
            return false;
        }
        TypeElement typeElement = (TypeElement) element;
        TypeMirror superType = typeElement.getSuperclass();
        if (isSubtypeOfType(superType, otherType)) {
            return true;
        }
        for (TypeMirror interfaceType : typeElement.getInterfaces()) {
            if (isSubtypeOfType(interfaceType, otherType)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTypeEqual(TypeMirror typeMirror, String otherType) {
        return otherType.equals(typeMirror.toString());
    }

    private Id elementToId(Element element, Class<? extends Annotation> annotation, int value) {
        JCTree tree = (JCTree) trees.getTree(element, getMirror(element, annotation));
        if (tree != null) { // tree can be null if the references are compiled types and not source
            rScanner.reset();
            tree.accept(rScanner);
            if (!rScanner.resourceIds.isEmpty()) {
                return rScanner.resourceIds.values().iterator().next();
            }
        }
        return new Id(value);
    }

    private static @Nullable
    AnnotationMirror getMirror(Element element,
                               Class<? extends Annotation> annotation) {
        for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
            if (annotationMirror.getAnnotationType().toString().equals(annotation.getCanonicalName())) {
                return annotationMirror;
            }
        }
        return null;
    }

    /**
     * Categories field, find his papa.
     *
     * @param elements Field need autowired
     */
    private void categories(Set<? extends Element> elements) throws IllegalAccessException {
        if (CollectionUtils.isNotEmpty(elements)) {
            for (Element element : elements) {
                TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

                if (element.getModifiers().contains(Modifier.PRIVATE)) {
                    throw new IllegalAccessException("The inject fields CAN NOT BE 'private'!!! please check field ["
                            + element.getSimpleName() + "] in class [" + enclosingElement.getQualifiedName() + "]");
                }
                if (parentAndChild.containsKey(enclosingElement)) { // Has categries
                    parentAndChild.get(enclosingElement).add(element);
                } else {
                    List<Element> childs = new ArrayList<>();
                    childs.add(element);
                    parentAndChild.put(enclosingElement, childs);
                }
            }

            logger.info("categories finished.");
        }
    }


}
