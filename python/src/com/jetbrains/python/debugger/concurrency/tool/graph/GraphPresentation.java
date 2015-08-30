/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.jetbrains.python.debugger.concurrency.tool.graph;

import com.jetbrains.python.debugger.concurrency.tool.graph.elements.DrawElement;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class GraphPresentation {
  private final GraphManager myGraphManager;
  private List<PresentationListener> myListeners = new ArrayList<PresentationListener>();
  private final Object myListenersObject = new Object();
  private GraphVisualSettings myVisualSettings;

  public GraphPresentation(final GraphManager graphManager, GraphVisualSettings visualSettings) {
    myGraphManager = graphManager;
    myVisualSettings = visualSettings;

    myVisualSettings.registerListener(new GraphVisualSettings.SettingsListener() {
      @Override
      public void settingsChanged() {
        updateGraphModel();
        notifyListeners();
      }
    });

    myGraphManager.registerListener(new GraphManager.GraphListener() {
      @Override
      public void graphChanged() {
        updateGraphModel();
        notifyListeners();
      }
    });
  }

  private void updateGraphModel() {
  }

  public GraphVisualSettings getVisualSettings() {
    return myVisualSettings;
  }

  public int getLinesNumber() {
    return myGraphManager.getMaxThread();
  }

  public int getCellsNumber() {
    return (int)myGraphManager.getDuration() / myVisualSettings.getMillisPerCell();
  }

  public ArrayList<String> getThreadNames() {
    return myGraphManager.getThreadNames();
  }

  public ArrayList<ArrayList<DrawElement>> getVisibleGraph() {
    synchronized (myListenersObject) {
      if (myVisualSettings.getHorizontalMax() == 0) {
        return new ArrayList<ArrayList<DrawElement>>();
      }
      int val = myVisualSettings.getHorizontalValue();
      long duration = myGraphManager.getDuration();
      long startTime = myGraphManager.getStartTime() + val * duration / myVisualSettings.getHorizontalMax();
      int first = myGraphManager.getLastEventIndexBeforeMoment(startTime);
      int numberOfBlocks = myVisualSettings.getHorizontalExtent() / GraphSettings.CELL_WIDTH;

      ArrayList<ArrayList<DrawElement>> ret = new ArrayList<ArrayList<DrawElement>>();
      ret.add(myGraphManager.getDrawElementsForRow(first));
      long millisPerCell = myVisualSettings.getMillisPerCell();

      for (int i = 0; i < numberOfBlocks; ++i) {
        long timeForNextCell = startTime + millisPerCell * i;
        int eventIndex = myGraphManager.getLastEventIndexBeforeMoment(timeForNextCell);
        ret.add(myGraphManager.getDrawElementsForRow(eventIndex));
      }
      return ret;
    }
  }


  public interface PresentationListener {
    void graphChanged(int padding, int size);
  }

  public void registerListener(@NotNull PresentationListener logListener) {
    synchronized (myListenersObject) {
      myListeners.add(logListener);
    }
  }

  public void notifyListeners() {
    synchronized (myListenersObject) {
      for (PresentationListener logListener : myListeners) {
        logListener.graphChanged(myVisualSettings.getHorizontalMax() == 0 ? myVisualSettings.getHorizontalValue() :
                                 myVisualSettings.getHorizontalValue() * getCellsNumber() / myVisualSettings.getHorizontalMax(),
                                 myGraphManager.getSize());
      }
    }
  }

}
