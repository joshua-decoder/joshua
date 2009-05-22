/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */
package joshua.util.io;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * This class provides an unchecked version of IOException. It is
 * generally preferable to use IOException directly, but this class
 * can be useful in contexts where an interface does not allow one
 * to throw checked exceptions.
 *
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate: 2009-03-26 15:06:57 -0400 (Thu, 26 Mar 2009) $
 */
public class UncheckedIOException extends RuntimeException {
	private IOException ioe;
	
	public UncheckedIOException(IOException ioe) {
		this.ioe = ioe;
	}
	
	/** Unwrap this object and return the underlying IOException. */
	public void throwCheckedException() throws IOException {
		throw this.ioe;
	}
	
	
//===============================================================
// Delegate all methods of IOException (same as java.lang.Throwable)
//===============================================================
	
	public String getMessage() {
		return this.ioe.getMessage();
	}
	
	public String getLocalizedMessage() {
		return this.ioe.getLocalizedMessage();
	}
	
	public Throwable getCause() {
		return this.ioe.getCause();
	}
	
	public Throwable initCause(Throwable cause)
	throws IllegalArgumentException, IllegalStateException {
		return this.ioe.initCause(cause);
	}
	
	public String toString() {
		return this.ioe.toString();
	}
	
	public void printStackTrace() {
		this.ioe.printStackTrace();
	}
	
	public void printStackTrace(PrintStream s) {
		this.ioe.printStackTrace(s);
	}
	
	public void printStackTrace(PrintWriter s) {
		this.ioe.printStackTrace(s);
	}
	
	public Throwable fillInStackTrace() {
		return this.ioe.fillInStackTrace();
	}
	
	public StackTraceElement[] getStackTrace() {
		return this.ioe.getStackTrace();
	}
	
	public void setStackTrace(StackTraceElement[] stackTrace)
	throws NullPointerException {
		this.ioe.setStackTrace(stackTrace);
	}
}
