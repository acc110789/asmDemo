package com.example.service_compiler;

import com.example.service_anno.ServiceImpl;
import com.google.auto.service.AutoService;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

@AutoService(Processor.class)
public class ServiceAnnotationProcessor extends AbstractProcessor {

    private static String TAG = "ServiceAnnotationProcessor";
    private static String BUILD = "build";
    private static String SERVICE_IMPL_METHOD_NAME = "service()";
    private static String SERVICE_JSON_PATH = "__service-config/service.json";

    private Messager messager;
    private File outputFile;

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new HashSet<>();
        types.add(ServiceImpl.class.getCanonicalName());
        return types;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    private void log(String msg) {
        if (msg.isEmpty()) return;
        messager.printMessage(Diagnostic.Kind.NOTE, msg);
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        Filer filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();

        FileObject resource = null;
        try {
            resource = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "temp_file");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        String classPath = resource.toUri().getPath();
        log("anchor class path: " + classPath);

        int indexOfBuild = classPath.lastIndexOf(BUILD);
        if (indexOfBuild < 0)
            throw new IllegalStateException(TAG + " can not find build dir from classPath: " + classPath);

        String buildDir = classPath.substring(0, indexOfBuild + BUILD.length()) + File.separator;
        File outputFile = new File(buildDir, SERVICE_JSON_PATH);
        this.outputFile = outputFile;
        outputFile.getParentFile().mkdirs();

        if (outputFile.exists()) outputFile.delete();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        Set<? extends Element> serviceImplElements = roundEnvironment.getElementsAnnotatedWith(ServiceImpl.class);
        JSONObject json = getAnnotationInfo(serviceImplElements);
        if (json.isEmpty()) return true;

        JSONObject existJson;
        try {
            existJson = Utils.getExistAnnotationInfo(outputFile);
            if (existJson != null) json = Utils.mergeJson(json, existJson, messager);
            Utils.writeJSONObjectToFile(json, outputFile);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return true;
    }

    @NotNull
    private JSONObject getAnnotationInfo(Set<? extends Element> serviceImplElements) {
        JSONObject json = new JSONObject();
        for (Element element : serviceImplElements) {
            TypeElement typeElement = (TypeElement) element;
            List<? extends AnnotationMirror> annotationMirrors = typeElement.getAnnotationMirrors();
            for (AnnotationMirror mirror : annotationMirrors) {
                Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = mirror.getElementValues();
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : elementValues.entrySet()) {
                    String key = entry.getKey().toString();
                    if (!SERVICE_IMPL_METHOD_NAME.equals(key)) continue;

                    String generalClassName = Utils.removeClassPostfix(entry.getValue().toString());
                    Object value = entry.getValue().getValue();
                    if (value instanceof Iterable) {
                        Iterable iterable = (Iterable) value;
                        for (Object one : iterable) {
                            String className = Utils.removeClassPostfix(one.toString());
                            Utils.putToJSON(json, className, typeElement.getQualifiedName().toString(), messager);
                        }
                    } else {
                        Utils.putToJSON(json, generalClassName, typeElement.getQualifiedName().toString(), messager);
                    }
                }
            }
        }
        return json;
    }
}
