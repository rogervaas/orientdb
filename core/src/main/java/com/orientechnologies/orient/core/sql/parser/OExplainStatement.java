/* Generated By:JJTree: Do not edit this line. OExplainStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.command.OBasicCommandContext;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.ODatabase;
import com.orientechnologies.orient.core.sql.executor.OExecutionPlan;
import com.orientechnologies.orient.core.sql.executor.OInternalExecutionPlan;
import com.orientechnologies.orient.core.sql.executor.OTodoResultSet;

import java.util.Map;

public class OExplainStatement extends OStatement {

  protected OStatement statement;

  public OExplainStatement(int id) {
    super(id);
  }

  public OExplainStatement(OrientSql p, int id) {
    super(p, id);
  }

  @Override public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("EXPLAIN ");
    statement.toString(params, builder);
  }

  @Override public OTodoResultSet execute(ODatabase db, Object[] args) {
    OBasicCommandContext ctx = new OBasicCommandContext();
    ctx.setDatabase(db);
    ctx.setArgs(args);

    OExecutionPlan executionPlan = statement.createExecutionPlan(ctx);

    OExplainResultSet result = new OExplainResultSet(executionPlan);
    return result;
  }

  @Override public OInternalExecutionPlan createExecutionPlan(OCommandContext ctx) {
    return statement.createExecutionPlan(ctx);
  }
}
/* JavaCC - OriginalChecksum=9fdd24510993cbee32e38a51c838bdb4 (do not edit this line) */
