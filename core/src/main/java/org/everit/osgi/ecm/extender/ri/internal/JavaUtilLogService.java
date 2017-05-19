/*
 * Copyright (C) 2011 Everit Kft. (http://www.everit.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.everit.osgi.ecm.extender.ri.internal;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

/**
 * LogService implementation that sends all log messages to {@link java.util.logging.Logger}.
 */
public class JavaUtilLogService implements LogService {

  private static Level convertLogServiceLevelToJavaLoggerLevel(final int logServiceLevel) {
    switch (logServiceLevel) {
      case LogService.LOG_DEBUG:
        return Level.FINE;
      case LogService.LOG_ERROR:
        return Level.SEVERE;
      case LogService.LOG_INFO:
        return Level.INFO;
      case LogService.LOG_WARNING:
        return Level.WARNING;
      default:
        return Level.INFO;
    }
  }

  private static String createMessage(@SuppressWarnings("rawtypes") final ServiceReference sr,
      final String message) {
    return "Service " + sr.getProperty(Constants.SERVICE_ID) + " - "
        + Arrays.toString((String[]) sr.getProperty(Constants.OBJECTCLASS)) + ": " + message;
  }

  private final Logger logger;

  public JavaUtilLogService(final String loggerName) {
    this.logger = Logger.getLogger(loggerName);
  }

  @Override
  public void log(final int level, final String message) {
    this.logger.log(convertLogServiceLevelToJavaLoggerLevel(level), message);
  }

  @Override
  public void log(final int level, final String message, final Throwable exception) {
    this.logger.log(convertLogServiceLevelToJavaLoggerLevel(level), message, exception);
  }

  @Override
  @SuppressWarnings("rawtypes")
  public void log(final ServiceReference sr, final int level, final String message) {
    this.logger.log(convertLogServiceLevelToJavaLoggerLevel(level), createMessage(sr, message));
  }

  @Override
  @SuppressWarnings("rawtypes")
  public void log(final ServiceReference sr, final int level, final String message,
      final Throwable exception) {

    this.logger.log(convertLogServiceLevelToJavaLoggerLevel(level), createMessage(sr, message),
        exception);
  }

}
