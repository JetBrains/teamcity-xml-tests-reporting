/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
