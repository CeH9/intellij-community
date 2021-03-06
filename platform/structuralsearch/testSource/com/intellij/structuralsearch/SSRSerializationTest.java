// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.structuralsearch;

import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.codeInspection.ex.InspectionToolsSupplier;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.profile.codeInspection.BaseInspectionProfileManager;
import com.intellij.profile.codeInspection.InspectionProfileManager;
import com.intellij.structuralsearch.inspection.highlightTemplate.SSBasedInspection;
import com.intellij.structuralsearch.plugin.ui.Configuration;
import com.intellij.structuralsearch.plugin.ui.SearchConfiguration;
import com.intellij.testFramework.LightPlatformTestCase;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * @author Bas Leijdekkers
 */
public class SSRSerializationTest extends LightPlatformTestCase {

  private InspectionProfileImpl myProfile;
  private SSBasedInspection myInspection;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    InspectionProfileImpl.INIT_INSPECTIONS = true;
    myInspection = new SSBasedInspection();
    final SearchConfiguration configuration1 = new SearchConfiguration("i", "user defined");
    final MatchOptions options = configuration1.getMatchOptions();
    options.setFileType(StdFileTypes.JAVA);
    options.setSearchPattern("int i;");
    myInspection.setConfigurations(Arrays.asList(configuration1));
    final InspectionToolsSupplier supplier = new InspectionToolsSupplier() {

      @Override
      public @NotNull List<InspectionToolWrapper> createTools() {
        return Arrays.asList(new LocalInspectionToolWrapper(myInspection));
      }
    };
    myProfile = new InspectionProfileImpl("test", supplier, (BaseInspectionProfileManager)InspectionProfileManager.getInstance());
    myProfile.enableTool(SSBasedInspection.SHORT_NAME, getProject());
    myProfile.lockProfile(true);
    myProfile.initInspectionTools(getProject());
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      InspectionProfileImpl.INIT_INSPECTIONS = false;
      myProfile.getProfileManager().deleteProfile(myProfile);
    }
    catch (Throwable e) {
      addSuppressedException(e);
    }
    finally {
      super.tearDown();
    }
  }

  private String buildXmlFromProfile() {
    final Element node = new Element("profile");
    myProfile.writeExternal(node);
    return JDOMUtil.writeElement(node);
  }

  public void testSimple() {
    final InspectionToolWrapper[] tools = myProfile.getInspectionTools(null);
    assertEquals("SSBasedInspection and 1 child tool should be available", 2, tools.length);
  }

  public void testDefaultToolsNotWritten() {
    final String expected = "<profile version=\"1.0\" is_locked=\"true\">\n" +
                            "  <option name=\"myName\" value=\"test\" />\n" +
                            "  <inspection_tool class=\"SSBasedInspection\" enabled=\"true\" level=\"WARNING\" enabled_by_default=\"true\">\n" +
                            "    <searchConfiguration name=\"i\" text=\"int i;\" recursive=\"false\" caseInsensitive=\"false\" type=\"JAVA\" />\n" +
                            "  </inspection_tool>\n" +
                            "</profile>";
    assertEquals(expected, buildXmlFromProfile());
  }

  public void testModifiedToolShouldBeWritten() {
    final Configuration configuration = myInspection.getConfigurations().get(0);
    myProfile.setToolEnabled(configuration.getUuid().toString(), false);

    final String expected =
      "<profile version=\"1.0\" is_locked=\"true\">\n" +
      "  <option name=\"myName\" value=\"test\" />\n" +
      "  <inspection_tool class=\"865c0c0b-4ab0-3063-a5ca-a3387c1a8741\" enabled=\"false\" level=\"WARNING\" enabled_by_default=\"false\" />\n" +
      "  <inspection_tool class=\"SSBasedInspection\" enabled=\"true\" level=\"WARNING\" enabled_by_default=\"true\">\n" +
      "    <searchConfiguration name=\"i\" text=\"int i;\" recursive=\"false\" caseInsensitive=\"false\" type=\"JAVA\" />\n" +
      "  </inspection_tool>\n" +
      "</profile>";
    assertEquals(expected, buildXmlFromProfile());
  }

  public void testWriteUuidWhenNameChanged() {
    final Configuration configuration = myInspection.getConfigurations().get(0);
    configuration.setName("j");

    final String expected =
      "<profile version=\"1.0\" is_locked=\"true\">\n" +
      "  <option name=\"myName\" value=\"test\" />\n" +
      "  <inspection_tool class=\"SSBasedInspection\" enabled=\"true\" level=\"WARNING\" enabled_by_default=\"true\">\n" +
      "    <searchConfiguration name=\"j\" uuid=\"865c0c0b-4ab0-3063-a5ca-a3387c1a8741\" text=\"int i;\" recursive=\"false\" caseInsensitive=\"false\" type=\"JAVA\" />\n" +
      "  </inspection_tool>\n" +
      "</profile>";
    assertEquals(expected, buildXmlFromProfile());
  }
}
