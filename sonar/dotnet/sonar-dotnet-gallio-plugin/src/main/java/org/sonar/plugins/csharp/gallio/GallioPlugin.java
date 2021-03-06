/*
 * Sonar .NET Plugin :: Gallio
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
package org.sonar.plugins.csharp.gallio;

import org.sonar.api.Extension;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarPlugin;
import org.sonar.dotnet.tools.gallio.GallioRunnerConstants;
import org.sonar.plugins.csharp.gallio.results.coverage.CoverageResultParser;
import org.sonar.plugins.csharp.gallio.results.execution.GallioResultParser;
import org.sonar.plugins.csharp.gallio.ui.GallioRubyWidget;
import org.sonar.plugins.dotnet.api.sensor.AbstractDotNetSensor;

import java.util.ArrayList;
import java.util.List;

/**
 * C# Gallio plugin class.
 */
@Properties({
  @Property(key = GallioConstants.INSTALL_FOLDER_KEY, defaultValue = GallioConstants.INSTALL_FOLDER_DEFVALUE,
    name = "Gallio install directory", description = "Absolute path of the Gallio installation folder.", global = true, project = false),
  @Property(key = GallioConstants.TIMEOUT_MINUTES_KEY, defaultValue = GallioConstants.TIMEOUT_MINUTES_DEFVALUE + "",
    name = "Gallio program timeout", description = "Maximum number of minutes before the Gallio program will be stopped.",
    global = true, project = true, type = PropertyType.INTEGER),
  @Property(key = GallioConstants.MODE, defaultValue = "", name = "Gallio activation mode",
    description = "Possible values : Default (means active), 'skip' and 'reuseReport'.",
    global = false, project = false, type = PropertyType.SINGLE_SELECT_LIST,
    options = {AbstractDotNetSensor.MODE_SKIP, AbstractDotNetSensor.MODE_REUSE_REPORT}),
  @Property(key = GallioConstants.SAFE_MODE_KEY, defaultValue = GallioConstants.SAFE_MODE_DEFVALUE + "", name = "Gallio safe mode",
    description = "When set to true, gallio is launched once per test assembly of the analysed solution. " +
      "Otherwise gallio is launched once for the whole solution", global = true, project = true,
    type = PropertyType.BOOLEAN),
  @Property(key = GallioConstants.REPORTS_PATH_KEY, defaultValue = "", name = "Name of the Gallio report files",
    description = "Path to the Gallio report file used when reuse report mode is activated. "
      + "This can be an absolute path, or a path relative to the solution base directory.", global = false, project = false),
  @Property(
    key = GallioConstants.FILTER_KEY,
    defaultValue = GallioConstants.FILTER_DEFVALUE,
    name = "Test filter",
    description = "Filter that can be used to execute only a specific test category (i.e. CategotyName:unit to consider only tests from the 'unit' category).",
    global = true, project = true),
  @Property(
    key = GallioConstants.COVERAGE_TOOL_KEY,
    defaultValue = GallioConstants.COVERAGE_TOOL_DEFVALUE,
    name = "Coverage tool",
    description = "Coverage tool used by Gallio: it currently can be 'PartCover' (default), " +
      "'OpenCover', 'NCover', 'dotCover' or 'none' (= means no coverage analysis will be done).",
    global = true, project = true,
    type = PropertyType.SINGLE_SELECT_LIST,
    options = {GallioRunnerConstants.COVERAGE_TOOL_NONE_KEY,
      GallioRunnerConstants.COVERAGE_TOOL_DOTCOVER_KEY,
      GallioRunnerConstants.COVERAGE_TOOL_NCOVER_KEY,
      GallioRunnerConstants.COVERAGE_TOOL_OPENCOVER_KEY,
      GallioRunnerConstants.COVERAGE_TOOL_PARTCOVER_KEY}),
  @Property(key = GallioConstants.PART_COVER_INSTALL_KEY, defaultValue = GallioConstants.PART_COVER_INSTALL_DEFVALUE,
    name = "PartCover install directory", description = "Absolute path of the PartCover installation folder.", global = true,
    project = false),
  @Property(key = GallioConstants.OPEN_COVER_INSTALL_KEY, defaultValue = GallioConstants.OPEN_COVER_INSTALL_DEFVALUE,
    name = "OpenCover install directory", description = "Absolute path of the OpenCover installation folder.", global = true,
    project = false),
  @Property(key = GallioConstants.DOT_COVER_INSTALL_KEY, defaultValue = GallioConstants.DOT_COVER_INSTALL_DEFVALUE,
    name = "JetBrains dotCover install directory", description = "Absolute path of the dotCover installation folder.", global = true,
    project = false),
  @Property(key = GallioConstants.REPORTS_COVERAGE_PATH_KEY, defaultValue = "", name = "Name of the Gallio coverage report files",
    description = "Path to the Gallio coverage report file used when reuse report mode is activated. "
      + "This can be an absolute path, or a path relative to the solution base directory.", global = false, project = false),
  @Property(key = GallioConstants.COVERAGE_EXCLUDES_KEY,
    name = "Coverage excludes", description = "Comma-separated list of namespaces and assemblies excluded from the code coverage. "
      + "For PartCover, the format for an exclusion is : '[assembly]namespace'. "
      + "For NCover, the format is just the name of the assemblies to exclude.", global = true, project = true),

  @Property(key = GallioConstants.IT_MODE_KEY, defaultValue = AbstractDotNetSensor.MODE_SKIP,
    name = "Gallio integration tests activation mode", description = "Possible values : Default (means 'skip'), " +
      "'active', 'skip' and 'reuseReport'.",
    global = false, project = false, type = PropertyType.SINGLE_SELECT_LIST,
    options = {GallioConstants.IT_MODE_ACTIVE, AbstractDotNetSensor.MODE_SKIP, AbstractDotNetSensor.MODE_REUSE_REPORT}),
  @Property(key = GallioConstants.IT_REPORTS_COVERAGE_PATH_KEY, defaultValue = "", name = "Name of the IT Gallio coverage report files",
    description = "Path to the Gallio coverage report file used when reuse report mode is activated for integration tests. "
      + "This can be an absolute path, or a path relative to the solution base directory.", global = false, project = false),
  @Property(key = GallioConstants.IT_REPORTS_PATH_KEY, defaultValue = "", name = "Name of the IT Gallio report files",
    description = "Path to the Gallio report file used when reuse report mode is activated for integration tests. "
      + "This can be an absolute path, or a path relative to the solution base directory.", global = false, project = false),
  @Property(
    key = GallioConstants.IT_FILTER_KEY,
    defaultValue = GallioConstants.IT_FILTER_DEFVALUE,
    name = "Integration test filter",
    description = "Filter that can be used to execute only a specific integration test category " +
      "(i.e. CategotyName:integ to consider only tests from the 'integ' category).",
    global = true, project = true)

})
public class GallioPlugin extends SonarPlugin {

  /**
   * {@inheritDoc}
   */
  public List<Class<? extends Extension>> getExtensions() {
    List<Class<? extends Extension>> extensions = new ArrayList<Class<? extends Extension>>();
    extensions.add(TestMetrics.class);

    // Parser(s)
    extensions.add(CoverageResultParser.class);
    extensions.add(GallioResultParser.class);

    // Sensors
    extensions.add(GallioSensor.class);
    extensions.add(TestReportSensor.class);
    extensions.add(CoverageReportSensor.class);

    // Decorators
    extensions.add(UnitCoverageDecorator.class);
    extensions.add(ItCoverageDecorator.class);

    // Widget
    extensions.add(GallioRubyWidget.class);

    return extensions;
  }
}
