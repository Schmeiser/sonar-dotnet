/*
 * Sonar .NET Plugin :: Gendarme
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
package org.sonar.plugins.csharp.gendarme;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.rules.ActiveRule;
import org.sonar.dotnet.tools.gendarme.GendarmeCommandBuilder;
import org.sonar.dotnet.tools.gendarme.GendarmeRunner;
import org.sonar.plugins.csharp.gendarme.profiles.GendarmeProfileExporter;
import org.sonar.plugins.csharp.gendarme.results.GendarmeResultParser;
import org.sonar.plugins.dotnet.api.DotNetConfiguration;
import org.sonar.plugins.dotnet.api.DotNetConstants;
import org.sonar.plugins.dotnet.api.microsoft.MicrosoftWindowsEnvironment;
import org.sonar.plugins.dotnet.api.microsoft.VisualStudioProject;
import org.sonar.plugins.dotnet.api.microsoft.VisualStudioSolution;
import org.sonar.plugins.dotnet.api.sensor.AbstractRegularDotNetSensor;
import org.sonar.plugins.dotnet.api.utils.FileFinder;
import org.sonar.plugins.dotnet.core.DotNetCorePlugin;
import org.sonar.test.TestUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GendarmeRunner.class, FileFinder.class})
public class GendarmeSensorTest {

  private ProjectFileSystem fileSystem;
  private VisualStudioSolution solution;
  private VisualStudioProject vsProject;
  private MicrosoftWindowsEnvironment microsoftWindowsEnvironment;
  private RulesProfile rulesProfile;
  private GendarmeResultParser resultParser;
  private GendarmeProfileExporter.CSharpRegularGendarmeProfileExporter profileExporter;
  private GendarmeSensor sensor;
  private Settings conf;

  @Before
  public void init() throws Exception {

    fileSystem = mock(ProjectFileSystem.class);
    when(fileSystem.getSonarWorkingDirectory()).thenReturn(TestUtils.getResource("/Sensor"));

    vsProject = mock(VisualStudioProject.class);
    when(vsProject.getName()).thenReturn("Project #1");
    when(vsProject.getGeneratedAssemblies("Debug", "Any CPU")).thenReturn(
        Sets.newHashSet(TestUtils.getResource("/Sensor/FakeAssemblies/Fake1.assembly")));
    VisualStudioProject project2 = mock(VisualStudioProject.class);
    when(project2.getName()).thenReturn("Project Test");
    when(project2.isTest()).thenReturn(true);
    solution = mock(VisualStudioSolution.class);
    when(solution.getProjects()).thenReturn(Lists.newArrayList(vsProject, project2));

    microsoftWindowsEnvironment = new MicrosoftWindowsEnvironment();
    microsoftWindowsEnvironment.setCurrentSolution(solution);
    microsoftWindowsEnvironment.setWorkingDirectory("target");

    rulesProfile = mock(RulesProfile.class);
    when(rulesProfile.getActiveRulesByRepository(anyString()))
        .thenReturn(Collections.singletonList(new ActiveRule()));

    resultParser = mock(GendarmeResultParser.class);

    profileExporter = mock(GendarmeProfileExporter.CSharpRegularGendarmeProfileExporter.class);

    conf = new Settings(new PropertyDefinitions(new DotNetCorePlugin(), new GendarmePlugin()));

    initializeSensor();
  }

  private void initializeSensor() {
    sensor = new GendarmeSensor.CSharpRegularGendarmeSensor(
        fileSystem,
        rulesProfile,
        profileExporter,
        resultParser,
        new DotNetConfiguration(conf),
        microsoftWindowsEnvironment
        );
  }

  @Test
  public void testExecuteWithoutRule() throws Exception {
    RulesProfile rulesProfile = mock(RulesProfile.class);
    when(rulesProfile.getActiveRulesByRepository(anyString())).thenReturn(new ArrayList<ActiveRule>());
    GendarmeSensor sensor = new GendarmeSensor.CSharpRegularGendarmeSensor(null, rulesProfile, profileExporter, null, new DotNetConfiguration(conf),
        microsoftWindowsEnvironment);

    Project project = mock(Project.class);
    assertFalse(sensor.shouldExecuteOnProject(project));
  }

  @Test
  public void testLaunchGendarme() throws Exception {
    GendarmeSensor sensor = new GendarmeSensor.CSharpRegularGendarmeSensor(fileSystem, null, null, null, new DotNetConfiguration(conf),
        microsoftWindowsEnvironment);

    GendarmeRunner runner = mock(GendarmeRunner.class);
    GendarmeCommandBuilder builder = GendarmeCommandBuilder.createBuilder(solution, vsProject);
    builder.setExecutable(new File("gendarme.exe"));
    when(runner.createCommandBuilder(solution, vsProject)).thenReturn(builder);
    Project project = mock(Project.class);
    when(project.getName()).thenReturn("Project #1");

    sensor.launchGendarme(project, runner, TestUtils.getResource("/Sensor/FakeGendarmeConfigFile.xml"));
    verify(runner).execute(any(GendarmeCommandBuilder.class), eq(10));
  }

  @Test
  public void testShouldLaunchGendarme() throws Exception {
    GendarmeRunner runner = mock(GendarmeRunner.class);
    GendarmeCommandBuilder builder = GendarmeCommandBuilder.createBuilder(null, vsProject);
    builder.setExecutable(new File("Gendarme.exe"));
    when(runner.createCommandBuilder(eq(solution), any(VisualStudioProject.class))).thenReturn(builder);

    PowerMockito.mockStatic(GendarmeRunner.class);
    when(GendarmeRunner.create(anyString(), anyString())).thenReturn(runner);

    Project project = mock(Project.class);
    when(project.getName()).thenReturn("Project #1");

    SensorContext context = mock(SensorContext.class);

    sensor.analyse(project, context);

    verify(runner).execute(any(GendarmeCommandBuilder.class), eq(10));
  }

  @Test
  public void testShouldAnalyseReusedReports() throws Exception {
    conf.setProperty(GendarmeConstants.MODE, GendarmeSensor.MODE_REUSE_REPORT);
    conf.setProperty(GendarmeConstants.REPORTS_PATH_KEY, "**/*.xml");
    initializeSensor();
    GendarmeRunner runner = mock(GendarmeRunner.class);
    GendarmeCommandBuilder builder = GendarmeCommandBuilder.createBuilder(null, vsProject);
    builder.setExecutable(new File("FxCopCmd.exe"));
    when(runner.createCommandBuilder(eq(solution), any(VisualStudioProject.class))).thenReturn(builder);

    PowerMockito.mockStatic(GendarmeRunner.class);
    when(GendarmeRunner.create(anyString(), anyString())).thenReturn(runner);

    File fakeReport = TestUtils.getResource("/Sensor/FakeGendarmeConfigFile.xml");
    PowerMockito.mockStatic(FileFinder.class);
    when(FileFinder.findFiles(solution, vsProject, "**/*.xml"))
        .thenReturn(Lists.newArrayList(fakeReport, fakeReport));

    Project project = mock(Project.class);
    when(project.getName()).thenReturn("Project #1");

    SensorContext context = mock(SensorContext.class);

    sensor.analyse(project, context);

    verify(resultParser, times(2)).parse(any(File.class));
  }

  @Test
  public void testShouldReuseReports() throws Exception {

    Project project = mock(Project.class);
    when(project.getName()).thenReturn("Project #1");
    when(project.getLanguageKey()).thenReturn("cs");
    conf.setProperty(GendarmeConstants.MODE, GendarmeSensor.MODE_REUSE_REPORT);
    initializeSensor();
    assertTrue(sensor.shouldExecuteOnProject(project));
  }

  @Test
  public void testShouldExecuteOnProject() throws Exception {
    GendarmeSensor sensor = new GendarmeSensor.CSharpRegularGendarmeSensor(null, rulesProfile, profileExporter, null, new DotNetConfiguration(conf), microsoftWindowsEnvironment);

    Project project = mock(Project.class);
    when(project.getName()).thenReturn("Project #1");
    when(project.getLanguageKey()).thenReturn("cs");
    assertTrue(sensor.shouldExecuteOnProject(project));

    conf.setProperty(GendarmeConstants.MODE, AbstractRegularDotNetSensor.MODE_SKIP);
    sensor = new GendarmeSensor.CSharpRegularGendarmeSensor(null, rulesProfile, profileExporter, null, new DotNetConfiguration(conf), microsoftWindowsEnvironment);
    assertFalse(sensor.shouldExecuteOnProject(project));
  }

  @Test
  public void testShouldSkipProject() throws Exception {

    Project project = mock(Project.class);
    when(project.getName()).thenReturn("Project #1");
    when(project.getLanguageKey()).thenReturn("cs");
    conf.setProperty(GendarmeConstants.MODE, GendarmeSensor.MODE_SKIP);
    initializeSensor();
    assertFalse(sensor.shouldExecuteOnProject(project));
  }

  @Test
  public void testShouldNotExecuteOnTestProject() throws Exception {

    Project project = mock(Project.class);
    when(project.getName()).thenReturn("Project Test");
    when(project.getLanguageKey()).thenReturn("cs");
    assertFalse(sensor.shouldExecuteOnProject(project));
  }

  @Test
  public void testShouldNotExecuteOnTestProjectOnReuseMode() throws Exception {

    conf.setProperty(GendarmeConstants.MODE, GendarmeSensor.MODE_REUSE_REPORT);

    Project project = mock(Project.class);
    when(project.getName()).thenReturn("Project Test");
    when(project.getLanguageKey()).thenReturn("cs");
    assertFalse(sensor.shouldExecuteOnProject(project));
  }

  @Test
  public void testShouldNotExecuteOnProjectUsingPatterns() throws Exception {

    conf.setProperty(GendarmeConstants.ASSEMBLIES_TO_SCAN_KEY, "**/*.whatever");
    conf.setProperty(DotNetConstants.BUILD_CONFIGURATION_KEY, "DummyBuildConf"); // we simulate no generated assemblies

    when(solution.getSolutionDir()).thenReturn(TestUtils.getResource("/Sensor"));
    when(vsProject.getDirectory()).thenReturn(TestUtils.getResource("/Sensor"));

    Project project = mock(Project.class);
    when(project.getName()).thenReturn("Project #1");
    when(project.getLanguageKey()).thenReturn("cs");

    assertFalse(sensor.shouldExecuteOnProject(project));
  }

  @Test
  public void testGenerateConfigurationFile() throws Exception {
    File sonarDir = new File("target/sonar");
    sonarDir.mkdirs();
    ProjectFileSystem fileSystem = mock(ProjectFileSystem.class);
    when(fileSystem.getSonarWorkingDirectory()).thenReturn(sonarDir);
    GendarmeProfileExporter.CSharpRegularGendarmeProfileExporter profileExporter = mock(GendarmeProfileExporter.CSharpRegularGendarmeProfileExporter.class);
    doAnswer(new Answer<Object>() {

      public Object answer(InvocationOnMock invocation) throws IOException {
        FileWriter writer = (FileWriter) invocation.getArguments()[1];
        writer.write("Hello");
        return null;
      }
    }).when(profileExporter).exportProfile((RulesProfile) anyObject(), (FileWriter) anyObject());
    GendarmeSensor sensor = new GendarmeSensor.CSharpRegularGendarmeSensor(fileSystem, null, profileExporter, null, new DotNetConfiguration(conf),
        microsoftWindowsEnvironment);

    sensor.generateConfigurationFile();
    File report = new File(sonarDir, GendarmeConstants.GENDARME_RULES_FILE);
    assertTrue(report.exists());
    report.delete();
  }

}
