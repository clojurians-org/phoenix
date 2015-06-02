package org.apache.phoenix.calcite.rel;

import java.util.List;

import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Project;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexNode;
import org.apache.phoenix.calcite.CalciteUtils;
import org.apache.phoenix.execute.TupleProjector;
import org.apache.phoenix.expression.Expression;
import org.apache.phoenix.schema.PTable;
import org.apache.phoenix.schema.TableRef;

import com.google.common.collect.Lists;

/**
 * Implementation of {@link org.apache.calcite.rel.core.Project}
 * relational expression in Phoenix.
 */
abstract public class PhoenixAbstractProject extends Project implements PhoenixRel {
    protected PhoenixAbstractProject(RelOptCluster cluster, RelTraitSet traits, RelNode input, List<? extends RexNode> projects, RelDataType rowType) {
        super(cluster, traits, input, projects, rowType);
        assert getConvention() == PhoenixRel.CONVENTION;
    }

    @Override
    public RelOptCost computeSelfCost(RelOptPlanner planner) {
        // This is to minimize the weight of cost of Project so that it
        // does not affect more important decisions like join algorithms.
        double rowCount = RelMetadataQuery.getRowCount(getInput());
        double rows = 2 * rowCount / (rowCount + 1);
        return planner.getCostFactory().makeCost(rows, 0, 0);
    }
    
    protected TupleProjector project(Implementor implementor) {        
        List<Expression> exprs = Lists.newArrayList();
        for (RexNode project : getProjects()) {
            exprs.add(CalciteUtils.toExpression(project, implementor));
        }
        TupleProjector tupleProjector = implementor.project(exprs);
        PTable projectedTable = implementor.createProjectedTable();
        implementor.setTableRef(new TableRef(projectedTable));

        return tupleProjector;
    }
}