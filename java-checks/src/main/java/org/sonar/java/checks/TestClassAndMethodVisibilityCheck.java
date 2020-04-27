/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.checks;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.UnitTestUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5786")
public class TestClassAndMethodVisibilityCheck extends IssuableSubscriptionVisitor {

  private static final List<Modifier> NON_COMPLIANT_MODIFIERS = Arrays.asList(Modifier.PUBLIC, Modifier.PRIVATE, Modifier.PROTECTED);

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }

    ClassTree classTree = (ClassTree) tree;
    List<MethodTree> testMethods = classTree.members().stream()
      .filter(member -> member.is(Tree.Kind.METHOD))
      .map(MethodTree.class::cast)
      .filter(UnitTestUtils::hasJUnit5TestAnnotation)
      .collect(Collectors.toList());

    testMethods.stream().map(MethodTree::modifiers).forEach(this::raiseIssueOnPresentAccessModifiers);

    if (!testMethods.isEmpty()) {
      raiseIssueOnPresentAccessModifiers(classTree.modifiers());
    }
  }

  private void raiseIssueOnPresentAccessModifiers(ModifiersTree modifierTree) {
    modifierTree.modifiers().stream()
      .filter(modifierKeyword -> NON_COMPLIANT_MODIFIERS.contains(modifierKeyword.modifier()))
      .map(modifierKeywordTree -> (Tree) modifierKeywordTree)
      .findFirst()
      .ifPresent(modifierKeyword -> reportIssue(modifierKeyword, "Remove this access modifier"));
  }
}
