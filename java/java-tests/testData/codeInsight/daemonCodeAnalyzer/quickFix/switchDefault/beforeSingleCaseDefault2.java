// "Unwrap 'switch'" "true"
class Test {
  void foo(Object obj) {
    int answer = sw<caret>itch (obj) {
      case default -> 42;
    };
  }
}