//
// Generated by JTB 1.3.2 DIT@UoA patched
//

package syntaxtree;

/**
 * Grammar production:
 * f0 -> "new"
 * f1 -> "boolean"
 * f2 -> "["
 * f3 -> Expression()
 * f4 -> "]"
 */
public class BooleanArrayAllocationExpression implements Node {
   public NodeToken f0;
   public NodeToken f1;
   public NodeToken f2;
   public Expression f3;
   public NodeToken f4;

   public BooleanArrayAllocationExpression(NodeToken n0, NodeToken n1, NodeToken n2, Expression n3, NodeToken n4) {
      f0 = n0;
      f1 = n1;
      f2 = n2;
      f3 = n3;
      f4 = n4;
   }

   public BooleanArrayAllocationExpression(Expression n0) {
      f0 = new NodeToken("new");
      f1 = new NodeToken("boolean");
      f2 = new NodeToken("[");
      f3 = n0;
      f4 = new NodeToken("]");
   }

   public void accept(visitor.Visitor v) {
      v.visit(this);
   }
   public <R,A> R accept(visitor.GJVisitor<R,A> v, A argu) {
      return v.visit(this,argu);
   }
}

