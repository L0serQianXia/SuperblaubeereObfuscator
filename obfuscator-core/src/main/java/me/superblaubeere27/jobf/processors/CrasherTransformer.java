/*
 * Copyright (c) 2017-2019 superblaubeere27, Sam Sun, MarcoMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package me.superblaubeere27.jobf.processors;

import me.superblaubeere27.annotations.ObfuscationTransformer;
import me.superblaubeere27.jobf.IClassTransformer;
import me.superblaubeere27.jobf.JObfImpl;
import me.superblaubeere27.jobf.ProcessorCallback;
import me.superblaubeere27.jobf.utils.NameUtils;
import me.superblaubeere27.jobf.utils.values.BooleanValue;
import me.superblaubeere27.jobf.utils.values.DeprecationLevel;
import me.superblaubeere27.jobf.utils.values.EnabledValue;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import javax.naming.directory.Attributes;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Random;

public class CrasherTransformer implements IClassTransformer {
    private static final String EMPTY_STRINGS;

    private Attribute BOOTSTRAP_METHODS = DummyAttribute("BootstrapMethods");
    private Attribute MODULE_MAIN_CLASS = DummyAttribute("ModuleMainClass");
    private Attribute NEST_HOST;

    {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(2);
            new DataOutputStream(byteArrayOutputStream).writeInt(6);
            NEST_HOST = DummyAttribute("NestHost", byteArrayOutputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Attribute CODE = DummyAttribute("Code");
    private Attribute CONSTANT_VALUE = DummyAttribute("ConstantValue");
    private Attribute STACKMAP = DummyAttribute("StackMap");

    static {
        StringBuilder stringBuilder = new StringBuilder();

        for (int j = 0; j < 50000; j++) {
            stringBuilder.append("\n");
        }

        EMPTY_STRINGS = stringBuilder.toString();
    }

    private EnabledValue enabled = new EnabledValue("Crasher", DeprecationLevel.GOOD, false);
    private BooleanValue invalidSignatures = new BooleanValue("Crasher", "Invalid Signatures", "Adds invalid signatures", DeprecationLevel.GOOD, true);
    private BooleanValue emptyAnnotation = new BooleanValue("Crasher", "Empty annotation spam", "Adds annotations which are repeated newline", DeprecationLevel.GOOD, true);
    private BooleanValue badAttribute = new BooleanValue("Crasher", "Bad Attribute", "Adds bad attribute to classes, methods, fields to break ASM", DeprecationLevel.GOOD, true);
    private JObfImpl inst;

    public CrasherTransformer(JObfImpl inst) {
        this.inst = inst;
    }

    @Override
    public void process(ProcessorCallback callback, ClassNode node) {
        if (Modifier.isInterface(node.access)) return;
        if (!enabled.getObject()) return;

        if (invalidSignatures.getObject()) {
            /*
             * By ItzSomebody
             */
            if (node.signature == null) {
                node.signature = NameUtils.crazyString(10);
            }
        }

        if (emptyAnnotation.getObject()) {
            node.methods.forEach(method -> {

                if (method.invisibleAnnotations == null)
                    method.invisibleAnnotations = new ArrayList<>();

                for (int i = 0; i < 50; i++) {
                    method.invisibleAnnotations.add(new AnnotationNode(EMPTY_STRINGS));
                }
            });
        }

        if (badAttribute.getObject()) {
            if (node.attrs == null) {
                node.attrs = new ArrayList<>();
            }
            if (!versionAtLeast(node, Opcodes.V1_7)) {
                node.attrs.add(BOOTSTRAP_METHODS);
            } else if (!versionAtLeast(node, Opcodes.V9)) {
                node.attrs.add(MODULE_MAIN_CLASS);
            } else if (!versionAtLeast(node, Opcodes.V11)) {
                node.attrs.add(NEST_HOST);
            }

            node.fields.forEach(field -> {
                if (field.attrs == null) {
                    field.attrs = new ArrayList<>();
                }
                field.attrs.add(CODE);
            });

            node.methods.forEach(method -> {
                if (method.attrs == null) {
                    method.attrs = new ArrayList<>();
                }
                method.attrs.add(CONSTANT_VALUE);
                method.attrs.add(STACKMAP);
            });
        }
        inst.setWorkDone();
    }

    private Attribute DummyAttribute(String name) {
        Random random = new Random();
        return DummyAttribute(name, new byte[2 + random.nextInt(4)]);
    }

    private Attribute DummyAttribute(String name, byte[] bytes) {
        Random random = new Random();
        random.nextBytes(bytes);
        try {
            Constructor<Attribute> constructor = Attribute.class.getDeclaredConstructor(String.class);
            constructor.setAccessible(true);
            Attribute attribute = constructor.newInstance(name);
            Field content = attribute.getClass().getDeclaredField("content");
            content.setAccessible(true);
            content.set(attribute, bytes);
            return attribute;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Boolean versionAtLeast(ClassNode node, int minVersion) {
        int thisMajor = node.version & 0xFFFF;
        int minMajor = minVersion & 0xFFFF;

        return thisMajor >= minMajor;
    }

    @Override
    public ObfuscationTransformer getType() {
        return ObfuscationTransformer.CRASHER;
    }


}