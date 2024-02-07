

package org.jetbrains.testProject;

import junit.framework.TestCase;

public class ExampleClass extends TestCase {
    public void test1() {
        System.out.println("from test1");
    }

    public void test2() {
        System.out.print("from test2");
    }

    public void test3() {
        System.err.println("from test3");
    }

    public void test4() {
        System.err.print("from test4");
    }

    public void test5() {
        throw new NullPointerException("from test5");
    }

    public void test6() {
        throw new NullPointerException("from test6");
    }

    public boolean equal(Object o) {
      return true;
    }
}