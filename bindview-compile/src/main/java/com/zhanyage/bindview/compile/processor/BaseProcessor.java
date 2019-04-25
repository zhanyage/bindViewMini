package com.zhanyage.bindview.compile.processor;



import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;
import com.zhanyage.bindview.compile.entry.Id;
import com.zhanyage.bindview.compile.utils.Logger;

import javax.annotation.Nullable;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Created by andya on 2019/4/12
 * Describe: base processor
 */
public abstract class BaseProcessor extends AbstractProcessor {

    Filer mFiler;
    Logger logger;
    Types typesUtil;
    Elements elementUtils;
    Trees trees;
    final RScanner rScanner = new RScanner();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        mFiler = processingEnvironment.getFiler();
        logger = new Logger(processingEnvironment.getMessager());
        typesUtil = processingEnvironment.getTypeUtils();
        elementUtils = processingEnvironment.getElementUtils();
        try {
            trees = Trees.instance(processingEnv);
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    static class RScanner extends TreeScanner {
        public Map<Integer, Id> resourceIds = new LinkedHashMap<>();

        @Override
        public void visitSelect(JCTree.JCFieldAccess jcFieldAccess) {
            Symbol symbol = jcFieldAccess.sym;
            if (symbol.getEnclosingElement() != null
                    && symbol.getEnclosingElement().getEnclosingElement() != null
                    && symbol.getEnclosingElement().getEnclosingElement().enclClass() != null) {
                try {
                    int value = (Integer) requireNonNull(((Symbol.VarSymbol) symbol).getConstantValue());
                    resourceIds.put(value, new Id(value, symbol));
                } catch (Exception ignored) {
                }
            }
        }
        void reset() {
            resourceIds.clear();
        }
    }
}
