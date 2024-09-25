package org.example;

import com.google.common.collect.Lists;
import com.gurobi.gurobi.*;
import org.example.vo.ReturnTrade;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Returns {

    public static <T> List<Long> max(List<T> list,
                                     long totalReturn,
                                     Function<T, Double> costFunction,
                                     Function<T, Long> maxFunction,
                                     GRBEnv env,
                                     String name) throws GRBException {
        GRBModel model = new GRBModel(env);
        model.set(GRB.StringAttr.ModelName, name);
        GRBLinExpr costExpr = new GRBLinExpr();
        GRBLinExpr assignedQuantityExpr = new GRBLinExpr();
        List<GRBVar> vars = Lists.newArrayList();
        for (T t : list) {
            double cost = costFunction.apply(t);
            GRBVar assignedQuantityVar = model.addVar(0, maxFunction.apply(t), 0, GRB.INTEGER, "" + t.hashCode());
            vars.add(assignedQuantityVar);
            assignedQuantityExpr.addTerm(1, assignedQuantityVar);
            costExpr.addTerm(cost, assignedQuantityVar);
        }
        model.addConstr(assignedQuantityExpr, GRB.LESS_EQUAL, totalReturn, "totalNeedToReturn");
        model.setObjective(costExpr, GRB.MAXIMIZE);
        model.optimize();

        List<Long> returnList = vars.stream().map(v -> getValue(v)).map(Double::longValue).collect(Collectors.toList());
        model.dispose();
        return returnList;
    }

    public static Double getValue(GRBVar var) {
        try {
            return var.get(GRB.DoubleAttr.Xn);
        } catch (GRBException e) {
            throw new RuntimeException(e);
        }
    }

    public static void ruleApply(List<ReturnTrade> trades, long needs, GRBEnv env) throws GRBException {
        GRBModel model = new GRBModel(env);
        model.set(GRB.StringAttr.ModelName, "rate/div rate");

        GRBLinExpr feeCost = new GRBLinExpr();
        GRBLinExpr assignedQty = new GRBLinExpr();
        GRBLinExpr divRateCost = new GRBLinExpr();
        List<GRBVar> suggestQtyList = Lists.newArrayList();

        for (ReturnTrade trade: trades) {
            GRBVar assignQtyVar = model.addVar(0, trade.getQty(), 0, GRB.CONTINUOUS, "" + trade.hashCode());
            suggestQtyList.add(assignQtyVar);
            assignedQty.addTerm(1, assignQtyVar);
            feeCost.addTerm(trade.getFee(), assignQtyVar);
            divRateCost.addTerm(trade.getDivRate(), assignQtyVar);
        }
        model.addConstr(assignedQty, GRB.LESS_EQUAL, needs, "needsConstr");
        model.setObjectiveN(feeCost, 0,10,1,0,0, "rateObjective");
        model.setObjectiveN(divRateCost, 1, 1, 1,0,0, "div rate objective");
        model.set(GRB.IntAttr.ModelSense, GRB.MAXIMIZE);
        model.optimize();
    }

    public static void main(String[] args) {

    }


    public static List<ReturnTrade> trades() {
        return Lists.newArrayList(
                new ReturnTrade("T1", 65000, 0.4d, 70d),
                new ReturnTrade("T2", 2300, 2.5d, 80d),
                new ReturnTrade("T3", 500, 2.4d, 90d),
                new ReturnTrade("T4", 5600, 0.3d, 100d)
        );
    }
}
