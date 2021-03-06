package com.devexperts.jagent.sample;

/*
 * #%L
 * Sample Transformer
 * %%
 * Copyright (C) 2015 - 2016 Devexperts, LLC
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import com.devexperts.jagent.*;
import org.objectweb.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

class MethodDeleterTransformer implements ClassFileTransformer {

    private static final int ASM_API = Opcodes.ASM5;

    private final String classNameToProcess;
    private final String methodToDelete;
    private final ClassInfoCache ciCache;

    MethodDeleterTransformer(String clazz, String method, Log log) {
        this.classNameToProcess = clazz.replace('.', '/');
        this.methodToDelete = method;
        this.ciCache = new ClassInfoCache(log);
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> clazz, ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {
        if (!classNameToProcess.equals(className))
            return null;
        ClassReader cr = new ClassReader(classfileBuffer);
        ClassInfoVisitor ciVisitor = new ClassInfoVisitor();
        cr.accept(ciVisitor, 0);
        ClassInfo cInfo = ciVisitor.buildClassInfo();
        ciCache.getOrInitClassInfoMap(loader).put(className, cInfo);
        ClassWriter cw = new FrameClassWriter(loader, ciCache, cInfo.getVersion());
        ClassVisitor transformer = new ClassVisitor(ASM_API, cw) {
            @Override
            public MethodVisitor visitMethod(int access, final String mname, final String desc, String signature, String[] exceptions) {
                if (methodToDelete.equals(mname))
                    return null;
                return super.visitMethod(access, mname, desc, signature, exceptions);
            }
        };
        cr.accept(transformer, 0);
        return cw.toByteArray();
    }
}
