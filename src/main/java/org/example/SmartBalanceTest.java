package org.example;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.gurobi.gurobi.*;
import org.example.vo.Account;
import org.example.vo.Security;

import java.util.Map;
import java.util.stream.Collectors;

public class SmartBalanceTest {

    // set GRB_LICENSE_FILE
    // set GUROBI_HOME
    public static void main(String[] args) throws GRBException {
        var securites = Lists.newArrayList(
                new Security(1, 50, 5000),
                new Security(2, 100, 1000),
                new Security(3, 0.5, 100000)
        );

        var accounts = Lists.newArrayList(
                new Account("A", 500, 0.5),
                new Account("B", 0, 0.1),
                new Account("C", 100000, 0.7)
        );

        double maxCost = accounts.stream()
                .map(Account::getCost)
                .max(Double::compareTo)
                .get();

        GRBEnv env = new GRBEnv();
        GRBModel model = new GRBModel(env);
        model.set(GRB.StringAttr.ModelName, "smart balance");

        var securityLinExpr = securites.stream()
                .collect(Collectors.toMap(Security::getFii, s -> new GRBLinExpr()));
        Map<String, GRBLinExpr> accountBalanceExpr = Maps.newConcurrentMap();
        Map<String, GRBVar> inBalanceVars = Maps.newConcurrentMap();
        Map<String, GRBVar> outBalanceVars = Maps.newConcurrentMap();
        GRBLinExpr systemCost = new GRBLinExpr();

        accounts.forEach(account -> {
            String name = account.getName();
            GRBLinExpr balanceExpr = new GRBLinExpr();
            accountBalanceExpr.put(name, balanceExpr);
            securites.forEach(security -> {
                try {
                    GRBVar inBalanceQuantity = model.addVar(0,
                            security.getNeed(),
                            0,
                            GRB.INTEGER,
                            String.format("%s in balance: %s", name, security.getFii()));
                    GRBVar outBalanceQuantity = model.addVar(0,
                            security.getNeed(),
                            0,
                            GRB.INTEGER,
                            String.format("%s out balance: %s", name, security.getFii()));
                    // balance = price * quantity(inBalanceQuantity)
                    balanceExpr.addTerm(security.getPrice(), inBalanceQuantity);

                    // security total booked quantity
                    GRBLinExpr expr = securityLinExpr.get(security.getFii());
                    expr.addTerm(1, inBalanceQuantity);
                    expr.addTerm(1, outBalanceQuantity);

                    //system cost = inBalanceQuantity * accountCost
                    systemCost.addTerm(account.getCost(), inBalanceQuantity);

                    //system cost = outBalanceQuantity * (accountCost + maxCost)
                    systemCost.addTerm(account.getCost() + maxCost, inBalanceQuantity);
                    String key = String.format("%s-%s", security.getFii(), name);
                    inBalanceVars.put(key, inBalanceQuantity);
                    outBalanceVars.put(key, outBalanceQuantity);
                } catch (GRBException e) {
                    throw new RuntimeException(e);
                }
            });
        });

        // minimize cost
        model.setObjective(systemCost, GRB.MINIMIZE);

        // all securities needs must be fulfilled.
        securites.forEach(s -> {
            try {
                model.addConstr(securityLinExpr.get(s.getFii()), GRB.EQUAL, s.getNeed(), String.format("total borrows for %s", s.getFii()));
            } catch (GRBException e) {
                throw new RuntimeException(e);
            }
        });

        // the balance in the scope.
        accounts.forEach(a-> {
            try {
                model.addConstr(accountBalanceExpr.get(a.getName()), GRB.LESS_EQUAL, a.getBalance(), String.format("balance limited %s", a.getName()));
            } catch (GRBException e) {
                throw new RuntimeException(e);
            }
        });

        model.optimize();
        //successful
        if (model.get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL) {
            int count = model.get(GRB.IntAttr.SolCount);
            for (int i = 0; i < count; i++){
                model.set(GRB.IntParam.SolutionNumber, i + 1);
//                print()
//                Map<String, GRBVar> inBalanceVars = Maps.newConcurrentMap();
//                Map<String, GRBVar> outBalanceVars = Maps.newConcurrentMap();
            }
        }

        model.dispose();
        env.dispose();
    }

}
