/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.launch.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.externaltools.internal.model.IExternalToolConstants;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.Messages;
import org.maven.ide.eclipse.embedder.MavenRuntime;
import org.maven.ide.eclipse.embedder.MavenRuntimeManager;
import org.maven.ide.eclipse.internal.launch.MavenLaunchConstants;
import org.maven.ide.eclipse.util.ITraceable;
import org.maven.ide.eclipse.util.Tracer;
import org.maven.ide.eclipse.util.Util;
import org.maven.ide.eclipse.wizards.MavenPropertyDialog;


/**
 * Maven Launch dialog Main tab 
 * 
 * @author Dmitri Maximovich
 * @author Eugene Kuleshov
 */
@SuppressWarnings("restriction")
public class MavenLaunchMainTab extends AbstractLaunchConfigurationTab implements MavenLaunchConstants, ITraceable {

  private static final boolean TRACE_ENABLED = Boolean.valueOf(Platform.getDebugOption("org.maven.ide.eclipse/launcher")).booleanValue();

  public static final String ID_EXTERNAL_TOOLS_LAUNCH_GROUP = "org.eclipse.ui.externaltools.launchGroup"; //$NON-NLS-1$

  private final boolean isBuilder;
  
  protected Text pomDirNameText;

  protected Text goalsText;
  protected Text goalsAutoBuildText;
  protected Text goalsManualBuildText;
  protected Text goalsCleanText;
  protected Text goalsAfterCleanText;
  
  protected Text profilesText;
  protected Table propsTable;

  private Button offlineButton;

  private Button updateSnapshotsButton;

  private Button debugOutputButton;

  private Button skipTestsButton;
  
  private Button enableWorkspaceResolution;

  ComboViewer runtimeComboViewer;

  public MavenLaunchMainTab(boolean isBuilder) {
    this.isBuilder = isBuilder;
  }

  public boolean isTraceEnabled() {
    return TRACE_ENABLED;
  }

  public Image getImage() {
    return MavenPlugin.getImage("icons/main_tab.gif");
  }

  public void createControl(Composite parent) {
    Composite mainComposite = new Composite(parent, SWT.NONE);
    setControl(mainComposite);
    //PlatformUI.getWorkbench().getHelpSystem().setHelp(mainComposite, IAntUIHelpContextIds.ANT_MAIN_TAB);
    GridLayout layout = new GridLayout();
    layout.numColumns = 4;
    GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
    mainComposite.setLayout(layout);
    mainComposite.setLayoutData(gridData);
    mainComposite.setFont(parent.getFont());

    class Listener implements ModifyListener, SelectionListener {
      public void modifyText(ModifyEvent e) {
        entriesChanged();
      }

      public void widgetDefaultSelected(SelectionEvent e) {
        entriesChanged();
      }

      public void widgetSelected(SelectionEvent e) {
        entriesChanged();
      }
    }
    Listener modyfyingListener = new Listener();
    
    // pom file 
    final Group pomGroup = new Group(mainComposite, SWT.NONE);
    pomGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
    pomGroup.setText(Messages.getString("launch.pomGroup")); //$NON-NLS-1$
    pomGroup.setLayout(new GridLayout());

    this.pomDirNameText = new Text(pomGroup, SWT.BORDER);
    this.pomDirNameText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
    this.pomDirNameText.addModifyListener(modyfyingListener);

    final Composite pomDirButtonsComposite = new Composite(pomGroup, SWT.NONE);
    pomDirButtonsComposite.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
    final GridLayout pomDirButtonsGridLayout = new GridLayout();
    pomDirButtonsGridLayout.marginWidth = 0;
    pomDirButtonsGridLayout.marginHeight = 0;
    pomDirButtonsGridLayout.numColumns = 3;
    pomDirButtonsComposite.setLayout(pomDirButtonsGridLayout);

    final Button browseWorkspaceButton = new Button(pomDirButtonsComposite, SWT.NONE);
    browseWorkspaceButton.setText(Messages.getString("launch.browseWorkspace")); //$NON-NLS-1$
    browseWorkspaceButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(), ResourcesPlugin.getWorkspace()
            .getRoot(), false, Messages.getString("launch.choosePomDir")); //$NON-NLS-1$
        dialog.showClosedProjects(false);
        
        int buttonId = dialog.open();
        if(buttonId == IDialogConstants.OK_ID) {
          Object[] resource = dialog.getResult();
          if(resource != null && resource.length > 0) {
            String fileLoc = VariablesPlugin.getDefault().getStringVariableManager().generateVariableExpression(
                "workspace_loc", ((IPath) resource[0]).toString()); //$NON-NLS-1$
            pomDirNameText.setText(fileLoc);
            entriesChanged();
          }
        }
      }
    });

    final Button browseFilesystemButton = new Button(pomDirButtonsComposite, SWT.NONE);
    browseFilesystemButton.setText(Messages.getString("launch.browseFs")); //$NON-NLS-1$
    browseFilesystemButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.NONE);
        dialog.setFilterPath(pomDirNameText.getText());
        String text = dialog.open();
        if(text != null) {
          pomDirNameText.setText(text);
          entriesChanged();
        }
      }
    });

    final Button browseVariablesButton = new Button(pomDirButtonsComposite, SWT.NONE);
    browseVariablesButton.setText(Messages.getString("launch.browseVariables")); //$NON-NLS-1$
    browseVariablesButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(getShell());
        dialog.open();
        String variable = dialog.getVariableExpression();
        if(variable != null) {
          pomDirNameText.insert(variable);
        }
      }
    });

    // goals
    
    if(isBuilder) {
      Label autoBuildGoalsLabel = new Label(mainComposite, SWT.NONE);
      autoBuildGoalsLabel.setText("Auto &Build Goals:");
      goalsAutoBuildText = new Text(mainComposite, SWT.BORDER);
      goalsAutoBuildText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
      goalsAutoBuildText.addModifyListener(modyfyingListener);
      goalsAutoBuildText.addFocusListener(new GoalsFocusListener(goalsAutoBuildText));
      Button goalsAutoBuildButton = new Button(mainComposite, SWT.NONE);
      goalsAutoBuildButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
      goalsAutoBuildButton.setText("&Select...");
      goalsAutoBuildButton.addSelectionListener(new GoalSelectionAdapter(goalsAutoBuildText));

      Label manualBuildGoalsLabel = new Label(mainComposite, SWT.NONE);
      manualBuildGoalsLabel.setText("Ma&nual Build Goals:");
      goalsManualBuildText = new Text(mainComposite, SWT.BORDER);
      goalsManualBuildText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
      goalsManualBuildText.addModifyListener(modyfyingListener);
      goalsManualBuildText.addFocusListener(new GoalsFocusListener(goalsManualBuildText));
      Button goalsManualBuildButton = new Button(mainComposite, SWT.NONE);
      goalsManualBuildButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
      goalsManualBuildButton.setText("S&elect...");
      goalsManualBuildButton.addSelectionListener(new GoalSelectionAdapter(goalsManualBuildText));
      
      Label cleanBuildGoalsLabel = new Label(mainComposite, SWT.NONE);
      cleanBuildGoalsLabel.setText("&During a Clean Goals:");
      goalsCleanText = new Text(mainComposite, SWT.BORDER);
      goalsCleanText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
      goalsCleanText.addModifyListener(modyfyingListener);
      goalsCleanText.addFocusListener(new GoalsFocusListener(goalsCleanText));
      Button goalsCleanButton = new Button(mainComposite, SWT.NONE);
      goalsCleanButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
      goalsCleanButton.setText("Se&lect...");
      goalsCleanButton.addSelectionListener(new GoalSelectionAdapter(goalsCleanText));
      
      Label afterCleanGoalsLabel = new Label(mainComposite, SWT.NONE);
      afterCleanGoalsLabel.setText("A&fter a Clean Goals:");
      goalsAfterCleanText = new Text(mainComposite, SWT.BORDER);
      goalsAfterCleanText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
      goalsAfterCleanText.addModifyListener(modyfyingListener);
      goalsAfterCleanText.addFocusListener(new GoalsFocusListener(goalsAfterCleanText));
      Button goalsAfterCleanButton = new Button(mainComposite, SWT.NONE);
      goalsAfterCleanButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
      goalsAfterCleanButton.setText("Selec&t...");
      goalsAfterCleanButton.addSelectionListener(new GoalSelectionAdapter(goalsAfterCleanText));
      
    } else {
      Label goalsLabel = new Label(mainComposite, SWT.NONE);
      goalsLabel.setText(Messages.getString("launch.goalsLabel")); //$NON-NLS-1$
      goalsText = new Text(mainComposite, SWT.BORDER);
      goalsText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
      goalsText.addModifyListener(modyfyingListener);
      goalsText.addFocusListener(new GoalsFocusListener(goalsText));

      Button selectGoalsButton = new Button(mainComposite, SWT.NONE);
      selectGoalsButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
      selectGoalsButton.setText(Messages.getString("launch.goals")); //$NON-NLS-1$
      selectGoalsButton.addSelectionListener(new GoalSelectionAdapter(goalsText));
    }

    Label profilesLabel = new Label(mainComposite, SWT.NONE);
    profilesLabel.setText(Messages.getString("launch.profilesLabel")); //$NON-NLS-1$
    // profilesLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

    profilesText = new Text(mainComposite, SWT.BORDER);
    profilesText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
    profilesText.addModifyListener(modyfyingListener);
    new Label(mainComposite, SWT.NONE);

    offlineButton = new Button(mainComposite, SWT.CHECK);
    GridData gd_offlineButton = new GridData();
    offlineButton.setLayoutData(gd_offlineButton);
    offlineButton.setText("&Offline");
    offlineButton.addSelectionListener(modyfyingListener);

    updateSnapshotsButton = new Button(mainComposite, SWT.CHECK);
    updateSnapshotsButton.addSelectionListener(modyfyingListener);
    GridData gd_updateSnapshotsButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
    gd_updateSnapshotsButton.horizontalIndent = 10;
    updateSnapshotsButton.setLayoutData(gd_updateSnapshotsButton);
    updateSnapshotsButton.setText("&Update Snapshots");
    new Label(mainComposite, SWT.NONE);

    debugOutputButton = new Button(mainComposite, SWT.CHECK);
    debugOutputButton.addSelectionListener(modyfyingListener);
    debugOutputButton.setLayoutData(new GridData());
    debugOutputButton.setText("Debu&g Output");

    skipTestsButton = new Button(mainComposite, SWT.CHECK);
    skipTestsButton.addSelectionListener(modyfyingListener);
    GridData gd_skipTestsButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
    gd_skipTestsButton.horizontalIndent = 10;
    skipTestsButton.setLayoutData(gd_skipTestsButton);
    skipTestsButton.setText("S&kip Tests");
    new Label(mainComposite, SWT.NONE);

    enableWorkspaceResolution = new Button(mainComposite, SWT.CHECK);
    enableWorkspaceResolution.addSelectionListener(modyfyingListener);
    enableWorkspaceResolution.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
    enableWorkspaceResolution.setText("Resolve Workspace artifacts");

    TableViewer tableViewer = new TableViewer(mainComposite, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
    tableViewer.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        TableItem[] selection = propsTable.getSelection();
        if(selection.length == 1) {
          editProperty(selection[0].getText(0), selection[0].getText(1));
        }
      }
    });
    
    this.propsTable = tableViewer.getTable();
    //this.tProps.setItemCount(10);
    this.propsTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 2));
    this.propsTable.setLinesVisible(true);
    this.propsTable.setHeaderVisible(true);

    final TableColumn propColumn = new TableColumn(this.propsTable, SWT.NONE, 0);
    propColumn.setWidth(120);
    propColumn.setText(Messages.getString("launch.propName")); //$NON-NLS-1$

    final TableColumn valueColumn = new TableColumn(this.propsTable, SWT.NONE, 1);
    valueColumn.setWidth(200);
    valueColumn.setText(Messages.getString("launch.propValue")); //$NON-NLS-1$

    final Button addPropButton = new Button(mainComposite, SWT.NONE);
    addPropButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    addPropButton.setText(Messages.getString("launch.propAddButton")); //$NON-NLS-1$
    addPropButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        addProperty();
      }
    });

    final Button removePropButton = new Button(mainComposite, SWT.NONE);
    removePropButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    removePropButton.setText(Messages.getString("launch.propRemoveButton")); //$NON-NLS-1$
    removePropButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if(propsTable.getSelectionCount() > 0) {
          propsTable.remove(propsTable.getSelectionIndices());
          entriesChanged();
        }
      }
    });

    {
      Composite composite = new Composite(mainComposite, SWT.NONE);
      composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));
      GridLayout gridLayout = new GridLayout(2, false);
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      composite.setLayout(gridLayout);

      Label mavenRuntimeLabel = new Label(composite, SWT.NONE);
      mavenRuntimeLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
      mavenRuntimeLabel.setText("Maven Runt&ime:");

      runtimeComboViewer = new ComboViewer(composite, SWT.BORDER | SWT.READ_ONLY);
      runtimeComboViewer.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      runtimeComboViewer.setContentProvider(new IStructuredContentProvider() {

        public Object[] getElements(Object input) {
          return ((List<?>) input).toArray();
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        }

        public void dispose() {
        }

      });

      runtimeComboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
        public void selectionChanged(SelectionChangedEvent event) {
          entriesChanged();
        }
      });
      
      MavenRuntimeManager runtimeManager = MavenPlugin.getDefault().getMavenRuntimeManager();
      runtimeComboViewer.setInput(runtimeManager.getMavenRuntimes());
      runtimeComboViewer.setSelection(new StructuredSelection(runtimeManager.getDefaultRuntime()));
    }
    
    Button configureRuntimesButton = new Button(mainComposite, SWT.NONE);
    GridData gd_configureRuntimesButton = new GridData(SWT.FILL, SWT.CENTER, false, false);
    configureRuntimesButton.setLayoutData(gd_configureRuntimesButton);
    configureRuntimesButton.setText("Configure...");
    configureRuntimesButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        PreferencesUtil.createPreferenceDialogOn(getShell(),
            "org.maven.ide.eclipse.preferences.MavenInstallationsPreferencePage", null, null).open(); //$NON-NLS-1$
        MavenRuntimeManager runtimeManager = MavenPlugin.getDefault().getMavenRuntimeManager();
        runtimeComboViewer.setInput(runtimeManager.getMavenRuntimes());
        runtimeComboViewer.setSelection(new StructuredSelection(runtimeManager.getDefaultRuntime()));
      }
    });
  }
  
  protected Shell getShell() {
    return super.getShell();
  }

  void addProperty() {
    MavenPropertyDialog dialog = new MavenPropertyDialog(getShell(), //
        Messages.getString("launch.propAddDialogTitle"), new String[] {}, true); //$NON-NLS-1$
    int res = dialog.open();
    if(res == IDialogConstants.OK_ID) {
      String[] result = dialog.getNameValuePair();
      TableItem item = new TableItem(propsTable, SWT.NONE);
      item.setText(0, result[0]);
      item.setText(1, result[1]);
      entriesChanged();
    }
  }

  void editProperty(String name, String value) {
    MavenPropertyDialog dialog = new MavenPropertyDialog(getShell(), //
        Messages.getString("launch.propEditDialogTitle"), new String[] {name, value}, true); //$NON-NLS-1$
    int res = dialog.open();
    if(res == IDialogConstants.OK_ID) {
      String[] result = dialog.getNameValuePair();
      TableItem[] item = propsTable.getSelection();
      // we expect only one row selected
      item[0].setText(0, result[0]);
      item[0].setText(1, result[1]);
      entriesChanged();
    }
  }

  public void initializeFrom(ILaunchConfiguration configuration) {
    String pomDirName = getAttribute(configuration, ATTR_POM_DIR, ""); //$NON-NLS-1$
    if(isBuilder && pomDirName.length()==0) {
      pomDirName = "${workspace_loc:/" + configuration.getFile().getProject().getName() + "}";
    }
    this.pomDirNameText.setText(pomDirName);
    
    if(isBuilder) {
      this.goalsAutoBuildText.setText(getAttribute(configuration, ATTR_GOALS_AUTO_BUILD, "install")); //$NON-NLS-1$
      this.goalsManualBuildText.setText(getAttribute(configuration, ATTR_GOALS_MANUAL_BUILD, "install")); //$NON-NLS-1$
      this.goalsCleanText.setText(getAttribute(configuration, ATTR_GOALS_CLEAN, "clean")); //$NON-NLS-1$
      this.goalsAfterCleanText.setText(getAttribute(configuration, ATTR_GOALS_AFTER_CLEAN, "install")); //$NON-NLS-1$
    } else {
      this.goalsText.setText(getAttribute(configuration, ATTR_GOALS, "")); //$NON-NLS-1$
    }
    
    this.profilesText.setText(getAttribute(configuration, ATTR_PROFILES, "")); //$NON-NLS-1$
    
    MavenPlugin plugin = MavenPlugin.getDefault();
    MavenRuntimeManager runtimeManager = plugin.getMavenRuntimeManager();
    
    this.offlineButton.setSelection(getAttribute(configuration, ATTR_OFFLINE, runtimeManager.isOffline()));
    this.debugOutputButton.setSelection(getAttribute(configuration, ATTR_DEBUG_OUTPUT, runtimeManager.isDebugOutput()));

    this.updateSnapshotsButton.setSelection(getAttribute(configuration, ATTR_UPDATE_SNAPSHOTS, false));
    this.skipTestsButton.setSelection(getAttribute(configuration, ATTR_SKIP_TESTS, false));
    this.enableWorkspaceResolution.setSelection(getAttribute(configuration, ATTR_WORKSPACE_RESOLUTION, false));

    String location = getAttribute(configuration, ATTR_RUNTIME, "");
    MavenRuntime runtime = runtimeManager.getRuntime(location);
    this.runtimeComboViewer.setSelection(new StructuredSelection(runtime));
    
    try {
      propsTable.removeAll();
      
      @SuppressWarnings("unchecked")
      List<String> properties = configuration.getAttribute(ATTR_PROPERTIES, Collections.EMPTY_LIST);
      for(String property : properties) {
        try {
          String[] ss = property.split("="); //$NON-NLS-1$
          TableItem item = new TableItem(propsTable, SWT.NONE);
          item.setText(0, ss[0]);
          if(ss.length > 1) {
            item.setText(1, ss[1]);
          }
        } catch(Exception e) {
          String msg = "Error parsing argument: " + property; //$NON-NLS-1$
          MavenPlugin.log(msg, e);
        }
      }
    } catch(CoreException ex) {
    }
    setDirty(false);
  }
  
  private String getAttribute(ILaunchConfiguration configuration, String name, String defaultValue) {
    try {
      return configuration.getAttribute(name, defaultValue);
    } catch(CoreException ex) {
      return defaultValue;
    }
  }

  private boolean getAttribute(ILaunchConfiguration configuration, String name, boolean defaultValue) {
    try {
      return configuration.getAttribute(name, defaultValue);
    } catch(CoreException ex) {
      return defaultValue;
    }
  }
  
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
  }

  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    configuration.setAttribute(ATTR_POM_DIR, this.pomDirNameText.getText());
    
    if(isBuilder) {
      configuration.setAttribute(ATTR_GOALS_AUTO_BUILD, goalsAutoBuildText.getText());
      configuration.setAttribute(ATTR_GOALS_MANUAL_BUILD, this.goalsManualBuildText.getText());
      configuration.setAttribute(ATTR_GOALS_CLEAN, this.goalsCleanText.getText());
      configuration.setAttribute(ATTR_GOALS_AFTER_CLEAN, this.goalsAfterCleanText.getText());
      
      StringBuffer sb = new StringBuffer();
      if(goalsAfterCleanText.getText().trim().length()>0) {
        sb.append(IExternalToolConstants.BUILD_TYPE_FULL).append(',');
      }
      if(goalsManualBuildText.getText().trim().length()>0) {
        sb.append(IExternalToolConstants.BUILD_TYPE_INCREMENTAL).append(',');
      }
      if(goalsAutoBuildText.getText().trim().length()>0) {
        sb.append(IExternalToolConstants.BUILD_TYPE_AUTO).append(',');
      }
      if(goalsCleanText.getText().trim().length()>0) {
        sb.append(IExternalToolConstants.BUILD_TYPE_CLEAN);
      }
      configuration.setAttribute(IExternalToolConstants.ATTR_RUN_BUILD_KINDS, sb.toString());
      
    } else {
      configuration.setAttribute(ATTR_GOALS, this.goalsText.getText());
    }
    
    configuration.setAttribute(ATTR_PROFILES, this.profilesText.getText());

    configuration.setAttribute(ATTR_OFFLINE, this.offlineButton.getSelection());
    configuration.setAttribute(ATTR_UPDATE_SNAPSHOTS, this.updateSnapshotsButton.getSelection());
    configuration.setAttribute(ATTR_SKIP_TESTS, this.skipTestsButton.getSelection());
    configuration.setAttribute(ATTR_WORKSPACE_RESOLUTION, this.enableWorkspaceResolution.getSelection());
    configuration.setAttribute(ATTR_DEBUG_OUTPUT, this.debugOutputButton.getSelection());

    String selectedRuntimeLocation = null;
    try {
      selectedRuntimeLocation = configuration.getAttribute(ATTR_RUNTIME, (String) null);
    } catch(CoreException ex) {
      // ignore
    }
    if (!MavenRuntimeManager.WORKSPACE.equals(selectedRuntimeLocation)) {
      // don't reset WORKSPACE runtime
      IStructuredSelection selection = (IStructuredSelection) runtimeComboViewer.getSelection();
      MavenRuntime runtime = (MavenRuntime) selection.getFirstElement();
      configuration.setAttribute(ATTR_RUNTIME, runtime.getLocation());
    }

    // store as String in "param=value" format 
    List<String> properties = new ArrayList<String>();
    for(TableItem item : this.propsTable.getItems()) {
      String p = item.getText(0);
      String v = item.getText(1);
      if(p != null && p.trim().length() > 0) {
        String prop = p.trim() + "=" + (v == null ? "" : v); //$NON-NLS-1$ //$NON-NLS-2$
        properties.add(prop);
        Tracer.trace(this, "property", prop);
      }
    }
    configuration.setAttribute(ATTR_PROPERTIES, properties);
  }

  public String getName() {
    return Messages.getString("launch.mainTabName"); //$NON-NLS-1$
  }

  public boolean isValid(ILaunchConfiguration launchConfig) {
    setErrorMessage(null);

    String pomFileName = this.pomDirNameText.getText();
    if(pomFileName == null || pomFileName.trim().length() == 0) {
      setErrorMessage(Messages.getString("launch.pomDirectoryEmpty"));
      return false;
    }
    if(!isDirectoryExist(pomFileName)) {
      setErrorMessage(Messages.getString("launch.pomDirectoryDoesntExist"));
      return false;
    }
    return true;
  }

  protected boolean isDirectoryExist(String name) {
    if(name == null || name.trim().length() == 0) {
      return false;
    }
    String dirName = Util.substituteVar(name);
    if(dirName == null) {
      return false;
    }
    File pomDir = new File(dirName);
    if(!pomDir.exists()) {
      return false;
    }
    if(!pomDir.isDirectory()) {
      return false;
    }
    return true;
  }

  void entriesChanged() {
    setDirty(true);
    updateLaunchConfigurationDialog();
  }

  
  private static final class GoalsFocusListener extends FocusAdapter {
    private Text text;

    public GoalsFocusListener(Text text) {
      this.text = text;
    }
    
    public void focusGained(FocusEvent e) {
      super.focusGained(e);
      text.setData("focus");
    }
  }


  private final class GoalSelectionAdapter extends SelectionAdapter {
    private Text text;

    public GoalSelectionAdapter(Text text) {
      this.text = text;
    }

    public void widgetSelected(SelectionEvent e) {
//        String fileName = Util.substituteVar(fPomDirName.getText());
//        if(!isDirectoryExist(fileName)) {
//          MessageDialog.openError(getShell(), Messages.getString("launch.errorPomMissing"), 
//              Messages.getString("launch.errorSelectPom")); //$NON-NLS-1$ //$NON-NLS-2$
//          return;
//        }
      MavenGoalSelectionDialog dialog = new MavenGoalSelectionDialog(getShell());
      int rc = dialog.open();
      if(rc == IDialogConstants.OK_ID) {
        text.insert("");  // clear selected text
        
        String txt = text.getText();
        int len = txt.length();
        int pos = text.getCaretPosition();
        
        StringBuffer sb = new StringBuffer();
        if((pos > 0 && txt.charAt(pos - 1) != ' ')) {
          sb.append(' ');
        }

        String sep = "";
        Object[] o = dialog.getResult();
        for(int i = 0; i < o.length; i++ ) {
          if(o[i] instanceof MavenGoalSelectionDialog.Entry) {
            if(dialog.isQualifiedName()) {
              sb.append(sep).append(((MavenGoalSelectionDialog.Entry) o[i]).getQualifiedName());
            } else {
              sb.append(sep).append(((MavenGoalSelectionDialog.Entry) o[i]).getName());
            }
          }
          sep = " ";
        }
        
        if(pos < len && txt.charAt(pos) != ' ') {
          sb.append(' ');
        }
        
        text.insert(sb.toString());
        text.setFocus();
        entriesChanged();
      }
    }
  }
  
}
