package au.edu.wehi.idsv.debruijn.subgraph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.collect.Lists;

import au.edu.wehi.idsv.AssemblyEvidence;
import au.edu.wehi.idsv.BreakpointSummary;
import au.edu.wehi.idsv.DirectedEvidence;
import au.edu.wehi.idsv.ProcessingContext;
import au.edu.wehi.idsv.SAMEvidenceSource;
import au.edu.wehi.idsv.SmallIndelSAMRecordAssemblyEvidence;
import au.edu.wehi.idsv.TestHelper;
import au.edu.wehi.idsv.configuration.AssemblyConfiguration;
import au.edu.wehi.idsv.picard.InMemoryReferenceSequenceFile;
import htsjdk.samtools.util.SequenceUtil;


public class DeBruijnSubgraphAssemblerTest extends TestHelper {
	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
	private ProcessingContext PC(int k) {
		ProcessingContext pc = getContext();
		AssemblyConfiguration p = pc.getAssemblyParameters();
		p.k = k;
		pc.getConfig().getVisualisation().assembly = true;
		pc.getConfig().getVisualisation().assemblyProgress = true;
		pc.getConfig().getVisualisation().directory = new File(testFolder.getRoot(), "visualisation");
		pc.getConfig().getVisualisation().directory.mkdir();
		return pc;
	}
	@Test
	public void should_assemble_all_contigs() {
		ProcessingContext pc = PC(3);
		SAMEvidenceSource ses = SES(pc);
		DeBruijnSubgraphAssembler ass = new DeBruijnSubgraphAssembler(AES(pc));
		List<AssemblyEvidence> results = Lists.newArrayList();
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, ses, withSequence("TAAAGTC", Read(0, 1, "4M3S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, ses, withSequence("AAAGTCT", Read(0, 2, "3M4S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, ses, withSequence("TAAAGTC", Read(1, 1, "4M3S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, ses, withSequence("AAAGTCT", Read(1, 2, "3M4S"))))));
		results.addAll(Lists.newArrayList(ass.endOfEvidence()));
		assertEquals(2, results.size());
	}
	@Test
	public void should_assemble_RP_with_SC_anchor() {
		ProcessingContext pc = PC(3);
		SAMEvidenceSource ses = SES(pc);
		DeBruijnSubgraphAssembler ass = new DeBruijnSubgraphAssembler(AES(pc));
		List<AssemblyEvidence> results = Lists.newArrayList();
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, ses, withSequence("TAAT", Read(0, 15, "3M1S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(NRRP(ses, withSequence(SequenceUtil.reverseComplement("TAAAGTC"), OEA(0, 1, "7M", true))))));
		results.addAll(Lists.newArrayList(ass.endOfEvidence()));
		assertEquals(1, results.size());
		assertEquals(3, results.get(0).getAssemblyAnchorLength());
	}
	@Test
	public void should_export_debruijn_graph() {
		ProcessingContext pc = PC(3);
		SAMEvidenceSource ses = SES(pc);
		DeBruijnSubgraphAssembler ass = new DeBruijnSubgraphAssembler(AES(pc));
		List<AssemblyEvidence> results = Lists.newArrayList();
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, ses, withSequence("TAAAGTC", Read(0, 1, "4M3S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, ses, withSequence("AAAGTCT", Read(0, 2, "3M4S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, ses, withSequence("TAAAGTC", Read(1, 1, "4M3S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, ses, withSequence("AAAGTCT", Read(1, 2, "3M4S"))))));
		results.addAll(Lists.newArrayList(ass.endOfEvidence()));
		assertTrue(new File(new File(testFolder.getRoot(), "visualisation"), "debruijn.kmers.polyA.gexf").exists());
		assertTrue(new File(new File(testFolder.getRoot(), "visualisation"), "debruijn.kmers.polyACGT.gexf").exists());
	}
	@Test
	public void should_track_progress() throws IOException {
		ProcessingContext pc = PC(5);
		SAMEvidenceSource ses = SES(pc);
		DeBruijnSubgraphAssembler ass = new DeBruijnSubgraphAssembler(AES(pc));
		List<AssemblyEvidence> results = Lists.newArrayList();
		results.addAll(Lists.newArrayList(ass.addEvidence(NRRP(ses, withSequence("GTCTTA", DP(0, 1, "6M", true, 0, 500, "6M", false))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, ses, withSequence("CTTAGA", Read(0, 100, "1M5S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, ses, withSequence("CTTAGA", Read(0, 101, "1M5S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, ses, withSequence("TAAAGTC", Read(0, 1, "4M3S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, ses, withSequence("AAAGTCT", Read(0, 2, "3M4S"))))));
		//results.addAll(Lists.newArrayList(ass.addEvidence(SCE(BWD, ses, withSequence("TAAAGTCATGTATT", Read(0, 1, "5S9M"))))));
		results.addAll(Lists.newArrayList(ass.endOfEvidence()));
		File file = new File(new File(testFolder.getRoot(), "visualisation"), "debruijn.assembly.metrics.polyA.bed");
		assertTrue(file.exists());
		String contents = new String(Files.readAllBytes(file.toPath())); 
		assertTrue(contents.contains("Times"));
		assertTrue(contents.contains("-"));
		assertTrue(contents.contains("polyA"));
		assertTrue(contents.contains("Kmers"));
		assertTrue(contents.contains("PathNodes \"1 (1 0 0)\"; "));
	}
	@Test
	public void should_assemble_both_directions() {
		ProcessingContext pc = PC(3);
		SAMEvidenceSource ses = SES(pc);
		DeBruijnSubgraphAssembler ass = new DeBruijnSubgraphAssembler(AES(pc));
		List<AssemblyEvidence> results = Lists.newArrayList();
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, ses, withSequence("TAAAGTC", Read(0, 1, "4M3S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, ses, withSequence("AAAGTCT", Read(0, 2, "3M4S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(BWD, ses, withSequence("TATG", Read(0, 10, "1S3M"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(BWD, ses, withSequence("TTATG", Read(0, 10, "2S3M"))))));
		results.addAll(Lists.newArrayList(ass.endOfEvidence()));
		assertEquals(2, results.size());
	}
	@Test
	public void should_anchor_at_reference_kmer() {
		ProcessingContext pc = PC(3);
		SAMEvidenceSource ses = SES(pc);
		DeBruijnSubgraphAssembler ass = new DeBruijnSubgraphAssembler(AES(pc));
		List<AssemblyEvidence> results = Lists.newArrayList();
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, ses, withSequence("TAAAGTC", Read(0, 1, "4M3S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, ses, withSequence("AAAGTCT", Read(0, 2, "3M4S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(BWD, ses, withSequence("CCGACAT", Read(0, 10, "3S4M"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(BWD, ses, withSequence("GCCGACA", Read(0, 10, "4S3M"))))));
		results.addAll(Lists.newArrayList(ass.endOfEvidence()));
		assertEquals(2, results.size());
		assertEquals(4, results.get(0).getBreakendSequence().length);
		assertEquals(4, results.get(1).getBreakendSequence().length);
	}
	@Test
	public void should_assemble_() {
		ProcessingContext pc = PC(3);
		SAMEvidenceSource ses = SES(pc);
		DeBruijnSubgraphAssembler ass = new DeBruijnSubgraphAssembler(AES(pc));
		List<AssemblyEvidence> results = Lists.newArrayList();
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, ses, withSequence("TAAAGTC", Read(0, 1, "4M3S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, ses, withSequence("AAAGTCT", Read(0, 2, "3M4S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(BWD, ses, withSequence("CCGACAT", Read(0, 10, "3S4M"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(BWD, ses, withSequence("GCCGACA", Read(0, 10, "4S3M"))))));
		results.addAll(Lists.newArrayList(ass.endOfEvidence()));
		assertEquals(2, results.size());
		assertEquals(4, results.get(0).getBreakendSequence().length);
	}
	@Test
	public void should_anchor_at_reference_kmer_large_kmer() {
		ProcessingContext pc = PC(32);
		SAMEvidenceSource ses = SES(pc);
		DeBruijnSubgraphAssembler ass = new DeBruijnSubgraphAssembler(AES(pc));
		List<AssemblyEvidence> results = Lists.newArrayList();
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, ses, withSequence(S(RANDOM).substring(0, 200), Read(0, 1, "100M100S"))))));
		results.addAll(Lists.newArrayList(ass.endOfEvidence()));
		assertEquals(1, results.size());
		assertEquals(100, results.get(0).getBreakendSequence().length);
		assertEquals(200, results.get(0).getAssemblySequence().length);
	}
	@Test
	public void soft_clip_assembly_should_anchor_at_reference_kmer() {
		ProcessingContext pc = PC(4);
		SAMEvidenceSource ses = SES(pc);
		DeBruijnSubgraphAssembler ass = new DeBruijnSubgraphAssembler(AES(pc));
		List<AssemblyEvidence> results = Lists.newArrayList();
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(BWD, ses, withSequence("TTGCTCAAAA", Read(0, 1, "6S4M"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(NRRP(ses, withSequence("TGCTG", OEA(0, 4, "5M", false))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(NRRP(ses, withSequence("TGCTG", OEA(0, 5, "5M", false))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(NRRP(ses, withSequence("TGCTG", OEA(0, 6, "5M", false))))));
		results.addAll(Lists.newArrayList(ass.endOfEvidence()));
		assertEquals(1, results.size());
		assertEquals("TTGCTCAAAA", S(results.get(0).getAssemblySequence()));
		assertEquals(1, results.get(0).getBreakendSummary().start);
		assertEquals(4, results.get(0).getAssemblySequence().length - results.get(0).getBreakendSequence().length);
	}
	@Test
	@Ignore("TODO: NYI: Not Yet Implemented")
	public void should_assemble_anchor_shorter_than_kmer() {
		ProcessingContext pc = PC(5);
		SAMEvidenceSource ses = SES(pc);
		DeBruijnSubgraphAssembler ass = new DeBruijnSubgraphAssembler(AES(pc));
		List<AssemblyEvidence> results = Lists.newArrayList();
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, ses, withSequence("ATTAGA", Read(0, 1, "1M5S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, ses, withSequence("ATTAGA", Read(0, 1, "1M5S"))))));
		results.addAll(Lists.newArrayList(ass.endOfEvidence()));
		assertEquals(1, results.size());
	}
	@Test
	@Ignore("TODO: NYI: Not Yet Implemented")
	public void should_assemble_anchor_shorter_than_kmer_with_indel_rp_support() {
		ProcessingContext pc = PC(5);
		SAMEvidenceSource ses = SES(pc);
		DeBruijnSubgraphAssembler ass = new DeBruijnSubgraphAssembler(AES(pc));
		List<AssemblyEvidence> results = Lists.newArrayList();
		results.addAll(Lists.newArrayList(ass.addEvidence(NRRP(ses, withSequence("GTCTTA", DP(0, 1, "8M", true, 0, 500, "8M", false))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, ses, withSequence("CTTAGA", Read(0, 100, "1M5S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, ses, withSequence("CTTAGA", Read(0, 100, "1M5S"))))));
		results.addAll(Lists.newArrayList(ass.endOfEvidence()));
		assertEquals(1, results.size());
		assertEquals(3, results.get(0).getBreakendSummary().start);
	}
	@Test
	public void should_assemble_deletion() {
		//          1         2         3         4
		// 1234567890123456789012345678901234567890123456789
		// CATTAATCGCAAGAGCGGGTTGTATTCGACGCCAAGTCAGCTGAAGCACCATTACCCGATCAAA
		// ***************         ***************
		//        12345678         12345678
		// start anchor position = 8 -> 8 + k-1 = 15
		// end anchor position = 25
		String seq = S(RANDOM).substring(0, 0+15) + S(RANDOM).substring(24, 24+15);
		ProcessingContext pc = PC(8);
		SAMEvidenceSource ses = SES(pc);
		DeBruijnSubgraphAssembler ass = new DeBruijnSubgraphAssembler(AES(pc));
		List<AssemblyEvidence> results = Lists.newArrayList();
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, ses, withSequence(seq, Read(2, 1, "15M15S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(BWD, ses, withSequence(seq, Read(2, 25, "15S15M"))))));
		results.addAll(Lists.newArrayList(ass.endOfEvidence()));
		assertEquals(1, results.size());
		SmallIndelSAMRecordAssemblyEvidence e = (SmallIndelSAMRecordAssemblyEvidence)results.get(0);
		assertEquals(seq, S(e.getAssemblySequence()));
		assertEquals("15M9D15M", e.getBackingRecord().getCigarString());
		assertEquals(1, e.getBackingRecord().getAlignmentStart());
		assertEquals(seq, S(e.getBackingRecord().getReadBases()));
		assertEquals(new BreakpointSummary(2, FWD, 15, 15, 2, BWD, 25, 25), e.getBreakendSummary());
	}
	@Test
	public void should_assemble_deletion_microhomology() {
		String contig = "CATTAATCGCAATAAAATGTTCAAAACGACGCCAAGTCAGCTGAAGCACCATTACCCGATCAAA";
		ProcessingContext context = getContext(new InMemoryReferenceSequenceFile(new String[] { "Contig" }, new byte[][] { B(contig) }));
		context.getAssemblyParameters().k = 8;
		MockSAMEvidenceSource ses = SES(context);
		//          1         2         3         4
		// 1234567890123456789012345678901234567890123456789
		// CATTAATCGCAATAAAATGTTCAAAACGACGCCAAGTCAGCTGAAGCACCATTACCCGATCAAA
		// ***************         ***************
		//        12345678         12345678
		// start anchor position = 8 -> 8 + k-1 = 15
		// end anchor position = 25
		String seq = contig.substring(0, 0+15) + contig.substring(24, 24+15);
		DeBruijnSubgraphAssembler ass = new DeBruijnSubgraphAssembler(AES(context));
		List<AssemblyEvidence> results = Lists.newArrayList();
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, ses, withSequence(seq, Read(0, 1, "15M15S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(BWD, ses, withSequence(seq, Read(0, 25, "15S15M"))))));
		results.addAll(Lists.newArrayList(ass.endOfEvidence()));
		assertEquals(1, results.size());
		SmallIndelSAMRecordAssemblyEvidence e = (SmallIndelSAMRecordAssemblyEvidence)results.get(0);
		assertEquals(seq, S(e.getAssemblySequence()));
		assertEquals("15M9D15M", e.getBackingRecord().getCigarString());
		assertEquals(1, e.getBackingRecord().getAlignmentStart());
		assertEquals(seq, S(e.getBackingRecord().getReadBases()));
		assertEquals(1, e.getSAMRecord().getAlignmentStart());
		assertEquals(25, e.getRemoteSAMRecord().getAlignmentStart());
		assertEquals("CATTAATCGCAATAAAACGACGCCAAGTCA", S(e.getSAMRecord().getReadBases()));
		assertEquals("AACGACGCCAAGTCA", S(e.getRemoteSAMRecord().getReadBases()));
		assertEquals(new BreakpointSummary(0, FWD, 13, 17, 0, BWD, 23, 27), e.getBreakendSummary());
	}
	@Test
	public void breakpoint_microhomology_should_be_centre_aligned() {
		String contig = "CATTAATCGCAATAAAATGTTCAAAACGACGCCAAGTCAGCTGAAGCACCATTACCCGATCAAA";
		ProcessingContext context = getContext(new InMemoryReferenceSequenceFile(new String[] { "Contig" }, new byte[][] { B(contig) }));
		context.getAssemblyParameters().k = 8;
		MockSAMEvidenceSource ses = SES(context);
		//          1         2         3         4
		// 1234567890123456789012345678901234567890123456789
		// CATTAATCGCAATAAAATGTTCAAAACGACGCCAAGTCAGCTGAAGCACCATTACCCGATCAAA
		// *****************     *****************
		//        12345678         12345678
		// start anchor position = 8 -> 8 + k-1 = 15
		// end anchor position = 25
		String seq = contig.substring(0, 0+15) + contig.substring(24, 24+15);
		DeBruijnSubgraphAssembler ass = new DeBruijnSubgraphAssembler(AES(context));
		List<AssemblyEvidence> results = Lists.newArrayList();
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, ses, withSequence(seq, Read(0, 1, "17M13S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(BWD, ses, withSequence(seq, Read(0, 23, "13S17M"))))));
		results.addAll(Lists.newArrayList(ass.endOfEvidence()));
		assertEquals(1, results.size());
		SmallIndelSAMRecordAssemblyEvidence e = (SmallIndelSAMRecordAssemblyEvidence)results.get(0);
		assertEquals(seq, S(e.getAssemblySequence()));
		assertEquals("15M9D15M", e.getBackingRecord().getCigarString());
		assertEquals(1, e.getBackingRecord().getAlignmentStart());
		assertEquals(seq, S(e.getBackingRecord().getReadBases()));
		assertEquals(1, e.getSAMRecord().getAlignmentStart());
		assertEquals(25, e.getRemoteSAMRecord().getAlignmentStart());
		assertEquals("CATTAATCGCAATAAAACGACGCCAAGTCA", S(e.getSAMRecord().getReadBases()));
		assertEquals("AACGACGCCAAGTCA", S(e.getRemoteSAMRecord().getReadBases()));
		assertEquals(new BreakpointSummary(0, FWD, 13, 17, 0, BWD, 23, 27), e.getBreakendSummary());
	}
	@Test
	@Ignore() //  TODO: special case these?
	public void should_not_call_reference_bubble() {
		String seq = S(RANDOM).substring(0, 10);
		ProcessingContext pc = PC(5);
		SAMEvidenceSource ses = SES(pc);
		DeBruijnSubgraphAssembler ass = new DeBruijnSubgraphAssembler(AES(pc));
		List<AssemblyEvidence> results = Lists.newArrayList();
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(FWD, ses, withSequence(seq, Read(0, 1, "5M5S"))))));
		results.addAll(Lists.newArrayList(ass.addEvidence(SCE(BWD, ses, withSequence(seq, Read(0, 6, "5S5M"))))));
		results.addAll(Lists.newArrayList(ass.endOfEvidence()));
		// no actual variant support - artifact of tracking reference/non-reference at kmer resolution, not base pair resolution  
		assertEquals(0, results.size());
	}
	@Test
	public void should_use_expected_read_pair_orientation() {
		ProcessingContext pc = PC(5);
		SAMEvidenceSource ses = SES(pc);
		DeBruijnSubgraphAssembler ass = new DeBruijnSubgraphAssembler(AES(pc));
		List<DirectedEvidence> in = new ArrayList<DirectedEvidence>();
		in.add(SCE(FWD, ses, withSequence("AACCGGTTC", Read(0, 1, "5M4S"))));
		in.add(SCE(FWD, ses, withSequence("AACCGGTTC", Read(0, 2, "4M5S"))));
		in.add(NRRP(ses, withSequence("AACCGGTTCC", DP(0, 1, "10M", false, 1, 100, "10M", true))));
		in.add(NRRP(ses, withSequence("AACCGGTTCC", DP(0, 2, "10M", false, 1, 100, "10M", true))));
		in.add(NRRP(ses, withSequence("GTACGATT", DP(0, 1, "8M", false, 1, 100, "8M", false))));
		in.add(NRRP(ses, withSequence("GTACGATT", DP(0, 2, "8M", false, 1, 100, "8M", false))));
		in.sort(DirectedEvidence.ByStartEnd);
		
		List<AssemblyEvidence> results = Lists.newArrayList();
		for (DirectedEvidence e : in) {
			results.addAll(Lists.newArrayList(ass.addEvidence(e)));
		}
		results.addAll(Lists.newArrayList(ass.endOfEvidence()));
		assertEquals(2, results.size());
		assertEquals("AACCGGTTCC", S(results.get(1).getAssemblySequence()));
		assertEquals(SequenceUtil.reverseComplement("GTACGATT"), S(results.get(0).getAssemblySequence()));
	}
}
