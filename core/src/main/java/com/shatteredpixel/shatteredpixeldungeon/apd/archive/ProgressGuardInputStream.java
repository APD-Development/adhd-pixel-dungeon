package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

//Wraps the raw source of an import (a file picker's stream, a file). The InputStream contract says a read that asked for bytes gives at
//least one, or -1 at the end; a source that answers 0 for ever breaks it, and the zip classes (which loop on "need more input") would spin
//on it for ever while the importer holds its lock. Such a source is a failed source, whatever Android version it comes from.
final class ProgressGuardInputStream extends FilterInputStream {

	ProgressGuardInputStream( InputStream in ){
		super(in);
	}

	@Override
	public int read( byte[] b, int off, int len ) throws IOException {
		int n = in.read(b, off, len);
		if (n == 0 && len > 0) throw new IOException("the source stopped answering (no data and no end), so the archive cannot be read");
		return n;
	}
}
