/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.preferences;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.apache.maven.archetype.catalog.ArchetypeCatalog;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.embedder.MavenEmbedderManager;
import org.maven.ide.eclipse.embedder.ArchetypeManager.ArchetypeCatalogFactory;
import org.maven.ide.eclipse.embedder.ArchetypeManager.LocalCatalogFactory;

/**
 * Local Archetype catalog dialog
 * 
 * @author Eugene Kuleshov
 */
public class LocalArchetypeCatalogDialog extends TitleAreaDialog {

  private static final String DIALOG_SETTINGS = LocalArchetypeCatalogDialog.class.getName();

  private static final String KEY_LOCATIONS = "catalogLocation";
  
  private static final int MAX_HISTORY = 15;

  private String title;

  private String message;

  Combo catalogLocationCombo;

  private Text catalogDescriptionText;

  private IDialogSettings dialogSettings;

  private ArchetypeCatalogFactory archetypeCatalogFactory;


  protected LocalArchetypeCatalogDialog(Shell shell, ArchetypeCatalogFactory factory) {
    super(shell);
    this.archetypeCatalogFactory = factory;
    this.title = "Local Archetype Catalog";
    this.message = "Specify catalog location and description";
    setShellStyle(SWT.DIALOG_TRIM);

    IDialogSettings pluginSettings = MavenPlugin.getDefault().getDialogSettings();
    dialogSettings = pluginSettings.getSection(DIALOG_SETTINGS);
    if(dialogSettings == null) {
      dialogSettings = new DialogSettings(DIALOG_SETTINGS);
      pluginSettings.addSection(dialogSettings);
    }
  }

  protected Control createContents(Composite parent) {
    Control control = super.createContents(parent);
    setTitle(title);
    setMessage(message);
    return control;
  }

  protected Control createDialogArea(Composite parent) {
    Composite composite1 = (Composite) super.createDialogArea(parent);

    Composite composite = new Composite(composite1, SWT.NONE);
    composite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    GridLayout gridLayout = new GridLayout();
    gridLayout.marginTop = 7;
    gridLayout.marginWidth = 12;
    gridLayout.numColumns = 3;
    composite.setLayout(gridLayout);

    Label catalogLocationLabel = new Label(composite, SWT.NONE);
    catalogLocationLabel.setText("&Catalog File:");

    catalogLocationCombo = new Combo(composite, SWT.NONE);
    GridData gd_catalogLocationCombo = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_catalogLocationCombo.widthHint = 250;
    catalogLocationCombo.setLayoutData(gd_catalogLocationCombo);
    catalogLocationCombo.setItems(getSavedValues(KEY_LOCATIONS));

    Button browseButton = new Button(composite, SWT.NONE);
    browseButton.setText("&Browse...");
    browseButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        FileDialog dialog = new FileDialog(getShell());
        dialog.setText("Select Archetype catalog");
        String location = dialog.open();
        if(location!=null) {
          catalogLocationCombo.setText(location);
          update();
        }
      }
    });
    setButtonLayoutData(browseButton);

    Label catalogDescriptionLabel = new Label(composite, SWT.NONE);
    catalogDescriptionLabel.setText("Description:");

    catalogDescriptionText = new Text(composite, SWT.BORDER);
    catalogDescriptionText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    
    ModifyListener modifyListener = new ModifyListener() {
      public void modifyText(final ModifyEvent e) {
        update();
      }
    };
    
    if(archetypeCatalogFactory!=null) {
      catalogLocationCombo.setText(archetypeCatalogFactory.getId());
      catalogDescriptionText.setText(archetypeCatalogFactory.getDescription());
    }
    
    catalogLocationCombo.addModifyListener(modifyListener);
    catalogDescriptionText.addModifyListener(modifyListener);

//    fullIndexButton = new Button(composite, SWT.CHECK);
//    fullIndexButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
//    fullIndexButton.setText("&Full Index");
//    fullIndexButton.setSelection(true);

    return composite;
  }

  private String[] getSavedValues(String key) {
    String[] array = dialogSettings.getArray(key);
    return array == null ? new String[0] : array;
  }

  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setText(title);
  }

  public void create() {
    super.create();
    getButton(IDialogConstants.OK_ID).setEnabled(false);
  }

  protected void okPressed() {
    String description = catalogDescriptionText.getText().trim();
    String location = catalogLocationCombo.getText().trim();
   
    archetypeCatalogFactory = new LocalCatalogFactory(location, description, true);
    
    saveValue(KEY_LOCATIONS, location);

    super.okPressed();
  }

  public ArchetypeCatalogFactory getArchetypeCatalogFactory() {
    return archetypeCatalogFactory;
  }

  private void saveValue(String key, String value) {
    List dirs = new ArrayList();
    dirs.addAll(Arrays.asList(getSavedValues(key)));

    dirs.remove(value);
    dirs.add(0, value);

    if(dirs.size() > MAX_HISTORY) {
      dirs = dirs.subList(0, MAX_HISTORY);
    }

    dialogSettings.put(key, (String[]) dirs.toArray(new String[dirs.size()]));
  }

  void update() {
    boolean isValid = isValid();
    // verifyButton.setEnabled(isValid);
    getButton(IDialogConstants.OK_ID).setEnabled(isValid);
  }

  private boolean isValid() {
    setErrorMessage(null);
    setMessage(null, IStatus.WARNING);

    String location = catalogLocationCombo.getText().trim();
    if(location.length()==0) {
      setErrorMessage("Archetype catalog location is required");
      return false;
    }
    
    if(!new File(location).exists()) {
      setErrorMessage("Archetype catalog does not exist");
      return false;
    }

    LocalCatalogFactory factory = new LocalCatalogFactory(location, null, true);
    MavenEmbedderManager embedderManager = MavenPlugin.getDefault().getMavenEmbedderManager();
    try {
      ArchetypeCatalog archetypeCatalog = factory.getArchetypeCatalog(embedderManager);
      List archetypes = archetypeCatalog.getArchetypes();
      if(archetypes==null || archetypes.size()==0) {
        setMessage("Archetype catalog is empty", IStatus.WARNING);
      }
      
    } catch(CoreException ex) {
      IStatus status = ex.getStatus();
      setErrorMessage("Invalid archetype catalog;\n" + status.getMessage());
      return false;
    }
    
    return true;
  }

}
