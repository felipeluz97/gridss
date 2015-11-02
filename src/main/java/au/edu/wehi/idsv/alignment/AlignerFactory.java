package au.edu.wehi.idsv.alignment;

import au.edu.wehi.idsv.Idsv;
import htsjdk.samtools.util.Log;

public class AlignerFactory {
	private static final Log log = Log.getInstance(Idsv.class);
	private static boolean sswjniLoaded;
    static {
        try {
        	System.loadLibrary("sswjni");
        	sswjniLoaded = true;
        } catch (UnsatisfiedLinkError e) {
        	sswjniLoaded = false;
        	log.warn("Unable to load sswjni library - assembly will be very slow. Please ensure libsswjni.so can be found by setting LD_LIBRARY_PATH or java.library.path");
        }
    }
	public static Aligner create(int match, int mismatch, int ambiguous, int gapOpen, int gapExtend) {
		if (sswjniLoaded) {
			return new SswJniAligner(match, mismatch, ambiguous, gapOpen, gapExtend);
		} else {
			return new JAlignerAligner(match, mismatch, ambiguous, gapOpen, gapExtend);
		}
	}
	private static Aligner defaultAligner = create(1, -4, -4, 6, 1); // bwa mem
	//private static Aligner defaultAligner = create(2, -6, -1, 5, 3); // bowtie2
	public static Aligner create() {
		return defaultAligner;
	}
}
