/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package org.intellij.images.thumbnail.actions;

import com.intellij.CommonBundle;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ex.ComboBoxAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import org.intellij.images.ImagesBundle;
import org.intellij.images.thumbnail.ThumbnailView;
import org.intellij.images.thumbnail.actionSystem.ThumbnailViewActionUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class FilterByThemeComboBoxAction extends ComboBoxAction {

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }


  @Override
    public void update(@NotNull final AnActionEvent e) {
        Project project = e.getProject();
        ThumbnailView view = ThumbnailViewActionUtil.getVisibleThumbnailView(e);
        boolean hasApplicableExtension =
          ContainerUtil.and(ThemeFilter.EP_NAME.getExtensions(), filter -> project != null && filter.isApplicableToProject(project));
        e.getPresentation().setVisible(view != null && hasApplicableExtension);
        ThemeFilter filter = view != null ? view.getFilter() : null;
        e.getPresentation().setText(filter == null ? CommonBundle.message("action.text.all") : filter.getDisplayName());
    }

    @NotNull
    @Override
    protected DefaultActionGroup createPopupActionGroup(JComponent button) {
        DefaultActionGroup group = new DefaultActionGroup();
        group.add(new FilterImagesAction(new ThemeFilter() {
            @Override
            public String getDisplayName() {
                return ImagesBundle.message("action.all.text");
            }

            @Override
            public boolean accepts(VirtualFile file) {
                return true;
            }

            @Override
            public boolean isApplicableToProject(Project project) {
                return true;
            }

            @Override
            public void setFilter(ThumbnailView view) {
                view.setFilter(this);
            }
        }));
        for (ThemeFilter filter : ThemeFilter.EP_NAME.getExtensions()) {
            group.add(new FilterImagesAction(filter));
        }

        return group;
    }
}
