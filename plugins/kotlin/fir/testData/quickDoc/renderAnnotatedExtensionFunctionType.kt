fun myFun<caret>(param: @MyAnnotation (String.() -> Unit)) {} // quick documentation myFun

@Target(AnnotationTarget.TYPE)
annotation class MyAnnotation

//INFO: <div class='definition'><pre>fun myFun(param: @MyAnnotation String.() -&gt; Unit): Unit</pre></div></pre></div><table class='sections'><p></table><div class='bottom'><icon src="file"/>&nbsp;renderAnnotatedExtensionFunctionType.kt<br/></div>
