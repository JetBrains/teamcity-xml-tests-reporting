/*
 * Copyright 2000-2007 JetBrains s.r.o.
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

package jetbrains.buildServer.testReportParserPlugin.visual;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class TestGenerator {
    public static final long TEST_DIR_NUMBER = 4;
    public static final long TEST_CASE_NUMBER = 1;
    public static final long TEST_NUMBER = 30;

    public static void main(String[] args) {
        for (int n = 0; n < TEST_DIR_NUMBER; ++n) {
            for (int i = 0; i < TEST_CASE_NUMBER; ++i) {
                String testCaseName = "TestClass" + i + "_" + n;
                File outFile = new File("out\\test\\TestReportParserPluginTests\\" + testCaseName + ".java");
                FileWriter out = null;
                try {
                    out = new FileWriter(outFile);

                    //                out.write("package jetbrains.buildServer.agent.testReportParserPlugin.visual;\n");
                    out.write("import static org.junit.Assert.assertTrue;\n");
                    out.write("import org.junit.Test;\n\n");

                    out.write("public class " + testCaseName + " {\n");

                    for (int j = 0; j < TEST_NUMBER; ++j) {
                        out.write(" @Test\n");
                        out.write(" public void test" + j + "() {\n");
                        boolean flag;
                        if (j % 2 == 0) {
                            flag = true;
                        } else {
                            flag = false;
                        }
                        out.write(" assertTrue(\"Assertion message form test\"," + flag + ");\n");
                        out.write(" }\n\n");
                    }

                    out.write("}");
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
