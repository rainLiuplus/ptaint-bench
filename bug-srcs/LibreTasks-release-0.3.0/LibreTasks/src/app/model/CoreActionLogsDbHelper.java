/*  
 * Copyright (c) 2016  LibreTasks - https://github.com/biotinker/LibreTasks  
 *  
 *  This file is free software: you may copy, redistribute and/or modify it  
 *  under the terms of the GNU General Public License as published by the  
 *  Free Software Foundation, either version 3 of the License, or (at your  
 *  option) any later version.  
 *  
 *  This file is distributed in the hope that it will be useful, but  
 *  WITHOUT ANY WARRANTY; without even the implied warranty of  
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU  
 *  General Public License for more details.  
 *  
 *  You should have received a copy of the GNU General Public License  
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.  
 *  
 * This file incorporates work covered by the following copyright and  
 * permission notice:  
 /*******************************************************************************
 * Copyright 2010 Omnidroid - http://code.google.com/p/omnidroid
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package libretasks.app.model;

import static libretasks.app.model.CursorHelper.getLongFromCursor;
import static libretasks.app.model.CursorHelper.getStringFromCursor;

import android.content.Context;
import android.database.Cursor;
import libretasks.app.model.db.LogActionDbAdapter;
import libretasks.app.model.db.LogDbAdapter;

/**
 * This class serves as a access layer to the database for Omnidroid's {@code LogAction} storage.
 * 
 */
public class CoreActionLogsDbHelper extends CoreLogsDbHelper {
  /**
   * Creates a new CoreActionDbHelper within the current context and initializes all necessary
   * database adapters.
   * 
   * @param context
   *          context for the application database resource
   */
  public CoreActionLogsDbHelper(Context context) {
    super(context);
    logDbAdapter = new LogActionDbAdapter(database);
  }

  @Override
  public Log getLog(Cursor cursor) {
    // Get log data from the DB
    long id = getLongFromCursor(cursor, LogActionDbAdapter.KEY_ID);
    long timestamp = getLongFromCursor(cursor, LogDbAdapter.KEY_TIMESTAMP);
    long logEventId = getLongFromCursor(cursor, LogActionDbAdapter.KEY_LOGEVENTID);
    String ruleName = getStringFromCursor(cursor, LogActionDbAdapter.KEY_RULENAME);
    String appName = getStringFromCursor(cursor, LogActionDbAdapter.KEY_ACTIONAPPNAME);
    String eventName = getStringFromCursor(cursor, LogActionDbAdapter.KEY_ACTIONEVENTNAME);
    String eventParams = getStringFromCursor(cursor, LogActionDbAdapter.KEY_ACTIONPARAMETERS);
    String text = getStringFromCursor(cursor, LogDbAdapter.KEY_DESCRIPTION);

    // Create a Log object from the data
    ActionLog log = new ActionLog(id, timestamp, logEventId, ruleName, appName, eventName,
        eventParams, text);
    return log;
  }
}
