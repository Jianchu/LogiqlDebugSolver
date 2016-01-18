package generalconstraintsolver.dataflowsolver;

import org.checkerframework.javacutil.AnnotationUtils;

import generalconstraintsolver.GeneralConstrainsSolver;
import generalconstraintsolver.LatticeGenerator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;

import checkers.inference.InferenceSolution;
import checkers.inference.model.ConstantSlot;
import checkers.inference.model.Slot;
import dataflow.quals.DataFlow;
import dataflow.solver.DataflowSolution;
import dataflow.solver.DatatypeSolution;
import dataflow.util.DataflowUtils;

public abstract class DataflowGeneralSolver extends GeneralConstrainsSolver {
    
    protected AnnotationMirror DATAFLOW;
       
    @Override
    protected InferenceSolution solve(){
        Elements elements = processingEnvironment.getElementUtils();
        DATAFLOW = AnnotationUtils.fromClass(elements, DataFlow.class);
        
        Collection<String> datatypesUsed = getDatatypessUsed(slots);
        List<DataflowImpliesLogic> dataflowLogics = new ArrayList<>();


        for (String datatype : datatypesUsed) {
            Set<String> datatypeSet = new HashSet<String>();
            datatypeSet.add(datatype);
            AnnotationMirror dataflowAnnotation= DataflowUtils.createDataflowAnnotation(datatypeSet, processingEnvironment);
            LatticeGenerator lattice = new LatticeGenerator(dataflowAnnotation,processingEnvironment);
            DataflowGeneralSerializer serializer = new DataflowGeneralSerializer(
                    slotManager, lattice);
            DataflowImpliesLogic logic = new DataflowImpliesLogic(lattice);
            logic.configure(constraints, serializer);
            dataflowLogics.add(logic);
            // List<DatatypeSolution> solutions = new ArrayList<>();
            // for (DatatypeSolver solver : dataflowSolvers) {
            // solutions.add(solver.solve());
            // }

            // for (ImpliesLogic i : logic.getLogics()) {
            // System.out.println("left: " + i.leftSide + "right: "
            // + i.rightSide + "variable: " + i.variable);
            // }
            //
            // // DatatypeSatSolver solver = new DatatypeSatSolver();
            //
            // for (AnnotationMirror i : lattice.subType.keySet()) {
            // System.out.println("key: " + i.toString() + "value: "
            // + lattice.modifierInt.get(i));
            // System.out.println("top: " + lattice.top.toString()
            // + "bottom: " + lattice.bottom.toString());
            // }
            
        }
        List<DatatypeSolution> Datatypesolutions = solveImpliesLogic(dataflowLogics);
        return getMergedSolution(processingEnvironment, Datatypesolutions);
        
    }
    
    
    private Collection<String> getDatatypessUsed(Collection<Slot> solts) {
        Set<String> types = new TreeSet<>();
        for (Slot slot : solts) {
            if (slot instanceof ConstantSlot) {
                ConstantSlot constantSlot = (ConstantSlot) slot;
                AnnotationMirror anno = constantSlot.getValue();
                if (AnnotationUtils.areSameIgnoringValues(anno, DATAFLOW)) {
                    String[] dataflowValues = DataflowUtils.getDataflowValue(anno);
                    for(String dataflowValue :dataflowValues){
                        types.add(dataflowValue);
                    }
                }
            }
        }
        return types;
    }
    
    public abstract List<DatatypeSolution> solveImpliesLogic(
            List<DataflowImpliesLogic> dataflowLogics);
    
    protected InferenceSolution getMergedSolution(
            ProcessingEnvironment processingEnvironment,
            List<DatatypeSolution> solutions) {
        return new DataflowSolution(solutions, processingEnvironment);
    }

}