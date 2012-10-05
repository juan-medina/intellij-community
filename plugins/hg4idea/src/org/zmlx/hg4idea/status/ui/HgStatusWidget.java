/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package org.zmlx.hg4idea.status.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.status.EditorBasedWidget;
import com.intellij.util.Consumer;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.zmlx.hg4idea.HgProjectSettings;
import org.zmlx.hg4idea.HgUpdater;
import org.zmlx.hg4idea.HgVcs;
import org.zmlx.hg4idea.Topics;
import org.zmlx.hg4idea.status.HgChangesetStatus;
import org.zmlx.hg4idea.status.HgCurrentBranchStatus;
import org.zmlx.hg4idea.status.HgRemoteStatusUpdater;

import java.awt.event.MouseEvent;

/**
 * Widget to display basic hg status in the IJ status bar.
 */
public class HgStatusWidget
  extends EditorBasedWidget
  implements StatusBarWidget.MultipleTextValuesPresentation, StatusBarWidget.Multiframe, HgUpdater {

	private final HgVcs vcs;
	private final HgProjectSettings projectSettings;

	private volatile String myText = "";
  private volatile String myTooltip = "";

  private final HgCurrentBranchStatus currentBranchStatus;
  private final HgChangesetStatus incomingChangesStatus;
  private final HgChangesetStatus outgoingChangesStatus;

  private static final String myMaxString = "hg: default; in: 99; out: 99";

	private MessageBusConnection busConnection;

	private HgRemoteStatusUpdater remoteUpdater;

	private HgCurrentBranchStatusUpdater branchStatusUpdater;


	public HgStatusWidget( HgVcs vcs, Project project, HgProjectSettings projectSettings) {
	  super( project );
	  this.vcs = vcs;
	  this.projectSettings = projectSettings;

	  this.incomingChangesStatus = new HgChangesetStatus( "In" );
    this.outgoingChangesStatus = new HgChangesetStatus( "Out" );
		currentBranchStatus = new HgCurrentBranchStatus();
	}

  @Override
  public StatusBarWidget copy() {
    return new HgStatusWidget( vcs, getProject(), projectSettings );
  }

  @NotNull
  @Override
  public String ID() {
    return HgStatusWidget.class.getName();
  }

  @Override
  public WidgetPresentation getPresentation(@NotNull PlatformType type) {
    return this;
  }

  @Override
  public void selectionChanged(FileEditorManagerEvent event) {
    update();
  }

  @Override
  public void fileOpened(FileEditorManager source, VirtualFile file) {
    update();
  }

  @Override
  public void fileClosed(FileEditorManager source, VirtualFile file) {
    update();
  }

  //@Override
  //public void repositoryChanged() {
  //  update();
  //}

  @Override
  public ListPopup getPopupStep() {
    Project project = getProject();
    if (project == null) {
      return null;
    }

    /*
    TODO:
    GitRepository repo = GitBranchUiUtil.getCurrentRepository(project);
    if (repo == null) {
      return null;
    }
    return GitBranchPopup.getInstance(project, repo).asListPopup();
    */

    return null;
  }

  @Override
  public String getSelectedValue() {
    final String text = myText;
    return StringUtil.isEmpty(text) ? "" : "Hg: " + text;
  }

  @NotNull
  @Override
  public String getMaxValue() {
    // todo: ????
    return myMaxString;
  }

  @Override
  public String getTooltipText() {
    return myTooltip;
  }

  @Override
  // Updates branch information on click
  public Consumer<MouseEvent> getClickConsumer() {
    return new Consumer<MouseEvent>() {
      public void consume(MouseEvent mouseEvent) {
        update();
      }
    };
  }


	@Override
	public void update( final Project project ) {

		ApplicationManager.getApplication().invokeLater(new Runnable() {
	    @Override
	    public void run() {
	      if ( ( project == null ) || project.isDisposed() ) {
	        emptyTextAndTooltip();
	        return;
	      }

		    emptyTextAndTooltip();

		    if ( null != currentBranchStatus.getStatusText() ) {
			    myText = currentBranchStatus.getStatusText();
		      myTooltip = currentBranchStatus.getToolTipText();
		    }

	      if ( incomingChangesStatus.getNumChanges() > 0 ) {
		      myText += "; " + incomingChangesStatus.getStatusName() + ": " + incomingChangesStatus.getNumChanges();
	        myTooltip = "\n" + incomingChangesStatus.getToolTip();
	      }

	      if ( outgoingChangesStatus.getNumChanges() > 0 ) {
		      myText += "; " + outgoingChangesStatus.getStatusName() + ": " + outgoingChangesStatus.getNumChanges();
	        myTooltip += "\n" + outgoingChangesStatus.getToolTip();
	      }

	      int maxLength = myMaxString.length() - 1; // -1, because there are arrows indicating that it is a popup
	      myText = StringUtil.shortenTextWithEllipsis(myText, maxLength, 5);

	      myStatusBar.updateWidget(ID());
	    }
	  });
	}


	public void activate() {

		Project project = getProject();
		if ( null == project ) {
			return;
		}

		busConnection = project.getMessageBus().connect();
		busConnection.subscribe( Topics.STATUS_TOPIC, this );

		branchStatusUpdater = new HgCurrentBranchStatusUpdater( vcs, currentBranchStatus );
		branchStatusUpdater.activate();

		remoteUpdater = new HgRemoteStatusUpdater( vcs, incomingChangesStatus, outgoingChangesStatus, projectSettings );
		remoteUpdater.activate();

		StatusBar statusBar = WindowManager.getInstance().getStatusBar( project );
		if ( null != statusBar  ) {
			statusBar.addWidget( this, project );
		}
	}

	public void deactivate() {

		// TODO: Note that at this point, we cannot get the status bar...the com.intellij.openapi.wm.impl.WindowManagerImpl.releaseFrame()
		//  has already been invoked, and so the window manager has no idea what we're talking about. This happens to be
		//  (or at least seems to be) OK, because when we add the widget to the status bar, we're doing so with the project
		//  as the parentDisposable, so IJ seems to Do The Right Thing and cleans things up.
		StatusBar statusBar = WindowManager.getInstance().getStatusBar( getProject() );
		if ( null != statusBar ) {
			statusBar.removeWidget( ID() );
		}

		busConnection.disconnect();

		remoteUpdater.deactivate();
		branchStatusUpdater.deactivate();
	}

  private void update() {
	  update( getProject() );
  }

  private void emptyTextAndTooltip() {
    myText = "";
    myTooltip = "";
  }


}  // End of HgStatusWidget class