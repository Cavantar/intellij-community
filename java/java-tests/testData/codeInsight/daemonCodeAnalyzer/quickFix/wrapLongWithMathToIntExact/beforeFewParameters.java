// "Adapt 2nd argument using 'Math.toIntExact()'" "true"
public class Test {

  void longMethod(int k, int thisIsInt) {

  }

  void m(long ll) {
    longMethod(13, l<caret>l);
  }

}
