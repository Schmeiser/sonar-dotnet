/*
 * Sonar C# Plugin :: C# Squid :: Squid
 * Copyright (C) 2010 Jose Chillan, Alexandre Victoor and SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package com.sonar.csharp.squid.parser.rules.statements;

import com.sonar.csharp.squid.parser.CSharpGrammar;
import com.sonar.csharp.squid.parser.RuleTest;
import org.junit.Before;
import org.junit.Test;

import static org.sonar.sslr.tests.Assertions.assertThat;

public class ForStatementTest extends RuleTest {

  @Before
  public void init() {
    p.setRootRule(p.getGrammar().rule(CSharpGrammar.FOR_STATEMENT));
  }

  @Test
  public void ok() {
    p.getGrammar().rule(CSharpGrammar.FOR_INITIALIZER).override("forInitializer");
    p.getGrammar().rule(CSharpGrammar.FOR_CONDITION).override("forCondition");
    p.getGrammar().rule(CSharpGrammar.FOR_ITERATOR).override("forIterator");
    p.getGrammar().rule(CSharpGrammar.EMBEDDED_STATEMENT).override("embeddedStatement");

    assertThat(p)
        .matches("for ( ;; ) embeddedStatement")
        .matches("for ( forInitializer;forCondition;forIterator ) embeddedStatement")
        .matches("for ( forInitializer;;forIterator ) embeddedStatement")
        .matches("for ( ;forCondition; ) embeddedStatement");
  }

  @Test
  public void reallife() {
    assertThat(p)
        .matches("for( int num = count;\n num > 0;\n --num)\n {}")
        .matches("for( int num = count;\n num > 0;\n --num)\n  { myClass.sayHello(); } ");
  }

}
