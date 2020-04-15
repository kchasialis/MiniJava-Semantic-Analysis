//
// Generated by JTB 1.3.2 DIT@UoA patched
//

package syntaxtree;

/**
 * Grammar production:
 * f0 -> "boolean"
 * f1 -> "["
 * f2 -> "]"
 */
public class BooleanArrayType implements Node {
   public NodeToken f0;
   public NodeToken f1;
   public NodeToken f2;

   public BooleanArrayType(NodeToken n0, NodeToken n1, NodeToken n2) {
      f0 = n0;
      f1 = n1;
      f2 = n2;
   }

   public BooleanArrayType() {
      f0 = new NodeToken("boolean");
      f1 = new NodeToken("[");
      f2 = new NodeToken("]");
   }

   public void accept(visitor.Visitor v) {
      v.visit(this);
   }
   public <R,A> R accept(visitor.GJVisitor<R,A> v, A argu) {
      return v.visit(this,argu);
   }
}

