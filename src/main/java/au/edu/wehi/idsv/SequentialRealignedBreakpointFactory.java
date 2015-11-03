package au.edu.wehi.idsv;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.util.Log;

import java.util.Set;

import com.google.common.collect.PeekingIterator;

/**
 * Finds corresponding realigned breakpoint sequence SAMRecords
 * <p>Input iterator must be ordered according to the start position of the source @See DirectedBreakpoint</p>
 * 
 * @author Daniel Cameron
 *
 */
public class SequentialRealignedBreakpointFactory extends SequentialSAMRecordFactoryBase<DirectedEvidence> {
	private static final Log log = Log.getInstance(SequentialRealignedBreakpointFactory.class);
	/**
	 * <p>read iterator <b>must</b> be coordinate sorted<p>
	 * @param reads
	 * @param mate
	 */
	public SequentialRealignedBreakpointFactory(
			PeekingIterator<SAMRecord> realigned) {
		super(realigned);
	}
	public SAMRecord findFirstAssociatedSAMRecord(DirectedEvidence source) {
		return findFirstAssociatedSAMRecord(source, false);
	}
	public SAMRecord findFirstAssociatedSAMRecord(DirectedEvidence source, boolean expected) {
		if (source == null) return null;
		Set<SAMRecord> records = findAllAssociatedSAMRecords(source, expected);
		if (records == null) return null;
		return records.iterator().next();
	}
	public Set<SAMRecord> findAllAssociatedSAMRecords(DirectedEvidence source, boolean expected) {
		if (source == null) return null;
		Set<SAMRecord> record = findMatching(
				BreakpointFastqEncoding.getReferenceIndex(source),
				BreakpointFastqEncoding.getStartPosition(source),
				BreakpointFastqEncoding.getID(source));
		if (expected && (record == null || record.isEmpty())) {
			String msg = String.format("Unable to find realignment record for %s. This is likely due to either " 
					+ "a) alignment not completed successfully or "
					+ "b) chosen aligner writing records out of order."
					+ "The aligner is required to write records in same order as the input fastq. "
					+ "Please raise a github enhancement request if use by an aligner with no capability to ensure this ordering is required.", source.getEvidenceID());
			log.equals(msg);
			throw new RuntimeException(msg);
		}
		return record;
	}
	@Override
	protected String getMatchingKey(SAMRecord record) {
		return BreakpointFastqEncoding.getEncodedID(record.getReadName());
	}
	@Override
	protected int getReferenceIndex(SAMRecord record) {
		return BreakpointFastqEncoding.getEncodedReferenceIndex(record.getReadName());
	}
	@Override
	protected int getPosition(SAMRecord record) {
		return BreakpointFastqEncoding.getEncodedStartPosition(record.getReadName());
	}
	@Override
	protected String trackedName() {
		return "matepairing.realigned";
	}
}
