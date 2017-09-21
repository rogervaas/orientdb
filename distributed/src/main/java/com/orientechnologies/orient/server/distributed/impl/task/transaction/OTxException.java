package com.orientechnologies.orient.server.distributed.impl.task.transaction;

import com.orientechnologies.common.exception.OException;

public class OTxException implements OTransactionResultPayload {
  private static final int ID = 5;

  private Throwable exception;

  public OTxException(Throwable exception) {
    this.exception = exception;
  }

  public Throwable getException() {
    return exception;
  }

  public void setException(Throwable exception) {
    this.exception = exception;
  }

  @Override
  public int getResponseType() {
    return ID;
  }
}
