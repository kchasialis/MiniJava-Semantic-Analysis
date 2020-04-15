//
// Generated by JTB 1.3.2 DIT@UoA patched
//

package syntaxtree;

/**
 * Grammar production:
 * f0 -> PrimaryExpression()
 * f1 -> "."
 * f2 -> Identifier()
 * f3 -> "("
 * f4 -> ( ExpressionList() )?
 * f5 -> ")"
 */
public class MessageSend implements Node {
   public PrimaryExpression f0;
   public NodeToken f1;
   public Identifier f2;
   public NodeToken f3;
   public NodeOptional f4;
   public NodeToken f5;

   public MessageSend(PrimaryExpression n0, NodeToken n1, Identifier n2, NodeToken n3, NodeOptional n4, NodeToken n5) {
      f0 = n0;
      f1 = n1;
      f2 = n2;
      f3 = n3;
      f4 = n4;
      f5 = n5;
   }

   public MessageSend(PrimaryExpression n0, Identifier n1, NodeOptional n2) {
      f0 = n0;
      f1 = new NodeToken(".");
      f2 = n1;
      f3 = new NodeToken("(");
      f4 = n2;
      f5 = new NodeToken(")");
   }

   public void accept(visitor.Visitor v) {
      v.visit(this);
   }
   public <R,A> R accept(visitor.GJVisitor<R,A> v, A argu) {
      return v.visit(this,argu);
   }
}

