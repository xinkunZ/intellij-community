
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
package com.jetbrains.python.debugger.concurrency.tool.threading;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.jetbrains.python.debugger.PyConcurrencyEvent;
import com.jetbrains.python.debugger.concurrency.PyConcurrencyLogManager;
import com.jetbrains.python.debugger.concurrency.tool.ConcurrencyNamesManager;
import com.jetbrains.python.debugger.concurrency.tool.ConcurrencyStat;

import java.util.HashMap;


public class PyThreadingLogManagerImpl extends PyConcurrencyLogManager {
  public ConcurrencyNamesManager myLockManager;

  public static PyThreadingLogManagerImpl getInstance(Project project) {
    return ServiceManager.getService(project, PyThreadingLogManagerImpl.class);
  }

  public PyThreadingLogManagerImpl(Project project) {
    super(project);
    myLockManager = new ConcurrencyNamesManager();
  }

  public String getThreadIdForEventAt(int index) {
    return getEventAt(index).getThreadId();
  }

  @Override
  public HashMap getStatistics() {
    HashMap<String, ConcurrencyStat> result = new HashMap<String, ConcurrencyStat>();
    for (int i = 0; i < getSize(); ++i) {
      PyConcurrencyEvent event = getEventAt(i);
      String threadId = event.getThreadName();
      if (event.isThreadEvent() && event.getType() == PyConcurrencyEvent.EventType.START) {
        ConcurrencyStat stat = new ConcurrencyStat(event.getTime());
        result.put(threadId, stat);
      } else if (event.getType() == PyConcurrencyEvent.EventType.STOP) {
        ConcurrencyStat stat = new ConcurrencyStat(event.getTime());
        stat.myFinishTime = event.getTime();
      } else if (event.getType() == PyConcurrencyEvent.EventType.ACQUIRE_BEGIN) {
        ConcurrencyStat stat = result.get(threadId);
        stat.myLockCount++;
        stat.myLastAcquireStartTime = event.getTime();
      } else if (event.getType() == PyConcurrencyEvent.EventType.ACQUIRE_END) {
        ConcurrencyStat stat = result.get(threadId);
        stat.myWaitTime += (event.getTime() - stat.myLastAcquireStartTime);
        stat.myLastAcquireStartTime = 0;
      }
    }
    PyConcurrencyEvent lastEvent = getEventAt(getSize() - 1);
    long lastTime = lastEvent.getTime();
    //set last time for stopping on a breakpoint
    for (ConcurrencyStat stat: result.values()) {
      if (stat.myFinishTime == 0) {
        stat.myFinishTime = lastTime;
      }
    }
    return result;
  }

}
