package com.orientechnologies.orient.server.distributed.impl.task;

import com.orientechnologies.orient.client.remote.message.OMessageHelper;
import com.orientechnologies.orient.client.remote.message.tx.ORecordOperationRequest;
import com.orientechnologies.orient.core.command.OCommandDistributedReplicateRequest;
import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal;
import com.orientechnologies.orient.core.db.record.ORecordOperation;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.ORecordInternal;
import com.orientechnologies.orient.core.serialization.serializer.record.binary.ORecordSerializerNetworkV37;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;
import com.orientechnologies.orient.core.storage.impl.local.paginated.wal.OLogSequenceNumber;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.distributed.ODistributedRequestId;
import com.orientechnologies.orient.server.distributed.ODistributedServerManager;
import com.orientechnologies.orient.server.distributed.ORemoteTaskFactory;
import com.orientechnologies.orient.server.distributed.impl.ODatabaseDocumentDistributed;
import com.orientechnologies.orient.server.distributed.impl.task.transaction.*;
import com.orientechnologies.orient.server.distributed.task.OAbstractReplicatedTask;
import com.orientechnologies.orient.server.distributed.task.ODistributedLockException;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

/**
 * @author Luigi Dell'Aquila (l.dellaquila - at - orientdb.com)
 */
public class OTransactionPhase1Task extends OAbstractReplicatedTask {
  public static final int FACTORYID = 43;

  private OLogSequenceNumber     lastLSN;
  private List<ORecordOperation> ops;

  public OTransactionPhase1Task() {
    ops = new ArrayList<>();
  }

  public OTransactionPhase1Task(List<ORecordOperation> ops) {
    this.ops = ops;
  }

  @Override
  public String getName() {
    return "TxPhase1";
  }

  @Override
  public OCommandDistributedReplicateRequest.QUORUM_TYPE getQuorumType() {
    return OCommandDistributedReplicateRequest.QUORUM_TYPE.WRITE;
  }

  @Override
  public Object execute(ODistributedRequestId requestId, OServer iServer, ODistributedServerManager iManager,
      ODatabaseDocumentInternal database) throws Exception {
    OTransactionResultPayload payload;
    try {
      ((ODatabaseDocumentDistributed) database).beginDistributedTx(ops);
      payload = new OTxSuccess();
    } catch (OConcurrentModificationException ex) {
      payload = new OTxConcurrentModification((ORecordId) ex.getRid(), ex.getEnhancedDatabaseVersion());
    } catch (ODistributedLockException ex) {
      payload = new OTxLockTimeout();
    } catch (ORecordDuplicatedException ex) {
      //TODO:Check if can get out the key
      payload = new OTxUniqueIndex((ORecordId) ex.getRid(), ex.getIndexName(), null);
    } catch (Exception ex) {
      payload = new OTxException(ex);
    }
    return new OTransactionPhase1TaskResult(payload);
  }

  @Override
  public void fromStream(DataInput in, ORemoteTaskFactory factory) throws IOException {

    int size = in.readInt();
    for (int i = 0; i < size; i++) {
      ORecordOperationRequest req = OMessageHelper.readTransactionEntry(in);

      byte type = req.getType();
      if (type == ORecordOperation.LOADED) {
        continue;
      }

      ORecord record = null;
      switch (type) {
      case ORecordOperation.CREATED:
      case ORecordOperation.UPDATED:
        record = ORecordSerializerNetworkV37.INSTANCE.fromStream(req.getRecord(), null, null);
        break;
      }
      ORecordOperation op = new ORecordOperation(record, type);
      ops.add(op);

    }

    lastLSN = new OLogSequenceNumber(in);
  }

  @Override
  public void toStream(DataOutput out) throws IOException {
    out.writeInt(ops.size());

    for (ORecordOperation txEntry : ops) {
      if (txEntry.type == ORecordOperation.LOADED)
        continue;
      ORecordOperationRequest request = new ORecordOperationRequest();
      request.setType(txEntry.type);
      request.setVersion(txEntry.getRecord().getVersion());
      request.setId(txEntry.getRecord().getIdentity());
      request.setRecordType(ORecordInternal.getRecordType(txEntry.getRecord()));
      switch (txEntry.type) {
      case ORecordOperation.CREATED:
      case ORecordOperation.UPDATED:
        request.setRecord(ORecordSerializerNetworkV37.INSTANCE.toStream(txEntry.getRecord(), false));
        request.setContentChanged(ORecordInternal.isContentChanged(txEntry.getRecord()));
        break;
      }
      OMessageHelper.writeTransactionEntry(out, request);
    }
    lastLSN.toStream(out);
  }

  @Override
  public int getFactoryId() {
    return FACTORYID;
  }

  public void init(List<ORecordOperation> operations) {
    this.ops = operations;
  }

  public void setLastLSN(OLogSequenceNumber lastLSN) {
    this.lastLSN = lastLSN;
  }
}
