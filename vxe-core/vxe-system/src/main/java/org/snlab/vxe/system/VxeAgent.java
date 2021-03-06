/*
 * Copyright © 2016 SNLAB and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.snlab.vxe.system;

import java.lang.instrument.Instrumentation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VxeAgent {

    protected static Logger LOG = LoggerFactory.getLogger(VxeAgent.class);

    private static boolean loaded = false;
    private static Instrumentation instrumentation = null;
    private static VxeTransformer transformer = null;

    public static void premain(String agentArgs, Instrumentation inst) {
        setup(agentArgs, inst, "Premain-Class");
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        setup(agentArgs, inst, "Agent-Class");
    }

    private static void setup(String agentArgs, Instrumentation inst, String how) {
        if (loaded) {
            LOG.warn("VxeAgent already loaded, skipping");
            return;
        }
        loaded = true;

        instrumentation = inst;

        //TODO initialize the system
        //TODO add classfile transformers
        transformer = new VxeTransformer();
        instrumentation.addTransformer(transformer);

        LOG.info("VxeAgent loaded as {}", how);
    }

    public static byte[] getOriginalBytecode(Class<?> clazz) {
        if (!loaded) {
            return null;
        }
        return (transformer.getOriginalBytecode(clazz));
    }
}
