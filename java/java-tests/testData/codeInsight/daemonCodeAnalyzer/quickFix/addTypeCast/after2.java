// "Cast to 'b'" "true-preview"
class a {
 void f(a a) {
   <caret>b b = (b) a;
 }
}
class b extends a {}
