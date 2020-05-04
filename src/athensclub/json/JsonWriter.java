package athensclub.json;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import athensclub.compiler.ACompilerUtils;

/**
 * A writer that is responsible for writing JSON files.
 * 
 * @author Athensclub
 *
 */
public class JsonWriter extends Writer {

    private Writer writer;

    public JsonWriter(Writer w) {
	writer = w;
    }

    /**
     * Get the value of the object that the type is not in the basic Java to JSON
     * mappable list (specified by mapping backward from
     * {@link athensclub.json.JsonParser} mappings). It must return object the type
     * of Java to JSON mappable list, else the exception is thrown
     * 
     * @param obj
     * @return
     */
    public Object valueOf(Object obj) {
	return obj == null ? null : obj.toString();
    }

    public void writeInt(int value) throws IOException {
	writeToStream(Integer.toString(value));
    }

    public void writeDouble(double value) throws IOException {
	writeToStream(Double.toString(value));
    }

    public void writeBoolean(boolean value) throws IOException {
	writeToStream(Boolean.toString(value));
    }

    public void writeList(List<?> list) throws IOException {
	Iterator<?> it = list.iterator();
	writeToStream('[');
	while (it.hasNext()) {
	    write(it.next());
	    if (it.hasNext()) {
		writeToStream(',');
	    }
	}
	writeToStream(']');
    }

    public <T> void writeMap(Map<String, T> list) throws IOException {
	Iterator<Entry<String, T>> it = list.entrySet().iterator();
	writeToStream('{');
	while (it.hasNext()) {
	    Entry<String, T> e = it.next();
	    writeString(e.getKey());
	    writeToStream(':');
	    write(e.getValue());
	    if (it.hasNext()) {
		writeToStream(',');
	    }
	}
	writeToStream('}');
    }

    public void writeString(String str) throws IOException {
	writeToStream('"');
	for (int i = 0; i < str.length(); i++) {
	    if (ACompilerUtils.escapesInversed.containsKey(str.charAt(i))) {
		writeToStream('\\');
		writeToStream(ACompilerUtils.escapesInversed.get(str.charAt(i)));
	    } else {
		writeToStream(str.charAt(i));
	    }
	}
	writeToStream('"');
    }

    public void writeNull() throws IOException {
	writeToStream("null");
    }

    private void writeToStream(String value) throws IOException {
	writer.write(value);
    }

    private void writeToStream(char value) throws IOException {
	writer.write(value);
    }

    /**
     * Write the given object mapped to json value.
     * 
     * @param obj
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public void write(Object obj) throws IOException {
	if (obj == null) {
	    writeNull();
	} else if (obj instanceof Integer) {
	    writeInt((Integer) obj);
	} else if (obj instanceof Double) {
	    writeDouble((Double) obj);
	} else if (obj instanceof Boolean) {
	    writeBoolean(true);
	} else if (obj instanceof CharSequence) {
	    writeString(obj.toString());
	} else if (obj instanceof List) {
	    writeList((List<?>) obj);
	} else if (obj instanceof Map) {
	    writeMap((Map<String, ?>) obj);
	} else {
	    writeWithException(valueOf(obj));
	}
    }

    @SuppressWarnings("unchecked")
    private void writeWithException(Object obj) throws IOException {
	if (obj == null) {
	    writeNull();
	} else if (obj instanceof Integer) {
	    writeInt((Integer) obj);
	} else if (obj instanceof Double) {
	    writeDouble((Double) obj);
	} else if (obj instanceof Boolean) {
	    writeBoolean(true);
	} else if (obj instanceof String) {
	    writeString((String) obj);
	} else if (obj instanceof List) {
	    writeList((List<?>) obj);
	} else if (obj instanceof Map) {
	    writeMap((Map<String, ?>) obj);
	} else {
	    throw new IOException("valueOf(Object) does not return java to json mappable values");
	}
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
	writer.write(cbuf, off, len);
    }

    @Override
    public void flush() throws IOException {
	writer.flush();
    }

    @Override
    public void close() throws IOException {
	writer.close();
    }

}
