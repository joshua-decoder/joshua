package edu.jhu.thrax.util;

import java.io.IOException;

public interface ExternalizableToUtf8 {

    public void readExternalUtf8(String fileName) throws IOException;

    public void writeExternalUtf8(String fileName) throws IOException;

}
