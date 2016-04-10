/*
 * Copyright © 2016 SNLAB and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.snlab.vxe.demo.opendaylight.impl;

import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snlab.vxe.api.Datastore;
import org.snlab.vxe.api.Identifier;
import org.snlab.vxe.demo.opendaylight.impl.VxeOpenDaylight.OpenDaylightRpc;

import com.google.common.base.Optional;

public class VxeOpenDaylightDatastore implements Datastore {

    private static final Logger LOG = LoggerFactory.getLogger(VxeOpenDaylightDatastore.class);

    private DataBroker broker = null;
    private ReadWriteTransaction rwt = null;
    private VxeOpenDaylight context = null;

    public VxeOpenDaylightDatastore(DataBroker broker, VxeOpenDaylight context) {
        this.broker = broker;
        this.context = context;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T read(Identifier<T> id) {
        if (id instanceof VxeOpenDaylightIdentifier) {
            if (rwt == null) {
                rwt = broker.newReadWriteTransaction();
            }
            VxeOpenDaylightIdentifier<? extends DataObject> vid;
            vid = (VxeOpenDaylightIdentifier<? extends DataObject>) id;

            try {
                Optional<? extends DataObject> result;
                result = rwt.read(vid.getType(), vid.getInstanceIdentifier()).get();
                return (T) result.get();
            } catch (InterruptedException | ExecutionException e) {
                return null;
            }
        } else if (id instanceof VxeOpenDaylightRpc) {
            VxeOpenDaylightRpc<? extends DataObject, ? extends DataObject> rpcId;
            rpcId = (VxeOpenDaylightRpc<? extends DataObject, ? extends DataObject>) id;

            return (T) readRpc(rpcId);
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private <I extends DataObject, O extends DataObject> O readRpc(VxeOpenDaylightRpc<I, O> id) {
        DataObject input = id.getInput();
        OpenDaylightRpc<?, ?> rpc = context.getRpc(input);

        if (rpc == null) {
            LOG.warn("Failed to find RPC instance!");
            return null;
        }
        try {
            rwt.submit().get();
            rwt = null;
        } catch (Exception e) {
            LOG.warn(e.getMessage());
        }
        return (O) forceExecute(rpc, input);
    }

    @SuppressWarnings("unchecked")
    private <O extends DataObject, I extends DataObject> O forceExecute(OpenDaylightRpc<I, O> rpc, DataObject input) {
        if (rwt == null) {
            rwt = broker.newReadWriteTransaction();
        }
        try {
            RpcResult<O> result = rpc.process((I) input).get();
            if (result.isSuccessful()) {
                return (O) result.getResult();
            } else {
                for (Object error: result.getErrors()) {
                    LOG.warn("Rpc error: {}", error);
                }
                return null;
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Failed to execute RPC: {}", e.getMessage());
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void write(Identifier<T> id, T value) {
        VxeOpenDaylightIdentifier<? extends DataObject> vid;
        vid = (VxeOpenDaylightIdentifier<? extends DataObject>) id;

        forceWrite(vid.getType(), vid.getInstanceIdentifier(), (Object) value);
    }

    @SuppressWarnings("unchecked")
    private <T extends DataObject> void forceWrite(LogicalDatastoreType type,
                                                    InstanceIdentifier<T> iid,
                                                    Object value) {
        rwt.put(type, iid, (T) value, true);
    }

    public ReadWriteTransaction getTx() {
        return rwt;
    }
}
