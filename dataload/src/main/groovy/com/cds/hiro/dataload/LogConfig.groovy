package com.cds.hiro.dataload

import org.apache.log4j.*

/**
 * Created by rahul on 10/15/15.
 */
class LogConfig {
  static void init() {
    def root = Logger.getRootLogger()

    ConsoleAppender console = new ConsoleAppender().
        with {
          layout = new EnhancedPatternLayout("%d [%p|%c{1.}] %m%n")
          threshold = Level.INFO
          activateOptions()
          it
        }
    root.addAppender(console)

    FileAppender fa = new FileAppender().
        with {
          name = "FileLogger"
          file = "build/dataload.log"
          layout = new PatternLayout("%d %-5p [%c{1}] %m%n")
          threshold = Level.DEBUG
          append = true
          activateOptions()

          it
        }
    root.addAppender(fa)
  }
}
