package joshua.util;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Log formatter that prints just the message, with no time stamp.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class QuietFormatter extends Formatter {

  public String format(LogRecord record) {
    return "" + formatMessage(record) + "\n";
  }

}
