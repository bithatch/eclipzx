package uk.co.bithatch.zxbasic;

import java.io.File;
import java.io.IOException;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.resource.XtextResourceSet;

import com.google.inject.Injector;

import uk.co.bithatch.zxbasic.basic.Program;
import uk.co.bithatch.zxbasic.interpreter.ZXBasicInterpreter;

public class ZxBasicRunner {

    public static void main(String[] args) throws IOException {

        if (args.length == 0) {
            System.err.println("Usage: ZxBasicRunner <file.zxbasic>");
            return;
        }

        File file = new File(args[0]);
        if (!file.exists()) {
            System.err.println("File not found: " + file.getAbsolutePath());
            return;
        }

        Injector injector = new BasicStandaloneSetup().createInjectorAndDoEMFRegistration();
        ResourceSet resourceSet = injector.getInstance(XtextResourceSet.class);
        Resource resource = resourceSet.getResource(org.eclipse.emf.common.util.URI.createFileURI(file.getAbsolutePath()), true);

        var program = (Program) resource.getContents().get(0);
        ZXBasicInterpreter interpreter = ZXBasicInterpreter.create();
        interpreter.run(program);
    }
}
