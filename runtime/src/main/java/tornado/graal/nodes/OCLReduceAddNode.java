package tornado.graal.nodes;

import org.graalvm.compiler.graph.NodeClass;
import org.graalvm.compiler.lir.gen.ArithmeticLIRGeneratorTool;
import org.graalvm.compiler.nodeinfo.NodeInfo;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.calc.AddNode;
import org.graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

import jdk.vm.ci.meta.Value;


@NodeInfo(shortName = "REDUCE(+)")
public class OCLReduceAddNode extends AddNode {

	public static final NodeClass<OCLReduceAddNode> TYPE = NodeClass.create(OCLReduceAddNode.class);
    	
	public OCLReduceAddNode(ValueNode x, ValueNode y) {
		super(TYPE, x, y);
	}
    
	@Override
    public void generate(NodeLIRBuilderTool tool, ArithmeticLIRGeneratorTool gen) {
		
        Value op1 = tool.operand(getX());
        assert op1 != null : getX() + ", this=" + this;
        Value op2 = tool.operand(getY());
        
        //System.out.println(op1 + ", " + op2);
        
        if (shouldSwapInputs(tool)) {
            Value tmp = op1;
            op1 = op2;
            op2 = tmp;
        }
        Value resultAdd = gen.emitAdd(op1, op2, false);
        tool.setResult(this, resultAdd);

    }
	
	
}
