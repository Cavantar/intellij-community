// "Surround with 'if (s != null)'" "true"
class A {
    void foo(){
        String s = null;
        <caret>if (s != null) {
            for (int i=0; i!=s.hashCode();i++){}
        }
    }
}