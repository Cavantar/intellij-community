// "Cast to 'B'" "true-preview"
class A {
 void f(B b) {
   B s = b == null ? <caret>this : b;
 }
}
class B extends A {}