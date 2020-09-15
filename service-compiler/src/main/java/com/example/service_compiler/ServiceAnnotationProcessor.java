package com.example.service_compiler;

import com.example.service_anno.Service;
import com.example.service_anno.ServiceImpl;
import com.google.auto.service.AutoService;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

@AutoService(Processor.class)
public class ServiceAnnotationProcessor extends AbstractProcessor {

    private static String TAG = "ServiceAnnotationProcessor";
    private static String BUILD = "build";
    private static String INTERFACE = "interface";
    private static String IMPL = "impl";

    private Messager messager;
    private File outputFile;

    private void log(String msg) {
        if (msg.isEmpty()) return;
        messager.printMessage(Diagnostic.Kind.NOTE, msg);
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        try {
            super.init(processingEnvironment);
            Filer filer = processingEnv.getFiler();
            this.messager = processingEnv.getMessager();

            FileObject resource = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "temp_file");
            String classPath = resource.toUri().getPath();
            log("anchor class path: " + classPath);

            int indexOfBuild = classPath.lastIndexOf(BUILD);
            if (indexOfBuild < 0)
                throw new IllegalStateException(TAG + " can not find build dir from classPath: " + classPath);

            String buildDir = classPath.substring(0, indexOfBuild + BUILD.length()) + File.separator;
            File outputFile = new File(buildDir, "__service-config/service.json");
            this.outputFile = outputFile;
            outputFile.getParentFile().mkdirs();

            if (outputFile.exists()) outputFile.delete();
        } catch (Throwable th) {
            throw new IllegalStateException("should not happen");
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        Set<? extends Element> serviceElements = roundEnvironment.getElementsAnnotatedWith(Service.class);
        Set<? extends Element> serviceImplElements = roundEnvironment.getElementsAnnotatedWith(ServiceImpl.class);

        JSONObject json = new JSONObject();

        List<String> serviceNameList = new ArrayList<>();
        for (Element element : serviceElements) {
            if (element instanceof TypeElement) {
                TypeElement typeElement = (TypeElement) element;
                serviceNameList.add(typeElement.getQualifiedName().toString());
            }
        }
        if (!serviceNameList.isEmpty()) {
            json.put(INTERFACE, serviceNameList);
        }

        List<String> serviceImplNameList = new ArrayList<>();
        for (Element element : serviceImplElements) {
            TypeElement typeElement = (TypeElement) element;
            serviceImplNameList.add(typeElement.getQualifiedName().toString());
        }
        if (!serviceImplNameList.isEmpty()) {
            json.put(IMPL, serviceImplNameList);
        }

        if (json.isEmpty()) {
            return true;
        }

        String result = json.toString();
        File outputFile = this.outputFile;
        try {
            if (outputFile != null) {
                FileOutputStream outputStream = new FileOutputStream(outputFile);
                outputStream.write(result.getBytes());
                outputStream.flush();
                outputStream.close();
            }
        } catch (Throwable th) {
            throw new IllegalStateException(th);
        }
        return true;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new HashSet<>();
        types.add(ServiceImpl.class.getCanonicalName());
        types.add(Service.class.getCanonicalName());
        return types;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}
